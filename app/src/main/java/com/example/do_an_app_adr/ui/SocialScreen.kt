package com.example.do_an_app_adr.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.do_an_app_adr.model.ChatMessage
import com.example.do_an_app_adr.model.Course
import com.example.do_an_app_adr.model.Notification
import com.example.do_an_app_adr.model.Post
import com.example.do_an_app_adr.model.SocialComment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeed(
    viewModel: QuizViewModel,
    onStartCourse: (Course) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onOpenChat: (String) -> Unit
) {
    var showCreatePost by remember { mutableStateOf(false) }
    var selectedPostForDetail by remember { mutableStateOf<Post?>(null) }
    var showMessageList by remember { mutableStateOf(false) }
    
    val posts = viewModel.posts

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cộng đồng", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = { showMessageList = true }) {
                    BadgedBox(
                        badge = {
                            // Ở đây có thể thêm logic đếm tin nhắn chưa đọc nếu có trường isRead trong ChatMessage
                        }
                    ) {
                        Icon(
                            Icons.Default.Mail, 
                            contentDescription = "Tin nhắn",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showCreatePost = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Bạn đang nghĩ gì?", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
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
                        onCourseClick = onStartCourse,
                        viewModel = viewModel
                    )
                    HorizontalDivider(
                        thickness = 8.dp, 
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (showCreatePost) {
            CreatePostDialog(
                viewModel = viewModel,
                onDismiss = { showCreatePost = false }
            )
        }

        if (showMessageList) {
            MessageListDialog(
                viewModel = viewModel,
                onDismiss = { showMessageList = false },
                onChatSelected = { email ->
                    showMessageList = false
                    onOpenChat(email)
                }
            )
        }

        selectedPostForDetail?.let { post ->
            PostDetailDialog(
                post = post,
                viewModel = viewModel,
                onDismiss = { selectedPostForDetail = null },
                onNavigateToProfile = onNavigateToProfile,
                onCourseClick = onStartCourse
            )
        }

        if (viewModel.isProcessingAi) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "AI đang vẽ lại hình ảnh của bạn...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun MessageListDialog(
    viewModel: QuizViewModel,
    onDismiss: () -> Unit,
    onChatSelected: (String) -> Unit
) {
    // Logic: Tìm tất cả các đối tác chat duy nhất từ tin nhắn
    val myEmail = viewModel.userSettings.email
    val chatPartners = remember(viewModel.chatMessages) {
        viewModel.chatMessages
            .map { if (it.senderEmail == myEmail) it.receiverEmail else it.senderEmail }
            .distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tin nhắn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (chatPartners.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có cuộc trò chuyện nào", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn {
                        items(chatPartners) { partnerEmail ->
                            var partnerName by remember { mutableStateOf(partnerEmail) }
                            val lastMsg = viewModel.chatMessages.findLast { it.senderEmail == partnerEmail || it.receiverEmail == partnerEmail }
                            
                            LaunchedEffect(partnerEmail) {
                                viewModel.getUserProfile(partnerEmail) { partnerName = it?.name ?: partnerEmail }
                            }
                            
                            ListItem(
                                modifier = Modifier.clickable { onChatSelected(partnerEmail) },
                                headlineContent = { Text(partnerName, fontWeight = FontWeight.Bold) },
                                supportingContent = { 
                                    Text(
                                        lastMsg?.content ?: partnerEmail,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    ) 
                                },
                                leadingContent = {
                                    AsyncImage(
                                        model = viewModel.getRandomAvatar(partnerEmail),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                },
                                trailingContent = {
                                    if (lastMsg != null) {
                                        Text(
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMsg.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
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
    onCourseClick: (Course) -> Unit,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAvatarClick() }
            ) {
                AsyncImage(
                    model = post.authorProfilePic ?: viewModel.getRandomAvatar(post.authorEmail),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(post.authorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(post.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
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
                            text = { Text("Xóa bài viết", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        
        val content = post.content
        if (content.contains("[Hình ảnh ASCII]")) {
            val mainContent = content.substringBefore("[Hình ảnh ASCII]").trim()
            val asciiContent = content.substringAfter("[Hình ảnh ASCII]").trim()
            
            if (mainContent.isNotEmpty()) {
                Text(mainContent, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Surface(
                color = Color.Black.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    asciiContent,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        lineHeight = 9.sp
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        } else {
            Text(content, style = MaterialTheme.typography.bodyLarge)
        }

        post.sharedCourse?.let { course ->
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                onClick = { 
                    viewModel.saveSharedCourse(course)
                    onCourseClick(course)
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(course.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${course.questions.size} câu hỏi • Nhấn để học ngay", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete) { // Placeholder for Like logic
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (post.likedBy.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${post.likedBy.size}", 
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onOpenDetail) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Comment, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (post.comments.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${post.comments.size}", 
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (topComment != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { onOpenDetail() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = topComment.authorName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (topComment.content.contains("[Hình ảnh ASCII]")) "Đã gửi một hình ảnh ASCII" else topComment.content,
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
    onNavigateToProfile: (String) -> Unit,
    onCourseClick: (Course) -> Unit
) {
    var commentToEdit by remember { mutableStateOf<SocialComment?>(null) }
    val currentPost = viewModel.posts.find { it.id == post.id } ?: post

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bài viết & Bình luận") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxSize()
                        .imePadding()
                ) {
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
                                onCourseClick = {
                                    onDismiss()
                                    onCourseClick(it)
                                },
                                viewModel = viewModel
                            )
                            HorizontalDivider()
                            if (currentPost.comments.isNotEmpty()) {
                                Text(
                                    "Bình luận (${currentPost.comments.size})",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        selectedImageUri = uri
                    }

                    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                        Column {
                            if (selectedImageUri != null) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp).clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(onClick = { selectedImageUri = null }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { launcher.launch("image/*") }) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                }
                                TextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Viết bình luận...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        autoCorrectEnabled = true,
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Send
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank() || selectedImageUri != null) {
                                            viewModel.addComment(currentPost, commentText, selectedImageUri) {
                                                commentText = ""
                                                selectedImageUri = null
                                            }
                                        }
                                    },
                                    enabled = (commentText.isNotBlank() || selectedImageUri != null) && !viewModel.isProcessingAi
                                ) {
                                    if (viewModel.isProcessingAi && selectedImageUri != null) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send, 
                                            contentDescription = null, 
                                            tint = if (commentText.isNotBlank() || selectedImageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            },
            confirmButton = {
                Button(onClick = {
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = viewModel.getRandomAvatar(comment.authorEmail),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAvatarClick() },
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        comment.authorName, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp, 
                        modifier = Modifier.clickable { onAvatarClick() },
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val content = comment.content
                    if (content.contains("[Hình ảnh ASCII]")) {
                        val mainPart = content.substringBefore("[Hình ảnh ASCII]").trim()
                        val asciiPart = content.substringAfter("[Hình ảnh ASCII]").trim()
                        
                        if (mainPart.isNotBlank()) {
                            Text(mainPart, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        Surface(
                            color = Color.Black.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                asciiPart,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 6.sp,
                                    lineHeight = 7.sp
                                ),
                                modifier = Modifier.padding(4.dp).horizontalScroll(rememberScrollState())
                            )
                        }
                    } else {
                        Text(content, fontSize = 14.sp)
                    }
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(comment.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "${comment.likes} thích",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable { onLike() }
                )
            }
        }
        
        if (isCurrentUser) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Menu")
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
                        text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                        onClick = { 
                            onDelete()
                            showMenu = false 
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = { if (!viewModel.isProcessingAi) onDismiss() },
        title = { Text("Tạo bài viết mới") },
        text = {
            Column(modifier = Modifier.widthIn(max = 500.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("Bạn đang nghĩ gì?") },
                    shape = MaterialTheme.shapes.medium,
                    enabled = !viewModel.isProcessingAi
                )
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        shape = MaterialTheme.shapes.medium,
                        enabled = !viewModel.isProcessingAi
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm ảnh (Chuyển AI)")
                    }
                    
                    if (selectedImageUri != null) {
                        Spacer(Modifier.width(16.dp))
                        Box {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
                            if (!viewModel.isProcessingAi) {
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
                
                if (viewModel.isProcessingAi) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI đang chuyển đổi ảnh sang ASCII...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.createPost(content, null, selectedImageUri) {
                        onDismiss()
                    }
                },
                enabled = !viewModel.isProcessingAi && (content.isNotBlank() || selectedImageUri != null),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Đăng bài")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !viewModel.isProcessingAi) { Text("Hủy") }
        }
    )
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
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .widthIn(max = 500.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thông báo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (notifications.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có thông báo nào", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn {
                        items(notifications) { notif ->
                            NotificationItem(notif = notif, onClick = { onNotifClick(notif) }, viewModel = viewModel)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .background(if (notif.isRead) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = when(notif.type) {
                "like" -> Color(0xFFFFEBEE)
                "friend_request" -> Color(0xFFE3F2FD)
                else -> Color(0xFFE8F5E9)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when(notif.type) {
                        "like" -> Icons.Default.Favorite
                        "friend_request" -> Icons.Default.PersonAdd
                        else -> Icons.Default.Comment
                    },
                    contentDescription = null,
                    tint = when(notif.type) {
                        "like" -> Color.Red
                        "friend_request" -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFF2E7D32)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                color = if (notif.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold
            )
            Text(
                SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(notif.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
