package com.example.prob1.ui.tests

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.prob1.data.Question
import com.example.prob1.databinding.ActivityTestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val db = Firebase.firestore

    private var partId: String? = null
    private var testId: String? = null
    private var questions: List<Question> = emptyList()

    private val questionRawData = mutableMapOf<String, Map<String, Any>>()

    private var correctAnswers = 0          // Для обычных тестов (не USPTU)
    private var rawScore = 0.0              // Для USPTU теста (максимум 35)
    private var attemptNumber = 1
    var attemptDocId: String? = null
    var timer: CountDownTimer? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var currentQuestionIndex = 0
    private var isTestCompleted = false
    private var hasParts: Boolean = false
    private var isUSPTUTest = false         // true, если есть хотя бы один вопрос с isManualInput

    // Ответы пользователя
    private val manualAnswers = mutableMapOf<String, String>()     // для Task 2,3,4
    private val selectedAnswers = mutableMapOf<String, Boolean>()  // для Task 1 (правильно/неправильно)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        partId = intent.getStringExtra("partId")
        testId = intent.getStringExtra("testId")
        hasParts = intent.getBooleanExtra("hasParts", true)

        if (partId == null || testId == null || userId == null) {
            Toast.makeText(this, "Ошибка: не переданы данные", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        checkTestAccess()
    }

    private fun checkTestAccess() {
        lifecycleScope.launch {
            if (!hasParts) {
                proceedToTest()
                return@launch
            }

            val lecId = getPartLectionId()
            val hasNoRealLecture = lecId.isNullOrEmpty() || lecId == "not" || lecId == "-"

            if (hasNoRealLecture) {
                proceedToTest()
                return@launch
            }

            val attempts = getAttemptsCount()
            val readCount = getReadCount(lecId!!)

            if (readCount <= attempts) {
                Toast.makeText(this@TestActivity, "Прочитайте лекцию сначала", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            proceedToTest()
        }
    }

    private suspend fun proceedToTest() {
        attemptNumber = getAttemptsCount() + 1
        showRulesDialog()
    }

    private fun showRulesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Правила теста")
            .setMessage("• 1 минута на вопрос\n• Нельзя выходить из приложения")
            .setPositiveButton("Начать") { _, _ -> loadTest() }
            .setNegativeButton("Назад") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun loadTest() {
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("tests/$testId/parts/$partId/questions").get().await()

                questionRawData.clear()
                val loadedQuestions = mutableListOf<Question>()

                var hasManualInput = false

                snapshot.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    questionRawData[doc.id] = data

                    val isManualInput = data["isManualInput"] as? Boolean ?: false
                    if (isManualInput) hasManualInput = true

                    val answersList = if (!isManualInput) {
                        // Загружаем ВСЕ варианты ответов для обычных вопросов
                        (data["answers"] as? List<Map<String, Any>>)?.map { map ->
                            com.example.prob1.data.Answer(
                                text = (map["text"] as? String ?: "").trim(),
                                isCorrect = map["isCorrect"] as? Boolean ?: false
                            )
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }

                    loadedQuestions.add(
                        Question(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            content = doc.getString("content") ?: "",
                            answers = answersList
                        )
                    )
                }

                questions = loadedQuestions
                isUSPTUTest = hasManualInput

                if (questions.isEmpty()) {
                    Toast.makeText(this@TestActivity, "Вопросы не найдены", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                attemptDocId = createAttemptDoc()
                setupViewPager()

            } catch (e: Exception) {
                Log.e("TestActivity", "Error loading questions", e)
                Toast.makeText(this@TestActivity, "Ошибка загрузки вопросов: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupViewPager() {
        binding.testViewPager.adapter = TestPagerAdapter(this, questions, partId!!, testId!!, false)
        binding.testViewPager.isUserInputEnabled = false

        binding.testViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentQuestionIndex = position
                startQuestionTimerIfNeeded()
            }
        })
    }

    // ====================== МЕТОДЫ ДЛЯ ФРАГМЕНТОВ ======================

    fun onAnswerSelected(isCorrect: Boolean) {
        if (isCorrect) {
            if (isUSPTUTest) rawScore += 1 else correctAnswers++
        }
        selectedAnswers[questions[currentQuestionIndex].id] = isCorrect
        timer?.cancel()
        moveToNextQuestion()
    }

    fun onManualAnswerSubmitted(questionId: String, answer: String) {
        manualAnswers[questionId] = answer.trim()
    }

    fun moveToNextQuestion() {
        if (currentQuestionIndex < questions.size - 1) {
            binding.testViewPager.currentItem = currentQuestionIndex + 1
        } else {
            finishTest()
        }
    }

    fun startQuestionTimerIfNeeded() {
        val currentRaw = questionRawData[questions.getOrNull(currentQuestionIndex)?.id]
        val isManual = currentRaw?.get("isManualInput") as? Boolean ?: false

        if (!isManual && !isTestCompleted) {
            startQuestionTimer()
        }
    }

    fun startQuestionTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.timerText.text = String.format("00:%02d", seconds)
            }

            override fun onFinish() {
                binding.timerText.text = "00:00"
                moveToNextQuestion()
            }
        }.start()
    }

    // ====================== ЗАВЕРШЕНИЕ ТЕСТА ======================

    fun finishTest() {
        isTestCompleted = true
        timer?.cancel()

        if (isUSPTUTest) {
            // USPTU тест — считаем 35 баллов
            rawScore = 0.0

            // Подсчёт обычных вопросов (Task 1)
            selectedAnswers.forEach { (_, isCorrect) ->
                if (isCorrect) rawScore += 1
            }

            // Подсчёт ручных вопросов (Task 2,3,4)
            manualAnswers.forEach { (qId, userAnswer) ->
                val raw = questionRawData[qId] ?: return@forEach
                if (raw["isManualInput"] as? Boolean == true) {
                    val correctSeq = raw["correctSequence"] as? String ?: ""
                    val maxPoints = (raw["maxPoints"] as? Long ?: 1L).toInt()
                    rawScore += calculatePartialScore(userAnswer, correctSeq, maxPoints)
                }
            }

            val finalScore = (rawScore / 35.0 * 20.0).roundToInt().toDouble()

            lifecycleScope.launch {
                updateAttemptDoc(finalScore, rawScore)
                updateBestGrade(finalScore)
                showResult(rawScore.toInt(), finalScore.toInt(), true)
            }

        } else {
            // Обычный тест — старое поведение
            val finalScore = correctAnswers.toDouble()
            lifecycleScope.launch {
                updateAttemptDoc(finalScore, finalScore)
                updateBestGrade(finalScore)
                showResult(correctAnswers, correctAnswers, false)
            }
        }
    }

    private fun calculatePartialScore(userInput: String, correct: String, maxPoints: Int): Int {
        if (userInput.isBlank() || correct.isBlank()) return 0

        // Убираем пробелы вокруг разделителей
        val userList = userInput.replace(" ", "").split(",").map { it.uppercase() }
        val correctList = correct.replace(" ", "").split(",").map { it.uppercase() }

        var matches = 0
        for (i in correctList.indices) {
            if (i < userList.size && userList[i] == correctList[i]) {
                matches++
            }
        }
        return matches
    }

    private fun showResult(raw: Int, final: Int, isScaled: Boolean) {
        val msg = if (isScaled) {
            "Итоговый балл: $final из 20"
        } else {
            "Правильных ответов: $final из ${questions.size}"
        }

        AlertDialog.Builder(this)
            .setTitle("Тест завершён")
            .setMessage(msg)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    // ====================== ВСПОМОГАТЕЛЬНЫЕ ======================

    fun getQuestionRawData(questionId: String): Map<String, Any>? = questionRawData[questionId]

    private suspend fun createAttemptDoc(): String {
        val data = hashMapOf(
            "userId" to userId,
            "testId" to testId,
            "partId" to partId,
            "attemptNumber" to attemptNumber,
            "rawScore" to 0.0,
            "finalScore" to 0.0,
            "isPassed" to false,
            "status" to "in_progress",
            "timestamp" to FieldValue.serverTimestamp()
        )
        return db.collection("test_attempts").add(data).await().id
    }

    private suspend fun updateAttemptDoc(finalScore: Double, rawScore: Double) {
        attemptDocId?.let {
            db.collection("test_attempts").document(it).update(
                mapOf(
                    "rawScore" to rawScore,
                    "finalScore" to finalScore,
                    "manualAnswers" to manualAnswers,
                    "isPassed" to true,
                    "status" to "completed",
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private suspend fun updateBestGrade(finalScore: Double) {
        val snap = db.collection("test_grades")
            .whereEqualTo("userId", userId)
            .whereEqualTo("partId", partId)
            .get().await()

        if (snap.isEmpty) {
            db.collection("test_grades").add(
                hashMapOf(
                    "userId" to userId,
                    "testId" to testId,
                    "partId" to partId,
                    "bestScore" to finalScore,
                    "bestAttemptId" to attemptDocId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val current = snap.documents[0].getDouble("bestScore") ?: 0.0
            if (finalScore > current) {
                snap.documents[0].reference.update("bestScore", finalScore, "bestAttemptId", attemptDocId).await()
            }
        }
    }

    private suspend fun getAttemptsCount(): Int =
        db.collection("test_attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("partId", partId)
            .get().await().size()

    private suspend fun getPartLectionId(): String? =
        db.document("tests/$testId/parts/$partId").get().await().getString("lecId")

    private suspend fun getReadCount(lectionId: String): Int {
        val snap = db.collection("user_lections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("lectionId", lectionId)
            .get().await()
        return if (snap.isEmpty) 0 else (snap.documents[0].getLong("readCount") ?: 0).toInt()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}