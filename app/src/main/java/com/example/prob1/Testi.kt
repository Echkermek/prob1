package com.example.prob1

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityTestiBinding
import com.example.prob1.databinding.DialogViewQuestionsBinding
import com.google.firebase.firestore.FirebaseFirestore

class Testi : AppCompatActivity() {

    private lateinit var binding: ActivityTestiBinding
    private lateinit var firestore: FirebaseFirestore

    private var testsList = mutableListOf<Test>()
    private var partsList = mutableListOf<Part>()
    private var questionsList = mutableListOf<Question>()

    private var selectedTestId: String? = null
    private var selectedPartId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        // Загружаем тесты, части и вопросы
        loadTests()
        loadParts()
        loadQuestions()

        // Настройка кнопок
        binding.btnAddQuestion.setOnClickListener {
            addQuestionToFirestore()
        }

        binding.btnViewQuestions.setOnClickListener {
            showQuestionsDialog()
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
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки вопросов: ${exception.message}", Toast.LENGTH_SHORT).show()
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
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun addQuestionToFirestore() {
        val questionNum = binding.etQuestionNum.text.toString().toIntOrNull()
        val questionText = binding.etQuestion.text.toString()
        val content = binding.etContent.text.toString()

        if (questionNum == null || questionText.isEmpty() || selectedPartId == null) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val questionData = hashMapOf(
            "content" to content,
            "idPart" to selectedPartId,
            "num" to questionNum,
            "quest" to questionText
        )

        firestore.collection("question")
            .add(questionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Вопрос успешно добавлен!", Toast.LENGTH_SHORT).show()
                clearFields()
                loadQuestions() // Перезагружаем вопросы после добавления
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка добавления: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showQuestionsDialog() {
        if (selectedPartId == null) {
            Toast.makeText(this, "Сначала выберите часть теста", Toast.LENGTH_SHORT).show()
            return
        }

        val filteredQuestions = questionsList.filter { it.idPart == selectedPartId }

        if (filteredQuestions.isEmpty()) {
            Toast.makeText(this, "В этой части нет вопросов", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogViewQuestionsBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val questionTitles = filteredQuestions.map { "Вопрос ${it.num}: ${it.quest}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, questionTitles)
        dialogBinding.listViewQuestions.adapter = adapter

        dialogBinding.listViewQuestions.setOnItemClickListener { parent, view, position, id ->
            val selectedQuestion = filteredQuestions[position]
            showDeleteQuestionDialog(selectedQuestion, dialog)
        }

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteQuestionDialog(question: Question, parentDialog: AlertDialog) {
        AlertDialog.Builder(this)
            .setTitle("Удаление вопроса")
            .setMessage("Вы уверены, что хотите удалить вопрос: \"${question.quest.take(50)}...\"?")
            .setPositiveButton("Удалить") { dialog, which ->
                deleteQuestion(question.id)
                parentDialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteQuestion(questionId: String) {
        firestore.collection("question")
            .document(questionId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Вопрос успешно удален!", Toast.LENGTH_SHORT).show()
                loadQuestions() // Перезагружаем вопросы после удаления

                // Также удаляем все ответы, связанные с этим вопросом
                deleteAnswersForQuestion(questionId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка удаления: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAnswersForQuestion(questionId: String) {
        firestore.collection("answer")
            .whereEqualTo("idQuest", questionId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    document.reference.delete()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка удаления ответов: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        binding.etQuestionNum.text.clear()
        binding.etQuestion.text.clear()
        binding.etContent.text.clear()
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
}