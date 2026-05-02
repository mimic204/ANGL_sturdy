package com.example.do_an_app_adr.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.do_an_app_adr.BuildConfig
import com.example.do_an_app_adr.model.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val _courses = mutableStateListOf<Course>()
    val courses: List<Course> = _courses

    private val _posts = mutableStateListOf<Post>()
    val posts: List<Post> = _posts

    private val _sharedCourses = mutableStateListOf<SharedCourse>()
    val sharedCourses: List<SharedCourse> = _sharedCourses

    private val _chatMessages = mutableStateListOf<ChatMessage>()
    val chatMessages: List<ChatMessage> = _chatMessages

    private val _notifications = mutableStateListOf<Notification>()
    val notifications: List<Notification> = _notifications

    var userSettings by mutableStateOf(UserSettings())
        private set

    var currentPopupNotification by mutableStateOf<Notification?>(null)
        private set
        
    private val shownPopupIds = mutableSetOf<String>()

    var errorMessage by mutableStateOf<String?>(null)
    
    // State to track AI image processing
    var isProcessingAi by mutableStateOf(false)
        private set

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val coursesFile = File(application.filesDir, "courses.json")
    private val settingsFile = File(application.filesDir, "settings.json")

    private var postsListener: ListenerRegistration? = null
    private var sharedCoursesListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var notifsListener: ListenerRegistration? = null
    private var myProfileListener: ListenerRegistration? = null
    private val commentListeners = mutableMapOf<String, ListenerRegistration>()

    // Gemini Model for ASCII Conversion
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.1f
        }
    )

    init {
        loadCourses()
        loadLocalSettings()
        observePosts()
        observeSharedCourses()
        if (auth.currentUser != null) login(auth.currentUser?.email ?: "")
    }

    private fun isUserValid(): Boolean = auth.currentUser != null && userSettings.email.isNotBlank()
    fun clearError() { errorMessage = null }
    fun dismissPopup() { currentPopupNotification = null }

    private fun loadCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (coursesFile.exists()) {
                    val jsonString = coursesFile.readText()
                    val loadedCourses = json.decodeFromString<List<Course>>(jsonString)
                    withContext(Dispatchers.Main) {
                        _courses.clear(); _courses.addAll(loadedCourses)
                    }
                }
            } catch (e: Exception) { 
                Log.e("QuizViewModel", "Error loading courses", e)
            }
        }
    }

    private fun saveCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                coursesFile.writeText(json.encodeToString(_courses.toList()))
            } catch (e: Exception) { 
                Log.e("QuizViewModel", "Error saving courses", e)
            }
        }
    }

    fun deleteCourse(course: Course) { _courses.remove(course); saveCourses() }

    fun addCourseFromJson(title: String, jsonText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val cleanedJson = cleanJsonString(jsonText)
                val questions = json.decodeFromString<List<QuizQuestion>>(cleanedJson)
                val newCourse = Course(id = UUID.randomUUID().toString(), title = title, questions = questions)
                
                withContext(Dispatchers.Main) {
                    _courses.add(newCourse)
                    saveCourses()
                    uploadCourseToServer(newCourse)
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error parsing AI JSON", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    private fun cleanJsonString(jsonText: String): String {
        return when {
            jsonText.contains("```json") -> jsonText.substringAfter("```json").substringBeforeLast("```").trim()
            jsonText.contains("```") -> jsonText.substringAfter("```").substringBeforeLast("```").trim()
            else -> jsonText.trim()
        }
    }

    private fun uploadCourseToServer(course: Course) {
        if (!isUserValid()) return
        val shared = SharedCourse(
            id = course.id, title = course.title, questions = course.questions,
            authorEmail = userSettings.email, authorName = getDisplayName(),
            authorProfilePic = userSettings.profilePicture, timestamp = System.currentTimeMillis()
        )
        firestore.collection("shared_courses").document(shared.id).set(shared)
            .addOnFailureListener { Log.e("QuizViewModel", "Error uploading course", it) }
    }

    fun observeSharedCourses() {
        sharedCoursesListener?.remove()
        sharedCoursesListener = firestore.collection("shared_courses")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { it.toObject(SharedCourse::class.java) } ?: emptyList()
                val sorted = list.sortedWith(compareByDescending<SharedCourse> { it.authorEmail in userSettings.friends }
                    .thenByDescending { it.views }.thenByDescending { it.stars.size })
                _sharedCourses.clear(); _sharedCourses.addAll(sorted)
            }
    }

    fun starCourse(courseId: String) {
        if (!isUserValid()) return
        val ref = firestore.collection("shared_courses").document(courseId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val stars = (snapshot.get("stars") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val newStars = stars.toMutableList()
            if (userSettings.email in stars) newStars.remove(userSettings.email) else newStars.add(userSettings.email)
            transaction.update(ref, "stars", newStars)
        }
    }

    fun incrementView(courseId: String) {
        firestore.collection("shared_courses").document(courseId)
            .update("views", com.google.firebase.firestore.FieldValue.increment(1))
    }

    // GENERAL CHAT OBSERVATION: Get all messages involving the user
    fun observeMessages() {
        chatListener?.remove()
        val myEmail = userSettings.email
        if (myEmail.isBlank()) return
        
        chatListener = firestore.collection("chats")
            .where(Filter.or(
                Filter.equalTo("senderEmail", myEmail),
                Filter.equalTo("receiverEmail", myEmail)
            ))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("QuizViewModel", "Messages observation failed", e)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                _chatMessages.clear()
                _chatMessages.addAll(messages)
            }
    }

    // SPECIFIC CHAT OBSERVATION: Get messages between current user and a partner
    fun observeChat(partnerEmail: String) {
        chatListener?.remove()
        val myEmail = userSettings.email
        if (myEmail.isBlank()) return
        
        chatListener = firestore.collection("chats")
            .where(Filter.or(
                Filter.and(Filter.equalTo("senderEmail", myEmail), Filter.equalTo("receiverEmail", partnerEmail)),
                Filter.and(Filter.equalTo("senderEmail", partnerEmail), Filter.equalTo("receiverEmail", myEmail))
            ))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("QuizViewModel", "Chat observation failed", e)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                _chatMessages.clear()
                _chatMessages.addAll(messages)
            }
    }

    fun sendMessage(receiverEmail: String, content: String) {
        if (!isUserValid() || content.isBlank()) return
        val msg = ChatMessage(id = UUID.randomUUID().toString(), senderEmail = userSettings.email, receiverEmail = receiverEmail, content = content, timestamp = System.currentTimeMillis())
        firestore.collection("chats").document(msg.id).set(msg)
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    login(email)
                    onResult(true, "Đăng nhập thành công")
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Lỗi đăng nhập")
                }
            }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Đăng ký thành công")
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Lỗi đăng ký")
                }
            }
    }

    fun login(email: String) {
        userSettings = userSettings.copy(email = email, isLoggedIn = true)
        saveLocalSettings(); observePosts(); observeSharedCourses(); observeNotifications(); observeMyProfile(); observeMessages()
        getUserProfile(email) { remote ->
            if (remote != null) {
                userSettings = userSettings.copy(
                    name = remote.name, 
                    profilePicture = remote.profilePicture, 
                    bio = remote.bio, 
                    friends = remote.friends, 
                    friendRequests = remote.friendRequests,
                    lastCourseId = remote.lastCourseId,
                    lastCourseTitle = remote.lastCourseTitle,
                    courseProgress = remote.courseProgress
                )
                saveLocalSettings()
                // TỰ ĐỘNG KIỂM TRA VÀ ĐỒNG BỘ TÊN KHI ĐĂNG NHẬP
                updateUserInfoInFirestore(newName = remote.name, newPic = remote.profilePicture)
            } else saveProfileToDatabase()
        }
    }

    private fun observeMyProfile() {
        myProfileListener?.remove()
        myProfileListener = firestore.collection("users").document(userSettings.email)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { doc ->
                    if (doc.exists()) {
                        val remoteName = doc.getString("name")
                        val remotePic = doc.getString("profilePicture")
                        
                        userSettings = userSettings.copy(
                            name = remoteName,
                            profilePicture = remotePic,
                            bio = doc.getString("bio"),
                            friends = (doc.get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            friendRequests = (doc.get("friendRequests") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            lastCourseId = doc.getString("lastCourseId"),
                            lastCourseTitle = doc.getString("lastCourseTitle"),
                            courseProgress = (doc.get("courseProgress") as? Map<*, *>)?.filterKeys { it is String }?.filterValues { it is Number }?.map { it.key as String to (it.value as Number).toFloat() }?.toMap() ?: emptyMap()
                        )
                        saveLocalSettings()
                    }
                }
            }
    }

    fun logout() {
        auth.signOut(); postsListener?.remove(); sharedCoursesListener?.remove(); chatListener?.remove()
        notifsListener?.remove(); myProfileListener?.remove()
        commentListeners.values.forEach { it.remove() }
        commentListeners.clear()
        _posts.clear(); _sharedCourses.clear(); _notifications.clear(); _chatMessages.clear()
        userSettings = UserSettings(); saveLocalSettings()
    }

    fun observePosts() {
        postsListener?.remove()
        postsListener = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("QuizViewModel", "Error observing posts", e)
                    return@addSnapshotListener
                }
                val updatedPosts = snapshot?.documents?.mapNotNull { doc ->
                    val post = doc.toObject(Post::class.java)?.copy(id = doc.id)
                    val existingPost = _posts.find { it.id == doc.id }
                    post?.copy(comments = existingPost?.comments ?: emptyList())
                } ?: emptyList()
                
                _posts.clear(); _posts.addAll(updatedPosts)
                updatedPosts.forEach { fetchCommentsForPost(it.id) }
            }
    }

    private fun fetchCommentsForPost(postId: String) {
        if (commentListeners.containsKey(postId)) return
        commentListeners[postId] = firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SocialComment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                val index = _posts.indexOfFirst { it.id == postId }
                if (index != -1) _posts[index] = _posts[index].copy(comments = comments)
            }
    }

    fun addComment(post: Post, content: String, imageUri: Uri? = null, onComplete: () -> Unit = {}) {
        if (!isUserValid()) return
        
        viewModelScope.launch {
            isProcessingAi = true
            var finalContent = content
            try {
                if (imageUri != null) {
                    val ascii = convertImageToAscii(imageUri)
                    if (ascii != null) {
                        finalContent += "\n[Hình ảnh ASCII]\n$ascii"
                    }
                }
                
                val commentData = hashMapOf(
                    "authorName" to getDisplayName(),
                    "authorEmail" to userSettings.email,
                    "content" to finalContent,
                    "timestamp" to System.currentTimeMillis(),
                    "likes" to 0
                )
                firestore.collection("posts").document(post.id).collection("comments").add(commentData)
                    .addOnCompleteListener { 
                        isProcessingAi = false
                        onComplete()
                    }
                
                if (post.authorEmail != userSettings.email) {
                    val notif = Notification(
                        id = UUID.randomUUID().toString(),
                        toEmail = post.authorEmail,
                        fromEmail = userSettings.email,
                        fromName = getDisplayName(),
                        type = "comment",
                        postId = post.id,
                        postContent = if (finalContent.length > 20) finalContent.take(20) + "..." else finalContent,
                        timestamp = System.currentTimeMillis(),
                        isRead = false
                    )
                    firestore.collection("notifications").document(notif.id).set(notif)
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error adding comment", e)
                isProcessingAi = false
                onComplete()
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) { firestore.collection("posts").document(postId).collection("comments").document(commentId).delete() }
    fun updateComment(postId: String, commentId: String, newContent: String) { firestore.collection("posts").document(postId).collection("comments").document(commentId).update("content", newContent) }

    fun saveProfileToDatabase() {
        if (!isUserValid()) return
        firestore.collection("users").document(userSettings.email).set(hashMapOf(
            "name" to userSettings.name, 
            "profilePicture" to userSettings.profilePicture, 
            "bio" to userSettings.bio, 
            "friends" to userSettings.friends, 
            "friendRequests" to userSettings.friendRequests,
            "lastCourseId" to userSettings.lastCourseId,
            "lastCourseTitle" to userSettings.lastCourseTitle,
            "courseProgress" to userSettings.courseProgress
        ))
    }

    fun getUserProfile(email: String, onResult: (UserSettings?) -> Unit) {
        firestore.collection("users").document(email).get().addOnSuccessListener { doc ->
            if (doc.exists()) onResult(doc.toObject(UserSettings::class.java)?.copy(email = email))
            else onResult(null)
        }.addOnFailureListener { onResult(null) }
    }

    fun updateUserName(newName: String) { 
        userSettings = userSettings.copy(name = newName)
        saveProfileToDatabase()
        updateUserInfoInFirestore(newName = newName) 
    }
    
    fun updateBio(newBio: String) { userSettings = userSettings.copy(bio = newBio); saveProfileToDatabase() }
    
    fun updateProfilePicture(picUrl: String) { 
        userSettings = userSettings.copy(profilePicture = picUrl)
        saveProfileToDatabase()
        updateUserInfoInFirestore(newPic = picUrl) 
    }

    /**
     * KIỂM TRA VÀ ĐỒNG BỘ TÊN/ẢNH TRÊN TOÀN HỆ THỐNG
     */
    private fun updateUserInfoInFirestore(newName: String? = null, newPic: String? = null) {
        if (!isUserValid()) return
        val email = userSettings.email

        // 1. Đồng bộ trong bài đăng (posts)
        firestore.collection("posts").whereEqualTo("authorEmail", email).get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            var count = 0
            for (doc in snapshot.documents) {
                val updates = mutableMapOf<String, Any?>()
                if (newName != null && doc.getString("authorName") != newName) updates["authorName"] = newName
                if (newPic != null && doc.getString("authorProfilePic") != newPic) updates["authorProfilePic"] = newPic
                
                if (updates.isNotEmpty()) {
                    batch.update(doc.reference, updates)
                    count++
                }
            }
            if (count > 0) batch.commit()
        }

        // 2. Đồng bộ trong khóa học đã chia sẻ (shared_courses)
        firestore.collection("shared_courses").whereEqualTo("authorEmail", email).get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            var count = 0
            for (doc in snapshot.documents) {
                val updates = mutableMapOf<String, Any?>()
                if (newName != null && doc.getString("authorName") != newName) updates["authorName"] = newName
                if (newPic != null && doc.getString("authorProfilePic") != newPic) updates["authorProfilePic"] = newPic
                
                if (updates.isNotEmpty()) {
                    batch.update(doc.reference, updates)
                    count++
                }
            }
            if (count > 0) batch.commit()
        }

        // 3. Đồng bộ trong tất cả bình luận (comments)
        if (newName != null) {
            firestore.collectionGroup("comments").whereEqualTo("authorEmail", email).get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                var count = 0
                for (doc in snapshot.documents) {
                    if (doc.getString("authorName") != newName) {
                        batch.update(doc.reference, "authorName", newName)
                        count++
                    }
                }
                if (count > 0) batch.commit()
            }
        }

        // 4. Đồng bộ trong thông báo đã gửi (notifications)
        if (newName != null) {
            firestore.collection("notifications").whereEqualTo("fromEmail", email).get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                var count = 0
                for (doc in snapshot.documents) {
                    if (doc.getString("fromName") != newName) {
                        batch.update(doc.reference, "fromName", newName)
                        count++
                    }
                }
                if (count > 0) batch.commit()
            }
        }
    }

    // New Image to ASCII Conversion using Gemini
    private suspend fun convertImageToAscii(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
            
            // Resize for AI processing efficiency
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
            
            val inputContent = content {
                image(scaledBitmap)
                text("Hãy chuyển đổi hình ảnh này thành nghệ thuật ASCII (ASCII Art). Chỉ trả về chuỗi ký tự ASCII, không kèm theo bất kỳ văn bản giải thích nào khác. Sử dụng các ký tự như @, #, *, +, -, ., ' ' để tạo độ đậm nhạt. Giữ kích thước khoảng 40x40 ký tự.")
            }
            
            val response = generativeModel.generateContent(inputContent)
            var result = response.text ?: ""
            
            // Clean up Markdown if AI includes it
            result = result.replace("```ascii", "").replace("```text", "").replace("```", "").trim()
            if (result.isEmpty()) null else result
        } catch (e: Exception) {
            Log.e("QuizViewModel", "ASCII Conversion failed", e)
            null
        }
    }

    fun sendFriendRequest(targetEmail: String) {
        if (!isUserValid() || targetEmail == userSettings.email) return
        val targetRef = firestore.collection("users").document(targetEmail)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(targetRef)
            val requests = (snapshot.get("friendRequests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (userSettings.email !in requests) {
                val newRequests = requests.toMutableList(); newRequests.add(userSettings.email)
                transaction.update(targetRef, "friendRequests", newRequests)
                
                val notif = Notification(
                    id = UUID.randomUUID().toString(),
                    toEmail = targetEmail,
                    fromEmail = userSettings.email,
                    fromName = getDisplayName(),
                    type = "friend_request",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                transaction.set(firestore.collection("notifications").document(notif.id), notif)
            }
        }
    }

    fun acceptFriendRequest(senderEmail: String) {
        if (!isUserValid()) return
        val myRef = firestore.collection("users").document(userSettings.email)
        val senderRef = firestore.collection("users").document(senderEmail)
        firestore.runTransaction { transaction ->
            val mySnap = transaction.get(myRef); val senderSnap = transaction.get(senderRef)
            val myFriends = (mySnap.get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val myRequests = (mySnap.get("friendRequests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val senderFriends = (senderSnap.get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            val newMyFriends = myFriends.toMutableList().apply { if (senderEmail !in this) add(senderEmail) }
            val newMyRequests = myRequests.toMutableList().apply { remove(senderEmail) }
            val newSenderFriends = senderFriends.toMutableList().apply { if (userSettings.email !in this) add(userSettings.email) }
            
            transaction.update(myRef, "friends", newMyFriends)
            transaction.update(myRef, "friendRequests", newMyRequests)
            transaction.update(senderRef, "friends", newSenderFriends)
            
            firestore.collection("notifications")
                .whereEqualTo("toEmail", userSettings.email)
                .whereEqualTo("fromEmail", senderEmail)
                .whereEqualTo("type", "friend_request")
                .get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { it.reference.delete() }
                }
        }
    }

    fun rejectFriendRequest(senderEmail: String) {
        if (!isUserValid()) return
        val myRef = firestore.collection("users").document(userSettings.email)
        firestore.runTransaction { transaction ->
            val mySnap = transaction.get(myRef)
            val myRequests = (mySnap.get("friendRequests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val newMyRequests = myRequests.toMutableList().apply { remove(senderEmail) }
            transaction.update(myRef, "friendRequests", newMyRequests)
            
            firestore.collection("notifications")
                .whereEqualTo("toEmail", userSettings.email)
                .whereEqualTo("fromEmail", senderEmail)
                .whereEqualTo("type", "friend_request")
                .get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { it.reference.delete() }
                }
        }
    }

    private fun observeNotifications() {
        if (userSettings.email.isBlank()) return
        notifsListener?.remove()
        notifsListener = firestore.collection("notifications").whereEqualTo("toEmail", userSettings.email)
            .orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("QuizViewModel", "Error observing notifications", e)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Notification::class.java) } ?: emptyList()
                
                // Hiển thị popup cho thông báo mới nhất CHƯA TỪNG HIỆN
                val latestNotif = list.firstOrNull { !it.isRead }
                if (latestNotif != null && !shownPopupIds.contains(latestNotif.id)) {
                    currentPopupNotification = latestNotif
                    shownPopupIds.add(latestNotif.id)
                }
                
                _notifications.clear(); _notifications.addAll(list)
            }
    }

    fun saveSharedCourse(course: Course) { if (_courses.none { it.id == course.id }) { _courses.add(course); saveCourses() } }
    
    fun createPost(content: String, sharedCourse: Course? = null, imageUri: Uri? = null, onComplete: () -> Unit = {}) {
        if (!isUserValid()) return
        
        viewModelScope.launch {
            isProcessingAi = true
            var finalContent = content
            try {
                if (imageUri != null) {
                    val ascii = convertImageToAscii(imageUri)
                    if (ascii != null) {
                        finalContent += "\n[Hình ảnh ASCII]\n$ascii"
                    }
                }

                // Fallback if content is completely empty
                if (finalContent.isBlank() && sharedCourse == null) {
                    finalContent = "Bài viết mới"
                }

                val post = Post(
                    id = "", authorName = getDisplayName(), authorEmail = userSettings.email,
                    authorProfilePic = userSettings.profilePicture, content = finalContent,
                    timestamp = System.currentTimeMillis(), imageUrl = null, sharedCourse = sharedCourse
                )
                firestore.collection("posts").add(post)
                    .addOnCompleteListener { 
                        isProcessingAi = false
                        onComplete()
                    }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error creating post", e)
                isProcessingAi = false
                onComplete()
            }
        }
    }

    fun toggleLikePost(post: Post) {
        if (!isUserValid()) return
        val ref = firestore.collection("posts").document(post.id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val newLikedBy = likedBy.toMutableList()
            val wasLiked = userSettings.email in likedBy
            if (wasLiked) newLikedBy.remove(userSettings.email) else newLikedBy.add(userSettings.email)
            transaction.update(ref, "likedBy", newLikedBy)
            
            if (!wasLiked && post.authorEmail != userSettings.email) {
                val notif = Notification(
                    id = UUID.randomUUID().toString(),
                    toEmail = post.authorEmail,
                    fromEmail = userSettings.email,
                    fromName = getDisplayName(),
                    type = "like",
                    postId = post.id,
                    postContent = if (post.content.length > 20) post.content.take(20) + "..." else post.content,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                firestore.collection("notifications").document(notif.id).set(notif)
            }
        }
    }
    fun deletePost(postId: String) { firestore.collection("posts").document(postId).delete() }
    fun markNotifRead(notifId: String) { if (isUserValid()) firestore.collection("notifications").document(notifId).update("isRead", true) }
    fun likeComment(postId: String, commentId: String) {
        if (!isUserValid()) return
        val ref = firestore.collection("posts").document(postId).collection("comments").document(commentId)
        firestore.runTransaction { transaction ->
            val likes = transaction.get(ref).getLong("likes") ?: 0L
            transaction.update(ref, "likes", likes + 1)
        }
    }
    fun getDisplayName(): String = userSettings.name ?: "User"
    fun getRandomAvatar(seed: String?): String = "https://api.dicebear.com/7.x/adventurer/png?seed=${seed ?: UUID.randomUUID()}&backgroundColor=b6e3f4,c0aede,d1d4f9"
    private fun loadLocalSettings() { viewModelScope.launch(Dispatchers.IO) { try { if (settingsFile.exists()) { val sets = json.decodeFromString<UserSettings>(settingsFile.readText()); withContext(Dispatchers.Main) { userSettings = sets } } } catch (e: Exception) {} } }
    private fun saveLocalSettings() { viewModelScope.launch(Dispatchers.IO) { try { settingsFile.writeText(json.encodeToString(userSettings)) } catch (e: Exception) {} } }

    fun updateCourseProgress(courseId: String, title: String, progress: Float) {
        val newProgressMap = userSettings.courseProgress.toMutableMap()
        newProgressMap[courseId] = progress
        userSettings = userSettings.copy(
            lastCourseId = courseId,
            lastCourseTitle = title,
            courseProgress = newProgressMap
        )
        saveLocalSettings()
        saveProfileToDatabase()
    }
}
