// com/example/prob1/data/repository/StudentScoreManager.kt
package com.example.prob1.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date

class StudentScoreManager {
    private val db = Firebase.firestore
    private val collectionName = "student_course_scores"

    suspend fun getStudentScoreForCourse(userId: String, courseId: String): Double {
        return try {
            val docId = "${userId}_${courseId}"
            val snapshot = db.collection(collectionName)
                .document(docId)
                .get()
                .await()
            snapshot.getDouble("totalScore") ?: 0.0
        } catch (e: Exception) {
            Log.e("StudentScoreManager", "Error getting score", e)
            0.0
        }
    }

    suspend fun isCourseCompleted(courseId: String): Boolean {
        return try {
            val courseDoc = db.collection("courses")
                .document(courseId)
                .get()
                .await()
            courseDoc.getBoolean("completed") == true
        } catch (e: Exception) {
            Log.e("StudentScoreManager", "Error checking course completion", e)
            false
        }
    }

    suspend fun getCurrentCourseInfo(userId: String): CurrentCourseInfo? {
        return try {
            Log.d("StudentScoreManager", "=== getCurrentCourseInfo START for userId: $userId ===")

            val groupSnapshot = db.collection("usersgroup")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            if (groupSnapshot.isEmpty()) {
                Log.e("StudentScoreManager", "No group found for user $userId")
                return null
            }

            val groupId = groupSnapshot.documents[0].getString("groupId") ?: return null

            val groupDoc = db.collection("groups")
                .document(groupId)
                .get()
                .await()

            val groupName = groupDoc.getString("name") ?: ""

            // Ищем ВСЕ курсы, связанные с этой группой в коллекции course_groups
            val courseGroupsSnapshot = db.collection("course_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            if (courseGroupsSnapshot.isEmpty()) {
                Log.e("StudentScoreManager", "No courses found for group $groupId")
                return null
            }

            Log.d("StudentScoreManager", "Found ${courseGroupsSnapshot.size()} course(s) for group $groupId")

            // Выбираем только активные курсы (completed = false)
            val activeCourses = mutableListOf<Pair<String, Date?>>()

            for (doc in courseGroupsSnapshot.documents) {
                val courseId = doc.getString("courseId")
                val assignedAt = doc.getDate("assignedAt")
                val isGroupCourseCompleted = doc.getBoolean("completed") == true

                if (courseId != null && !isGroupCourseCompleted) {
                    val courseDoc = db.collection("courses")
                        .document(courseId)
                        .get()
                        .await()

                    val isCourseCompleted = courseDoc.getBoolean("completed") == true

                    if (!isCourseCompleted) {
                        activeCourses.add(Pair(courseId, assignedAt))
                        Log.d("StudentScoreManager", "  ✅ Active course: $courseId, assignedAt: $assignedAt")
                    }
                }
            }

            if (activeCourses.isEmpty()) {
                Log.e("StudentScoreManager", "No active courses found for group $groupId")
                return null
            }

            // Выбираем самый новый активный курс
            val selectedCourse = activeCourses.maxByOrNull { it.second ?: Date(0) }
            val courseId = selectedCourse?.first ?: return null

            val courseDoc = db.collection("courses")
                .document(courseId)
                .get()
                .await()

            val courseName = courseDoc.getString("name") ?: ""
            val semester = courseDoc.getLong("semester")?.toInt() ?: 1
            val isCompleted = courseDoc.getBoolean("completed") == true

            Log.d("StudentScoreManager", "Selected Course: $courseName, semester: $semester")

            CurrentCourseInfo(
                courseId = courseId,
                courseName = courseName,
                groupId = groupId,
                groupName = groupName,
                semester = semester,
                isCompleted = isCompleted
            )
        } catch (e: Exception) {
            Log.e("StudentScoreManager", "Error getting current course", e)
            null
        }
    }

    suspend fun saveStudentScore(
        userId: String,
        firstName: String,
        lastName: String,
        courseId: String,
        courseName: String,
        semester: Int,
        totalScore: Double
    ): Boolean {
        return try {
            val docId = "${userId}_${courseId}"
            val data = mapOf(
                "userId" to userId,
                "firstName" to firstName,
                "lastName" to lastName,
                "totalScore" to totalScore,
                "courseId" to courseId,
                "courseName" to courseName,
                "semester" to semester,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection(collectionName)
                .document(docId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Log.d("StudentScoreManager", "Saved score for $userId in course $courseId: $totalScore")
            true
        } catch (e: Exception) {
            Log.e("StudentScoreManager", "Error saving student score", e)
            false
        }
    }

    suspend fun syncStudentScoreWithCurrentCourse(userId: String): Boolean {
        return try {
            val currentCourse = getCurrentCourseInfo(userId) ?: return false

            val userDoc = db.collection("users").document(userId).get().await()
            val firstName = userDoc.getString("name") ?: ""
            val lastName = userDoc.getString("surname") ?: ""

            val existingScore = getStudentScoreForCourse(userId, currentCourse.courseId)

            if (existingScore == 0.0) {
                saveStudentScore(
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    courseId = currentCourse.courseId,
                    courseName = currentCourse.courseName,
                    semester = currentCourse.semester,
                    totalScore = 0.0
                )
                Log.d("StudentScoreManager", "Created new score record for course ${currentCourse.courseId} with 0 points")
            }

            true
        } catch (e: Exception) {
            Log.e("StudentScoreManager", "Error syncing student score", e)
            false
        }
    }

    data class CurrentCourseInfo(
        val courseId: String,
        val courseName: String,
        val groupId: String,
        val groupName: String,
        val semester: Int,
        val isCompleted: Boolean
    )
}