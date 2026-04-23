// com/example/prob1/data/database/entities/UserDataEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_data")
data class UserDataEntity(
    @PrimaryKey
    val userId: String,
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    val groupId: String? = null,
    val groupName: String? = null,
    val courseId: String? = null,
    val semester: Int = 1,
    val coins: Int = 100,
    val credit: Int = 0,
    val avatarLevel: Int = 1,
    val totalScore: Double = 0.0,
    val grade: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)