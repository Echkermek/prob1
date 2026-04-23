// com/example/prob1/data/database/dao/AnswerDao.kt
package com.example.prob1.data.database.dao

import androidx.room.*
import com.example.prob1.data.database.entities.AnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: AnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<AnswerEntity>)

    @Update
    suspend fun updateAnswer(answer: AnswerEntity)

    @Delete
    suspend fun deleteAnswer(answer: AnswerEntity)

    @Query("SELECT * FROM answers WHERE questionId = :questionId ORDER BY orderNum")
    suspend fun getAnswersForQuestion(questionId: String): List<AnswerEntity>

    @Query("SELECT * FROM answers WHERE questionId IN (:questionIds) ORDER BY questionId, orderNum")
    suspend fun getAnswersForQuestions(questionIds: List<String>): List<AnswerEntity>

    @Query("DELETE FROM answers WHERE questionId = :questionId")
    suspend fun deleteAnswersForQuestion(questionId: String)

    @Query("DELETE FROM answers")
    suspend fun deleteAllAnswers()

    @Query("SELECT * FROM answers WHERE questionId = :questionId ORDER BY orderNum")
    fun observeAnswersForQuestion(questionId: String): Flow<List<AnswerEntity>>
}