package com.example.do_an_app_adr.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.do_an_app_adr.model.Course
import com.example.do_an_app_adr.model.SharedCourse
import com.example.do_an_app_adr.model.UserSettings
import com.example.do_an_app_adr.ui.theme.Do_an_app_adrTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class AnglDestination(val label: String, val icon: ImageVector) {
    HOME("Trang chủ", Icons.Default.Home),
    LEARN("Học tập", Icons.Default.PlayArrow),
    AI_GEN("Tạo AI", Icons.Default.AutoAwesome),
    SOCIAL("Cộng đồng", Icons.Default.Public),
    PROFILE("Cá nhân", Icons.Default.Person),
}

@Composable
fun MainTopBar(viewModel: QuizViewModel, isRefreshing: Boolean, onShowNotifications: () -> Unit) {
    val unreadCount = viewModel.notifications.count { !it.isRead }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = { if (unreadCount > 0) Badge { Text(unreadCount.toString()) } },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            IconButton(onClick = onShowNotifications) {
                Icon(Icons.Default.Notifications, contentDescription = "Thông báo")
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        ArtisticLogo(isRefreshing = isRefreshing)
        
        Spacer(Modifier.weight(1f))
        
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
fun ArtisticLogo(modifier: Modifier = Modifier, isRefreshing: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = "A", style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = MaterialTheme.colorScheme.primary),
            modifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier)
    }
}

@Composable
fun PopupNotification(viewModel: QuizViewModel) {
    val notification = viewModel.currentPopupNotification
    AnimatedVisibility(visible = notification != null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
        if (notification != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (notification.type == "friend_request") Icons.Default.PersonAdd else Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notification.fromName, fontWeight = FontWeight.Bold)
                        Text(when(notification.type) {
                            "like" -> "đã thích bài viết của bạn"
                            "comment" -> "đã bình luận bài viết của bạn"
                            "friend_request" -> "đã gửi lời mời kết bạn"
                            else -> "thông báo mới"
                        })
                    }
                    IconButton(onClick = { viewModel.dismissPopup() }) { Icon(Icons.Default.Close, contentDescription = null) }
                }
            }
            LaunchedEffect(notification) { delay(4000); viewModel.dismissPopup() }
        }
    }
}

