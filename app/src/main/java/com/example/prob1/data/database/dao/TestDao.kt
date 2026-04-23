// com/example/prob1/data/database/dao/TestDao.kt
package com.example.prob1.data.database.dao

import androidx.room.*
import com.example.prob1.data.database.entities.TestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDao {

    // ==================== CRUD операции ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTests(tests: List<TestEntity>)

    @Update
    suspend fun updateTest(test: TestEntity)

    @Delete
    suspend fun deleteTest(test: TestEntity)

    // ==================== Запросы ====================

    @Query("SELECT * FROM tests WHERE id = :testId")
    suspend fun getTestById(testId: String): TestEntity?

    @Query("SELECT * FROM tests WHERE courseId = :courseId ORDER BY num")
    suspend fun getTestsForCourse(courseId: String): List<TestEntity>

    @Query("SELECT * FROM tests ORDER BY num")
    suspend fun getAllTests(): List<TestEntity>

    @Query("SELECT * FROM tests WHERE semester = :semester ORDER BY num")
    suspend fun getTestsBySemester(semester: Int): List<TestEntity>

    @Query("SELECT * FROM tests WHERE isAvailable = 1 ORDER BY num")
    suspend fun getAvailableTests(): List<TestEntity>

    // ==================== Удаление ====================

    @Query("DELETE FROM tests WHERE id = :testId")
    suspend fun deleteTestById(testId: String)

    @Query("DELETE FROM tests WHERE courseId = :courseId")
    suspend fun deleteTestsForCourse(courseId: String)

    @Query("DELETE FROM tests")
    suspend fun deleteAllTests()

    // ==================== Проверки ====================

    @Query("SELECT COUNT(*) FROM tests WHERE id = :testId")
    suspend fun testExists(testId: String): Int

    @Query("SELECT COUNT(*) FROM tests WHERE courseId = :courseId")
    suspend fun getTestsCountForCourse(courseId: String): Int

    // ==================== Flow (реактивные запросы) ====================

    @Query("SELECT * FROM tests WHERE courseId = :courseId ORDER BY num")
    fun observeTestsForCourse(courseId: String): Flow<List<TestEntity>>

    @Query("SELECT * FROM tests ORDER BY num")
    fun observeAllTests(): Flow<List<TestEntity>>

    @Query("SELECT * FROM tests WHERE id = :testId")
    fun observeTestById(testId: String): Flow<TestEntity?>
}