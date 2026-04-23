// com/example/prob1/data/database/entities/PartEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parts")
data class PartEntity(
    @PrimaryKey
    val id: String,
    val testId: String,
    val title: String,
    val num: Int,
    val enterAnswer: Boolean = false,
    val lecId: String? = null,
    val description: String? = null,
    val passingScore: Int? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)