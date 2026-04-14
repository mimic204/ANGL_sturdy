package com.example.do_an_app_adr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenScreen(
    onCourseCreated: () -> Unit,
    viewModel: QuizViewModel
) {
    var prompt by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Cấu hình Model
    val config = generationConfig {
        // Bỏ responseMimeType tạm thời nếu gặp lỗi 404 trên các phiên bản v1beta cũ
        // responseMimeType = "application/json" 
    }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = "AIzaSyDVJjfmxyJvIIkdh3J9M5FNyewseviGxlI",
            generationConfig = config
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tạo trắc nghiệm bằng AI") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Nhập chủ đề trắc nghiệm") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ví dụ: Toán 12") }
            )

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        result = ""
                        try {
                            val fullPrompt = """
                                Tạo 30 bài tập trắc nghiệm về chủ đề: $prompt.
                                
                                Yêu cầu JSON format:
                                [
                                  {
                                    "question": "Câu hỏi",
                                    "options": {
                                      "A": "...",
                                      "B": "...",
                                      "C": "...",
                                      "D": "..."
                                    },
                                    "correct_answer": "A",
                                    "explanation": "Giải thích ngắn"
                                  }
                                ]
                                
                                Trả về kết quả dưới dạng JSON.
                            """.trimIndent()
                            
                            val response = generativeModel.generateContent(fullPrompt)
                            val jsonText = response.text ?: ""
                            
                            if (jsonText.isNotEmpty()) {
                                val success = viewModel.addCourseFromJson(prompt, jsonText)
                                if (success) {
                                    onCourseCreated()
                                } else {
                                    result = "Lỗi khi phân tích JSON từ AI. Hãy thử lại."
                                }
                            }
                        } catch (e: Exception) {
                            result = "Lỗi hệ thống: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = prompt.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tạo bài tập")
                }
            }

            if (result.isNotEmpty()) {
                Text(
                    text = "Thông báo:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (!isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Nhập chủ đề để bắt đầu", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
