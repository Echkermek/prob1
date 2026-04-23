// com/example/prob1/data/database/entities/LectionEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lections")
data class LectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val num: String,
    val url: String,
    val courseId: String? = null,
    val description: String? = null,
    val duration: Int? = null, // в минутах
    val lastUpdated: Long = System.currentTimeMillis()
)