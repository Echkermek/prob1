package com.example.prob1.ui.tests

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.prob1.LectionWebViewActivity
import com.example.prob1.R
import com.example.prob1.data.Question
import com.example.prob1.databinding.ActivityTestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import java.text.SimpleDateFormat
import java.util.*

class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val db = Firebase.firestore
    private var partId: String? = null
    private var testId: String? = null
    private var questions: List<Question> = emptyList()
    private var correctAnswers = 0
    private var attemptNumber = 1
    private var isTestPassed = false
    var attemptDocId: String? = null
    var timer: CountDownTimer? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var currentQuestionIndex = 0
    private var isTestCompleted = false
    private var lectionId: String? = null
    private var isManualTest: Boolean = false
    private var deadlineCoefficient = 1.0 // Коэффициент срока сдачи

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        partId = intent.getStringExtra("partId")
        testId = intent.getStringExtra("testId")
        isManualTest = intent.getBooleanExtra("isManual", false)

        if (partId == null || testId == null || userId == null) {
            Toast.makeText(this, "Ошибка: не переданы данные", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        checkTestAccess()
    }

    private fun checkTestAccess() {
        lifecycleScope.launch {
            // Для ручных тестов не проверяем лекции
            if (isManualTest) {
                attemptNumber = getAttemptsCount() + 1
                calculateDeadlineCoefficient() // Рассчитываем коэффициент для ручных тестов
                showRulesDialog()
                return@launch
            }

            // Для обычных тестов проверяем лекцию
            val lecId = getPartLectionId() ?: run {
                Toast.makeText(this@TestActivity, "Лекция не найдена", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            if (lecId.isEmpty()) {
                Toast.makeText(this@TestActivity, "Лекция не указана", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val attempts = getAttemptsCount()
            val readCount = getReadCount(lecId)

            if (readCount <= attempts) {
                Toast.makeText(this@TestActivity, "Прочитайте лекцию сначала", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            attemptNumber = attempts + 1
            calculateDeadlineCoefficient() // Рассчитываем коэффициент для обычных тестов
            showRulesDialog()
        }
    }

    private suspend fun calculateDeadlineCoefficient() {
        try {
            // Получаем группу пользователя
            val userGroupDoc = db.collection("usersgroup")
                .whereEqualTo("userId", userId)
                .get().await()

            if (userGroupDoc.isEmpty) {
                deadlineCoefficient = 1.0
                return
            }

            val groupId = userGroupDoc.documents[0].getString("groupId") ?: return

            // Ищем дедлайн для этой группы и теста
            val deadlineDoc = db.collection("deadlines")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("testId", testId)
                .get().await()

            if (deadlineDoc.isEmpty) {
                deadlineCoefficient = 1.0 // Нет дедлайна - стандартный коэффициент
                return
            }

            val deadlineStr = deadlineDoc.documents[0].getString("deadline") ?: return
            val deadlineDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(deadlineStr)
            val currentDate = Date()

            if (deadlineDate == null) {
                deadlineCoefficient = 1.0
                return
            }

            // Вычисляем разницу в неделях (округление в большую сторону)
            val diffInMillis = currentDate.time - deadlineDate.time
            val diffInWeeks = Math.ceil(diffInMillis / (7.0 * 24 * 60 * 60 * 1000)).toInt()

            // Применяем коэффициенты
            deadlineCoefficient = when {
                diffInWeeks < 0 -> 1.2 // До дедлайна
                diffInWeeks == 0 -> 1.0 // Неделя дедлайна
                else -> max(1.0 - (diffInWeeks * 0.2), 0.2) // После дедлайна
            }

            Log.d("TestActivity", "Deadline coefficient: $deadlineCoefficient, diffInWeeks: $diffInWeeks, deadline: $deadlineStr, current: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)}")

        } catch (e: Exception) {
            Log.e("TestActivity", "Error calculating deadline coefficient", e)
            deadlineCoefficient = 1.0
        }
    }

    private fun showRulesDialog() {
        val message = if (isManualTest) {
            """
                • Ответы отправляются в виде ссылки на ваш ответ
                • Преподаватель оценит ответ и выставит баллы
                ${if (deadlineCoefficient != 1.0) "• Коэффициент срока сдачи: ${"%.1f".format(deadlineCoefficient)}" else ""}
            """.trimIndent()
        } else {
            """
                • 1 минута на вопрос 
                • Коэффициент попытки уменьшается с каждой новой попыткой
                ${if (deadlineCoefficient != 1.0) "• Коэффициент срока сдачи: ${"%.1f".format(deadlineCoefficient)}" else ""}
                • Нельзя сворачиваться или выходить
                • +5 монет — только за первую успешную попытку
            """.trimIndent()
        }

        AlertDialog.Builder(this)
            .setTitle("Правила ")
            .setMessage(message)
            .setPositiveButton("Начать") { _, _ -> loadTest() }
            .setNegativeButton("Назад") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun loadTest() {
        lifecycleScope.launch {
            val partDoc = db.document("tests/$testId/parts/$partId").get().await()
            val isManualPart = partDoc.getBoolean("enterAnswer") ?: false

            val snap = db.collection("tests/$testId/parts/$partId/questions").get().await()
            questions = snap.documents.mapNotNull { doc ->
                Question(
                    id = doc.id,
                    text = doc.getString("text") ?: "",
                    content = doc.getString("content") ?: "",
                    answers = if (!isManualPart) {
                        doc.get("answers")?.let { answers ->
                            (answers as? List<Map<String, Any>>)?.map { answerMap ->
                                com.example.prob1.data.Answer(
                                    text = answerMap["text"] as? String ?: "",
                                    isCorrect = answerMap["isCorrect"] as? Boolean ?: false
                                )
                            } ?: emptyList()
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                )
            }

            if (questions.isEmpty()) {
                Toast.makeText(this@TestActivity, "Вопросы не найдены", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            attemptDocId = createAttemptDoc()
            setupViewPager(isManualPart)
        }
    }

    private fun setupViewPager(isManual: Boolean) {
        binding.testViewPager.adapter = TestPagerAdapter(
            this,
            questions,
            partId!!,
            testId!!,
            isManual
        )

        binding.testViewPager.isUserInputEnabled = false

        binding.testViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentQuestionIndex = position
                if (!isManual) {
                    // Небольшая задержка для гарантии инициализации фрагмента
                    binding.timerText.postDelayed({
                        startQuestionTimer()
                    }, 100)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                // Отменяем таймер при начале скролла
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    timer?.cancel()
                }
            }
        })

        // Запускаем таймер для первого вопроса
        if (!isManual) {
            startQuestionTimer()
        }
    }

    fun startQuestionTimer() {
        if (isManualTest) return

        timer?.cancel()

        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                val timeText = String.format("00:%02d", seconds)
                binding.timerText.text = timeText
            }

            override fun onFinish() {
                binding.timerText.text = "00:00"
                moveToNextQuestionOrFinish()
            }
        }.start()
    }

    private fun moveToNextQuestionOrFinish() {
        if (currentQuestionIndex < questions.size - 1) {
            binding.testViewPager.currentItem = currentQuestionIndex + 1
        } else {
            finishTest()
        }
    }

    fun onAnswerSelected(isCorrect: Boolean) {
        if (isCorrect) correctAnswers++
        timer?.cancel()

        moveToNextQuestionOrFinish()
    }

    fun finishTest() {
        isTestCompleted = true

        if (isManualTest) {
            AlertDialog.Builder(this)
                .setTitle("Тест завершен")
                .setMessage("Ваши ответы отправлены на проверку. Ожидайте оценку от преподавателя.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else {
            calculateAndShowResults("Тест завершен")
        }
    }

    fun onManualAnswerSaved() {
        // Можно добавить дополнительную логику при сохранении ручного ответа
        Log.d("TestActivity", "Manual answer saved")
    }

    private fun calculateAndShowResults(title: String) {
        timer?.cancel()

        // РАСЧЕТ БАЛЛОВ С КОЭФФИЦИЕНТАМИ ПОПЫТОК И СРОКОВ СДАЧИ
        val rawScore = correctAnswers.toDouble()
        val attemptCoeff = max(1.0 - (attemptNumber - 1) * 0.2, 0.2)

        // ОБЩИЙ КОЭФФИЦИЕНТ = коэффициент попытки × коэффициент срока сдачи
        val totalCoefficient = attemptCoeff * deadlineCoefficient
        val finalScore = rawScore * totalCoefficient

        isTestPassed = true

        if (attemptNumber == 1 && correctAnswers > 0) addCoins(5)

        lifecycleScope.launch {
            updateAttemptDoc(finalScore, attemptCoeff, deadlineCoefficient)
            updateBestGrade(finalScore)
            showResult(title, finalScore, attemptCoeff, deadlineCoefficient, totalCoefficient)
        }
    }

    private suspend fun updateAttemptDoc(finalScore: Double, attemptCoeff: Double, deadlineCoeff: Double) {
        attemptDocId?.let { id ->
            db.collection("test_attempts").document(id).update(
                mapOf(
                    "correctAnswers" to correctAnswers,
                    "rawScore" to correctAnswers.toDouble(),
                    "attemptCoeff" to attemptCoeff,
                    "deadlineCoeff" to deadlineCoeff,
                    "finalScore" to finalScore,
                    "isPassed" to isTestPassed,
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
            val currentBest = snap.documents[0].getDouble("bestScore") ?: 0.0
            if (finalScore > currentBest) {
                snap.documents[0].reference.update(
                    mapOf(
                        "bestScore" to finalScore,
                        "bestAttemptId" to attemptDocId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    private fun showResult(title: String, finalScore: Double, attemptCoeff: Double, deadlineCoeff: Double, totalCoefficient: Double) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("""
                Правильных ответов: $correctAnswers из ${questions.size}
                Коэффициент попытки: ${"%.1f".format(attemptCoeff)}
                ${if (deadlineCoeff != 1.0) "Коэффициент срока: ${"%.1f".format(deadlineCoeff)}" else ""}
                Общий коэффициент: ${"%.1f".format(totalCoefficient)}
                Итоговый балл: ${"%.1f".format(finalScore)}
            """.trimIndent())
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private suspend fun createAttemptDoc(): String {
        val attemptData = hashMapOf(
            "userId" to userId,
            "testId" to testId,
            "partId" to partId,
            "attemptNumber" to attemptNumber,
            "correctAnswers" to 0,
            "totalQuestions" to questions.size,
            "rawScore" to 0.0,
            "attemptCoeff" to 0.0,
            "deadlineCoeff" to deadlineCoefficient,
            "finalScore" to 0.0,
            "isPassed" to false,
            "status" to "in_progress",
            "interrupted" to false,
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Для ручных тестов добавляем дополнительные поля
        if (isManualTest) {
            attemptData["isManual"] = true
            attemptData["manualAnswer"] = ""
            attemptData["manualQuestionId"] = ""
        }

        val ref = db.collection("test_attempts").add(attemptData).await()
        return ref.id
    }

    private suspend fun getAttemptsCount(): Int {
        return db.collection("test_attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("partId", partId)
            .get().await().size()
    }

    private suspend fun getPartLectionId(): String? {
        val doc = db.document("tests/$testId/parts/$partId").get().await()
        val lecId = doc.getString("lecId")
        Log.d("TestActivity", "For partId: $partId, testId: $testId, found lecId: $lecId")
        return lecId
    }

    private suspend fun getReadCount(lectionId: String): Int {
        val snap = db.collection("user_lections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("lectionId", lectionId)
            .get().await()
        return if (snap.isEmpty) 0 else (snap.documents[0].getLong("readCount") ?: 0).toInt()
    }

    private fun addCoins(amount: Int) {
        db.collection("user_coins")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    snap.documents[0].reference.update("coins", FieldValue.increment(amount.toLong()))
                } else {
                    db.collection("user_coins").add(
                        hashMapOf(
                            "userId" to userId,
                            "coins" to amount
                        )
                    )
                }
            }
    }

    override fun onResume() {
        super.onResume()
        // Перезапускаем таймер при возвращении на активность
        if (!isTestCompleted && !isManualTest && ::binding.isInitialized) {
            startQuestionTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel() // Отменяем таймер при паузе
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        AlertDialog.Builder(this)
            .setTitle("Выход из теста")
            .setMessage("Тест будет завершен с текущими результатами. Продолжить?")
            .setPositiveButton("Да") { _, _ ->
                calculateAndShowResults("Тест прерван")
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}