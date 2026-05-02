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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.do_an_app_adr.model.*
import com.example.do_an_app_adr.ui.theme.Do_an_app_adrTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AnglDestination(val label: String, val icon: ImageVector) {
    HOME("Trang chủ", Icons.Default.Home),
    LEARN("Học tập", Icons.Default.MenuBook),
    AI_GEN("Sáng tạo", Icons.Default.AutoAwesome),
    SOCIAL("Cộng đồng", Icons.Default.Public),
    PROFILE("Hồ sơ", Icons.Default.Person)
}

@Composable
fun ArtisticLogo(modifier: Modifier = Modifier, isRefreshing: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "A", 
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = FontFamily.Cursive, 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 32.sp, 
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(viewModel: QuizViewModel, isRefreshing: Boolean, onShowNotifications: () -> Unit) {
    val unreadCount = viewModel.notifications.count { !it.isRead }
    
    CenterAlignedTopAppBar(
        title = { 
            ArtisticLogo(isRefreshing = isRefreshing)
        },
        actions = {
            BadgedBox(
                badge = { if (unreadCount > 0) Badge { Text(unreadCount.toString()) } },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                IconButton(onClick = onShowNotifications) {
                    Icon(Icons.Default.Notifications, contentDescription = "Thông báo")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        )
    )
}

@Composable
fun PopupNotification(viewModel: QuizViewModel) {
    val notification = viewModel.currentPopupNotification
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            notification?.let {
                Card(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), 
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
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
            Snackbar(
                snackbarData = data, 
                containerColor = MaterialTheme.colorScheme.errorContainer, 
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.widthIn(max = 500.dp).padding(16.dp)
            )
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
                            QuizScreen(
                                course = activeQuizCourse!!, 
                                onExit = { activeQuizCourse = null },
                                onProgressUpdate = { progress ->
                                    viewModel.updateCourseProgress(activeQuizCourse!!.id, activeQuizCourse!!.title, progress)
                                }
                            )
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
                                    PullToRefreshBox(
                                        isRefreshing = isRefreshing, 
                                        onRefresh = { 
                                            scope.launch { 
                                                isRefreshing = true
                                                viewModel.observePosts()
                                                viewModel.observeSharedCourses()
                                                delay(1000)
                                                isRefreshing = false 
                                            } 
                                        }
                                    ) {
                                        when (currentDestination) {
                                            AnglDestination.HOME -> HomeScreen(viewModel, onStartCourse = { activeQuizCourse = it })
                                            AnglDestination.LEARN -> LearnScreen(viewModel, { activeQuizCourse = it }, { viewModel.deleteCourse(it) })
                                            AnglDestination.AI_GEN -> AiGenScreen({ currentDestination = AnglDestination.LEARN }, viewModel)
                                            AnglDestination.SOCIAL -> SocialFeed(viewModel, onStartCourse = { activeQuizCourse = it }, onNavigateToProfile = { viewingProfileEmail = it }, onOpenChat = { chatPartnerEmail = it })
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
                        onNotifClick = { notif: Notification ->
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
    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Chào bạn, ${viewModel.getDisplayName()}!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            ) 
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxSize()
                    .padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text("Tiến độ hôm nay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp), 
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val lastCourseId = viewModel.userSettings.lastCourseId
                            val lastCourseTitle = viewModel.userSettings.lastCourseTitle
                            val progress = lastCourseId?.let { viewModel.userSettings.courseProgress[it] } ?: 0f
                            
                            if (lastCourseTitle != null) {
                                Text("Đang học: $lastCourseTitle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { progress }, 
                                    modifier = Modifier.fillMaxWidth().height(8.dp), 
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Hoàn thành ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            } else { 
                                Text("Bắt đầu bài học đầu tiên ngay thôi!", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { /* Navigate to explore */ }) {
                                    Text("Khám phá ngay")
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Bài học cộng đồng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Ưu tiên từ bạn bè và thịnh hành nhất", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }

                items(viewModel.sharedCourses) { shared ->
                    SharedCourseItem(shared = shared, onStart = {
                        viewModel.incrementView(shared.id)
                        onStartCourse(Course(shared.id, shared.title, shared.questions))
                    }, onStar = { viewModel.starCourse(shared.id) }, currentUserId = viewModel.userSettings.email)
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun SharedCourseItem(shared: SharedCourse, onStart: () -> Unit, onStar: () -> Unit, currentUserId: String) {
    val isStarred = currentUserId in shared.stars
    Card(
        modifier = Modifier.fillMaxWidth(), 
        onClick = onStart,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = shared.authorProfilePic, 
                    contentDescription = null, 
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(shared.authorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Người tạo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onStar) {
                    Icon(
                        if (isStarred) Icons.Default.Star else Icons.Default.StarBorder, 
                        contentDescription = null, 
                        tint = if (isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(shared.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${shared.questions.size} câu hỏi • ${shared.views} lượt học", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
        topBar = { CenterAlignedTopAppBar(title = { Text(partnerName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(viewModel.chatMessages) { msg ->
                        val isMe = msg.senderEmail == viewModel.userSettings.email
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
                            Surface(
                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp), 
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Text(msg.content, modifier = Modifier.padding(12.dp), color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .imePadding(), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText, 
                            onValueChange = { messageText = it }, 
                            modifier = Modifier.weight(1f), 
                            placeholder = { Text("Nhắn tin...") }, 
                            shape = CircleShape,
                            maxLines = 4
                        )
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = { if (messageText.isNotBlank()) { viewModel.sendMessage(partnerEmail, messageText); messageText = "" } },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        }
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

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Khóa học của bạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxSize()
                    .padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.courses.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Bạn chưa có bài học nào. Hãy tạo bằng AI ngay!", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                items(viewModel.courses) { course ->
                    Card(
                        onClick = { onCourseClick(course) }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        ListItem(
                            headlineContent = { Text(course.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text("${course.questions.size} câu hỏi") },
                            trailingContent = { 
                                Row {
                                    IconButton(onClick = { 
                                        selectedCourseToShare = course
                                        showShareOptions = true
                                    }) { Icon(Icons.Default.Share, "Chia sẻ", tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = { onDeleteCourse(course) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showShareOptions && selectedCourseToShare != null) {
        ModalBottomSheet(onDismissRequest = { showShareOptions = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Chia sẻ bài học", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
        }
    }

    if (showFriendSelector && selectedCourseToShare != null) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredFriends = viewModel.userSettings.friends.filter { it.contains(searchQuery, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showFriendSelector = false },
            title = { Text("Gửi cho bạn bè") },
            text = {
                Column(modifier = Modifier.widthIn(max = 500.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tìm kiếm tên hoặc email...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = CircleShape
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filteredFriends) { friendEmail ->
                            var friendName by remember { mutableStateOf(friendEmail) }
                            LaunchedEffect(friendEmail) { viewModel.getUserProfile(friendEmail) { friendName = it?.name ?: friendEmail } }
                            ListItem(
                                headlineContent = { Text(friendName) },
                                supportingContent = { Text(friendEmail) },
                                leadingContent = { 
                                    AsyncImage(
                                        model = viewModel.getRandomAvatar(friendEmail), 
                                        contentDescription = null, 
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                    ) 
                                },
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
            CenterAlignedTopAppBar(
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
                                    text = { Text("Đăng xuất", color = MaterialTheme.colorScheme.error) },
                                    onClick = { onLogout(); showOptionsMenu = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            ) 
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            profileData?.let { profile ->
                LazyColumn(modifier = Modifier.widthIn(max = 700.dp).fillMaxSize()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = profile.profilePicture ?: viewModel.getRandomAvatar(targetEmail), 
                                contentDescription = null, 
                                modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), 
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(profile.name ?: "Chưa đặt tên", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(profile.bio ?: "Học hỏi là không ngừng nghỉ 📚", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(24.dp))
                            
                            if (isOwnProfile) {
                                Button(
                                    onClick = { showFriendsList = true },
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Default.Group, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Bạn bè (${profile.friends.size})")
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { onChat(targetEmail) },
                                        modifier = Modifier.weight(1f),
                                        shape = MaterialTheme.shapes.medium
                                    ) { 
                                        Icon(Icons.Default.Chat, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Nhắn tin") 
                                    }
                                    
                                    val buttonModifier = Modifier.weight(1f)
                                    when {
                                        isFriend -> {
                                            OutlinedButton(onClick = { }, enabled = false, modifier = buttonModifier, shape = MaterialTheme.shapes.medium) { 
                                                Icon(Icons.Default.Check, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Bạn bè") 
                                            }
                                        }
                                        hasReceivedFromThem -> {
                                            Button(onClick = { viewModel.acceptFriendRequest(targetEmail) }, modifier = buttonModifier, shape = MaterialTheme.shapes.medium) { 
                                                Text("Chấp nhận") 
                                            }
                                        }
                                        hasSentToThem -> {
                                            OutlinedButton(onClick = { }, enabled = false, modifier = buttonModifier, shape = MaterialTheme.shapes.medium) { 
                                                Text("Đã gửi mời") 
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = { viewModel.sendFriendRequest(targetEmail) },
                                                modifier = buttonModifier,
                                                shape = MaterialTheme.shapes.medium,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                            ) { Text("Kết bạn") }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Text(
                            "Bài viết cá nhân", 
                            modifier = Modifier.padding(16.dp), 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(viewModel.posts.filter { it.authorEmail == targetEmail }) { post ->
                        PostItem(
                            post = post, 
                            isCurrentUser = viewModel.userSettings.email == post.authorEmail, 
                            currentUserId = viewModel.userSettings.email, 
                            onDelete = { viewModel.deletePost(post.id) }, 
                            onToggleLike = { viewModel.toggleLikePost(post) }, 
                            onOpenDetail = { }, 
                            onAvatarClick = { }, 
                            onCourseClick = {}, 
                            viewModel = viewModel
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showFriendsList) {
        AlertDialog(
            onDismissRequest = { showFriendsList = false },
            title = { Text("Bạn bè") },
            text = {
                Column(modifier = Modifier.widthIn(max = 500.dp)) {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        if (viewModel.userSettings.friends.isEmpty()) {
                            item { 
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Bạn chưa có người bạn nào", color = MaterialTheme.colorScheme.outline) 
                                }
                            }
                        }
                        items(viewModel.userSettings.friends) { friendEmail ->
                            var friendName by remember { mutableStateOf(friendEmail) }
                            LaunchedEffect(friendEmail) { viewModel.getUserProfile(friendEmail) { friendName = it?.name ?: friendEmail } }
                            ListItem(
                                headlineContent = { Text(friendName, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(friendEmail) },
                                leadingContent = { 
                                    AsyncImage(
                                        model = viewModel.getRandomAvatar(friendEmail), 
                                        contentDescription = null, 
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) 
                                },
                                modifier = Modifier.clickable { 
                                    showFriendsList = false
                                    // Could navigate to their profile
                                }
                            )
                        }
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
                Column(modifier = Modifier.widthIn(max = 500.dp)) {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        if (viewModel.userSettings.friendRequests.isEmpty()) {
                            item { 
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Không có lời mời nào", color = MaterialTheme.colorScheme.outline) 
                                }
                            }
                        }
                        items(viewModel.userSettings.friendRequests) { senderEmail ->
                            var senderName by remember { mutableStateOf(senderEmail) }
                            LaunchedEffect(senderEmail) { viewModel.getUserProfile(senderEmail) { senderName = it?.name ?: senderEmail } }
                            ListItem(
                                headlineContent = { Text(senderName, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(senderEmail) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { viewModel.acceptFriendRequest(senderEmail) }) { Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50)) }
                                        IconButton(onClick = { viewModel.rejectFriendRequest(senderEmail) }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFriendRequests = false }) { Text("Xong") } }
        )
    }

    if (showEditDialog) {
        var name by remember { mutableStateOf(viewModel.userSettings.name ?: "") }
        var bio by remember { mutableStateOf(viewModel.userSettings.bio ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false }, 
            title = { Text("Chỉnh sửa hồ sơ") }, 
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.widthIn(max = 400.dp)) { 
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên hiển thị") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Tiểu sử") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, minLines = 3) 
                } 
            },
            confirmButton = { 
                Button(onClick = { viewModel.updateUserName(name); viewModel.updateBio(bio); showEditDialog = false }, shape = MaterialTheme.shapes.medium) { 
                    Text("Lưu thay đổi") 
                } 
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Hủy") }
            }
        )
    }
}
