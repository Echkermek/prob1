// com/example/prob1/data/database/entities/QuestionEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey
    val id: String,
    val partId: String,
    val testId: String,
    val text: String,
    val content: String? = null,
    val num: Int = 0,
    val isManualInput: Boolean = false,
    val correctSequence: String? = null,
    val maxPoints: Int = 1,
    val questionType: String = "single_choice",
    val explanation: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)