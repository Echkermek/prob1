package com.example.prob1.ui.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.databinding.ActivityManualAnswersBinding
import com.example.prob1.databinding.DialogRateAnswerBinding
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.CoroutineContext

class ManualAnswersActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityManualAnswersBinding
    private val db = Firebase.firestore
    private var selectedGroupId: String? = null
    private lateinit var groupsAdapter: ArrayAdapter<String>
    private val groups = mutableListOf<String>()
    private val groupIds = mutableListOf<String>()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupRecyclerView()
        loadGroups()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun setupSpinner() {
        groupsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groups)
        groupsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.groupsSpinner.adapter = groupsAdapter

        binding.groupsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGroupId = groupIds[position]
                loadManualAnswersForGroup()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun setupRecyclerView() {
        binding.answersRecycler.layoutManager = LinearLayoutManager(this)
        binding.answersRecycler.adapter = ManualAnswersAdapter { answer ->
            showRatingDialog(answer)
        }
    }

    private fun loadGroups() {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("usersgroup")
            .get()
            .addOnSuccessListener { snapshot ->
                val uniqueGroups = mutableMapOf<String, String>()
                for (doc in snapshot.documents) {
                    val groupId = doc.getString("groupId") ?: continue
                    val groupName = doc.getString("groupName") ?: continue
                    uniqueGroups[groupId] = groupName
                }

                groups.clear()
                groupIds.clear()
                uniqueGroups.forEach { (id, name) ->
                    groups.add(name)
                    groupIds.add(id)
                }

                binding.progressBar.visibility = View.GONE

                if (groups.isNotEmpty()) {
                    groupsAdapter.notifyDataSetChanged()
                    selectedGroupId = groupIds[0]
                    loadManualAnswersForGroup()
                } else {
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun loadManualAnswersForGroup() {
        if (selectedGroupId == null) return

        binding.progressBar.visibility = View.VISIBLE
        (binding.answersRecycler.adapter as ManualAnswersAdapter).submitList(emptyList())

        launch {
            try {
                val answers = mutableListOf<ManualAnswer>()

                // 1. Загружаем всех студентов выбранной группы
                val groupStudents = withContext(Dispatchers.IO) {
                    db.collection("usersgroup")
                        .whereEqualTo("groupId", selectedGroupId)
                        .get()
                        .await()
                }

                val studentIds = groupStudents.mapNotNull { it.getString("userId") }
                Log.d("ManualAnswers", "Found ${studentIds.size} students in group: $studentIds")

                if (studentIds.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                // 2. Загружаем ручные попытки тестов
                val manualAttempts = withContext(Dispatchers.IO) {
                    db.collection("test_attempts")
                        .whereIn("userId", studentIds)
                        .whereEqualTo("isManual", true)
                        .get()
                        .await()
                }

                Log.d("ManualAnswers", "Found ${manualAttempts.size()} manual attempts")

                if (manualAttempts.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                // 3. Для каждой попытки загружаем дополнительную информацию
                for (attemptDoc in manualAttempts) {
                    try {
                        val userId = attemptDoc.getString("userId") ?: continue
                        val manualQuestionId = attemptDoc.getString("manualQuestionId") ?: continue
                        val partId = attemptDoc.getString("partId") ?: continue
                        val manualAnswer = attemptDoc.getString("manualAnswer") ?: continue

                        // Пропускаем уже оцененные ответы
                        val isPassed = attemptDoc.getBoolean("isPassed") ?: false
                        if (isPassed) continue

                        Log.d("ManualAnswers", "Processing attempt: ${attemptDoc.id}, partId: $partId, questionId: $manualQuestionId")

                        // Параллельно загружаем все данные
                        val userDeferred = async {
                            db.collection("users").document(userId).get().await()
                        }

                        // УЛУЧШЕННАЯ ЗАГРУЗКА ДАННЫХ О ТЕСТЕ, ЧАСТИ И ВОПРОСЕ
                        val testPartQuestionDeferred = async {
                            try {
                                // СПОСОБ 1: Ищем часть по partId во всех тестах
                                var partDoc: com.google.firebase.firestore.DocumentSnapshot? = null
                                var testId: String? = null

                                // Пробуем найти часть в collectionGroup
                                try {
                                    val partsQuery = db.collectionGroup("parts")
                                        .whereEqualTo(FieldPath.documentId(), partId)
                                        .limit(1)
                                        .get()
                                        .await()
                                    partDoc = partsQuery.documents.firstOrNull()

                                    if (partDoc != null) {
                                        testId = partDoc.getString("testId") ?:
                                                partDoc.reference.parent.parent?.id
                                        Log.d("ManualAnswers", "Found part via collectionGroup: $partId, testId: $testId")
                                    }
                                } catch (e: Exception) {
                                    Log.d("ManualAnswers", "Part not found via collectionGroup: $partId")
                                }

                                // СПОСОБ 2: Если часть не найдена, ищем testId другим способом
                                if (testId == null) {
                                    // Попробуем найти testId в самом attempt
                                    testId = attemptDoc.getString("testId")
                                    Log.d("ManualAnswers", "Trying testId from attempt: $testId")

                                    // Если testId есть, загружаем часть напрямую
                                    if (testId != null) {
                                        try {
                                            partDoc = db.collection("tests")
                                                .document(testId)
                                                .collection("parts")
                                                .document(partId)
                                                .get()
                                                .await()
                                            Log.d("ManualAnswers", "Found part via direct path: $partId")
                                        } catch (e: Exception) {
                                            Log.d("ManualAnswers", "Part not found via direct path: $partId")
                                        }
                                    }
                                }

                                // СПОСОБ 3: Если testId все еще неизвестен, ищем его по partId
                                if (testId == null) {
                                    testId = findTestIdByPartId(partId)
                                    Log.d("ManualAnswers", "Found testId via search: $testId")
                                }

                                // Загружаем тест
                                val testDoc = if (testId != null) {
                                    try {
                                        db.collection("tests").document(testId).get().await()
                                    } catch (e: Exception) {
                                        Log.e("ManualAnswers", "Error loading test $testId", e)
                                        null
                                    }
                                } else null

                                // Загружаем вопрос
                                val questionDoc = if (testId != null) {
                                    try {
                                        db.collection("tests")
                                            .document(testId)
                                            .collection("parts")
                                            .document(partId)
                                            .collection("questions")
                                            .document(manualQuestionId)
                                            .get()
                                            .await()
                                    } catch (e: Exception) {
                                        Log.e("ManualAnswers", "Error loading question", e)
                                        null
                                    }
                                } else null

                                Triple(partDoc, testDoc, questionDoc)
                            } catch (e: Exception) {
                                Log.e("ManualAnswers", "Error loading test/part/question data", e)
                                null
                            }
                        }

                        // Ждем завершения всех запросов
                        val userDoc = userDeferred.await()
                        val testPartQuestion = testPartQuestionDeferred.await()

                        // Получаем имя пользователя
                        val name = userDoc?.getString("name") ?: ""
                        val surname = userDoc?.getString("surname") ?: ""
                        val userName = if (name.isNotEmpty() || surname.isNotEmpty())
                            "$name $surname".trim()
                        else
                            "Студент ID: ${userId.take(8)}"

                        // Получаем данные о тесте, части и вопросе
                        val (partDoc, testDoc, questionDoc) = testPartQuestion ?: Triple(null, null, null)

                        // Название теста
                        val testTitle = testDoc?.getString("title") ?: "Неизвестный тест (ID: ${attemptDoc.getString("testId") ?: "?"})"

                        // Название части
                        val partTitle = partDoc?.getString("title") ?: "Неизвестная часть (ID: $partId)"

                        // Текст вопроса
                        val questionText = questionDoc?.getString("text") ?:
                        questionDoc?.getString("question") ?:
                        "Вопрос ID: $manualQuestionId"

                        // Получаем текущий статус и оценку
                        val currentScore = attemptDoc.getDouble("finalScore") ?: 0.0
                        val isEvaluated = attemptDoc.getBoolean("isPassed") ?: false

                        answers.add(ManualAnswer(
                            attemptId = attemptDoc.id,
                            userId = userId,
                            userName = userName,
                            testTitle = testTitle,
                            partTitle = partTitle,
                            questionText = questionText,
                            answer = manualAnswer,
                            score = currentScore,
                            isEvaluated = isEvaluated
                        ))

                        Log.d("ManualAnswers", "Added answer: $testTitle - $partTitle")

                    } catch (e: Exception) {
                        Log.e("ManualAnswers", "Error processing attempt document ${attemptDoc.id}", e)
                    }
                }

                // Обновляем UI
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    (binding.answersRecycler.adapter as ManualAnswersAdapter).submitList(answers)


                }

            } catch (e: Exception) {
                Log.e("ManualAnswers", "Error loading manual answers", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE

                }
            }
        }
    }

    // Дополнительный метод для поиска testId по partId
    private suspend fun findTestIdByPartId(partId: String): String? {
        return try {
            // Ищем во всех тестах часть с данным partId
            val allTests = db.collection("tests").get().await()

            for (testDoc in allTests) {
                try {
                    val partDoc = db.collection("tests")
                        .document(testDoc.id)
                        .collection("parts")
                        .document(partId)
                        .get()
                        .await()

                    if (partDoc.exists()) {
                        Log.d("ManualAnswers", "Found testId for part $partId: ${testDoc.id}")
                        return testDoc.id
                    }
                } catch (e: Exception) {
                    // Продолжаем поиск
                }
            }
            null
        } catch (e: Exception) {
            Log.e("ManualAnswers", "Error finding testId for part $partId", e)
            null
        }
    }

    private fun showRatingDialog(answer: ManualAnswer) {
        val dialogBinding = DialogRateAnswerBinding.inflate(layoutInflater)

        // Добавляем отображение вопроса
        dialogBinding.answerText.text = "Ответ: ${answer.answer}"
        dialogBinding.currentScore.text = if (answer.isEvaluated) {
            "Текущая оценка: ${answer.score}"
        } else {
            "Работа не оценена"
        }

        // Устанавливаем текущую оценку в поле ввода
        dialogBinding.scoreInput.setText(if (answer.isEvaluated) answer.score.toString() else "")

        AlertDialog.Builder(this)
            .setTitle("Оценка работы: ${answer.testTitle} - ${answer.partTitle}")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val scoreText = dialogBinding.scoreInput.text.toString().trim()
                if (scoreText.isEmpty()) {
                    return@setPositiveButton
                }
                val score = scoreText.toDoubleOrNull() ?: 0.0
                // ИСПРАВЛЕНИЕ: используем attemptId вместо gradeId
                updateManualGrade(answer.attemptId, score)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateManualGrade(attemptId: String, score: Double) {
        binding.progressBar.visibility = View.VISIBLE

        // Обновляем попытку - помечаем как завершенную и устанавливаем оценку
        val updates = hashMapOf<String, Any>(
            "status" to "finished",
            "isPassed" to true,
            "finalScore" to score,
            "rawScore" to score
        )

        db.collection("test_attempts").document(attemptId)
            .update(updates)
            .addOnSuccessListener {
                // Также создаем или обновляем запись в test_grades
                createOrUpdateTestGrade(attemptId, score)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun createOrUpdateTestGrade(attemptId: String, score: Double) {
        // Сначала получаем информацию о попытке
        db.collection("test_attempts").document(attemptId).get()
            .addOnSuccessListener { attemptDoc ->
                val userId = attemptDoc.getString("userId")
                val testId = attemptDoc.getString("testId") // нужно добавить это поле в test_attempts
                val partId = attemptDoc.getString("partId")

                if (userId != null && testId != null && partId != null) {
                    val gradeData = hashMapOf(
                        "userId" to userId,
                        "testId" to testId,
                        "partId" to partId,
                        "bestScore" to score,
                        "bestAttemptId" to attemptId,
                        "isManual" to true,
                        "isEvaluated" to true,
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )

                    // Ищем существующую оценку или создаем новую
                    db.collection("test_grades")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("testId", testId)
                        .whereEqualTo("partId", partId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { grades ->
                            if (grades.isEmpty) {
                                // Создаем новую оценку
                                db.collection("test_grades").add(gradeData)
                            } else {
                                // Обновляем существующую оценку
                                db.collection("test_grades").document(grades.documents[0].id)
                                    .update(gradeData as Map<String, Any>)
                            }
                        }
                        .addOnCompleteListener {
                            binding.progressBar.visibility = View.GONE
                            loadManualAnswersForGroup() // Перезагружаем список
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
            }
    }

    data class ManualAnswer(
        val attemptId: String, // используем attemptId вместо gradeId
        val userId: String,
        val userName: String,
        val testTitle: String,
        val partTitle: String,
        val questionText: String, // добавляем текст вопроса
        val answer: String,
        val score: Double,
        val isEvaluated: Boolean
    )
}