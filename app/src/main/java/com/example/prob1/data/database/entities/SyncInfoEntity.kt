// com/example/prob1/data/database/entities/SyncInfoEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_info")
data class SyncInfoEntity(
    @PrimaryKey
    val dataType: String, // "tests", "parts", "questions", "lections", "courses", "user_data"
    val lastSyncTime: Long = System.currentTimeMillis(),
    val syncStatus: String = "success", // success, pending, error
    val errorMessage: String? = null,
    val itemsCount: Int = 0
)