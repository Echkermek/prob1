// com/example/prob1/data/database/dao/QuestionDao.kt
package com.example.prob1.data.database.dao

import androidx.room.*
import com.example.prob1.data.database.entities.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    // ==================== CRUD операции ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    // ==================== Запросы ====================

    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE partId = :partId ORDER BY num")
    suspend fun getQuestionsForPart(partId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE testId = :testId AND (partId = '' OR partId IS NULL) ORDER BY num")
    suspend fun getQuestionsForTestWithoutParts(testId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE isManualInput = 1")
    suspend fun getManualQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE partId IN (:partIds) ORDER BY partId, num")
    suspend fun getQuestionsForParts(partIds: List<String>): List<QuestionEntity>

    // ==================== Удаление ====================

    @Query("DELETE FROM questions WHERE id = :questionId")
    suspend fun deleteQuestionById(questionId: String)

    @Query("DELETE FROM questions WHERE partId = :partId")
    suspend fun deleteQuestionsForPart(partId: String)

    @Query("DELETE FROM questions WHERE testId = :testId")
    suspend fun deleteQuestionsForTest(testId: String)

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()

    // ==================== Подсчет ====================

    @Query("SELECT COUNT(*) FROM questions WHERE partId = :partId")
    suspend fun getQuestionsCountForPart(partId: String): Int

    @Query("SELECT COUNT(*) FROM questions WHERE testId = :testId")
    suspend fun getQuestionsCountForTest(testId: String): Int

    // ==================== Flow (реактивные запросы) ====================

    @Query("SELECT * FROM questions WHERE partId = :partId ORDER BY num")
    fun observeQuestionsForPart(partId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :questionId")
    fun observeQuestionById(questionId: String): Flow<QuestionEntity?>
}