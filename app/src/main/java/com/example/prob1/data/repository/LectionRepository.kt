// com/example/prob1/data/database/repository/LectionRepository.kt
package com.example.prob1.data.database.repository

import android.content.Context
import android.util.Log
import com.example.prob1.data.Lection
import com.example.prob1.data.database.AppDatabase
import com.example.prob1.data.database.entities.LectionEntity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LectionRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val firestore = Firebase.firestore

    companion object {
        private const val TAG = "LectionRepository"
        private const val SYNC_TYPE_LECTIONS = "lections"
        private const val CACHE_STALE_TIME = 30 * 60 * 1000L
    }

    // ==================== Получение лекций ====================

    suspend fun getLectionsForCourse(courseId: String, forceRefresh: Boolean = false): List<Lection> {
        return withContext(Dispatchers.IO) {
            val cachedLections = db.lectionDao().getLectionsForCourse(courseId)
            val isStale = isCacheStale()

            if (cachedLections.isNotEmpty() && !forceRefresh && !isStale) {
                return@withContext cachedLections.map { it.toLection() }
            }

            try {
                val lections = fetchLectionsForCourseFromFirestore(courseId)
                db.lectionDao().insertLections(lections)
                updateSyncTime()
                lections.map { it.toLection() }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching lections", e)
                cachedLections.map { it.toLection() }
            }
        }
    }

    private suspend fun fetchLectionsForCourseFromFirestore(courseId: String): List<LectionEntity> {
        val snapshot = firestore.collection("lecture_course")
            .whereEqualTo("courseId", courseId)
            .get()
            .await()

        val lectureIds = snapshot.documents.mapNotNull { it.getString("lectureId") }

        return lectureIds.mapNotNull { lectureId ->
            fetchLectionFromFirestore(lectureId)
        }
    }

    private suspend fun fetchLectionFromFirestore(lectionId: String): LectionEntity? {
        return try {
            val doc = firestore.collection("lections").document(lectionId).get().await()
            if (!doc.exists()) return null

            LectionEntity(
                id = doc.id,
                name = doc.getString("name") ?: "",
                num = doc.getString("num") ?: "0",
                url = doc.getString("url") ?: "",
                courseId = doc.getString("courseId")
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLectionById(lectionId: String): Lection? {
        return withContext(Dispatchers.IO) {
            db.lectionDao().getLectionById(lectionId)?.toLection()
                ?: fetchLectionFromFirestore(lectionId)?.toLection()
        }
    }

    suspend fun getAllLections(): List<Lection> {
        return withContext(Dispatchers.IO) {
            val cached = db.lectionDao().getAllLections()
            if (cached.isNotEmpty()) {
                return@withContext cached.map { it.toLection() }
            }

            try {
                val snapshot = firestore.collection("lections").get().await()
                val lections = snapshot.documents.map { doc ->
                    LectionEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        num = doc.getString("num") ?: "0",
                        url = doc.getString("url") ?: "",
                        courseId = doc.getString("courseId")
                    )
                }
                db.lectionDao().insertLections(lections)
                updateSyncTime()
                lections.map { it.toLection() }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all lections", e)
                cached.map { it.toLection() }
            }
        }
    }

    // ==================== Flow ====================

    fun observeLectionsForCourse(courseId: String): Flow<List<Lection>> {
        return db.lectionDao().observeLectionsForCourse(courseId).map { entities ->
            entities.map { it.toLection() }
        }
    }

    fun observeAllLections(): Flow<List<Lection>> {
        return db.lectionDao().observeAllLections().map { entities ->
            entities.map { it.toLection() }
        }
    }

    // ==================== Управление чтением лекций ====================

    suspend fun markLectionAsRead(userId: String, lectionId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("user_lections")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("lectionId", lectionId)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    // Первое прочтение
                    firestore.collection("user_lections").add(
                        mapOf(
                            "userId" to userId,
                            "lectionId" to lectionId,
                            "readCount" to 1,
                            "lastReadTimestamp" to com.google.firebase.Timestamp.now()
                        )
                    ).await()
                    1
                } else {
                    // Повторное прочтение
                    val doc = snapshot.documents[0]
                    val currentCount = doc.getLong("readCount")?.toInt() ?: 0
                    val newCount = currentCount + 1
                    doc.reference.update(
                        "readCount", newCount,
                        "lastReadTimestamp", com.google.firebase.Timestamp.now()
                    ).await()
                    newCount
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking lection as read", e)
                0
            }
        }
    }

    suspend fun getReadCount(userId: String, lectionId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("user_lections")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("lectionId", lectionId)
                    .get()
                    .await()

                if (snapshot.isEmpty) 0 else snapshot.documents[0].getLong("readCount")?.toInt() ?: 0
            } catch (e: Exception) {
                0
            }
        }
    }

    suspend fun isLectionRead(userId: String, lectionId: String): Boolean {
        return getReadCount(userId, lectionId) > 0
    }

    // ==================== Предзагрузка ====================

    suspend fun preloadLectionsForCourse(courseId: String) {
        withContext(Dispatchers.IO) {
            try {
                val lections = fetchLectionsForCourseFromFirestore(courseId)
                db.lectionDao().insertLections(lections)
                updateSyncTime()
                Log.d(TAG, "Preloaded ${lections.size} lections for course: $courseId")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading lections", e)
            }
        }
    }

    suspend fun preloadAllLections() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("lections").get().await()
                val lections = snapshot.documents.map { doc ->
                    LectionEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        num = doc.getString("num") ?: "0",
                        url = doc.getString("url") ?: "",
                        courseId = doc.getString("courseId")
                    )
                }
                db.lectionDao().insertLections(lections)
                updateSyncTime()
                Log.d(TAG, "Preloaded ${lections.size} lections")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading all lections", e)
            }
        }
    }

    // ==================== Управление кэшем ====================

    private suspend fun isCacheStale(): Boolean {
        val lastSync = db.syncDao().getLastSyncTime(SYNC_TYPE_LECTIONS) ?: 0L
        return System.currentTimeMillis() - lastSync > CACHE_STALE_TIME
    }

    private suspend fun updateSyncTime() {
        db.syncDao().updateSyncTime(SYNC_TYPE_LECTIONS, System.currentTimeMillis())
    }

    suspend fun clearLectionsCache() {
        withContext(Dispatchers.IO) {
            db.lectionDao().deleteAllLections()
            db.syncDao().deleteSyncInfoByType(SYNC_TYPE_LECTIONS)
        }
    }

    // ==================== Расширение для конвертации ====================

    private fun LectionEntity.toLection(): Lection {
        return Lection(
            id = id,
            name = name,
            num = num,
            url = url
        )
    }
}