@Composable
fun ErrorSnackbar(viewModel: QuizViewModel) {
    val error = viewModel.errorMessage
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) { if (error != null) { snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Long, withDismissAction = true); viewModel.clearError() } }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(snackbarData = data, containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnglApp(viewModel: QuizViewModel = viewModel()) {
    val settings = viewModel.userSettings
    var currentDestination by rememberSaveable { mutableStateOf(AnglDestination.HOME) }
    var activeQuizCourse by remember { mutableStateOf<Course?>(null) }
    var viewingProfileEmail by remember { mutableStateOf<String?>(null) }
    var chatPartnerEmail by remember { mutableStateOf<String?>(null) }
    var showAllNotifications by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Do_an_app_adrTheme(darkTheme = settings.isDarkMode) {
        if (!settings.isLoggedIn) {
            LoginScreen(onLoginSuccess = { email -> viewModel.login(email) })
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MainTopBar(viewModel, isRefreshing, onShowNotifications = { showAllNotifications = true })
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeQuizCourse != null) {
                            QuizScreen(course = activeQuizCourse!!, onExit = { activeQuizCourse = null })
                        } else if (chatPartnerEmail != null) {
                            ChatScreen(viewModel = viewModel, partnerEmail = chatPartnerEmail!!, onBack = { chatPartnerEmail = null })
                        } else if (viewingProfileEmail != null) {
                            ProfileScreen(viewModel = viewModel, targetEmail = viewingProfileEmail!!, isOwnProfile = viewingProfileEmail == settings.email,
                                onBack = { viewingProfileEmail = null }, onLogout = { viewModel.logout() }, onChat = { chatPartnerEmail = it })
                        } else {
                            NavigationSuiteScaffold(
                                navigationSuiteItems = {
                                    AnglDestination.entries.forEach { destination ->
                                        item(icon = { Icon(destination.icon, contentDescription = null) }, label = { Text(destination.label) },
                                            selected = destination == currentDestination, onClick = { currentDestination = destination })
                                    }
                                }
                            ) {
                                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { scope.launch { isRefreshing = true; viewModel.observePosts(); viewModel.observeSharedCourses(); delay(1000); isRefreshing = false } }, indicator = {}) {
                                        when (currentDestination) {
                                            AnglDestination.HOME -> HomeScreen(viewModel, onStartCourse = { activeQuizCourse = it })
                                            AnglDestination.LEARN -> LearnScreen(viewModel, { activeQuizCourse = it }, { viewModel.deleteCourse(it) })
                                            AnglDestination.AI_GEN -> AiGenScreen({ currentDestination = AnglDestination.LEARN }, viewModel)
                                            AnglDestination.SOCIAL -> SocialFeed(viewModel, { /* Logic */ }, onNavigateToProfile = { viewingProfileEmail = it })
                                            AnglDestination.PROFILE -> ProfileScreen(viewModel = viewModel, targetEmail = settings.email, isOwnProfile = true, onBack = { currentDestination = AnglDestination.HOME }, onLogout = { viewModel.logout() })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (showAllNotifications) {
                    NotificationDialog(
                        notifications = viewModel.notifications,
                        onDismiss = { showAllNotifications = false },
                        onNotifClick = { notif ->
                            viewModel.markNotifRead(notif.id)
                            if (notif.postId.isNotBlank()) {
                                currentDestination = AnglDestination.SOCIAL
                                showAllNotifications = false
                            }
                        },
                        viewModel = viewModel
                    )
                }
                
                PopupNotification(viewModel)
                ErrorSnackbar(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: QuizViewModel, onStartCourse: (Course) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Chào bạn, ${viewModel.getDisplayName()}!") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                Text("Tiến độ hôm nay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val lastCourse = viewModel.courses.lastOrNull()
                        if (lastCourse != null) {
                            Text("Đang học: ${lastCourse.title}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.fillMaxWidth(), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                        } else { Text("Chưa có bài học nào dở dang") }
                    }
                }
            }

            item {
                Text("Bài học cộng đồng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Ưu tiên từ bạn bè và thịnh hành nhất", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            items(viewModel.sharedCourses) { shared ->
                SharedCourseItem(shared = shared, onStart = {
                    viewModel.incrementView(shared.id)
                    onStartCourse(Course(shared.id, shared.title, shared.questions))
                }, onStar = { viewModel.starCourse(shared.id) }, currentUserId = viewModel.userSettings.email)
            }
        }
    }
}

@Composable
fun SharedCourseItem(shared: SharedCourse, onStart: () -> Unit, onStar: () -> Unit, currentUserId: String) {
    val isStarred = currentUserId in shared.stars
    Card(modifier = Modifier.fillMaxWidth(), onClick = onStart) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = shared.authorProfilePic, contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(shared.authorName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Người tạo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onStar) {
                    Icon(if (isStarred) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = if (isStarred) Color(0xFFFFD700) else Color.Gray)
                }
                Text("${shared.stars.size}", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(shared.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${shared.questions.size} câu hỏi • ${shared.views} lượt học", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: QuizViewModel, partnerEmail: String, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var partnerName by remember { mutableStateOf(partnerEmail) }
    
    LaunchedEffect(partnerEmail) { 
        viewModel.observeChat(partnerEmail)
        viewModel.getUserProfile(partnerEmail) { partnerName = it?.name ?: partnerEmail }
    }
    
    LaunchedEffect(viewModel.chatMessages.size) {
        if (viewModel.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(partnerName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(viewModel.chatMessages) { msg ->
                    val isMe = msg.senderEmail == viewModel.userSettings.email
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
                        Surface(color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                            Text(msg.content, modifier = Modifier.padding(12.dp), color = if (isMe) Color.White else Color.Black)
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            Surface(tonalElevation = 2.dp) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = messageText, onValueChange = { messageText = it }, modifier = Modifier.weight(1f), placeholder = { Text("Nhắn tin...") }, shape = CircleShape)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (messageText.isNotBlank()) { viewModel.sendMessage(partnerEmail, messageText); messageText = "" } }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(viewModel: QuizViewModel, onCourseClick: (Course) -> Unit, onDeleteCourse: (Course) -> Unit) {
    var selectedCourseToShare by remember { mutableStateOf<Course?>(null) }
    var showShareOptions by remember { mutableStateOf(false) }
    var showFriendSelector by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Khóa học của bạn") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.courses) { course ->
                Card(onClick = { onCourseClick(course) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(course.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${course.questions.size} câu hỏi") },
                        trailingContent = { 
                            Row {
                                IconButton(onClick = { 
                                    selectedCourseToShare = course
                                    showShareOptions = true
                                }) { Icon(Icons.Default.Share, "Chia sẻ", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { onDeleteCourse(course) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showShareOptions && selectedCourseToShare != null) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("Chia sẻ bài học") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Đăng lên cộng đồng") },
                        leadingContent = { Icon(Icons.Default.Public, null) },
                        modifier = Modifier.clickable {
                            viewModel.createPost("Mình vừa tạo bài học mới: ${selectedCourseToShare!!.title}. Mọi người cùng vào học nhé!", sharedCourse = selectedCourseToShare)
                            showShareOptions = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Chia sẻ với bạn bè") },
                        leadingContent = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.clickable {
                            showShareOptions = false
                            showFriendSelector = true
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (showFriendSelector && selectedCourseToShare != null) {
        var searchQuery by remember { mutableStateOf("") }
        // Mô phỏng sắp xếp theo tương tác: Ở đây ta sắp xếp theo tên, có thể cải tiến sau
        val filteredFriends = viewModel.userSettings.friends.filter { it.contains(searchQuery, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showFriendSelector = false },
            title = { Text("Chọn bạn bè") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tìm kiếm...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filteredFriends) { friendEmail ->
                            var friendName by remember { mutableStateOf(friendEmail) }
                            LaunchedEffect(friendEmail) { viewModel.getUserProfile(friendEmail) { friendName = it?.name ?: friendEmail } }
                            ListItem(
                                headlineContent = { Text(friendName) },
                                supportingContent = { Text(friendEmail) },
                                leadingContent = { AsyncImage(model = viewModel.getRandomAvatar(friendEmail), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape)) },
                                modifier = Modifier.clickable {
                                    viewModel.sendMessage(friendEmail, "Mình muốn chia sẻ bài học này với bạn: ${selectedCourseToShare!!.title}. Hãy vào mục 'Học tập' để xem nhé!")
                                    showFriendSelector = false
                                    viewModel.errorMessage = "Đã chia sẻ với $friendName"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFriendSelector = false }) { Text("Đóng") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: QuizViewModel, targetEmail: String, isOwnProfile: Boolean, onBack: () -> Unit, onLogout: () -> Unit, onChat: (String) -> Unit = {}) {
    var profileData by remember { mutableStateOf<UserSettings?>(if (isOwnProfile) viewModel.userSettings else null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFriendsList by remember { mutableStateOf(false) }
    var showFriendRequests by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    val myEmail = viewModel.userSettings.email
    val isFriend = targetEmail in viewModel.userSettings.friends
    val hasReceivedFromThem = targetEmail in viewModel.userSettings.friendRequests
    val hasSentToThem = profileData?.friendRequests?.contains(myEmail) == true
    
    LaunchedEffect(targetEmail, viewModel.userSettings) { 
        if (!isOwnProfile) viewModel.getUserProfile(targetEmail) { profileData = it } 
        else profileData = viewModel.userSettings 
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Hồ sơ") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (isOwnProfile) {
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                            DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Chỉnh sửa hồ sơ") },
                                    onClick = { showEditDialog = true; showOptionsMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Lời mời kết bạn") },
                                    onClick = { showFriendRequests = true; showOptionsMenu = false },
                                    leadingIcon = { Icon(Icons.Default.PersonAdd, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Đăng xuất", color = Color.Red) },
                                    onClick = { onLogout(); showOptionsMenu = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) }
                                )
                            }
                        }
                    }
                }
            ) 
        }
    ) { padding ->
        profileData?.let { profile ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = profile.profilePicture ?: viewModel.getRandomAvatar(targetEmail), contentDescription = null, modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.LightGray), contentScale = ContentScale.Crop)
                        Spacer(Modifier.height(16.dp))
                        Text(profile.name ?: "Chưa đặt tên", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(profile.bio ?: "Chưa có giới thiệu", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        
                        if (isOwnProfile) {
                            Button(
                                onClick = { showFriendsList = true },
                                modifier = Modifier.fillMaxWidth(0.8f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Group, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Bạn bè (${profile.friends.size})")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onChat(targetEmail) }) { Icon(Icons.Default.Chat, null); Spacer(Modifier.width(8.dp)); Text("Nhắn tin") }
                                
                                when {
                                    isFriend -> {
                                        OutlinedButton(onClick = { }, enabled = false) { 
                                            Icon(Icons.Default.Check, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Bạn bè") 
                                        }
                                    }
                                    hasReceivedFromThem -> {
                                        Button(onClick = { viewModel.acceptFriendRequest(targetEmail) }) { Text("Chấp nhận kết bạn") }
                                    }
                                    hasSentToThem -> {
                                        OutlinedButton(onClick = { }, enabled = false) { Text("Đã gửi lời mời") }
                                    }
                                    else -> {
                                        Button(
                                            onClick = { viewModel.sendFriendRequest(targetEmail) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) { Text("Kết bạn") }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    Text("Bài viết", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(viewModel.posts.filter { it.authorEmail == targetEmail }) { post ->
                    PostItem(post = post, isCurrentUser = viewModel.userSettings.email == post.authorEmail, currentUserId = viewModel.userSettings.email, onDelete = { viewModel.deletePost(post.id) }, onToggleLike = { viewModel.toggleLikePost(post) }, onOpenDetail = { }, onAvatarClick = { }, viewModel = viewModel)
                }
            }
        }
    }

    if (showFriendsList) {
        AlertDialog(
            onDismissRequest = { showFriendsList = false },
            title = { Text("Danh sách bạn bè") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (viewModel.userSettings.friends.isEmpty()) {
                        item { Text("Bạn chưa có người bạn nào", modifier = Modifier.padding(16.dp)) }
                    }
                    items(viewModel.userSettings.friends) { friendEmail ->
                        var friendName by remember { mutableStateOf(friendEmail) }
                        LaunchedEffect(friendEmail) { viewModel.getUserProfile(friendEmail) { friendName = it?.name ?: friendEmail } }
                        ListItem(
                            headlineContent = { Text(friendName) },
                            supportingContent = { Text(friendEmail) },
                            leadingContent = { AsyncImage(model = viewModel.getRandomAvatar(friendEmail), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape)) },
                            modifier = Modifier.clickable { 
                                showFriendsList = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFriendsList = false }) { Text("Đóng") } }
        )
    }

    if (showFriendRequests) {
        AlertDialog(
            onDismissRequest = { showFriendRequests = false },
            title = { Text("Lời mời kết bạn") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (viewModel.userSettings.friendRequests.isEmpty()) {
                        item { Text("Không có lời mời nào", modifier = Modifier.padding(16.dp)) }
                    }
                    items(viewModel.userSettings.friendRequests) { senderEmail ->
                        var senderName by remember { mutableStateOf(senderEmail) }
                        LaunchedEffect(senderEmail) { viewModel.getUserProfile(senderEmail) { senderName = it?.name ?: senderEmail } }
                        ListItem(
                            headlineContent = { Text(senderName) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { viewModel.acceptFriendRequest(senderEmail) }) { Icon(Icons.Default.Check, null, tint = Color.Green) }
                                    IconButton(onClick = { viewModel.rejectFriendRequest(senderEmail) }) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFriendRequests = false }) { Text("Xong") } }
        )
    }

    if (showEditDialog) {
        var name by remember { mutableStateOf(viewModel.userSettings.name ?: "") }
        var bio by remember { mutableStateOf(viewModel.userSettings.bio ?: "") }
        AlertDialog(onDismissRequest = { showEditDialog = false }, title = { Text("Chỉnh sửa hồ sơ") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên hiển thị") }); OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Tiểu sử") }) } },
            confirmButton = { TextButton(onClick = { viewModel.updateUserName(name); viewModel.updateBio(bio); showEditDialog = false }) { Text("Lưu") } })
    }
}
