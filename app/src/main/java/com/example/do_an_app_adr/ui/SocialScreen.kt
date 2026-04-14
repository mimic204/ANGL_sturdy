package com.example.do_an_app_adr.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.do_an_app_adr.model.Notification
import com.example.do_an_app_adr.model.Post
import com.example.do_an_app_adr.model.SocialComment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeed(
    viewModel: QuizViewModel,
    onShareCourse: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    var showCreatePost by remember { mutableStateOf(false) }
    var selectedPostForDetail by remember { mutableStateOf<Post?>(null) }
    
    val posts = viewModel.posts

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cộng đồng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showCreatePost = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Bạn đang nghĩ gì?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(posts) { post ->
                    PostItem(
                        post = post,
                        isCurrentUser = post.authorEmail == viewModel.userSettings.email,
                        currentUserId = viewModel.userSettings.email,
                        onDelete = { viewModel.deletePost(post.id) },
                        onToggleLike = { viewModel.toggleLikePost(post) },
                        onOpenDetail = { selectedPostForDetail = post },
                        onAvatarClick = { onNavigateToProfile(post.authorEmail) },
                        viewModel = viewModel
                    )
                    HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }

        if (showCreatePost) {
            CreatePostDialog(
                viewModel = viewModel,
                onDismiss = { showCreatePost = false }
            )
        }

        selectedPostForDetail?.let { post ->
            PostDetailDialog(
                post = post,
                viewModel = viewModel,
                onDismiss = { selectedPostForDetail = null },
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    isCurrentUser: Boolean,
    currentUserId: String,
    onDelete: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenDetail: () -> Unit,
    onAvatarClick: () -> Unit,
    viewModel: QuizViewModel
) {
    val topComment = post.comments.maxByOrNull { it.likes }
    val isLiked = currentUserId in post.likedBy

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.authorProfilePic ?: viewModel.getRandomAvatar(post.authorEmail),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray).clickable { onAvatarClick() },
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.clickable { onAvatarClick() }) {
                    Text(post.authorName, fontWeight = FontWeight.Bold)
                    Text(
                        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(post.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            if (isCurrentUser) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Xóa bài viết", color = Color.Red) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(post.content)

        post.sharedCourse?.let { course ->
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                onClick = { viewModel.saveSharedCourse(course) }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(course.title, fontWeight = FontWeight.Bold)
                        Text("${course.questions.size} câu hỏi - Nhấn để học", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        post.imageUrl?.let { url ->
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onToggleLike) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${post.likedBy.size}", fontSize = 14.sp)
                }
            }
            IconButton(onClick = onOpenDetail) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("${post.comments.size}", fontSize = 14.sp)
                }
            }
        }

        if (topComment != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onOpenDetail() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = topComment.authorName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = topComment.content,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailDialog(
    post: Post,
    viewModel: QuizViewModel,
    onDismiss: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    var commentToEdit by remember { mutableStateOf<SocialComment?>(null) }
    
    // Luôn lấy dữ liệu mới nhất từ viewModel
    val currentPost = viewModel.posts.find { it.id == post.id } ?: post

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bình luận") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        PostItem(
                            post = currentPost,
                            isCurrentUser = currentPost.authorEmail == viewModel.userSettings.email,
                            currentUserId = viewModel.userSettings.email,
                            onDelete = { 
                                viewModel.deletePost(currentPost.id)
                                onDismiss()
                            },
                            onToggleLike = { viewModel.toggleLikePost(currentPost) },
                            onOpenDetail = {},
                            onAvatarClick = { 
                                onDismiss()
                                onNavigateToProfile(currentPost.authorEmail)
                            },
                            viewModel = viewModel
                        )
                        HorizontalDivider()
                    }
                    items(currentPost.comments) { comment ->
                        CommentItem(
                            comment = comment,
                            isCurrentUser = comment.authorEmail == viewModel.userSettings.email,
                            onLike = { viewModel.likeComment(currentPost.id, comment.id) },
                            onDelete = { viewModel.deleteComment(currentPost.id, comment.id) },
                            onEdit = { commentToEdit = comment },
                            onAvatarClick = {
                                onDismiss()
                                onNavigateToProfile(comment.authorEmail)
                            },
                            viewModel = viewModel
                        )
                    }
                }
                
                var commentText by remember { mutableStateOf("") }
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Viết bình luận...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(currentPost, commentText)
                                commentText = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (commentToEdit != null) {
        var editContent by remember { mutableStateOf(commentToEdit!!.content) }
        AlertDialog(
            onDismissRequest = { commentToEdit = null },
            title = { Text("Chỉnh sửa bình luận") },
            text = {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateComment(currentPost.id, commentToEdit!!.id, editContent)
                    commentToEdit = null
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { commentToEdit = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun NotificationDialog(
    notifications: List<Notification>,
    onDismiss: () -> Unit,
    onNotifClick: (Notification) -> Unit,
    viewModel: QuizViewModel
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Thông báo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                if (notifications.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có thông báo nào")
                    }
                } else {
                    LazyColumn {
                        items(notifications) { notif ->
                            NotificationItem(notif = notif, onClick = { onNotifClick(notif) }, viewModel = viewModel)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notif: Notification, onClick: () -> Unit, viewModel: QuizViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (notif.isRead) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when(notif.type) {
                    "like" -> Icons.Default.Favorite
                    "friend_request" -> Icons.Default.PersonAdd
                    else -> Icons.Default.Comment
                },
                contentDescription = null,
                tint = if (notif.type == "like") Color.Red else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = buildString {
                    append(notif.fromName)
                    append(when(notif.type) {
                        "like" -> " đã thích bài viết của bạn: "
                        "comment" -> " đã bình luận về bài viết: "
                        "friend_request" -> " đã gửi lời mời kết bạn."
                        else -> " thông báo mới."
                    })
                    if (notif.postContent.isNotBlank()) {
                        append("\"${notif.postContent}...\"")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )
            Text(
                SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(notif.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CommentItem(
    comment: SocialComment,
    isCurrentUser: Boolean,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onAvatarClick: () -> Unit,
    viewModel: QuizViewModel
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = viewModel.getRandomAvatar(comment.authorEmail),
            contentDescription = null,
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray).clickable { onAvatarClick() },
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onAvatarClick() })
            Text(comment.content, fontSize = 14.sp)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(comment.timestamp)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "${comment.likes} lượt thích",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLike) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", modifier = Modifier.size(16.dp))
            }
            if (isCurrentUser) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa") },
                            onClick = { 
                                onEdit()
                                showMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Xóa", color = Color.Red) },
                            onClick = { 
                                onDelete()
                                showMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    viewModel: QuizViewModel,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo bài viết mới") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Bạn đang nghĩ gì?") }
                )
                Spacer(Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Chọn ảnh")
                    }
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isUploading = true
                    if (selectedImageUri != null) {
                        viewModel.uploadImage(selectedImageUri!!, "posts/${UUID.randomUUID()}") { url ->
                            viewModel.createPost(content, null, url)
                            onDismiss()
                        }
                    } else {
                        viewModel.createPost(content)
                        onDismiss()
                    }
                },
                enabled = !isUploading && (content.isNotBlank() || selectedImageUri != null)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đăng")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
