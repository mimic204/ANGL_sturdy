package com.example.do_an_app_adr.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestion(
    val question: String = "",
    val options: Map<String, String> = emptyMap(),
    val correct_answer: String = "",
    val explanation: String = ""
)

@Serializable
data class Course(
    val id: String = "",
    val title: String = "",
    val questions: List<QuizQuestion> = emptyList()
)

@Serializable
data class SharedCourse(
    val id: String = "",
    val title: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val authorEmail: String = "",
    val authorName: String = "",
    val authorProfilePic: String? = null,
    val timestamp: Long = 0L,
    val views: Int = 0,
    val stars: List<String> = emptyList() // Emails of users who starred
)

@Serializable
data class ChatMessage(
    val id: String = "",
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

@Serializable
data class UserSettings(
    val name: String? = null,
    val isDarkMode: Boolean = false,
    val email: String = "",
    val password: String = "123456",
    val isLoggedIn: Boolean = false,
    val profilePicture: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val notificationsEnabled: Boolean = true,
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList(),
    val lastCourseId: String? = null,
    val lastCourseTitle: String? = null,
    val courseProgress: Map<String, Float> = emptyMap() // Map of courseId to progress (0.0 to 1.0)
)

@Serializable
data class Post(
    val id: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val authorProfilePic: String? = null,
    val content: String = "",
    val timestamp: Long = 0L,
    val sharedCourse: Course? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val likedBy: List<String> = emptyList(),
    val comments: List<SocialComment> = emptyList(),
    val imageUrl: String? = null
)

@Serializable
data class SocialComment(
    val id: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isVoiceComment: Boolean = false,
    val likes: Int = 0
)

@Serializable
data class Notification(
    val id: String = "",
    val toEmail: String = "",
    val fromEmail: String = "",
    val fromName: String = "",
    val type: String = "", // "like", "comment", "friend_request"
    val postId: String = "",
    val postContent: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
