// com/example/prob1/data/database/entities/TestEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tests")
data class TestEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val num: Int,
    val semester: Int,
    val isAvailable: Boolean = true,
    val hasParts: Boolean = true,
    val courseId: String? = null,
    val description: String? = null,
    val timeLimit: Int? = null, // в минутах
    val maxAttempts: Int? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)