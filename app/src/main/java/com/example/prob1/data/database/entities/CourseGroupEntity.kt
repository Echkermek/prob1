// com/example/prob1/data/database/entities/CourseGroupEntity.kt
package com.example.prob1.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_groups")
data class CourseGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: String,
    val groupId: String,
    val groupName: String,
    val semester: Int = 1,
    val assignedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)