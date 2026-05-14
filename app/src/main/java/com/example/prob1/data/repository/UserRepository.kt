package com.example.prob1.data.repository

import android.content.Context
import android.util.Log
import com.example.prob1.data.database.AppDatabase
import com.example.prob1.data.database.entities.CourseEntity
import com.example.prob1.data.database.entities.UserDataEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val firestore = Firebase.firestore

    companion object {
        private const val TAG = "UserRepository"
        private const val SYNC_TYPE_USER_DATA = "user_data"
        private const val SYNC_TYPE_COURSES = "courses"
        private const val CACHE_STALE_TIME = 30 * 60 * 1000L
    }


    suspend fun getUserData(userId: String, forceRefresh: Boolean = false): UserDataEntity? {
        return withContext(Dispatchers.IO) {
            val cachedData = db.userDataDao().getUserData(userId)

            try {
                val userData = fetchUserDataFromFirestore(userId)
                userData?.let {
                    db.userDataDao().insertUserData(it)
                    updateSyncTime(SYNC_TYPE_USER_DATA)
                }
                userData ?: cachedData
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user data", e)
                cachedData
            }
        }
    }

    private suspend fun fetchUserDataFromFirestore(userId: String): UserDataEntity? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()

            val groupSnapshot = firestore.collection("usersgroup")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val groupDoc = groupSnapshot.documents.firstOrNull()
            val groupId = groupDoc?.getString("groupId")
            val groupName = groupDoc?.getString("groupName")

            var courseId: String? = null
            var semester = 1

            if (groupId != null) {
                val courseGroup = firestore.collection("course_groups")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

                courseId = courseGroup?.getString("courseId")
                semester = (courseGroup?.get("semester") as? Long)?.toInt() ?: 1
            }


            val coins = userDoc.getLong("coins")?.toInt() ?: 100
            val credit = userDoc.getLong("credit")?.toInt() ?: 0

            // Загружаем общий балл
            val gradesSnapshot = firestore.collection("test_grades")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val totalScore = gradesSnapshot.documents.sumOf { it.getDouble("bestScore") ?: 0.0 }
            val grade = calculateGradeFromFirestore(totalScore, semester)

            UserDataEntity(
                userId = userId,
                name = userDoc.getString("name"),
                surname = userDoc.getString("surname"),
                email = userDoc.getString("email"),
                groupId = groupId,
                groupName = groupName,
                courseId = courseId,
                semester = semester,
                coins = coins,
                credit = credit,
                totalScore = totalScore,
                grade = grade
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user data from Firestore", e)
            null
        }
    }

    private suspend fun calculateGradeFromFirestore(
        totalPoints: Double,
        semester: Int
    ): Int {
        return try {
            val ratingDoc = firestore.collection("rating")
                .whereEqualTo("semester", semester)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val min3 = ratingDoc?.getLong("min3")?.toDouble() ?: 76.0
            val min4 = ratingDoc?.getLong("min4")?.toDouble() ?: 90.0
            val min5 = ratingDoc?.getLong("min5")?.toDouble() ?: 106.0

            when {
                totalPoints >= min5 -> 5
                totalPoints >= min4 -> 4
                totalPoints >= min3 -> 3
                else -> 2
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating grade from Firestore", e)

            when {
                totalPoints >= 106 -> 5
                totalPoints >= 90 -> 4
                totalPoints >= 76 -> 3
                else -> 2
            }
        }
    }

    // ==================== ИСПРАВЛЕННЫЙ МЕТОД saveUserData ====================

    suspend fun saveUserData(
        userId: String,
        groupId: String,
        groupName: String,
        courseId: String?,
        semester: Int
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Получаем существующие данные пользователя
                val existing = db.userDataDao().getUserData(userId)

                // Создаем обновленную сущность
                val userData = UserDataEntity(
                    userId = userId,
                    name = existing?.name,
                    surname = existing?.surname,
                    email = existing?.email,
                    groupId = groupId,
                    groupName = groupName,
                    courseId = courseId,
                    semester = semester,
                    coins = existing?.coins ?: 100,
                    credit = existing?.credit ?: 0,
                    totalScore = existing?.totalScore ?: 0.0,
                    grade = existing?.grade ?: 0
                )

                db.userDataDao().insertUserData(userData)
                updateSyncTime(SYNC_TYPE_USER_DATA)
                Log.d(TAG, "User data saved for: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user data", e)
            }
        }
    }

    // ==================== Обновление данных пользователя ====================

    suspend fun updateUserGroup(userId: String, groupId: String, groupName: String) {
        withContext(Dispatchers.IO) {
            try {
                db.userDataDao().updateUserGroup(userId, groupId, groupName)
                updateSyncTime(SYNC_TYPE_USER_DATA)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user group", e)
            }
        }
    }

    suspend fun updateUserCourse(userId: String, courseId: String, semester: Int) {
        withContext(Dispatchers.IO) {
            try {
                db.userDataDao().updateUserCourse(userId, courseId, semester)
                updateSyncTime(SYNC_TYPE_USER_DATA)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user course", e)
            }
        }
    }

    fun observeUserData(userId: String): Flow<UserDataEntity?> {
        return db.userDataDao().observeUserData(userId)
    }

    // ==================== Монеты ====================

    suspend fun getUserCoins(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                doc.getLong("coins")?.toInt() ?: 100
            } catch (e: Exception) {
                100
            }
        }
    }

    suspend fun updateUserCoins(userId: String, newCoins: Int) {
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId)
                    .update("coins", newCoins)
                    .await()

                db.userDataDao().updateUserCoins(userId, newCoins)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating coins", e)
            }
        }
    }

    suspend fun deductCoin(userId: String, amount: Int = 1): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                    val currentCoins = getUserCoins(userId)
                if (currentCoins < amount) return@withContext false

                firestore.collection("users").document(userId)
                    .update("coins", FieldValue.increment(-amount.toLong()))
                    .await()

                db.userDataDao().updateUserCoins(userId, currentCoins - amount)

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun addCoins(userId: String, amount: Int) {
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId)
                    .update("coins", FieldValue.increment(amount.toLong()))
                    .await()

                val currentData = db.userDataDao().getUserData(userId)
                currentData?.let {
                    db.userDataDao().updateUserCoins(userId, it.coins + amount)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding coins", e)
            }
        }
    }

    // ==================== Курсы ====================

    suspend fun getCourseForGroup(groupId: String): CourseEntity? {
        return withContext(Dispatchers.IO) {
            val cached = db.courseDao().getCourseByGroupId(groupId)

            if (cached != null && !isCacheStale(SYNC_TYPE_COURSES)) {
                return@withContext cached
            }

            try {
                val course = fetchCourseForGroupFromFirestore(groupId)
                course?.let { db.courseDao().insertCourse(it) }
                updateSyncTime(SYNC_TYPE_COURSES)
                course
            } catch (e: Exception) {
                cached
            }
        }
    }

    private suspend fun fetchCourseForGroupFromFirestore(groupId: String): CourseEntity? {
        return try {
            val snapshot = firestore.collection("course_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            val courseGroupDoc = snapshot.documents.firstOrNull() ?: return null
            val courseId = courseGroupDoc.getString("courseId") ?: return null
            val semester = (courseGroupDoc.get("semester") as? Long)?.toInt() ?: 1

            val courseDoc = firestore.collection("courses").document(courseId).get().await()

            CourseEntity(
                id = courseId,
                name = courseDoc.getString("name") ?: "",
                groupId = groupId,
                groupName = courseGroupDoc.getString("groupName") ?: "",
                semester = semester
            )
        } catch (e: Exception) {
            null
        }
    }

    // ==================== Группа пользователя ====================

    suspend fun getUserGroup(userId: String): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("usersgroup")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val doc = snapshot.documents.firstOrNull()
                Pair(doc?.getString("groupId"), doc?.getString("groupName"))
            } catch (e: Exception) {
                Pair(null, null)
            }
        }
    }

    // ==================== Оценки ====================

    suspend fun getUserTotalScore(userId: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("test_grades")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                snapshot.documents.sumOf { it.getDouble("bestScore") ?: 0.0 }
            } catch (e: Exception) {
                0.0
            }
        }
    }

    // ==================== Управление кэшем ====================

    private suspend fun isCacheStale(dataType: String): Boolean {
        val lastSync = db.syncDao().getLastSyncTime(dataType) ?: 0L
        return System.currentTimeMillis() - lastSync > CACHE_STALE_TIME
    }

    private suspend fun updateSyncTime(dataType: String) {
        db.syncDao().updateSyncTime(dataType, System.currentTimeMillis())
    }

    suspend fun clearUserDataCache(userId: String) {
        withContext(Dispatchers.IO) {
            db.userDataDao().deleteUserDataById(userId)
        }
    }

    suspend fun clearAllUserCache() {
        withContext(Dispatchers.IO) {
            db.userDataDao().deleteAllUsersData()
            db.courseDao().deleteAllCourses()
            db.syncDao().deleteSyncInfoByType(SYNC_TYPE_USER_DATA)
            db.syncDao().deleteSyncInfoByType(SYNC_TYPE_COURSES)
        }
    }

    // ==================== Предзагрузка ====================

    suspend fun preloadUserData(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val userData = fetchUserDataFromFirestore(userId)
                userData?.let {
                    db.userDataDao().insertUserData(it)
                    updateSyncTime(SYNC_TYPE_USER_DATA)
                }
                Log.d(TAG, "Preloaded user data for: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading user data", e)
            }
        }
    }
}