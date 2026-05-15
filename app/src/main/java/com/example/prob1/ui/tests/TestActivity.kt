package com.example.prob1.ui.tests

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.prob1.data.repository.StudentScoreManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.prob1.data.Question
import com.example.prob1.databinding.ActivityTestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val db = Firebase.firestore

    private var partId: String? = null
    private var testId: String? = null
    private var questions: List<Question> = emptyList()
    private val questionRawData = mutableMapOf<String, Map<String, Any>>()

    private var isDebtTest = false

    private var debtCourseId: String? = null  // ID курса, по которому долг
    private var debtCourseName: String? = null  // Название курса-долга

    private lateinit var studentScoreManager: StudentScoreManager

    private var correctAnswers = 0
    private var rawScore = 0.0
    private var attemptNumber = 1

    var attemptDocId: String? = null
    var timer: CountDownTimer? = null

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var currentQuestionIndex = 0
    private var isTestCompleted = false
    private var isUSPTUTest = false

    private var testType: String = ""
    private var testMaxScore: Double = 20.0

    private var testSemester: Int = 1
    private var semesterMaxScore: Double = 0.0
    private var semesterTotalQuestions: Int = 0

    private val manualAnswers = mutableMapOf<String, String>()
    private val selectedAnswers = mutableMapOf<String, Boolean>()

    // Новые поля для 3-го типа
    private var isInputOnlyTest = false
    private var partType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ИНИЦИАЛИЗАЦИЯ ДО ВСЕГО!!!
        studentScoreManager = StudentScoreManager()

        partId = intent.getStringExtra("partId")
        testId = intent.getStringExtra("testId")
        isDebtTest = intent.getBooleanExtra("isDebtTest", false)

        // ЕСЛИ ЭТО ТЕСТ-ДОЛГ - ЗАГРУЖАЕМ ДАННЫЕ О ДОЛГЕ
        if (isDebtTest) {
            lifecycleScope.launch {
                loadDebtCourseInfo()
            }
        }

        lifecycleScope.launch {
            val uid = userId ?: return@launch
            val currentCourse = studentScoreManager.getCurrentCourseInfo(uid)
            Log.d("TestActivity", "Current course on start: $currentCourse")
            if (currentCourse != null) {
                studentScoreManager.syncStudentScoreWithCurrentCourse(uid)
            } else {
                Log.e("TestActivity", "COULD NOT FIND CURRENT COURSE FOR USER $uid")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TestActivity, "Ошибка: не удалось определить текущий курс. Обратитесь к преподавателю.", Toast.LENGTH_LONG).show()
                }
            }
        }

        if (partId == null || testId == null || userId == null) {
            Toast.makeText(this, "Ошибка: не переданы данные", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        checkTestAccess()
    }

    // ДОБАВЬТЕ НОВЫЙ МЕТОД ДЛЯ ЗАГРУЗКИ ДАННЫХ О ДОЛГЕ
    private suspend fun loadDebtCourseInfo() {
        try {
            val uid = userId ?: return
            Log.d("TestActivity", "=== LOAD DEBT COURSE INFO ===")

            val debtSnapshot = db.collection("dolg")
                .whereEqualTo("studentId", uid)
                .whereEqualTo("status", "active")
                .get()
                .await()

            // ИСПРАВЛЕНО: используем debtSnapshot.isEmpty (без скобок)
            if (!debtSnapshot.isEmpty) {
                val debtDoc = debtSnapshot.documents[0]
                debtCourseId = debtDoc.getString("courseId")
                debtCourseName = debtDoc.getString("courseName")
                Log.d("TestActivity", "Debt course loaded: $debtCourseId - $debtCourseName")
            } else {
                Log.e("TestActivity", "No active debt found for user $uid")
            }
        } catch (e: Exception) {
            Log.e("TestActivity", "Error loading debt course info", e)
        }
    }

    private fun checkTestAccess() {
        lifecycleScope.launch {
            loadTestType()
            loadPartType()

            // Если это тест-долг - пропускаем проверку лекции
            if (isDebtTest) {
                Log.d("TestActivity", "Debt test - skipping lecture check")
                proceedToTest()
                return@launch
            }

            // Для 3-го типа (input) проверяем, можно ли проходить
            if (isInputOnlyTest) {
                if (!canTakeInputTest()) {
                    Toast.makeText(
                        this@TestActivity,
                        "Вы уже отправили ответ на проверку. Дождитесь оценки преподавателя.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }
                attemptNumber = getAttemptsCount() + 1
                showRulesDialog()
                return@launch
            }

            // Для 1 и 2 типа проверяем лекции
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
        loadTestSettings()
        showRulesDialog()
    }

    private suspend fun loadTestType() {
        val testDoc = db.collection("tests")
            .document(testId!!)
            .get()
            .await()

        testType = testDoc.getString("type") ?: ""
        isInputOnlyTest = testType == "input"
    }

    private suspend fun loadPartType() {
        val partDoc = db.collection("tests")
            .document(testId!!)
            .collection("parts")
            .document(partId!!)
            .get()
            .await()

        partType = partDoc.getString("type")
    }

    fun getPartType(): String? = partType

    private suspend fun canTakeInputTest(): Boolean {
        val attempts = db.collection("test_attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("testId", testId)
            .whereEqualTo("partId", partId)
            .get()
            .await()

        // Если есть попытка, которая ещё не проверена
        val hasPendingReview = attempts.documents.any { doc ->
            doc.getString("status") == "pending_review"
        }

        // Если есть успешно проверенная попытка
        val hasApproved = attempts.documents.any { doc ->
            doc.getBoolean("isPassed") == true
        }

        return !hasPendingReview && !hasApproved
    }

    private suspend fun loadTestSettings() {
        val testDoc = db.collection("tests")
            .document(testId!!)
            .get()
            .await()

        testType = testDoc.getString("type") ?: ""

        testMaxScore = when {
            testDoc.getLong("max") != null -> testDoc.getLong("max")!!.toDouble()
            testDoc.getDouble("max") != null -> testDoc.getDouble("max")!!
            testDoc.getLong("totalScore") != null -> testDoc.getLong("totalScore")!!.toDouble()
            testDoc.getDouble("totalScore") != null -> testDoc.getDouble("totalScore")!!
            else -> 20.0
        }

        testSemester = testDoc.getLong("semester")?.toInt() ?: 1

        val semesterDoc = db.collection("semester_question_stats")
            .document("semester_$testSemester")
            .get()
            .await()

        semesterMaxScore = when {
            semesterDoc.getLong("max") != null -> semesterDoc.getLong("max")!!.toDouble()
            semesterDoc.getDouble("max") != null -> semesterDoc.getDouble("max")!!
            else -> 0.0
        }

        semesterTotalQuestions = semesterDoc.getLong("totalQuestions")?.toInt() ?: 0

        Log.d("TEST_SETTINGS", "type=$testType testMax=$testMaxScore semester=$testSemester")
    }

    private suspend fun applyFinalCoefficients(baseScore: Double): Double {
        val deadlineCoefficient = getDeadlineCoefficient()
        val attemptCoefficient = getAttemptCoefficient()

        val finalScore = baseScore * deadlineCoefficient * attemptCoefficient

        Log.d("SCORE_COEFFICIENTS", "base=$baseScore deadlineCoef=$deadlineCoefficient attemptCoef=$attemptCoefficient final=$finalScore")

        return finalScore
    }

    private fun getAttemptCoefficient(): Double {
        val coefficient = 1.0 - ((attemptNumber - 1) * 0.2)
        return max(0.2, coefficient)
    }

    private suspend fun getDeadlineCoefficient(): Double {
        // Если это тест из долга - не применяем коэффициент срока сдачи
        if (isDebtTest) {
            Log.d("TestActivity", "Debt test - deadline coefficient = 1.0")
            return 1.0
        }

        val uid = userId ?: return 1.0
        val currentTestId = testId ?: return 1.0

        val groupSnapshot = db.collection("usersgroup")
            .whereEqualTo("userId", uid)
            .limit(1)
            .get()
            .await()

        if (groupSnapshot.isEmpty) {
            return 1.0
        }

        val groupId = groupSnapshot.documents[0].getString("groupId") ?: return 1.0

        val deadlineSnapshot = db.collection("deadlines")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("testId", currentTestId)
            .limit(1)
            .get()
            .await()

        if (deadlineSnapshot.isEmpty) {
            return 1.0
        }

        val deadlineString = deadlineSnapshot.documents[0].getString("deadline") ?: return 1.0
        val deadlineDate = parseDate(deadlineString) ?: return 1.0

        val today = startOfDay(Date())
        val deadline = startOfDay(deadlineDate)

        val diffMillis = today.time - deadline.time
        val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffDays < 0 -> 1.2
            diffDays == 0L -> 1.0
            else -> {
                val weeksLate = kotlin.math.ceil(diffDays / 7.0).toInt()
                val coefficient = 1.0 - (weeksLate * 0.2)
                max(0.2, coefficient)
            }
        }
    }

    private fun parseDate(value: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value)
        } catch (e: Exception) {
            null
        }
    }

    private fun startOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun showRulesDialog() {
        val message = if (isInputOnlyTest) {
            "• Введите ваш ответ в текстовое поле\n• Ответ будет отправлен на проверку преподавателю\n• Дождитесь оценки"
        } else {
            "• 1 минута на вопрос\n• Нельзя выходить из приложения"
        }

        AlertDialog.Builder(this)
            .setTitle("Правила теста")
            .setMessage(message)
            .setPositiveButton("Начать") { _, _ -> loadTest() }
            .setNegativeButton("Назад") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun loadTest() {
        lifecycleScope.launch {
            try {
                var snapshot = db.collection("tests")
                    .document(testId!!)
                    .collection("parts")
                    .document(partId!!)
                    .collection("questions")
                    .get()
                    .await()

                if (snapshot.isEmpty && partId == testId) {
                    snapshot = db.collection("tests")
                        .document(testId!!)
                        .collection("questions")
                        .get()
                        .await()
                }

                questionRawData.clear()
                val loadedQuestions = mutableListOf<Question>()
                var hasManualInput = false

                val sortedDocuments = snapshot.documents.sortedBy {
                    it.getLong("num") ?: 0L
                }

                sortedDocuments.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    questionRawData[doc.id] = data

                    val isManualInput = data["isManualInput"] as? Boolean ?: false
                    if (isManualInput) hasManualInput = true

                    val answersList = if (!isManualInput) {
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
                isUSPTUTest = hasManualInput || isInputOnlyTest

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
                if (!isInputOnlyTest) {
                    startQuestionTimerIfNeeded()
                }
            }
        })
    }

    fun onAnswerSelected(isCorrect: Boolean) {
        if (isCorrect) {
            correctAnswers++
        }
        selectedAnswers[questions[currentQuestionIndex].id] = isCorrect
    }

    fun onManualAnswerSubmitted(questionId: String, answer: String) {
        Log.d("TestActivity", "onManualAnswerSubmitted - questionId: $questionId, answer: $answer")
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
        if (isInputOnlyTest) return

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

    fun finishTest() {
        if (isTestCompleted) return

        isTestCompleted = true
        timer?.cancel()

        lifecycleScope.launch {
            try {
                when {
                    isInputOnlyTest -> finishInputTest()
                    testType == "complex" -> finishComplexTest()
                    else -> finishDefaultTest()
                }
            } catch (e: Exception) {
                Log.e("TestActivity", "Error finishing test", e)
                Toast.makeText(this@TestActivity, "Ошибка завершения теста: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showWaitingForReviewDialog() {
        AlertDialog.Builder(this)
            .setTitle("Ответ отправлен")
            .setMessage("Ваш ответ отправлен на проверку преподавателю.\n\nОжидайте оценки. После проверки вы сможете увидеть результат в списке тестов.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private suspend fun finishInputTest() {
        val userAnswer = manualAnswers.values.firstOrNull() ?: ""
        val questionId = manualAnswers.keys.firstOrNull() ?: ""

        Log.d("TestActivity", "=== FINISH INPUT TEST ===")
        Log.d("TestActivity", "userAnswer: $userAnswer")
        Log.d("TestActivity", "questionId: $questionId")
        Log.d("TestActivity", "attemptDocId: $attemptDocId")
        Log.d("TestActivity", "isDebtTest: $isDebtTest")

        val questionRaw = questionRawData[questionId]
        val questionText = questionRaw?.get("text") as? String ?: ""

        Log.d("TestActivity", "questionText from rawData: $questionText")

        attemptDocId?.let { attemptId ->
            val updates = hashMapOf<String, Any>(
                "status" to "pending_review",
                "isPassed" to false,
                "needsTeacherReview" to true,
                "isManual" to true,
                "manualAnswer" to userAnswer,
                "manualQuestionId" to questionId,
                "questionText" to questionText,
                "testId" to testId!!,
                "partId" to partId!!,
                "userId" to userId!!,
                "finalScore" to 0.0,
                "rawScore" to 0.0,
                "attemptNumber" to attemptNumber,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("test_attempts").document(attemptId).update(updates).await()
            Log.d("TestActivity", "Updated attempt $attemptId with answer: $userAnswer")
        }

        if (isDebtTest) {
            Log.d("TestActivity", "Updating total score for debt test")
            updateStudentTotalScore()
        }

        showWaitingForReviewDialog()
    }

    private suspend fun finishComplexTest() {
        val raw = calculateComplexRawScore()
        val maxPossibleRaw = calculateComplexMaxPossibleRawScore()

        val baseScore = if (maxPossibleRaw > 0) {
            raw * (testMaxScore / maxPossibleRaw)
        } else {
            0.0
        }

        val finalScore = applyFinalCoefficients(baseScore)

        addCoinsForFirstTestPassing()
        updateAttemptDoc(finalScore, raw)
        updateBestGrade(finalScore)
        updateStudentCourseScore()
        updateStudentTotalScore()

        showComplexResult(
            raw = raw.toInt(),
            maxRaw = maxPossibleRaw,
            final = finalScore.toInt(),
            maxFinal = testMaxScore.toInt()
        )
    }

    private suspend fun finishDefaultTest() {
        if (isUSPTUTest) {
            rawScore = 0.0

            selectedAnswers.forEach { (_, isCorrect) ->
                if (isCorrect) rawScore += 1
            }

            manualAnswers.forEach { (qId, userAnswer) ->
                val raw = questionRawData[qId] ?: return@forEach
                val isManualInput = raw["isManualInput"] as? Boolean ?: false

                if (isManualInput) {
                    val questionType = raw["questionType"] as? String ?: "manual_text"

                    when (questionType) {
                        "spinner_sequence", "spinner_matching" -> {
                            val correctSeq = raw["correctSequence"] as? String ?: ""
                            val maxPoints = getIntFromAny(raw["maxPoints"], correctSeq.split(",").filter { it.isNotBlank() }.size)
                            rawScore += calculatePartialScore(userAnswer, correctSeq, maxPoints)
                        }
                        else -> {
                            Log.d("TestActivity", "Manual text answer for $qId: $userAnswer (waiting for teacher review)")
                        }
                    }
                }
            }

            val maxPossibleRaw = calculateComplexMaxPossibleRawScore()

            val baseScore = if (maxPossibleRaw > 0) {
                rawScore / maxPossibleRaw * testMaxScore
            } else {
                0.0
            }
            val finalScore = applyFinalCoefficients(baseScore)

            addCoinsForFirstTestPassing()
            updateAttemptDoc(finalScore, rawScore)
            updateBestGrade(finalScore)
            updateStudentCourseScore()
            updateStudentTotalScore()

            showComplexResult(
                raw = rawScore.toInt(),
                maxRaw = maxPossibleRaw,
                final = finalScore.toInt(),
                maxFinal = testMaxScore.toInt()
            )
        } else {
            val raw = correctAnswers.toDouble()

            val baseScore = if (semesterTotalQuestions > 0 && semesterMaxScore > 0.0) {
                raw * (semesterMaxScore / semesterTotalQuestions)
            } else {
                raw
            }

            val finalScore = applyFinalCoefficients(baseScore)

            addCoinsForFirstTestPassing()
            updateAttemptDoc(finalScore, raw)
            updateBestGrade(finalScore)
            updateStudentCourseScore()

            showSemesterScaledResult(
                raw = raw.toInt(),
                final = finalScore,
                semesterMax = semesterMaxScore,
                semesterTotal = semesterTotalQuestions
            )
        }
    }

    private fun showSemesterScaledResult(raw: Int, final: Double, semesterMax: Double, semesterTotal: Int) {
        val msg = """
            Балл за тест: ${"%.2f".format(final)}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Тест завершён")
            .setMessage(msg)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun calculateComplexRawScore(): Double {
        var score = 0.0

        selectedAnswers.forEach { (_, isCorrect) ->
            if (isCorrect) score += 1.0
        }

        manualAnswers.forEach { (questionId, userAnswer) ->
            val raw = questionRawData[questionId] ?: return@forEach
            val isManualInput = raw["isManualInput"] as? Boolean ?: false

            if (isManualInput) {
                val correctSequence = raw["correctSequence"] as? String ?: ""
                val maxPoints = getIntFromAny(
                    raw["maxPoints"],
                    correctSequence.split(",").filter { it.isNotBlank() }.size
                )

                score += calculatePartialScore(userAnswer, correctSequence, maxPoints)
            }
        }

        return score
    }

    private fun calculateComplexMaxPossibleRawScore(): Int {
        var maxScore = 0

        questions.forEach { question ->
            val raw = questionRawData[question.id]
            val isManualInput = raw?.get("isManualInput") as? Boolean ?: false

            maxScore += if (isManualInput) {
                val correctSequence = raw?.get("correctSequence") as? String ?: ""
                getIntFromAny(
                    raw?.get("maxPoints"),
                    correctSequence.split(",").filter { it.isNotBlank() }.size
                )
            } else {
                1
            }
        }

        return maxScore
    }

    private suspend fun addCoinsForFirstTestPassing() {
        val uid = userId ?: return
        val currentTestId = testId ?: return
        val currentPartId = partId ?: return

        val previousCompletedAttempts = db.collection("test_attempts")
            .whereEqualTo("userId", uid)
            .whereEqualTo("testId", currentTestId)
            .whereEqualTo("partId", currentPartId)
            .whereEqualTo("status", "completed")
            .get()
            .await()

        if (!previousCompletedAttempts.isEmpty) return

        db.collection("users")
            .document(uid)
            .update("coins", FieldValue.increment(5))
            .await()
    }

    private fun calculatePartialScore(userInput: String, correct: String, maxPoints: Int): Int {
        if (userInput.isBlank() || correct.isBlank()) return 0

        val userList = userInput
            .replace(" ", "")
            .split(",")
            .map { it.uppercase() }

        val correctList = correct
            .replace(" ", "")
            .split(",")
            .map { it.uppercase() }

        var matches = 0

        for (i in correctList.indices) {
            if (i < userList.size && userList[i] == correctList[i]) {
                matches++
            }
        }

        return matches.coerceAtMost(maxPoints)
    }

    fun getTestType(): String? = testType

    private fun showComplexResult(raw: Int, maxRaw: Int, final: Int, maxFinal: Int) {
        val msg = """
                        
            Итоговый балл: $final из $maxFinal
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Тест завершён")
            .setMessage(msg)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    fun stopTimer() {
        timer?.cancel()
        timer = null
        binding.timerText.text = "00:00"
    }

    private fun getIntFromAny(value: Any?, default: Int): Int {
        return when (value) {
            is Long -> value.toInt()
            is Int -> value
            is Double -> value.toInt()
            is Number -> value.toInt()
            else -> default
        }
    }

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
            "isManual" to isInputOnlyTest,
            "status" to if (isInputOnlyTest) "pending_review" else "in_progress",
            "timestamp" to FieldValue.serverTimestamp()
        )

        if (isInputOnlyTest) {
            try {
                val testDoc = db.collection("tests").document(testId!!).get().await()
                val testTitle = testDoc.getString("title") ?: "Тест"

                val partDoc = db.collection("tests")
                    .document(testId!!)
                    .collection("parts")
                    .document(partId!!)
                    .get()
                    .await()
                val partTitle = partDoc.getString("title") ?: "Часть"

                data["testTitle"] = testTitle
                data["partTitle"] = partTitle
            } catch (e: Exception) {
                Log.e("TestActivity", "Error loading titles", e)
            }
        }

        return db.collection("test_attempts").add(data).await().id
    }

    private suspend fun updateAttemptDoc(finalScore: Double, rawScore: Double) {
        attemptDocId?.let {
            db.collection("test_attempts").document(it).update(
                mapOf(
                    "rawScore" to rawScore,
                    "finalScore" to finalScore,
                    "manualAnswers" to manualAnswers,
                    "selectedAnswers" to selectedAnswers,
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
            .get()
            .await()

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
                snap.documents[0].reference.update(
                    "bestScore", finalScore,
                    "bestAttemptId", attemptDocId
                ).await()
            }
        }
    }

    // ОБНОВЛЕННЫЙ МЕТОД updateStudentTotalScore
    // TestActivity.kt - исправленный метод updateStudentTotalScore

    private suspend fun updateStudentTotalScore() {
        try {
            val uid = userId ?: return
            Log.d("TestActivity", "=== updateStudentTotalScore START ===")
            Log.d("TestActivity", "isDebtTest: $isDebtTest")

            // Определяем, для какого курса обновляем баллы
            val targetCourseId: String
            val targetCourseName: String

            if (isDebtTest && debtCourseId != null) {
                targetCourseId = debtCourseId!!
                targetCourseName = debtCourseName ?: "Курс"
                Log.d("TestActivity", "Updating score for DEBT course: $targetCourseId")
            } else {
                val currentCourse = studentScoreManager.getCurrentCourseInfo(uid)
                if (currentCourse == null) {
                    Log.e("TestActivity", "Could not get current course info")
                    return
                }
                targetCourseId = currentCourse.courseId
                targetCourseName = currentCourse.courseName
                Log.d("TestActivity", "Updating score for CURRENT course: $targetCourseId")
            }

            // Получаем список тестов для этого курса
            val courseTests = db.collection("test_course")
                .whereEqualTo("courseId", targetCourseId)
                .get()
                .await()

            val testIdsInCourse = courseTests.documents.mapNotNull { it.getString("testId") }.toSet()
            Log.d("TestActivity", "Tests in course $targetCourseId: $testIdsInCourse")

            // Получаем все лучшие результаты студента
            val gradesSnapshot = db.collection("test_grades")
                .whereEqualTo("userId", uid)
                .get()
                .await()

            // Суммируем баллы только за тесты этого курса
            var totalScore = 0.0
            for (doc in gradesSnapshot.documents) {
                val testId = doc.getString("testId")
                if (testId != null && testId in testIdsInCourse) {
                    val bestScore = doc.getDouble("bestScore") ?: 0.0
                    totalScore += bestScore
                    Log.d("TestActivity", "Added bestScore: $bestScore from test: $testId")
                }
            }

            Log.d("TestActivity", "Total score for course $targetCourseId: $totalScore")

            // Сохраняем в student_course_scores
            val docId = "${uid}_${targetCourseId}"
            val data = hashMapOf(
                "userId" to uid,
                "totalScore" to totalScore,
                "courseId" to targetCourseId,
                "courseName" to targetCourseName,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val userDoc = db.collection("users").document(uid).get().await()
            data["firstName"] = userDoc.getString("name") ?: ""
            data["lastName"] = userDoc.getString("surname") ?: ""

            db.collection("student_course_scores")
                .document(docId)
                .set(data)
                .await()

            Log.d("TestActivity", "Successfully updated total score to $totalScore for course $targetCourseId")

        } catch (e: Exception) {
            Log.e("TestActivity", "Failed to update student total score", e)
        }
    }

    // НОВЫЙ МЕТОД ДЛЯ ОБНОВЛЕНИЯ avgScore В КОЛЛЕКЦИИ dolg
    // TestActivity.kt - полностью исправленный метод updateDebtAvgScore

    // TestActivity.kt - исправленный метод updateDebtAvgScore

    private suspend fun updateDebtAvgScore(uid: String, newTotalScore: Double) {
        try {
            Log.d("TestActivity", "=== UPDATE DEBT AVG SCORE ===")
            Log.d("TestActivity", "newTotalScore passed: $newTotalScore")
            Log.d("TestActivity", "debtCourseId: $debtCourseId")

            // Получаем актуальный totalScore из student_course_scores
            val docId = "${uid}_${debtCourseId}"
            val scoreDoc = db.collection("student_course_scores")
                .document(docId)
                .get()
                .await()

            val actualTotalScore = if (scoreDoc.exists()) {
                scoreDoc.getDouble("totalScore") ?: 0.0
            } else {
                newTotalScore
            }

            Log.d("TestActivity", "Actual totalScore from student_course_scores: $actualTotalScore")

            // Находим активный долг пользователя
            val debtSnapshot = db.collection("dolg")
                .whereEqualTo("studentId", uid)
                .whereEqualTo("status", "active")
                .get()
                .await()

            if (!debtSnapshot.isEmpty) {
                val debtDoc = debtSnapshot.documents[0]

                Log.d("TestActivity", "Before update - avgScore: ${debtDoc.getDouble("avgScore")}")

                // Обновляем avgScore в dolg (берем актуальное значение из student_course_scores)
                val updates = hashMapOf<String, Any>(
                    "avgScore" to actualTotalScore,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                // Проверяем, достиг ли студент проходного балла
                val courseDoc = db.collection("courses")
                    .document(debtCourseId!!)
                    .get()
                    .await()
                val minPassScore = courseDoc.getDouble("min3") ?: 40.0

                if (actualTotalScore >= minPassScore) {
                    updates["status"] = "resolved"
                    updates["resolvedAt"] = FieldValue.serverTimestamp()
                    Log.d("TestActivity", "Debt resolved! Score $actualTotalScore >= $minPassScore")
                }

                debtDoc.reference.update(updates).await()
                Log.d("TestActivity", "Updated debt: avgScore=$actualTotalScore, status=${updates["status"] ?: "active"}")

            } else {
                Log.e("TestActivity", "No active debt found for user $uid")
            }
        } catch (e: Exception) {
            Log.e("TestActivity", "Error updating debt avgScore", e)
        }
    }

    // ОБНОВЛЕННЫЙ МЕТОД updateStudentCourseScore
    private suspend fun updateStudentCourseScore() {
        try {
            val uid = userId ?: return
            Log.d("TestActivity", "=== updateStudentCourseScore START ===")
            Log.d("TestActivity", "isDebtTest: $isDebtTest")

            val targetCourseId: String
            val targetCourseName: String

            if (isDebtTest && debtCourseId != null) {
                targetCourseId = debtCourseId!!
                targetCourseName = debtCourseName ?: "Курс"
                Log.d("TestActivity", "Updating for DEBT course: $targetCourseId")
            } else {
                val currentCourse = studentScoreManager.getCurrentCourseInfo(uid)
                if (currentCourse == null) {
                    Log.e("TestActivity", "currentCourse is NULL")
                    return
                }
                targetCourseId = currentCourse.courseId
                targetCourseName = currentCourse.courseName
                Log.d("TestActivity", "Updating for CURRENT course: $targetCourseId")
            }

            val userDoc = db.collection("users").document(uid).get().await()
            val firstName = userDoc.getString("name") ?: ""
            val lastName = userDoc.getString("surname") ?: ""

            val testCourseSnapshot = db.collection("test_course")
                .whereEqualTo("courseId", targetCourseId)
                .get()
                .await()

            val testIdsInCourse = testCourseSnapshot.documents.mapNotNull { it.getString("testId") }.toSet()

            val gradesSnapshot = db.collection("test_grades")
                .whereEqualTo("userId", uid)
                .get()
                .await()

            var totalScore = 0.0
            for (doc in gradesSnapshot.documents) {
                val testId = doc.getString("testId")
                if (testId != null && testId in testIdsInCourse) {
                    val bestScore = doc.getDouble("bestScore") ?: 0.0
                    totalScore += bestScore
                    Log.d("TestActivity", "Added bestScore: $bestScore from test: $testId")
                }
            }

            Log.d("TestActivity", "Total score calculated: $totalScore")

            val docId = "${uid}_${targetCourseId}"
            val data = hashMapOf(
                "userId" to uid,
                "firstName" to firstName,
                "lastName" to lastName,
                "totalScore" to totalScore,
                "courseId" to targetCourseId,
                "courseName" to targetCourseName,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("student_course_scores")
                .document(docId)
                .set(data)
                .await()

            Log.d("TestActivity", "Successfully saved to student_course_scores")

        } catch (e: Exception) {
            Log.e("TestActivity", "Exception in updateStudentCourseScore", e)
        }
    }

    private suspend fun getAttemptsCount(): Int =
        db.collection("test_attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("partId", partId)
            .get()
            .await()
            .size()

    private suspend fun getPartLectionId(): String? =
        db.document("tests/$testId/parts/$partId")
            .get()
            .await()
            .getString("lecId")

    private suspend fun getReadCount(lectionId: String): Int {
        val snap = db.collection("user_lections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("lectionId", lectionId)
            .get()
            .await()

        return if (snap.isEmpty) {
            0
        } else {
            (snap.documents[0].getLong("readCount") ?: 0).toInt()
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}