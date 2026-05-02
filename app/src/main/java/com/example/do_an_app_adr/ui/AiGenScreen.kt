package com.example.do_an_app_adr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.do_an_app_adr.BuildConfig
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
    val scrollState = rememberScrollState()

    // Cấu hình Model
    val config = generationConfig {
        // responseMimeType = "application/json"
    }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = BuildConfig.GEMINI_API_KEY, // Sử dụng API Key từ BuildConfig để bảo mật
            generationConfig = config
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo trắc nghiệm bằng AI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        // Box này giúp căn giữa nội dung trên màn hình lớn (như máy tính bảng)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp) // Giới hạn chiều rộng để hiển thị đẹp trên tablet
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding() // Tự động đẩy nội dung lên khi bàn phím hiện ra
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Nhập chủ đề bạn muốn tạo bài tập. AI sẽ giúp bạn soạn câu hỏi và đáp án tự động.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Chủ đề trắc nghiệm") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ví dụ: Toán lớp 12, Lịch sử Việt Nam...") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            result = ""
                            try {
                                val fullPrompt = """
                                    Hãy tạo 10 bài tập trắc nghiệm bằng tiếng Việt về chủ đề: ${prompt}.
                                    
                                    Yêu cầu JSON format (CHỈ TRẢ VỀ JSON, KHÔNG KÈM TEXT KHÁC):
                                    [
                                      {
                                        "question": "Câu hỏi bằng tiếng Việt",
                                        "options": {
                                          "A": "Lựa chọn 1",
                                          "B": "Lựa chọn 2",
                                          "C": "Lựa chọn 3",
                                          "D": "Lựa chọn 4"
                                        },
                                        "correct_answer": "A",
                                        "explanation": "Giải thích chi tiết bằng tiếng Việt"
                                      }
                                    ]
                                """.trimIndent()
                                
                                val response = generativeModel.generateContent(fullPrompt)
                                val jsonText = response.text ?: ""
                                
                                if (jsonText.isNotEmpty()) {
                                    viewModel.addCourseFromJson(prompt, jsonText) { success ->
                                        if (success) {
                                            onCourseCreated()
                                        } else {
                                            result = "Lỗi khi phân tích JSON từ AI. Hãy thử lại."
                                        }
                                    }
                                } else {
                                    result = "AI không trả về kết quả. Hãy thử lại."
                                }
                            } catch (e: Exception) {
                                result = "Lỗi kết nối AI: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = prompt.isNotBlank() && !isLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bắt đầu tạo bài")
                    }
                }

                if (result.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Thông báo",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else if (!isLoading) {
                    // Trạng thái chờ
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Chờ nhập chủ đề...",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Thêm khoảng trống cuối để tránh nội dung bị sát mép khi cuộn
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
