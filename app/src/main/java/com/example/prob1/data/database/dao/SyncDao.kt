// com/example/prob1/data/database/dao/SyncDao.kt
package com.example.prob1.data.database.dao

import androidx.room.*
import com.example.prob1.data.database.entities.SyncInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    // ==================== CRUD операции ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncInfo(syncInfo: SyncInfoEntity)

    @Update
    suspend fun updateSyncInfo(syncInfo: SyncInfoEntity)

    @Delete
    suspend fun deleteSyncInfo(syncInfo: SyncInfoEntity)

    // ==================== Запросы ====================

    @Query("SELECT * FROM sync_info WHERE dataType = :dataType")
    suspend fun getSyncInfo(dataType: String): SyncInfoEntity?

    @Query("SELECT * FROM sync_info")
    suspend fun getAllSyncInfo(): List<SyncInfoEntity>

    // ==================== Обновление времени ====================

    @Query("UPDATE sync_info SET lastSyncTime = :time WHERE dataType = :dataType")
    suspend fun updateSyncTime(dataType: String, time: Long)

    @Query("UPDATE sync_info SET lastSyncTime = :time, syncStatus = :status WHERE dataType = :dataType")
    suspend fun updateSyncStatus(dataType: String, time: Long, status: String)

    // ==================== Удаление ====================

    @Query("DELETE FROM sync_info WHERE dataType = :dataType")
    suspend fun deleteSyncInfoByType(dataType: String)

    @Query("DELETE FROM sync_info")
    suspend fun deleteAllSyncInfo()

    // ==================== Проверки ====================

    @Query("SELECT lastSyncTime FROM sync_info WHERE dataType = :dataType")
    suspend fun getLastSyncTime(dataType: String): Long?

    // ==================== Flow (реактивные запросы) ====================

    @Query("SELECT * FROM sync_info WHERE dataType = :dataType")
    fun observeSyncInfo(dataType: String): Flow<SyncInfoEntity?>

    @Query("SELECT * FROM sync_info")
    fun observeAllSyncInfo(): Flow<List<SyncInfoEntity>>
}