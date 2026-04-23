// com/example/prob1/data/database/dao/LectionDao.kt
package com.example.prob1.data.database.dao

import androidx.room.*
import com.example.prob1.data.database.entities.LectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LectionDao {

    // ==================== CRUD операции ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLection(lection: LectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLections(lections: List<LectionEntity>)

    @Update
    suspend fun updateLection(lection: LectionEntity)

    @Delete
    suspend fun deleteLection(lection: LectionEntity)

    // ==================== Запросы ====================

    @Query("SELECT * FROM lections WHERE id = :lectionId")
    suspend fun getLectionById(lectionId: String): LectionEntity?

    @Query("SELECT * FROM lections WHERE courseId = :courseId ORDER BY CAST(num AS REAL)")
    suspend fun getLectionsForCourse(courseId: String): List<LectionEntity>

    @Query("SELECT * FROM lections ORDER BY CAST(num AS REAL)")
    suspend fun getAllLections(): List<LectionEntity>

    // ==================== Удаление ====================

    @Query("DELETE FROM lections WHERE id = :lectionId")
    suspend fun deleteLectionById(lectionId: String)

    @Query("DELETE FROM lections WHERE courseId = :courseId")
    suspend fun deleteLectionsForCourse(courseId: String)

    @Query("DELETE FROM lections")
    suspend fun deleteAllLections()

    // ==================== Подсчет ====================

    @Query("SELECT COUNT(*) FROM lections WHERE courseId = :courseId")
    suspend fun getLectionsCountForCourse(courseId: String): Int

    // ==================== Flow (реактивные запросы) ====================

    @Query("SELECT * FROM lections WHERE courseId = :courseId ORDER BY CAST(num AS REAL)")
    fun observeLectionsForCourse(courseId: String): Flow<List<LectionEntity>>

    @Query("SELECT * FROM lections ORDER BY CAST(num AS REAL)")
    fun observeAllLections(): Flow<List<LectionEntity>>
}