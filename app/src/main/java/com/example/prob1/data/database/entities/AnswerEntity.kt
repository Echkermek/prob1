// com/example/prob1/data/database/entities/AnswerEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean = false,
    val orderNum: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)