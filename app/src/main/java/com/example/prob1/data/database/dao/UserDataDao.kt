// com/example/prob1/data/database/dao/UserDataDao.kt
package com.example.prob1.data.database.dao

import androidx.room.*
import com.example.prob1.data.database.entities.UserDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataDao {

    // ==================== CRUD операции ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserData(userData: UserDataEntity)

    @Update
    suspend fun updateUserData(userData: UserDataEntity)

    @Delete
    suspend fun deleteUserData(userData: UserDataEntity)

    // ==================== Запросы ====================

    @Query("SELECT * FROM user_data WHERE userId = :userId")
    suspend fun getUserData(userId: String): UserDataEntity?

    @Query("SELECT * FROM user_data WHERE groupId = :groupId")
    suspend fun getUsersByGroupId(groupId: String): List<UserDataEntity>

    @Query("SELECT * FROM user_data")
    suspend fun getAllUsersData(): List<UserDataEntity>

    // ==================== Обновление полей ====================

    @Query("UPDATE user_data SET groupId = :groupId, groupName = :groupName WHERE userId = :userId")
    suspend fun updateUserGroup(userId: String, groupId: String, groupName: String)

    @Query("UPDATE user_data SET courseId = :courseId, semester = :semester WHERE userId = :userId")
    suspend fun updateUserCourse(userId: String, courseId: String, semester: Int)

    @Query("UPDATE user_data SET coins = :coins WHERE userId = :userId")
    suspend fun updateUserCoins(userId: String, coins: Int)

    @Query("UPDATE user_data SET lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun updateLastUpdated(userId: String, timestamp: Long)

    // ==================== Удаление ====================

    @Query("DELETE FROM user_data WHERE userId = :userId")
    suspend fun deleteUserDataById(userId: String)

    @Query("DELETE FROM user_data")
    suspend fun deleteAllUsersData()

    // ==================== Flow (реактивные запросы) ====================

    @Query("SELECT * FROM user_data WHERE userId = :userId")
    fun observeUserData(userId: String): Flow<UserDataEntity?>

    @Query("SELECT * FROM user_data")
    fun observeAllUsersData(): Flow<List<UserDataEntity>>
}