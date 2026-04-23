// com/example/prob1/data/database/repository/TestRepository.kt
package com.example.prob1.data.database.repository

import android.content.Context
import android.util.Log
import com.example.prob1.data.database.AppDatabase
import com.example.prob1.data.database.entities.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await  // ← ВАЖНО: правильный импорт
import kotlinx.coroutines.withContext

class TestRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "TestRepository"
        private const val SYNC_TYPE_TESTS = "tests"
        private const val SYNC_TYPE_PARTS = "parts"
        private const val SYNC_TYPE_QUESTIONS = "questions"
        private const val CACHE_STALE_TIME = 30 * 60 * 1000L // 30 минут
    }

    // ==================== Тесты ====================

    suspend fun getTestsForCourse(courseId: String, forceRefresh: Boolean = false): List<TestEntity> {
        return withContext(Dispatchers.IO) {
            val cachedTests = db.testDao().getTestsForCourse(courseId)
            val isStale = isCacheStale(SYNC_TYPE_TESTS)

            if (cachedTests.isNotEmpty() && !forceRefresh && !isStale) {
                return@withContext cachedTests
            }

            try {
                val tests = fetchTestsForCourseFromFirestore(courseId)
                db.testDao().insertTests(tests)
                updateSyncTime(SYNC_TYPE_TESTS)
                tests
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tests", e)
                cachedTests
            }
        }
    }

    suspend fun getAllTests(): List<TestEntity> {
        return withContext(Dispatchers.IO) {
            val cached = db.testDao().getAllTests()

            if (cached.isNotEmpty() && !isCacheStale(SYNC_TYPE_TESTS)) {
                return@withContext cached
            }

            try {
                val snapshot = firestore.collection("tests").get().await()  // ← работает
                val tests = snapshot.documents.mapNotNull { doc ->
                    try {
                        TestEntity(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            num = doc.getLong("num")?.toInt() ?: 0,
                            semester = doc.getLong("semester")?.toInt() ?: 1,
                            isAvailable = doc.getBoolean("isAvailable") ?: true,
                            hasParts = doc.getBoolean("hasParts") ?: true,
                            courseId = doc.getString("courseId")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                db.testDao().insertTests(tests)
                updateSyncTime(SYNC_TYPE_TESTS)
                tests
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all tests", e)
                cached
            }
        }
    }

    suspend fun getTestById(testId: String): TestEntity? {
        return withContext(Dispatchers.IO) {
            db.testDao().getTestById(testId) ?: fetchTestFromFirestore(testId)
        }
    }

    private suspend fun fetchTestsForCourseFromFirestore(courseId: String): List<TestEntity> {
        val snapshot = firestore.collection("test_course")
            .whereEqualTo("courseId", courseId)
            .get()
            .await()  // ← работает

        val testIds = snapshot.documents.mapNotNull { it.getString("testId") }

        return testIds.mapNotNull { testId ->
            fetchTestFromFirestore(testId)
        }
    }

    private suspend fun fetchTestFromFirestore(testId: String): TestEntity? {
        return try {
            val doc = firestore.collection("tests").document(testId).get().await()  // ← работает
            if (!doc.exists()) return null

            TestEntity(
                id = doc.id,
                title = doc.getString("title") ?: "",
                num = doc.getLong("num")?.toInt() ?: 0,
                semester = doc.getLong("semester")?.toInt() ?: 1,
                isAvailable = doc.getBoolean("isAvailable") ?: true,
                hasParts = doc.getBoolean("hasParts") ?: true,
                courseId = doc.getString("courseId")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun observeTestsForCourse(courseId: String): Flow<List<TestEntity>> {
        return db.testDao().observeTestsForCourse(courseId)
    }

    // ==================== Части тестов ====================

    suspend fun getPartsForTest(testId: String, forceRefresh: Boolean = false): List<PartEntity> {
        return withContext(Dispatchers.IO) {
            val cachedParts = db.partDao().getPartsForTest(testId)

            if (cachedParts.isNotEmpty() && !forceRefresh) {
                return@withContext cachedParts
            }

            try {
                val parts = fetchPartsForTestFromFirestore(testId)
                db.partDao().insertParts(parts)
                parts
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching parts", e)
                cachedParts
            }
        }
    }

    private suspend fun fetchPartsForTestFromFirestore(testId: String): List<PartEntity> {
        val snapshot = firestore.collection("tests/$testId/parts").get().await()  // ← работает

        return snapshot.documents.map { doc ->
            PartEntity(
                id = doc.id,
                testId = testId,
                title = doc.getString("title") ?: "",
                num = doc.getLong("num")?.toInt() ?: 0,
                enterAnswer = doc.getBoolean("enterAnswer") ?: false,
                lecId = doc.getString("lecId")
            )
        }
    }

    fun observePartsForTest(testId: String): Flow<List<PartEntity>> {
        return db.partDao().observePartsForTest(testId)
    }

    // ==================== Вопросы ====================

    suspend fun getQuestionsForPart(partId: String, forceRefresh: Boolean = false): List<QuestionEntity> {
        return withContext(Dispatchers.IO) {
            val cachedQuestions = db.questionDao().getQuestionsForPart(partId)

            if (cachedQuestions.isNotEmpty() && !forceRefresh) {
                return@withContext cachedQuestions
            }

            try {
                val questions = fetchQuestionsForPartFromFirestore(partId)
                db.questionDao().insertQuestions(questions)
                questions
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching questions", e)
                cachedQuestions
            }
        }
    }

    private suspend fun fetchQuestionsForPartFromFirestore(partId: String): List<QuestionEntity> {
        val part = db.partDao().getPartById(partId)
        val testId = part?.testId ?: return emptyList()

        val snapshot = firestore.collection("tests/$testId/parts/$partId/questions").get().await()  // ← работает

        return snapshot.documents.mapIndexed { index, doc ->
            QuestionEntity(
                id = doc.id,
                partId = partId,
                testId = testId,
                text = doc.getString("text") ?: "",
                content = doc.getString("content"),
                num = doc.getLong("num")?.toInt() ?: index,
                isManualInput = doc.getBoolean("isManualInput") ?: false,
                correctSequence = doc.getString("correctSequence"),
                maxPoints = doc.getLong("maxPoints")?.toInt() ?: 1
            )
        }
    }

    suspend fun getQuestionsForTestWithoutParts(testId: String): List<QuestionEntity> {
        return withContext(Dispatchers.IO) {
            val cached = db.questionDao().getQuestionsForTestWithoutParts(testId)

            if (cached.isNotEmpty()) {
                return@withContext cached
            }

            try {
                val snapshot = firestore.collection("tests/$testId/questions").get().await()  // ← работает
                val questions = snapshot.documents.mapIndexed { index, doc ->
                    QuestionEntity(
                        id = doc.id,
                        partId = "",
                        testId = testId,
                        text = doc.getString("text") ?: "",
                        content = doc.getString("content"),
                        num = doc.getLong("num")?.toInt() ?: index,
                        isManualInput = doc.getBoolean("isManualInput") ?: false,
                        correctSequence = doc.getString("correctSequence"),
                        maxPoints = doc.getLong("maxPoints")?.toInt() ?: 1
                    )
                }
                db.questionDao().insertQuestions(questions)
                questions
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching questions", e)
                cached
            }
        }
    }

    // ==================== Ответы ====================

    suspend fun getAnswersForQuestion(questionId: String): List<AnswerEntity> {
        return withContext(Dispatchers.IO) {
            db.answerDao().getAnswersForQuestion(questionId)
        }
    }

    suspend fun saveAnswers(answers: List<AnswerEntity>) {
        withContext(Dispatchers.IO) {
            db.answerDao().insertAnswers(answers)
        }
    }

    // ==================== Предзагрузка всех данных теста ====================

    suspend fun preloadFullTest(testId: String) {
        withContext(Dispatchers.IO) {
            try {
                val test = fetchTestFromFirestore(testId)
                test?.let { db.testDao().insertTest(it) }

                val parts = fetchPartsForTestFromFirestore(testId)
                db.partDao().insertParts(parts)

                parts.forEach { part ->
                    val questions = fetchQuestionsForPartFromFirestore(part.id)
                    db.questionDao().insertQuestions(questions)
                }

                updateSyncTime(SYNC_TYPE_TESTS)
                Log.d(TAG, "Preloaded full test: $testId")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading test", e)
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

    suspend fun clearAllTestsCache() {
        withContext(Dispatchers.IO) {
            db.testDao().deleteAllTests()
            db.partDao().deleteAllParts()
            db.questionDao().deleteAllQuestions()
            db.answerDao().deleteAllAnswers()
            db.syncDao().deleteSyncInfoByType(SYNC_TYPE_TESTS)
        }
    }
}