package com.example.prob1.data

import java.io.Serializable
import com.google.firebase.Timestamp

data class Test(
    val id: String = "",
    val title: String = "",
    val num: Int = 0,
    val semester: Int = 1,
    val isAvailable: Boolean = true
) // Тест теперь универсальный, без привязки к курсу

data class Part(
    val id: String = "",
    val title: String = "",
    val num: Int = 0,
    val enterAnswer: Boolean = false,
    val idLectures: String = ""
)

data class Question(
    val id: String = "",
    val text: String = "",  // Было quest
    val content: String? = null,
    val num: Int = 0,
    val answers: List<Answer> = emptyList()
) : Serializable

data class Answer(
    val text: String = "",
    val isCorrect: Boolean = false
)

// Новая модель: Ассоциация курс-тест с дедлайном
data class CourseTestAssignment(
    val id: String = "",
    val courseId: String = "",
    val testId: String = "",
    val deadline: String = ""  // Формат "YYYY-MM-DD"
)

// Модель курса (на основе вашей БД)
data class Course(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val name: String = "",
    val semester: Int = 1
)

data class TestAttempt(
    val id: String = "",
    val userId: String = "",
    val partId: String = "",
    val testId: String = "",
    val attemptNumber: Int = 1,
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val percentage: Double = 0.0,
    val grade: Int = 0,
    val rawScore: Double = 0.0,
    val attemptCoeff: Double = 1.0,
    val timeCoeff: Double = 1.0,
    val finalScore: Double = 0.0,
    val isPassed: Boolean = false,
    val status: String = "completed",
    val interrupted: Boolean = false,
    val timestamp: Timestamp? = null
)

data class TestGrade(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val partId: String = "",
    val bestAttemptId: String = "",
    val bestScore: Double = 0.0,
    val grade: Int = 0,
    val timestamp: Timestamp? = null
)

data class Lection(
    val id: String = "",
    val name: String = "",
    val num: String = "",
    val url: String = ""
)