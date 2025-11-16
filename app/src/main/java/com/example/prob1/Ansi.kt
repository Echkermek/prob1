package com.example.prob1

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityAnsiBinding
import com.example.prob1.databinding.DialogViewAnswersBinding
import com.google.firebase.firestore.FirebaseFirestore

class Ansi : AppCompatActivity() {

    private lateinit var binding: ActivityAnsiBinding
    private lateinit var firestore: FirebaseFirestore

    private var testsList = mutableListOf<Test>()
    private var partsList = mutableListOf<Part>()
    private var questionsList = mutableListOf<Question>()
    private var answersList = mutableListOf<Answer>()

    private var selectedTestId: String? = null
    private var selectedPartId: String? = null
    private var selectedQuestionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnsiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        // Загружаем тесты, части и вопросы
        loadTests()
        loadParts()
        loadQuestions()
        loadAnswers()

        // Настройка кнопок
        binding.btnAddAnswer.setOnClickListener {
            addAnswerToFirestore()
        }

        binding.btnViewAnswers.setOnClickListener {
            showAnswersDialog()
        }
    }

    private fun loadTests() {
        firestore.collection("test")
            .get()
            .addOnSuccessListener { result ->
                testsList.clear()
                testsList.addAll(result.map { doc ->
                    Test(
                        id = doc.id,
                        num = doc.getLong("num")?.toInt() ?: 0,
                        semester = doc.getLong("semester")?.toInt() ?: 0,
                        title = doc.getString("title") ?: ""
                    )
                })

                // Заполняем спиннер тестами
                val testTitles = testsList.map { "Тест ${it.num}: ${it.title}" }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, testTitles)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTest.adapter = adapter

                // Обработчик выбора теста
                binding.spinnerTest.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                        selectedTestId = testsList[position].id
                        filterPartsByTest(selectedTestId)
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки тестов: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadParts() {
        firestore.collection("part")
            .get()
            .addOnSuccessListener { result ->
                partsList.clear()
                partsList.addAll(result.map { doc ->
                    Part(
                        id = doc.id,
                        enterAnswer = doc.getBoolean("enterAnswer") ?: false,
                        idLectures = doc.getString("idLectures") ?: "",
                        idtest = doc.getString("idtest") ?: "",
                        num = doc.getLong("num")?.toInt() ?: 0,
                        title = doc.getString("title") ?: ""
                    )
                })
                filterPartsByTest(selectedTestId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки частей: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadQuestions() {
        firestore.collection("question")
            .get()
            .addOnSuccessListener { result ->
                questionsList.clear()
                questionsList.addAll(result.map { doc ->
                    Question(
                        id = doc.id,
                        content = doc.getString("content") ?: "",
                        idPart = doc.getString("idPart") ?: "",
                        num = doc.getLong("num")?.toInt() ?: 0,
                        quest = doc.getString("quest") ?: ""
                    )
                })
                filterQuestionsByPart(selectedPartId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки вопросов: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAnswers() {
        firestore.collection("answer")
            .get()
            .addOnSuccessListener { result ->
                answersList.clear()
                answersList.addAll(result.map { doc ->
                    Answer(
                        id = doc.id,
                        ans = doc.getString("ans") ?: "",
                        idQuest = doc.getString("idQuest") ?: "",
                        isTrue = doc.getBoolean("isTrue") ?: false
                    )
                })
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки ответов: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterPartsByTest(testId: String?) {
        val filteredParts = if (testId != null) {
            partsList.filter { it.idtest == testId }
        } else {
            partsList
        }

        val partTitles = filteredParts.map { "Часть ${it.num}: ${it.title}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPart.adapter = adapter

        // Обработчик выбора части
        binding.spinnerPart.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedPartId = filteredParts[position].id
                filterQuestionsByPart(selectedPartId)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun filterQuestionsByPart(partId: String?) {
        val filteredQuestions = if (partId != null) {
            questionsList.filter { it.idPart == partId }
        } else {
            questionsList
        }

        val questionTitles = filteredQuestions.map { "Вопрос ${it.num}: ${it.quest.take(50)}${if (it.quest.length > 50) "..." else ""}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questionTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQuestion.adapter = adapter

        // Обработчик выбора вопроса
        binding.spinnerQuestion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedQuestionId = filteredQuestions[position].id
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun addAnswerToFirestore() {
        val answerText = binding.etAnswer.text.toString()
        val isCorrect = binding.cbIsCorrect.isChecked

        if (answerText.isEmpty() || selectedQuestionId == null) {
            Toast.makeText(this, "Заполните текст ответа и выберите вопрос", Toast.LENGTH_SHORT).show()
            return
        }

        val answerData = hashMapOf(
            "ans" to answerText,
            "idQuest" to selectedQuestionId,
            "isTrue" to isCorrect
        )

        firestore.collection("answer")
            .add(answerData)
            .addOnSuccessListener {
                Toast.makeText(this, "Ответ успешно добавлен!", Toast.LENGTH_SHORT).show()
                clearFields()
                loadAnswers() // Перезагружаем ответы после добавления
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка добавления: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAnswersDialog() {
        if (selectedQuestionId == null) {
            Toast.makeText(this, "Сначала выберите вопрос", Toast.LENGTH_SHORT).show()
            return
        }

        val filteredAnswers = answersList.filter { it.idQuest == selectedQuestionId }

        if (filteredAnswers.isEmpty()) {
            Toast.makeText(this, "Для этого вопроса нет ответов", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogViewAnswersBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val answerTitles = filteredAnswers.map { answer ->
            val correctness = if (answer.isTrue) "✓ Правильный" else "✗ Неправильный"
            "${answer.ans} ($correctness)"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, answerTitles)
        dialogBinding.listViewAnswers.adapter = adapter

        dialogBinding.listViewAnswers.setOnItemClickListener { parent, view, position, id ->
            val selectedAnswer = filteredAnswers[position]
            showDeleteAnswerDialog(selectedAnswer, dialog)
        }

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteAnswerDialog(answer: Answer, parentDialog: AlertDialog) {
        AlertDialog.Builder(this)
            .setTitle("Удаление ответа")
            .setMessage("Вы уверены, что хотите удалить ответ: \"${answer.ans.take(50)}...\"?")
            .setPositiveButton("Удалить") { dialog, which ->
                deleteAnswer(answer.id)
                parentDialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteAnswer(answerId: String) {
        firestore.collection("answer")
            .document(answerId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Ответ успешно удален!", Toast.LENGTH_SHORT).show()
                loadAnswers() // Перезагружаем ответы после удаления
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка удаления: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        binding.etAnswer.text.clear()
        binding.cbIsCorrect.isChecked = false
    }

    // Data classes
    data class Test(
        val id: String,
        val num: Int,
        val semester: Int,
        val title: String
    )

    data class Part(
        val id: String,
        val enterAnswer: Boolean,
        val idLectures: String,
        val idtest: String,
        val num: Int,
        val title: String
    )

    data class Question(
        val id: String,
        val content: String,
        val idPart: String,
        val num: Int,
        val quest: String
    )

    data class Answer(
        val id: String,
        val ans: String,
        val idQuest: String,
        val isTrue: Boolean
    )
}