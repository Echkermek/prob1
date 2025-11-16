package com.example.prob1

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityRoleSelectionBinding
import com.example.prob1.ui.teacher.TeacherAuthActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private lateinit var db: FirebaseFirestore
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var logTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация Firestore
        db = Firebase.firestore

        // Связывание элементов интерфейса
        //logTextView = findViewById(R.id.logTextView)

        // Настройка кнопок
        binding.btnTeacher.setOnClickListener {
            startActivity(Intent(this, TeacherAuthActivity::class.java))
        }

        binding.btnStudent.setOnClickListener {
            startActivity(Intent(this, StudentAuthActivity::class.java))
        }

        /*   // Добавление кнопки Migrate
        val migrateButton: Button = findViewById(R.id.btnMigrate)
        migrateButton.setOnClickListener {
            migrateData()
        }*/

        // Комментированные кнопки оставим как есть
        /*
        binding.btnTest.setOnClickListener{
            startActivity(Intent(this, Testi::class.java))
        }
        binding.btnQuest.setOnClickListener{
            startActivity(Intent(this, Ansi::class.java))
        }*/
    }

    /*
    private fun log(message: String) {
        runOnUiThread {
            logTextView.append("$message\n")
        }
    }

    private fun migrateData() {
        coroutineScope.launch {
            try {
                // Шаг 1: Перенос тестов
                log("Starting migration of tests...")
                val tests = db.collection("test").get().await()
                for (test in tests) {
                    val testData = test.data
                    val testId = test.id
                    val testMap = mutableMapOf<String, Any?>().apply {
                        put("title", testData["title"] ?: "")
                        put("num", (testData["num"] as? Long)?.toInt() ?: 0)
                        put("semester", (testData["semester"] as? Long)?.toInt() ?: 0)
                    }
                    db.collection("tests").document(testId).set(testMap).await()
                    log("Migrated test: $testId")
                }

                // Шаг 2: Перенос частей тестов
                log("Starting migration of parts...")
                val parts = db.collection("part").get().await()
                for (part in parts) {
                    val partData = part.data
                    val testId = partData["idtest"] as? String ?: continue
                    val partId = part.id
                    val partMap = mutableMapOf<String, Any?>().apply {
                        put("title", partData["title"] ?: "")
                        put("num", (partData["num"] as? Long)?.toInt() ?: 0)
                        put("enterAnswer", partData["enterAnswer"] ?: false)
                    }
                    db.collection("tests").document(testId).collection("parts").document(partId).set(partMap).await()
                    log("Migrated part: $partId under test $testId")
                }

                // Шаг 3: Перенос вопросов и ответов
                log("Starting migration of questions and answers...")
                val questions = db.collection("question").get().await()
                for (question in questions) {
                    val questionData = question.data
                    val partId = questionData["idPart"] as? String ?: continue
                    val questionId = question.id
                    // Получение всех ответов для данного вопроса
                    val answers = db.collection("answer")
                        .whereEqualTo("idQuest", questionId)
                        .get()
                        .await()
                    val answersList = answers.map { answer ->
                        val answerData = answer.data
                        mutableMapOf<String, Any?>().apply {
                            put("text", answerData["ans"] ?: "")
                            put("isCorrect", answerData["isTrue"] ?: false)
                        }
                    }
                    // Найти testId через partId
                    val testId = parts.firstOrNull { it.id == partId }?.data?.get("idtest") as? String ?: continue
                    val questionMap = mutableMapOf<String, Any?>().apply {
                        put("text", questionData["quest"] ?: "")
                        put("content", questionData["content"] ?: "")
                        put("num", (questionData["num"] as? Long)?.toInt() ?: 0)
                        put("answers", answersList)
                    }
                    db.collection("tests").document(testId).collection("parts").document(partId)
                        .collection("questions").document(questionId).set(questionMap).await()
                    log("Migrated question: $questionId under part $partId")
                }

                // Шаг 4: Подтверждение удаления старых коллекций
                log("Migration completed. Checking for old collection deletion...")
                showDeleteConfirmationDialog()

            } catch (e: Exception) {
                log("Error during migration: ${e.message}")
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Old Collections")
            .setMessage("Do you want to delete old collections (test, part, question, answer)?")
            .setPositiveButton("Yes") { _, _ ->
                coroutineScope.launch {
                    try {
                        val collections = listOf("test", "part", "question", "answer")
                        for (collection in collections) {
                            val docs = db.collection(collection).get().await()
                            for (doc in docs) {
                                doc.reference.delete().await()
                            }
                            log("Deleted collection: $collection")
                        }
                        log("Old collections deleted successfully!")
                    } catch (e: Exception) {
                        log("Error deleting collections: ${e.message}")
                    }
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                log("Old collections were not deleted.")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }*/
}