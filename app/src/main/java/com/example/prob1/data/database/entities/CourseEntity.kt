// com/example/prob1/data/database/entities/CourseEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val groupId: String,
    val groupName: String,
    val semester: Int,
    val description: String? = null,
    val teacherId: String? = null,
    val teacherName: String? = null,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)