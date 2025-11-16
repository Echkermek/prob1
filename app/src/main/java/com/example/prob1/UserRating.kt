// models/UserRating.kt
package com.example.prob1.models

data class UserRating(
    val name: String,
    val coins: Long,
    val userId: String = "", // Добавляем параметр userId
    val testScores: Map<String, Int> = emptyMap(),
    val totalTestScore: Int = 0
)