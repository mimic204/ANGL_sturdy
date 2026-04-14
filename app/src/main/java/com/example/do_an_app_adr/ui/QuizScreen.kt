package com.example.do_an_app_adr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.do_an_app_adr.model.Course
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(course: Course, onExit: () -> Unit) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(1800) } // 30 mins in seconds
    
    val currentQuestion = course.questions[currentQuestionIndex]
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeDisplay = String.format("%02d:%02d", minutes, seconds)

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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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
                        Text(
                            text = "$key.",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(text = value)
                    }
                }
            }
            
            if (showExplanation) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
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
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = {
                        if (currentQuestionIndex < course.questions.size - 1) {
                            currentQuestionIndex++
                            selectedAnswer = null
                            showExplanation = false
                        } else {
                            onExit()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentQuestionIndex < course.questions.size - 1) "Câu tiếp theo" else "Hoàn thành")
                }
            }
        }
    }
}
