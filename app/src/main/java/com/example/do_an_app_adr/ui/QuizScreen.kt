package com.example.do_an_app_adr.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.do_an_app_adr.model.Course
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(course: Course, onExit: () -> Unit, onProgressUpdate: (Float) -> Unit = {}) {
    // Kiểm tra an toàn để tránh crash nếu danh sách câu hỏi rỗng
    if (course.questions.isEmpty()) {
        AlertDialog(
            onDismissRequest = onExit,
            title = { Text("Lỗi") },
            text = { Text("Bài học này không có dữ liệu câu hỏi.") },
            confirmButton = {
                Button(onClick = onExit) { Text("Quay lại") }
            }
        )
        return
    }

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(3600) } // Đã đổi thành 60 phút (3600 giây)
    var correctAnswersCount by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    
    val currentQuestion = course.questions.getOrNull(currentQuestionIndex) ?: course.questions[0]
    val scrollState = rememberScrollState()
    
    LaunchedEffect(showResult) {
        if (!showResult) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
        }
    }

    // Update progress when question index changes
    LaunchedEffect(currentQuestionIndex) {
        val progress = (currentQuestionIndex).toFloat() / course.questions.size
        onProgressUpdate(progress)
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeDisplay = String.format("%02d:%02d", minutes, seconds)

    if (showResult) {
        QuizResultScreen(
            score = correctAnswersCount,
            totalQuestions = course.questions.size,
            timeUsed = 3600 - timeLeft,
            onClose = {
                onProgressUpdate(1.0f)
                onExit()
            },
            onRestart = {
                currentQuestionIndex = 0
                selectedAnswer = null
                showExplanation = false
                timeLeft = 3600
                correctAnswersCount = 0
                showResult = false
                onProgressUpdate(0.0f)
            }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(course.title, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onExit) {
                            Icon(Icons.Default.Close, contentDescription = "Thoát")
                        }
                    },
                    actions = {
                        Text(
                            text = timeDisplay,
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeft < 60) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        ) { padding ->
            // Căn giữa nội dung cho màn hình lớn
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp) // Giới hạn chiều rộng cho tablet
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / course.questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Câu ${currentQuestionIndex + 1}/${course.questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = currentQuestion.question,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    currentQuestion.options.forEach { (key, value) ->
                        val isCorrect = key == currentQuestion.correct_answer
                        val isSelected = selectedAnswer == key
                        
                        val containerColor = when {
                            showExplanation && isCorrect -> Color(0xFFC8E6C9) // Green
                            showExplanation && isSelected && !isCorrect -> Color(0xFFFFCDD2) // Red
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        OutlinedCard(
                            onClick = {
                                if (!showExplanation) {
                                    selectedAnswer = key
                                    showExplanation = true
                                    if (key == currentQuestion.correct_answer) {
                                        correctAnswersCount++
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                            enabled = !showExplanation
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = value, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    
                    if (showExplanation) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedAnswer == currentQuestion.correct_answer) 
                                    Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (selectedAnswer == currentQuestion.correct_answer) "Chính xác!" else "Chưa đúng. Đáp án là ${currentQuestion.correct_answer}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedAnswer == currentQuestion.correct_answer) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = currentQuestion.explanation, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                if (currentQuestionIndex < course.questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedAnswer = null
                                    showExplanation = false
                                } else {
                                    showResult = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = if (currentQuestionIndex < course.questions.size - 1) "Câu tiếp theo" else "Hoàn thành",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    // Khoảng trống cuối để scroll thoải mái
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    score: Int,
    totalQuestions: Int,
    timeUsed: Int,
    onClose: () -> Unit,
    onRestart: () -> Unit
) {
    val percentage = (score.toFloat() / totalQuestions * 100).toInt()
    val minutes = timeUsed / 60
    val seconds = timeUsed % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kết quả bài tập",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { score.toFloat() / totalQuestions },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 12.dp,
                        color = if (percentage >= 50) Color(0xFF4CAF50) else Color(0xFFF44336),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Hoàn thành",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultStatItem(
                        label = "Số câu đúng",
                        value = "$score/$totalQuestions",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )
                    ResultStatItem(
                        label = "Thời gian",
                        value = timeFormatted,
                        icon = Icons.Default.Refresh, // Placeholder for Timer icon
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                val feedback = when {
                    percentage == 100 -> "Tuyệt vời! Bạn đã trả lời đúng tất cả."
                    percentage >= 80 -> "Rất tốt! Bạn nắm kiến thức rất chắc."
                    percentage >= 50 -> "Khá ổn! Hãy cố gắng thêm chút nữa nhé."
                    else -> "Cố gắng lên! Bạn cần ôn tập kỹ hơn."
                }
                
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Về trang chủ")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Làm lại bài")
                }
            }
        }
    }
}

@Composable
fun ResultStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}
