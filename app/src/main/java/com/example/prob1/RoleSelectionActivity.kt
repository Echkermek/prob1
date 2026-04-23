package com.example.prob1

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore

        binding.btnTeacher.setOnClickListener {
            startActivity(Intent(this, TeacherAuthActivity::class.java))
        }

        binding.btnStudent.setOnClickListener {
            startActivity(Intent(this, StudentAuthActivity::class.java))
        }


        // Кнопка для теста Drilling Rig
        binding.button.setOnClickListener {   // измените ID кнопки под вашу разметку
            uploadParticipleTest()
        }

        // Кнопка для теста Global Financial Systems
        /*binding.buttonFinance.setOnClickListener {    // измените ID кнопки под вашу разметку
            uploadGlobalFinancialSystemsTest()
        }*/
    }


    // ==================== ТЕСТ: PARTICIPLE (Причастие) – III семестр ====================

    private fun uploadParticipleTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Participle (Причастие)")
                    .setMessage("Загружаем тест с 30 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Participle (Причастие)",
                    "num" to 15,
                    "semester" to 3,
                    "totalScore" to 30,
                    "hasParts" to false
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем все 30 вопросов
                val questions = getParticipleQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Participle (Причастие)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 30",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Participle test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 30 ВОПРОСОВ ====================

    private fun getParticipleQuestions(): List<Map<String, Any>> {
        return listOf(

            // Блок 1 – Выберите правильный вариант (12 вопросов)
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант:\n1. … (wait) in the hall, he thought over the problem he was planning to discuss with the old lady.",
                "answers" to listOf(
                    mapOf("text" to "a) waiting", "isCorrect" to true),
                    mapOf("text" to "b) be waiting", "isCorrect" to false),
                    mapOf("text" to "c) having waited", "isCorrect" to false)
                )),
            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n2. … (write) in very bad handwriting, the letter was difficult to read.",
                "answers" to listOf(
                    mapOf("text" to "a) being written", "isCorrect" to true),
                    mapOf("text" to "b) writing", "isCorrect" to false),
                    mapOf("text" to "c) having been written", "isCorrect" to false)
                )),
            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n3. … (spend) twenty years abroad, he was happy to be coming home.",
                "answers" to listOf(
                    mapOf("text" to "a) spending", "isCorrect" to false),
                    mapOf("text" to "b) having spent", "isCorrect" to true),
                    mapOf("text" to "c) being spent", "isCorrect" to false)
                )),
            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n4. … (not wish) to discuss the problem, he changed the conversation.",
                "answers" to listOf(
                    mapOf("text" to "a) having not wished", "isCorrect" to false),
                    mapOf("text" to "b) wishing not", "isCorrect" to false),
                    mapOf("text" to "c) not wishing", "isCorrect" to true)
                )),
            mapOf("id" to "q5", "text" to "1. Выберите правильный вариант:\n5. …(reject) by the publisher, the story was returned to the author.",
                "answers" to listOf(
                    mapOf("text" to "a) rejecting", "isCorrect" to false),
                    mapOf("text" to "b) being rejected", "isCorrect" to true),
                    mapOf("text" to "c) having been rejected", "isCorrect" to false)
                )),
            mapOf("id" to "q6", "text" to "1. Выберите правильный вариант:\n6. … (reject) by publishers several times, the story was accepted by a weekly magazine.",
                "answers" to listOf(
                    mapOf("text" to "a) rejecting", "isCorrect" to false),
                    mapOf("text" to "b) being rejected", "isCorrect" to false),
                    mapOf("text" to "c) having been rejected", "isCorrect" to true)
                )),
            mapOf("id" to "q7", "text" to "1. Выберите правильный вариант:\n7. They reached the peak at dusk, … (leave) their camp with the first light.",
                "answers" to listOf(
                    mapOf("text" to "a) leaving", "isCorrect" to true),
                    mapOf("text" to "b) having left", "isCorrect" to false),
                    mapOf("text" to "c) left", "isCorrect" to false)
                )),
            mapOf("id" to "q8", "text" to "1. Выберите правильный вариант:\n8. The friends went out into the city … (leave) their cases at the left-luggage department.",
                "answers" to listOf(
                    mapOf("text" to "a) leaving", "isCorrect" to false),
                    mapOf("text" to "b) having left", "isCorrect" to true),
                    mapOf("text" to "c) left", "isCorrect" to false)
                )),
            mapOf("id" to "q9", "text" to "1. Выберите правильный вариант:\n9. … (be) away so long he was happy to be coming back.",
                "answers" to listOf(
                    mapOf("text" to "a) being been", "isCorrect" to false),
                    mapOf("text" to "b) being", "isCorrect" to true),
                    mapOf("text" to "c) having been away", "isCorrect" to false)
                )),
            mapOf("id" to "q10", "text" to "1. Выберите правильный вариант:\n10. I cannot forget the story ... by him. They listened breathlessly to the story ... by the old man. (tell).",
                "answers" to listOf(
                    mapOf("text" to "a) told, being told", "isCorrect" to true),
                    mapOf("text" to "b) being told, told", "isCorrect" to false),
                    mapOf("text" to "c) being told, being told", "isCorrect" to false)
                )),
            mapOf("id" to "q11", "text" to "1. Выберите правильный вариант:\n11. One can't fail to notice the progress ... by our group during the last term. These are only a few of the attempts now ... to improve the methods of teaching adult students, (make).",
                "answers" to listOf(
                    mapOf("text" to "a) made, made", "isCorrect" to false),
                    mapOf("text" to "b) made, being made", "isCorrect" to true),
                    mapOf("text" to "c) having been made, being made", "isCorrect" to false)
                )),
            mapOf("id" to "q12", "text" to "1. Выберите правильный вариант:\n12. We could hear the noise of furniture ... upstairs. For a moment they sat silent ... by the story, (move).",
                "answers" to listOf(
                    mapOf("text" to "a) being moved, being moved", "isCorrect" to false),
                    mapOf("text" to "b) moved, moved", "isCorrect" to false),
                    mapOf("text" to "c) being moved, moved", "isCorrect" to true)
                )),

            // Блок 2 – Выберите правильный вариант перевода (4 вопроса)
            mapOf("id" to "q13", "text" to "2. Выберите правильный вариант перевода слов в скобках:\n1. We came up to the man … (стоявшему на углу) and asked him the way. The man (стоящий у окна) was our teacher last year.",
                "answers" to listOf(
                    mapOf("text" to "a) standing, standing", "isCorrect" to true),
                    mapOf("text" to "b) having stood, stood", "isCorrect" to false),
                    mapOf("text" to "c) stood, standing", "isCorrect" to false)
                )),
            mapOf("id" to "q14", "text" to "2. Выберите правильный вариант перевода:\n2. … (Рассказав все, что он знал) the man left the room. Each time … (рассказывая об этом случае) she could not help crying.",
                "answers" to listOf(
                    mapOf("text" to "a) Having told all he knew, told about this accident", "isCorrect" to false),
                    mapOf("text" to "b) Telling all he knew, telling about this accident", "isCorrect" to false),
                    mapOf("text" to "c) Having told all he knew, telling about this accident", "isCorrect" to true)
                )),
            mapOf("id" to "q15", "text" to "2. Выберите правильный вариант перевода:\n3. … (Приехав в гостиницу) she found a telegram awaiting her. … (Приехав сюда) many years before he knew those parts perfectly.",
                "answers" to listOf(
                    mapOf("text" to "a) Having arrived to the hotel, Having come", "isCorrect" to false),
                    mapOf("text" to "b) Arriving to the hotel, Came", "isCorrect" to false),
                    mapOf("text" to "c) Arriving to the hotel, Having come", "isCorrect" to true)
                )),
            mapOf("id" to "q16", "text" to "2. Выберите правильный вариант перевода:\n4. The conference … (проходящая сейчас) in our city is devoted to problems of environment protection. Unable to attend the conference … (проходившую тогда) at the University, we asked to inform us about its decisions.",
                "answers" to listOf(
                    mapOf("text" to "a) being held, being held", "isCorrect" to true),
                    mapOf("text" to "b) being held, having been held", "isCorrect" to false),
                    mapOf("text" to "c) being held, hold", "isCorrect" to false)
                )),

            // Блок 3 – Объектный падеж с причастием настоящего времени (4 вопроса)
            mapOf("id" to "q17", "text" to "3. Выберите правильный перевод предложений с оборотом \"объектный падеж с причастием настоящего времени\":\n1. Я слышал, как он сказал ей об этом.",
                "answers" to listOf(
                    mapOf("text" to "a) I heard her tell him about it.", "isCorrect" to false),
                    mapOf("text" to "b) I heard her telling him about it.", "isCorrect" to true),
                    mapOf("text" to "c) I heard her having told him about it.", "isCorrect" to false),
                    mapOf("text" to "d) I heard her to tell him about it.", "isCorrect" to false)
                )),
            mapOf("id" to "q18", "text" to "3. Выберите правильный перевод:\n2. Я слышал, как она рассказывала ему об этом.",
                "answers" to listOf(
                    mapOf("text" to "a) I heard her tell him about it.", "isCorrect" to true),
                    mapOf("text" to "b) I heard her telling him about it.", "isCorrect" to false),
                    mapOf("text" to "c) I heard her having told him about it.", "isCorrect" to false),
                    mapOf("text" to "d) I heard her to tell him about it.", "isCorrect" to false)
                )),
            mapOf("id" to "q19", "text" to "3. Выберите правильный перевод:\n3. Я наблюдал, как они спускались с горы.",
                "answers" to listOf(
                    mapOf("text" to "a) I watched they were going down the mountain.", "isCorrect" to false),
                    mapOf("text" to "b) I watched them going down the mountain.", "isCorrect" to true),
                    mapOf("text" to "c) I watched them to be going down the mountain.", "isCorrect" to false),
                    mapOf("text" to "d) I watched them to go down the mountain.", "isCorrect" to false)
                )),
            mapOf("id" to "q20", "text" to "3. Выберите правильный перевод:\n4. Собака голодными глазами наблюдала, как ей несли косточку.",
                "answers" to listOf(
                    mapOf("text" to "a) The dog was watching with hungry eyes a bone had been brought.", "isCorrect" to false),
                    mapOf("text" to "b) The dog was watching with hungry eyes a bone having been brought.", "isCorrect" to false),
                    mapOf("text" to "c) The dog was watching with hungry eyes a bone being brought.", "isCorrect" to true),
                    mapOf("text" to "d) The dog was watching with hungry eyes a bone to be brought.", "isCorrect" to false)
                )),

            // Блок 4 – Объектный падеж с причастием прошедшего времени (4 вопроса)
            mapOf("id" to "q21", "text" to "4. Выберите правильный перевод предложений с оборотом \"объектный падеж с причастием прошедшего времени\":\n1. Он хочет, чтобы документы были отосланы не позднее пятницы.",
                "answers" to listOf(
                    mapOf("text" to "a) He wants the documents being sent not later than Friday.", "isCorrect" to false),
                    mapOf("text" to "b) He wants the documents sent not later than Friday.", "isCorrect" to true),
                    mapOf("text" to "c) He wants to have the documents sent not later than Friday.", "isCorrect" to false)
                )),
            mapOf("id" to "q22", "text" to "4. Выберите правильный перевод:\n2. Почему вы покрасили стены в такой темный цвет?",
                "answers" to listOf(
                    mapOf("text" to "a) Why have you had the walls of your room painted dark?", "isCorrect" to true),
                    mapOf("text" to "b) Why did you have the walls of your room painted dark?", "isCorrect" to false),
                    mapOf("text" to "c) Why have you the walls of your room having been painted dark?", "isCorrect" to false)
                )),
            mapOf("id" to "q23", "text" to "4. Выберите правильный перевод:\n3. Он отослал отчет вчера.",
                "answers" to listOf(
                    mapOf("text" to "a) He had the report sent yesterday.", "isCorrect" to true),
                    mapOf("text" to "b) He had the report send yesterday.", "isCorrect" to false),
                    mapOf("text" to "c) He had the report having been sent yesterday.", "isCorrect" to false)
                )),
            mapOf("id" to "q24", "text" to "4. Выберите правильный перевод:\n4. Сейчас мне моют машину.",
                "answers" to listOf(
                    mapOf("text" to "a) They are washing my car.", "isCorrect" to false),
                    mapOf("text" to "b) I have my car washed", "isCorrect" to false),
                    mapOf("text" to "c) I have my car being washed", "isCorrect" to false),
                    mapOf("text" to "d) I am having my car washed", "isCorrect" to true)
                )),

            // Блок 5 – Самостоятельный причастный оборот (4 вопроса)
            mapOf("id" to "q25", "text" to "5. Выберите правильный перевод предложений с самостоятельным причастным оборотом:\n1. После того как мое задание было окончено, я лег спать.",
                "answers" to listOf(
                    mapOf("text" to "a) My task having been finished, I went to bed.", "isCorrect" to true),
                    mapOf("text" to "b) My task being finished, I went to bed.", "isCorrect" to false),
                    mapOf("text" to "c) Having finished my task, I went to bed.", "isCorrect" to false)
                )),
            mapOf("id" to "q26", "text" to "5. Выберите правильный перевод:\n2. Так как в комнате было темно, я не видел его.",
                "answers" to listOf(
                    mapOf("text" to "a) Being dark in the room, I couldn't see him.", "isCorrect" to false),
                    mapOf("text" to "b) The room being dark, I couldn't see him.", "isCorrect" to true),
                    mapOf("text" to "c) Being the dark room, I couldn't see him.", "isCorrect" to false)
                )),
            mapOf("id" to "q27", "text" to "5. Выберите правильный перевод:\n3. Так как было холодно, они развели костер.",
                "answers" to listOf(
                    mapOf("text" to "a) Being very cold, they made a fire.", "isCorrect" to false),
                    mapOf("text" to "b) It being very cold, they made a fire.", "isCorrect" to true),
                    mapOf("text" to "c) It was very cold they made a fire.", "isCorrect" to false)
                )),
            mapOf("id" to "q28", "text" to "5. Выберите правильный перевод:\n4. Было темно, так как солнце зашло за час до этого.",
                "answers" to listOf(
                    mapOf("text" to "a) It was dark, the sun set an hour before.", "isCorrect" to false),
                    mapOf("text" to "b) It was dark, because the sun had set an hour before.", "isCorrect" to false),
                    mapOf("text" to "c) It was dark, the sun having set an hour before.", "isCorrect" to true)
                ))
        )
    }

    /*
    // ==================== ТЕСТ: NUMERALS (Числительные) – III семестр ====================

    private fun uploadNumeralsTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Numerals (Числительные)")
                    .setMessage("Загружаем тест с 32 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Numerals (Числительные)",
                    "num" to 14,
                    "semester" to 3,
                    "totalScore" to 32,
                    "hasParts" to false
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем все 32 вопроса
                val questions = getNumeralsQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Numerals (Числительные)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 32",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Numerals test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 32 ВОПРОСА ====================

    private fun getNumeralsQuestions(): List<Map<String, Any>> {
        return listOf(

            // Блок 1 – Количественные числительные (12 вопросов)
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант написания количественных числительных:\n1. 14",
                "answers" to listOf(
                    mapOf("text" to "a) forteen", "isCorrect" to false),
                    mapOf("text" to "b) forty", "isCorrect" to false),
                    mapOf("text" to "c) fourteen", "isCorrect" to true),
                    mapOf("text" to "d) fourtin", "isCorrect" to false)
                )),
            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n2. 12",
                "answers" to listOf(
                    mapOf("text" to "a) twelve", "isCorrect" to true),
                    mapOf("text" to "b) twoteen", "isCorrect" to false),
                    mapOf("text" to "c) twotin", "isCorrect" to false),
                    mapOf("text" to "d) tvelwe", "isCorrect" to false)
                )),
            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n3. 30",
                "answers" to listOf(
                    mapOf("text" to "a) thity", "isCorrect" to false),
                    mapOf("text" to "b) thirty", "isCorrect" to true),
                    mapOf("text" to "c) therty", "isCorrect" to false),
                    mapOf("text" to "d) thety", "isCorrect" to false)
                )),
            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n4. 93",
                "answers" to listOf(
                    mapOf("text" to "a) ninetee-three", "isCorrect" to false),
                    mapOf("text" to "b) nainety-thry", "isCorrect" to false),
                    mapOf("text" to "c) ninty-thrу", "isCorrect" to false),
                    mapOf("text" to "d) ninety-three", "isCorrect" to true)
                )),
            mapOf("id" to "q5", "text" to "1. Выберите правильный вариант:\n5. 86",
                "answers" to listOf(
                    mapOf("text" to "a) eithy-sixs", "isCorrect" to false),
                    mapOf("text" to "b) eighty-sixs", "isCorrect" to false),
                    mapOf("text" to "c) eihty-six", "isCorrect" to false),
                    mapOf("text" to "d) eighty-six", "isCorrect" to true)
                )),
            mapOf("id" to "q6", "text" to "1. Выберите правильный вариант:\n6. 100",
                "answers" to listOf(
                    mapOf("text" to "a) hundreed", "isCorrect" to false),
                    mapOf("text" to "b) hundred", "isCorrect" to true),
                    mapOf("text" to "c) handred", "isCorrect" to false),
                    mapOf("text" to "d) handreed", "isCorrect" to false)
                )),
            mapOf("id" to "q7", "text" to "1. Выберите правильный вариант:\n7. 226",
                "answers" to listOf(
                    mapOf("text" to "a) two hundred twenty-six", "isCorrect" to true),
                    mapOf("text" to "b) two hundreed twentysix", "isCorrect" to false),
                    mapOf("text" to "c) two hundreds twentysix", "isCorrect" to false),
                    mapOf("text" to "d) hundred two twenty-six", "isCorrect" to false)
                )),
            mapOf("id" to "q8", "text" to "1. Выберите правильный вариант:\n8. 705",
                "answers" to listOf(
                    mapOf("text" to "a) seventy hundred five", "isCorrect" to false),
                    mapOf("text" to "b) seven hundred and five", "isCorrect" to true),
                    mapOf("text" to "c) hundreds seven and five", "isCorrect" to false),
                    mapOf("text" to "d) seven zero five", "isCorrect" to false)
                )),
            mapOf("id" to "q9", "text" to "1. Выберите правильный вариант:\n9. 1,008",
                "answers" to listOf(
                    mapOf("text" to "a) one thousend and eight", "isCorrect" to false),
                    mapOf("text" to "b) one thosand and eiht", "isCorrect" to false),
                    mapOf("text" to "c) one thousand double zero eiht", "isCorrect" to false),
                    mapOf("text" to "d) one thousand and eight", "isCorrect" to true)
                )),
            mapOf("id" to "q10", "text" to "1. Выберите правильный вариант:\n10. 75,137",
                "answers" to listOf(
                    mapOf("text" to "a) one hundred thirty-seven and seventy-five thousand", "isCorrect" to false),
                    mapOf("text" to "b) seventy-five thousand one hundred and thirty-seven", "isCorrect" to true),
                    mapOf("text" to "c) seventy-five thosand one hundred and thity-seven", "isCorrect" to false),
                    mapOf("text" to "d) seventy-five and thirty-seven hundred thousands", "isCorrect" to false)
                )),
            mapOf("id" to "q11", "text" to "1. Выберите правильный вариант:\n11. 425,712",
                "answers" to listOf(
                    mapOf("text" to "a) twenty-five thousand four hundred and seven hundred and twelve", "isCorrect" to false),
                    mapOf("text" to "b) four hundred and twenty-five thousand twelve and seven hundred", "isCorrect" to false),
                    mapOf("text" to "c) four hundred and twenty-five thousand seven hundred and twelve", "isCorrect" to true),
                    mapOf("text" to "d) twenty-five thousand four hundred and twelve and seven hundred", "isCorrect" to false)
                )),
            mapOf("id" to "q12", "text" to "1. Выберите правильный вариант:\n12. 1,306,527",
                "answers" to listOf(
                    mapOf("text" to "a) five hundred twenty-seven thousand three hundred and six one million", "isCorrect" to false),
                    mapOf("text" to "b) one million three hundred and six thousand five hundred and twenty-seven", "isCorrect" to true),
                    mapOf("text" to "c) three hundred and six thousand one million five and twenty-seven hundred", "isCorrect" to false),
                    mapOf("text" to "d) five hundred and twenty-seven one million three hundred and six thousand", "isCorrect" to false)
                )),

            // Блок 2 – Единицы измерения и множественное число (6 вопросов)
            mapOf("id" to "q13", "text" to "2. Выберите правильный вариант:\n1. 50 килограммов",
                "answers" to listOf(
                    mapOf("text" to "a) fifty kilograms", "isCorrect" to true),
                    mapOf("text" to "b) fifty kilogram", "isCorrect" to false)
                )),
            mapOf("id" to "q14", "text" to "2. Выберите правильный вариант:\n2. 300 автомобилей",
                "answers" to listOf(
                    mapOf("text" to "a) three hundreds automobiles", "isCorrect" to false),
                    mapOf("text" to "b) three hundred automobiles", "isCorrect" to true)
                )),
            mapOf("id" to "q15", "text" to "2. Выберите правильный вариант:\n3. 21 грамм",
                "answers" to listOf(
                    mapOf("text" to "a) twenty-one grams", "isCorrect" to true),
                    mapOf("text" to "b) twenty-one gram", "isCorrect" to false)
                )),
            mapOf("id" to "q16", "text" to "2. Выберите правильный вариант:\n4. 2,000,000 тонн",
                "answers" to listOf(
                    mapOf("text" to "a) two million tons", "isCorrect" to true),
                    mapOf("text" to "b) two millions ton", "isCorrect" to false)
                )),
            mapOf("id" to "q17", "text" to "2. Выберите правильный вариант:\n5. Сотни машин",
                "answers" to listOf(
                    mapOf("text" to "a) hundred of machines", "isCorrect" to false),
                    mapOf("text" to "b) hundreds of machines", "isCorrect" to true)
                )),
            mapOf("id" to "q18", "text" to "2. Выберите правильный вариант:\n6. 281 доллар",
                "answers" to listOf(
                    mapOf("text" to "a) two hundred and eighty-one dollars", "isCorrect" to true),
                    mapOf("text" to "b) two hundreds and eighty-one dollars", "isCorrect" to false)
                )),

            // Блок 3 – Порядковые числительные (8 вопросов)
            mapOf("id" to "q19", "text" to "3. Выберите правильный вариант написания порядковых числительных:\n1. 3th",
                "answers" to listOf(
                    mapOf("text" to "a) therd", "isCorrect" to false),
                    mapOf("text" to "b) threeth", "isCorrect" to false),
                    mapOf("text" to "c) third", "isCorrect" to true)
                )),
            mapOf("id" to "q20", "text" to "3. Выберите правильный вариант:\n2. 30th",
                "answers" to listOf(
                    mapOf("text" to "a) thirtith", "isCorrect" to false),
                    mapOf("text" to "b) thirtieth", "isCorrect" to true),
                    mapOf("text" to "c) thirteth", "isCorrect" to false)
                )),
            mapOf("id" to "q21", "text" to "3. Выберите правильный вариант:\n3. 19th",
                "answers" to listOf(
                    mapOf("text" to "a) nineteenth", "isCorrect" to true),
                    mapOf("text" to "b) ninetienth", "isCorrect" to false),
                    mapOf("text" to "c) ninetinth", "isCorrect" to false)
                )),
            mapOf("id" to "q22", "text" to "3. Выберите правильный вариант:\n4. 90th",
                "answers" to listOf(
                    mapOf("text" to "a) nineteth", "isCorrect" to false),
                    mapOf("text" to "b) nineteeth", "isCorrect" to false),
                    mapOf("text" to "c) ninetieth", "isCorrect" to true)
                )),
            mapOf("id" to "q23", "text" to "3. Выберите правильный вариант:\n5. 201th",
                "answers" to listOf(
                    mapOf("text" to "a) one and two hundreth", "isCorrect" to false),
                    mapOf("text" to "b) one and second hundred", "isCorrect" to false),
                    mapOf("text" to "c) two hundred and first", "isCorrect" to true)
                )),
            mapOf("id" to "q24", "text" to "3. Выберите правильный вариант:\n6. 300th",
                "answers" to listOf(
                    mapOf("text" to "a) three hundreth", "isCorrect" to false),
                    mapOf("text" to "b) three hundredth", "isCorrect" to true),
                    mapOf("text" to "c) three hundreeth", "isCorrect" to false)
                )),
            mapOf("id" to "q25", "text" to "3. Выберите правильный вариант:\n7. 1,000th",
                "answers" to listOf(
                    mapOf("text" to "a) thousandth", "isCorrect" to true),
                    mapOf("text" to "b) first thousandth", "isCorrect" to false),
                    mapOf("text" to "c) thousandth one", "isCorrect" to false)
                )),
            mapOf("id" to "q26", "text" to "3. Выберите правильный вариант:\n8. 1,015",
                "answers" to listOf(
                    mapOf("text" to "a) thousand and fiftienth", "isCorrect" to false),
                    mapOf("text" to "b) fifteen thousandth", "isCorrect" to false),
                    mapOf("text" to "c) thousand and fifteenth", "isCorrect" to true)
                )),

            // Блок 4 – Дробные числительные (6 вопросов)
            mapOf("id" to "q27", "text" to "4. Выберите правильный вариант:\n1. 1/2 тонны",
                "answers" to listOf(
                    mapOf("text" to "a) three and five of tons", "isCorrect" to false),
                    mapOf("text" to "b) three and five of a ton", "isCorrect" to false),
                    mapOf("text" to "c) three fifth of a ton", "isCorrect" to true)
                )),
            mapOf("id" to "q28", "text" to "4. Выберите правильный вариант:\n2. 2/3 процента",
                "answers" to listOf(
                    mapOf("text" to "a) two third per cent", "isCorrect" to false),
                    mapOf("text" to "b) two thirds per cents", "isCorrect" to false),
                    mapOf("text" to "c) two and third of per cent", "isCorrect" to true)
                )),
            mapOf("id" to "q29", "text" to "4. Выберите правильный вариант:\n3. 1 1/2 часа",
                "answers" to listOf(
                    mapOf("text" to "a) one hour and a half", "isCorrect" to true),
                    mapOf("text" to "b) one and a half hour", "isCorrect" to false),
                    mapOf("text" to "c) one and one and two hour", "isCorrect" to false)
                )),
            mapOf("id" to "q30", "text" to "4. Выберите правильный вариант:\n4. 0,105 метра",
                "answers" to listOf(
                    mapOf("text" to "a) nought point one nought five of a metre", "isCorrect" to true),
                    mapOf("text" to "b) nought point one hundred and five of a metre", "isCorrect" to false),
                    mapOf("text" to "c) nought and one hundred and five of a metre", "isCorrect" to false)
                )),
            mapOf("id" to "q31", "text" to "4. Выберите правильный вариант:\n5. 2.5 процента",
                "answers" to listOf(
                    mapOf("text" to "a) two and five per cent", "isCorrect" to false),
                    mapOf("text" to "b) two and five per cents", "isCorrect" to false),
                    mapOf("text" to "c) two point five per cent", "isCorrect" to true)
                )),
            mapOf("id" to "q32", "text" to "4. Выберите правильный вариант:\n6. 17.562",
                "answers" to listOf(
                    mapOf("text" to "a) seventeen point five six two tons", "isCorrect" to false),
                    mapOf("text" to "b) one seven point five six two tons", "isCorrect" to true),
                    mapOf("text" to "c) seventeen point five hundred sixty-two tons", "isCorrect" to false)
                ))
        )
    }*/

    /*
    // ==================== ТЕСТ: MOOD (Наклонение) – III семестр ====================

    private fun uploadMoodTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Mood (Наклонение)")
                    .setMessage("Загружаем тест с 22 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Mood (Наклонение)",
                    "num" to 13,
                    "semester" to 3,
                    "totalScore" to 22,
                    "hasParts" to false
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем все 22 вопроса
                val questions = getMoodQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Mood (Наклонение)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 22",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Mood test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 22 ВОПРОСА ====================

    private fun getMoodQuestions(): List<Map<String, Any>> {
        return listOf(

            // Блок 1 – Выберите правильный вариант (6 вопросов)
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант:\n1. They … (go) to the beach if it … (be) warmer.",
                "answers" to listOf(
                    mapOf("text" to "a) They went to the beach if it would be warmer.", "isCorrect" to false),
                    mapOf("text" to "b) They went to the beach if it were warmer.", "isCorrect" to false),
                    mapOf("text" to "c) They would went to the beach if it were warmer.", "isCorrect" to false),
                    mapOf("text" to "d) They would go to the beach if it were warmer.", "isCorrect" to true)
                )),
            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n2. The dinner … (not be spoiled) if you … (not forget) the dish in the oven.",
                "answers" to listOf(
                    mapOf("text" to "a) The dinner would have not be spoiled if you did not had forgotten the dish in the oven.", "isCorrect" to false),
                    mapOf("text" to "b) The dinner would be not spoiled if you had not forgotten the dish in the oven.", "isCorrect" to false),
                    mapOf("text" to "c) The dinner would not have been spoiled if you had not forgotten the dish in the oven.", "isCorrect" to true),
                    mapOf("text" to "d) The dinner would not had been spoiled if you had not forgotten the dish in the oven.", "isCorrect" to false)
                )),
            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n3. Even though he … (know) how difficult the situation was, he … (not stop) the preparations.",
                "answers" to listOf(
                    mapOf("text" to "a) Even though he would know how difficult the situation was, he would not stop the preparations.", "isCorrect" to false),
                    mapOf("text" to "b) Even though he knew know how difficult the situation was, he would not stop the preparations.", "isCorrect" to false),
                    mapOf("text" to "c) Even though he had known how difficult the situation was, he would not have stopped the preparations.", "isCorrect" to false),
                    mapOf("text" to "d) Even though he had known how difficult the situation was, he would have not stopped the preparations.", "isCorrect" to true)
                )),
            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n4. If you really … (want) to buy the house, you … (can do) it even now.",
                "answers" to listOf(
                    mapOf("text" to "a) If you really want to buy the house, you could do it even now.", "isCorrect" to false),
                    mapOf("text" to "b) If you really wanted to buy the house, you could do it even now.", "isCorrect" to true),
                    mapOf("text" to "c) If you really wanted to buy the house, you would can do it even now.", "isCorrect" to false),
                    mapOf("text" to "d) If you really wanted to buy the house, you could did it even now.", "isCorrect" to false)
                )),
            mapOf("id" to "q5", "text" to "1. Выберите правильный вариант:\n5. Even if I … (have) a dictionary, I don't believe I … (be able) to write the test.",
                "answers" to listOf(
                    mapOf("text" to "a) Even if I had had a dictionary, I don't believe I would have been able to write the test.", "isCorrect" to true),
                    mapOf("text" to "b) Even if I had a dictionary, I don't believe I would have been able to write the test.", "isCorrect" to false),
                    mapOf("text" to "c) Even if I had a dictionary, I don't believe I would have been abled to write the test.", "isCorrect" to false),
                    mapOf("text" to "d) Even if I had had a dictionary, I don't believe I would had been able to write the test.", "isCorrect" to false)
                )),
            mapOf("id" to "q6", "text" to "1. Выберите правильный вариант:\n6. She … (know) how to behave if she … (be) a born lady.",
                "answers" to listOf(
                    mapOf("text" to "a) She would has known how to behave if she were a born lady.", "isCorrect" to false),
                    mapOf("text" to "b) She would know how to behave if she is a born lady.", "isCorrect" to false),
                    mapOf("text" to "c) She would knew how to behave if she were a born lady.", "isCorrect" to false),
                    mapOf("text" to "d) She would have known how to behave if she were a born lady.", "isCorrect" to true)
                )),

            // Блок 2 – Выберите правильный вариант перевода (8 вопросов)
            mapOf("id" to "q7", "text" to "2. Выберите правильный вариант перевода предложений в изъявительном наклонении в предложения нереального условия:\n1. She thought of her future and refused to marry the young man.",
                "answers" to listOf(
                    mapOf("text" to "a) If she had thought of her future she would not have refused to marry the young man.", "isCorrect" to false),
                    mapOf("text" to "b) If she had not thought of her future she would not have refused to marry the young man.", "isCorrect" to true),
                    mapOf("text" to "c) If she would have thought of her future and she refused to marry the young man.", "isCorrect" to false),
                    mapOf("text" to "d) If she did not think of her future she would have refused to marry the young man.", "isCorrect" to false)
                )),
            mapOf("id" to "q8", "text" to "2. Выберите правильный вариант перевода:\n2. He was deep in his thoughts and did not notice the \"no parking\" sign.",
                "answers" to listOf(
                    mapOf("text" to "a) If he had been deep in his thoughts he would not have notice the \"no parking\" sign.", "isCorrect" to false),
                    mapOf("text" to "b) If he were not deep in his thoughts he would notice the \"no parking\" sign.", "isCorrect" to false),
                    mapOf("text" to "c) If he had been not deep in his thoughts he would have notice the \"no parking\" sign.", "isCorrect" to false),
                    mapOf("text" to "d) If he had not been deep in his thoughts he would have noticed the \"no parking\" sign.", "isCorrect" to true)
                )),
            mapOf("id" to "q9", "text" to "2. Выберите правильный вариант перевода:\n3. There is no one to sit with the baby, I have to stay at home.",
                "answers" to listOf(
                    mapOf("text" to "a) If there were anyone to sit with the baby I would not have to stay at home.", "isCorrect" to true),
                    mapOf("text" to "b) If there were no one to sit with the baby I would have to stay at home.", "isCorrect" to false),
                    mapOf("text" to "c) If there would be anyone to sit with the baby I don't have to stay at home.", "isCorrect" to false),
                    mapOf("text" to "d) If there were anyone to sit with the baby I would not have had to stay at home.", "isCorrect" to false)
                )),
            mapOf("id" to "q10", "text" to "2. Выберите правильный вариант перевода:\n4. She did not think of the consequences and agreed to forge the document.",
                "answers" to listOf(
                    mapOf("text" to "a) If she thought of the consequences she would not agreed to forge the document.", "isCorrect" to false),
                    mapOf("text" to "b) If she had thought of the consequences she would not have agreed to forge the document.", "isCorrect" to true),
                    mapOf("text" to "c) If she had thought of the consequences she would not had agreed to forge the document.", "isCorrect" to false),
                    mapOf("text" to "d) If she had thought of the consequences she would not agree to forge the document.", "isCorrect" to false)
                )),
            mapOf("id" to "q11", "text" to "2. Выберите правильный вариант перевода:\n5. There were so many people there that nobody noticed his absence.",
                "answers" to listOf(
                    mapOf("text" to "a) If there had not been so many people there nobody would have noticed his absence.", "isCorrect" to false),
                    mapOf("text" to "b) If there had not were so many people anybody would noticed his absence.", "isCorrect" to false),
                    mapOf("text" to "c) If there had not been so many people there anybody would have noticed his absence.", "isCorrect" to true),
                    mapOf("text" to "d) If there had were not been so many people anybody would had noticed his absence.", "isCorrect" to false)
                )),
            mapOf("id" to "q12", "text" to "2. Выберите правильный вариант перевода:\n6. We don't like cheese. We don't buy it.",
                "answers" to listOf(
                    mapOf("text" to "a) If we don't like cheese we would not buy it.", "isCorrect" to false),
                    mapOf("text" to "b) If we liked cheese we would not buy it.", "isCorrect" to false),
                    mapOf("text" to "c) If we liked cheese we would buy it", "isCorrect" to true),
                    mapOf("text" to "d) If we like cheese we would bought it.", "isCorrect" to false)
                )),
            mapOf("id" to "q13", "text" to "2. Выберите правильный вариант перевода:\n7. He lost his temper and said things he did not really mean.",
                "answers" to listOf(
                    mapOf("text" to "a) If he had not lost his temper he would not have said the things he really meant.", "isCorrect" to false),
                    mapOf("text" to "b) If he had not lost his temper he would not have said the things he did not really mean.", "isCorrect" to true),
                    mapOf("text" to "c) If he had not loose his temper he would not have say the things he did not really mean.", "isCorrect" to false),
                    mapOf("text" to "d) If he have not lost his temper he would not had said the things he did not really mean.", "isCorrect" to false)
                )),
            mapOf("id" to "q14", "text" to "2. Выберите правильный вариант перевода:\n8. I don't know your aunt, I can't meet her at the station.",
                "answers" to listOf(
                    mapOf("text" to "a) If I know your aunt I would can meet her at the station.", "isCorrect" to false),
                    mapOf("text" to "b) If I knew your aunt I would could meet her at the station.", "isCorrect" to false),
                    mapOf("text" to "c) If I knew your aunt I could her at the station.", "isCorrect" to false),
                    mapOf("text" to "d) If I knew your aunt I could meet her at the station.", "isCorrect" to true)
                )),

            // Блок 3 – Выберите правильный вариант (4 вопроса)
            mapOf("id" to "q15", "text" to "3. Выберите правильный вариант:\n1. I had a sandwich for lunch. If I … (have) a proper lunch, I … (not feel) so hungry now.",
                "answers" to listOf(
                    mapOf("text" to "a) had, would not feel", "isCorrect" to false),
                    mapOf("text" to "b) had had, would not have felt", "isCorrect" to false),
                    mapOf("text" to "c) had had, would not feel", "isCorrect" to true)
                )),
            mapOf("id" to "q16", "text" to "3. Выберите правильный вариант:\n2. He told his friend, \"I'm not feeling very well. I … (not be) here today if I … (not promise) to come.\"",
                "answers" to listOf(
                    mapOf("text" to "a) would not be, had not promised", "isCorrect" to true),
                    mapOf("text" to "b) would be not, had not promised", "isCorrect" to false),
                    mapOf("text" to "c) would have been not, had not promised", "isCorrect" to false)
                )),
            mapOf("id" to "q17", "text" to "3. Выберите правильный вариант:\n3. If it (be) all the same to me, I (not come) and (talk) with you.",
                "answers" to listOf(
                    mapOf("text" to "a) is, would not came, talked", "isCorrect" to false),
                    mapOf("text" to "b) had been, would not come, talked", "isCorrect" to false),
                    mapOf("text" to "c) were, would not have come, talked", "isCorrect" to true)
                )),
            mapOf("id" to "q18", "text" to "3. Выберите правильный вариант:\n4. I can hardly keep my eyes open. If I … (go) to bed earlier last night, I … (not be) so tired now.",
                "answers" to listOf(
                    mapOf("text" to "a) went, am not be", "isCorrect" to false),
                    mapOf("text" to "b) had gone, would not be", "isCorrect" to true),
                    mapOf("text" to "c) went, would not be", "isCorrect" to false)
                )),

            // Блок 4 – I wish… (4 вопроса)
            mapOf("id" to "q19", "text" to "4. Выберите правильный вариант переделки следующих предложений в предложения, начинающиеся с I wish…:\n1. We lost the game yesterday. (win)",
                "answers" to listOf(
                    mapOf("text" to "a) I wish we won the game yesterday.", "isCorrect" to false),
                    mapOf("text" to "b) I wish we didn't win the game yesterday.", "isCorrect" to false),
                    mapOf("text" to "c) I wish we had won the game yesterday.", "isCorrect" to true)
                )),
            mapOf("id" to "q20", "text" to "4. Выберите правильный вариант:\n2. I sat at the back of the hall, and couldn't hear his speech very well. (every word)",
                "answers" to listOf(
                    mapOf("text" to "a) I wish I could heard every word.", "isCorrect" to false),
                    mapOf("text" to "b) I wish I could have heard every word.", "isCorrect" to true),
                    mapOf("text" to "c) I wish I could hear every word.", "isCorrect" to false)
                )),
            mapOf("id" to "q21", "text" to "4. Выберите правильный вариант:\n3. This house is very nice and comfortable. I'd like to buy it, but it is very expensive. (less expensive)",
                "answers" to listOf(
                    mapOf("text" to "a) I wish the house were less expensive.", "isCorrect" to true),
                    mapOf("text" to "b) I wish the house have been less expensive", "isCorrect" to false),
                    mapOf("text" to "c) I wish the house is less expensive.", "isCorrect" to false)
                )),
            mapOf("id" to "q22", "text" to "4. Выберите правильный вариант:\n4. Why didn't you watch the cat? It ate all the fish. I'm so angry with you. (more attentive)",
                "answers" to listOf(
                    mapOf("text" to "a) I wish you have been more attentive.", "isCorrect" to false),
                    mapOf("text" to "b) I wish you were not more attentive.", "isCorrect" to false),
                    mapOf("text" to "c) I wish you were more attentive.", "isCorrect" to true)
                ))
        )
    }*/

    /*
    // ==================== ТЕСТ: REPORTED SPEECH (Косвенная речь) ====================

    private fun uploadReportedSpeechTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Reported Speech")
                    .setMessage("Загружаем тест с 16 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Reported Speech (Косвенная речь)",
                    "num" to 12,
                    "semester" to 2,
                    "totalScore" to 16,
                    "hasParts" to false
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем все 16 вопросов
                val questions = getReportedSpeechQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Reported Speech (Косвенная речь)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 16",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Reported Speech test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 16 ВОПРОСОВ ====================

    private fun getReportedSpeechQuestions(): List<Map<String, Any>> {
        return listOf(

            // Блок 1 – Выберите правильный вариант (4 вопроса)
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант:\n1. He said (that) ostriches … fly.",
                "answers" to listOf(
                    mapOf("text" to "a) could", "isCorrect" to true),
                    mapOf("text" to "b) can", "isCorrect" to false)
                )),
            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n2. She said that the Amazon … the widest river in the world.",
                "answers" to listOf(
                    mapOf("text" to "a) is", "isCorrect" to false),
                    mapOf("text" to "b) was", "isCorrect" to true)
                )),
            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n3. He said that the Earth … the largest planet in the universe.",
                "answers" to listOf(
                    mapOf("text" to "a) is", "isCorrect" to false),
                    mapOf("text" to "b) was", "isCorrect" to true)
                )),
            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n4. She said that penguins … in the desert.",
                "answers" to listOf(
                    mapOf("text" to "a) was", "isCorrect" to true),
                    mapOf("text" to "b) is", "isCorrect" to false)
                )),

            // Блок 2 – Выберите правильный вариант (4 вопроса)
            mapOf("id" to "q5", "text" to "2. Выберите правильный вариант:\n1. She said (that) she … finished all her work.",
                "answers" to listOf(
                    mapOf("text" to "a) has", "isCorrect" to false),
                    mapOf("text" to "b) had", "isCorrect" to true)
                )),
            mapOf("id" to "q6", "text" to "2. Выберите правильный вариант:\n2. She asked him why he … looking at her like that.",
                "answers" to listOf(
                    mapOf("text" to "a) is", "isCorrect" to false),
                    mapOf("text" to "b) was", "isCorrect" to true)
                )),
            mapOf("id" to "q7", "text" to "2. Выберите правильный вариант:\n3. His mother said … play with matches.",
                "answers" to listOf(
                    mapOf("text" to "a) not to", "isCorrect" to true),
                    mapOf("text" to "b) don't", "isCorrect" to false)
                )),
            mapOf("id" to "q8", "text" to "2. Выберите правильный вариант:\n4. She asked her husband if he … be home soon.",
                "answers" to listOf(
                    mapOf("text" to "a) would", "isCorrect" to true),
                    mapOf("text" to "b) will", "isCorrect" to false)
                )),

            // Блок 3 – Выберите правильный вариант (4 вопроса)
            mapOf("id" to "q9", "text" to "3. Выберите правильный вариант:\n1. She asked how she … tell Tom the bad news.",
                "answers" to listOf(
                    mapOf("text" to "a) shall", "isCorrect" to false),
                    mapOf("text" to "b) should", "isCorrect" to true)
                )),
            mapOf("id" to "q10", "text" to "3. Выберите правильный вариант:\n2. He asked if he … go home immediately.",
                "answers" to listOf(
                    mapOf("text" to "a) can", "isCorrect" to false),
                    mapOf("text" to "b) could", "isCorrect" to true)
                )),
            mapOf("id" to "q11", "text" to "3. Выберите правильный вариант:\n3. He asked her if he … call her first name.",
                "answers" to listOf(
                    mapOf("text" to "a) might", "isCorrect" to true),
                    mapOf("text" to "b) may", "isCorrect" to false)
                )),
            mapOf("id" to "q12", "text" to "3. Выберите правильный вариант:\n4. He asked what time they … arrive in London.",
                "answers" to listOf(
                    mapOf("text" to "a) should", "isCorrect" to false),
                    mapOf("text" to "b) would", "isCorrect" to true)
                )),

            // Блок 4 – Выберите правильный вариант (4 вопроса)
            mapOf("id" to "q13", "text" to "4. Выберите правильный вариант:\n1. He asked what time the next bus … because he needed to get to the station.",
                "answers" to listOf(
                    mapOf("text" to "a) leaves", "isCorrect" to false),
                    mapOf("text" to "b) left", "isCorrect" to true)
                )),
            mapOf("id" to "q14", "text" to "4. Выберите правильный вариант:\n2. She told … go swimming in the lake, because the water was filthy.",
                "answers" to listOf(
                    mapOf("text" to "a) don't", "isCorrect" to false),
                    mapOf("text" to "b) not to", "isCorrect" to true)
                )),
            mapOf("id" to "q15", "text" to "4. Выберите правильный вариант:\n3. She asked him … take her ring, explaining that it was a present.",
                "answers" to listOf(
                    mapOf("text" to "a) not to", "isCorrect" to true),
                    mapOf("text" to "b) don't", "isCorrect" to false)
                )),
            mapOf("id" to "q16", "text" to "4. Выберите правильный вариант:\n4. She told him to stop making that noise, because she … concentrate.",
                "answers" to listOf(
                    mapOf("text" to "a) can't", "isCorrect" to false),
                    mapOf("text" to "b) couldn't", "isCorrect" to true)
                ))
        )
    }*/

    /*// ==================== ТЕСТ: SEQUENCE OF TENSES (Согласование времён) ====================

    private fun uploadSequenceOfTensesTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Sequence of Tenses")
                    .setMessage("Загружаем тест с 18 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Sequence of Tenses (Согласование времён)",
                    "num" to 11,
                    "semester" to 2,
                    "totalScore" to 18,
                    "hasParts" to false
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем все 18 вопросов
                val questions = getSequenceOfTensesQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Sequence of Tenses (Согласование времён)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 18",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Sequence of Tenses test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 18 ВОПРОСОВ ====================

    private fun getSequenceOfTensesQuestions(): List<Map<String, Any>> {
        return listOf(

            // Блок 1 – Выберите правильный вариант (8 вопросов)
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант:\n1. Vadim wanted to know what ... of the books which he (leave) here a day before.",
                "answers" to listOf(
                    mapOf("text" to "a) had become…had left", "isCorrect" to true),
                    mapOf("text" to "b) have become…have left", "isCorrect" to false)
                )),
            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n2. I finally said that I ... to hear any more about the subject.",
                "answers" to listOf(
                    mapOf("text" to "a) had not wished", "isCorrect" to false),
                    mapOf("text" to "b) did not wish", "isCorrect" to true)
                )),
            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n3. Mark remembered that he ... the cab at the hotel.",
                "answers" to listOf(
                    mapOf("text" to "a) had left", "isCorrect" to true),
                    mapOf("text" to "b) have left", "isCorrect" to false)
                )),
            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n4. We have heard that he ... very clever.",
                "answers" to listOf(
                    mapOf("text" to "a) was", "isCorrect" to false),
                    mapOf("text" to "b) is", "isCorrect" to true)
                )),
            mapOf("id" to "q5", "text" to "1. Выберите правильный вариант:\n5. My wife told me that in an hour she ... for a walk.",
                "answers" to listOf(
                    mapOf("text" to "a) would go", "isCorrect" to true),
                    mapOf("text" to "b) will go", "isCorrect" to false)
                )),
            mapOf("id" to "q6", "text" to "1. Выберите правильный вариант:\n6. The boy did not know that water (boil) at 100°.",
                "answers" to listOf(
                    mapOf("text" to "a) boiled", "isCorrect" to false),
                    mapOf("text" to "b) boils", "isCorrect" to true)
                )),
            mapOf("id" to "q7", "text" to "1. Выберите правильный вариант:\n7. Last year I skated much better than I ... now.",
                "answers" to listOf(
                    mapOf("text" to "a) do", "isCorrect" to true),
                    mapOf("text" to "b) did", "isCorrect" to false)
                )),
            mapOf("id" to "q8", "text" to "1. Выберите правильный вариант:\n8. The teacher told us that there ... 26 letters in the English alphabet.",
                "answers" to listOf(
                    mapOf("text" to "a) would be", "isCorrect" to false),
                    mapOf("text" to "b) are", "isCorrect" to true)
                )),

            // Блок 2 – Выберите правильный вариант перевода (4 вопроса)
            mapOf("id" to "q9", "text" to "2. Выберите правильный вариант перевода:\n1. Он сказал, что любит эту пьесу.",
                "answers" to listOf(
                    mapOf("text" to "a) He said he likes that play.", "isCorrect" to false),
                    mapOf("text" to "b) He said he liked that play.", "isCorrect" to true)
                )),
            mapOf("id" to "q10", "text" to "2. Выберите правильный вариант перевода:\n2. На прошлом уроке Павел не знал, что свет движется скорее, чем звук.",
                "answers" to listOf(
                    mapOf("text" to "a) Last lesson Paul didn't know that the light moves faster than the sound.", "isCorrect" to true),
                    mapOf("text" to "b) Last lesson Paul didn't know that the light moved faster than the sound.", "isCorrect" to false)
                )),
            mapOf("id" to "q11", "text" to "2. Выберите правильный вариант перевода:\n3. Он сказал мне вчера, что раньше он учился в университете.",
                "answers" to listOf(
                    mapOf("text" to "a) He told me yesterday that he had studied at a university.", "isCorrect" to true),
                    mapOf("text" to "b) He told me yesterday that he studied at a university.", "isCorrect" to false)
                )),
            mapOf("id" to "q12", "text" to "2. Выберите правильный вариант перевода:\n4. Мы решили на прошлой неделе, что будущим летом мы все поедем в Крым.",
                "answers" to listOf(
                    mapOf("text" to "a) Last week we decided that we all would go to the Crimea next summer.", "isCorrect" to true),
                    mapOf("text" to "b) Last week we decided that we all will go to the Crimea next summer.", "isCorrect" to false)
                )),

            // Блок 3 – Выберите правильный вариант (6 вопросов)
            mapOf("id" to "q13", "text" to "3. Выберите правильный вариант:\n1. When Jack came home, his sister told him that Peter had rung him up half an hour….",
                "answers" to listOf(
                    mapOf("text" to "a) ago", "isCorrect" to false),
                    mapOf("text" to "b) before", "isCorrect" to true)
                )),
            mapOf("id" to "q14", "text" to "3. Выберите правильный вариант:\n2. \"Did you work or were you still going to school two years…?\" the teacher asked one of the students.",
                "answers" to listOf(
                    mapOf("text" to "a) ago", "isCorrect" to true),
                    mapOf("text" to "b) before", "isCorrect" to false)
                )),
            mapOf("id" to "q15", "text" to "3. Выберите правильный вариант:\n3. Last week I asked my friend to translate this article, but he said he couldn't do it…and. said he would do it….",
                "answers" to listOf(
                    mapOf("text" to "a) now…in two days", "isCorrect" to false),
                    mapOf("text" to "b) then…two days later", "isCorrect" to true)
                )),
            mapOf("id" to "q16", "text" to "3. Выберите правильный вариант:\n4. My friend spent his last week-end in the country. He says the weather was fine….",
                "answers" to listOf(
                    mapOf("text" to "a) today", "isCorrect" to false),
                    mapOf("text" to "b) that day", "isCorrect" to true)
                )),
            mapOf("id" to "q17", "text" to "3. Выберите правильный вариант:\n5. I gave my friend a book last week and he said he would return it …, but he hasn't done so yet.",
                "answers" to listOf(
                    mapOf("text" to "a) tomorrow", "isCorrect" to false),
                    mapOf("text" to "b) next day", "isCorrect" to true)
                )),
            mapOf("id" to "q18", "text" to "3. Выберите правильный вариант:\n6. \"Are you going to give a talk…?\" my friend asked me.",
                "answers" to listOf(
                    mapOf("text" to "a) tomorrow", "isCorrect" to true),
                    mapOf("text" to "b) next day", "isCorrect" to false)
                ))
        )
    }*/

    /*
    // ==================== ТЕСТ: PASSIVE VOICE (Страдательный залог) ====================

    // ==================== ТЕСТ: MODAL VERBS (Модальные глаголы) ====================

    private fun uploadModalVerbsTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Modal Verbs")
                    .setMessage("Загружаем тест с 11 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Modal Verbs (Модальные глаголы)",
                    "num" to 10,
                    "semester" to 2,
                    "totalScore" to 11,
                    "hasParts" to false
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем все 11 вопросов
                val questions = getModalVerbsQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Modal Verbs (Модальные глаголы)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 11",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Modal Verbs test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 11 ВОПРОСОВ ====================

    private fun getModalVerbsQuestions(): List<Map<String, Any>> {
        return listOf(

            // Блок 1 – Вставьте can или be able to (5 вопросов)
            mapOf("id" to "q1", "text" to "1. Вставьте в пропуски can или be able to в нужной временной форме:\n1. By the time Phillis was ten, he … speak three languages.",
                "answers" to listOf(
                    mapOf("text" to "a) could", "isCorrect" to true),
                    mapOf("text" to "b) was able", "isCorrect" to false)
                )),
            mapOf("id" to "q2", "text" to "1. Вставьте в пропуски can или be able to в нужной временной форме:\n2. If you don't tell me what your problem is, I … help you.",
                "answers" to listOf(
                    mapOf("text" to "a) will not be able to", "isCorrect" to true),
                    mapOf("text" to "b) will not can", "isCorrect" to false)
                )),
            mapOf("id" to "q3", "text" to "1. Вставьте в пропуски can или be able to в нужной временной форме:\n3. I got home early last night, so I … watch my favourite program on TV.",
                "answers" to listOf(
                    mapOf("text" to "a) could", "isCorrect" to false),
                    mapOf("text" to "b) was able to", "isCorrect" to true)
                )),
            mapOf("id" to "q4", "text" to "1. Вставьте в пропуски can или be able to в нужной временной форме:\n4. I … eat anything when I was younger, but now I have to be more careful.",
                "answers" to listOf(
                    mapOf("text" to "a) could", "isCorrect" to true),
                    mapOf("text" to "b) was able to", "isCorrect" to false)
                )),
            mapOf("id" to "q5", "text" to "1. Вставьте в пропуски can или be able to в нужной временной форме:\n5. He … pass the exam because he had studied hard.",
                "answers" to listOf(
                    mapOf("text" to "a) could", "isCorrect" to false),
                    mapOf("text" to "b) was able to", "isCorrect" to true)
                )),

            // Блок 2 – Выберите правильный вариант (6 вопросов)
            mapOf("id" to "q6", "text" to "2. Выберите правильный вариант:\n1. I wonder where Paul is. He ... be at work because he never works on Sunday.",
                "answers" to listOf(
                    mapOf("text" to "a) is not able to", "isCorrect" to false),
                    mapOf("text" to "b) can't", "isCorrect" to true)
                )),
            mapOf("id" to "q7", "text" to "2. Выберите правильный вариант:\n2. He … be at Sally's, but I doubt it because they haven't been speaking lately.",
                "answers" to listOf(
                    mapOf("text" to "a) may", "isCorrect" to true),
                    mapOf("text" to "b) ought to", "isCorrect" to false)
                )),
            mapOf("id" to "q8", "text" to "2. Выберите правильный вариант:\n3. He … have gone bowling, I'm almost sure, he told me he was going to.",
                "answers" to listOf(
                    mapOf("text" to "a) might", "isCorrect" to false),
                    mapOf("text" to "b) must", "isCorrect" to true)
                )),
            mapOf("id" to "q9", "text" to "2. Выберите правильный вариант:\n4. Your car is in terrible condition. You … get a new one before the police stop you.",
                "answers" to listOf(
                    mapOf("text" to "a) may", "isCorrect" to false),
                    mapOf("text" to "b) ought to", "isCorrect" to true)
                )),
            mapOf("id" to "q10", "text" to "2. Выберите правильный вариант:\n5. Don't deceive me, you ... regret it.",
                "answers" to listOf(
                    mapOf("text" to "a) shall", "isCorrect" to true),
                    mapOf("text" to "b) must", "isCorrect" to false)
                )),
            mapOf("id" to "q11", "text" to "2. Выберите правильный вариант:\n6. Sally, have you spent the money I lent you last week? You … have spent it all!",
                "answers" to listOf(
                    mapOf("text" to "a) ought not to", "isCorrect" to true),
                    mapOf("text" to "b) could not", "isCorrect" to false)
                ))
        )
    }*/
    /*private fun uploadPassiveVoiceTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Passive Voice")
                    .setMessage("Загружаем тест с 36 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Passive Voice (Страдательный залог)",
                    "num" to 9,
                    "semester" to 2,
                    "totalScore" to 36,
                    "hasParts" to false
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем все 36 вопросов
                val questions = getPassiveVoiceQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Passive Voice (Страдательный залог)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 36",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Passive Voice test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 36 ВОПРОСОВ ====================

    private fun getPassiveVoiceQuestions(): List<Map<String, Any>> {
        return listOf(

            // Блок 1 – Выберите правильный вариант (вопросы 1–12)
            mapOf("id" to "q1", "text" to "Выберите правильный вариант:\n1. She is very selfish, she … (spoil) by her parents.",
                "answers" to listOf(
                    mapOf("text" to "a) is spoiled", "isCorrect" to true),
                    mapOf("text" to "b) has been spoiled", "isCorrect" to false),
                    mapOf("text" to "c) was been spoiled", "isCorrect" to false)
                )),
            mapOf("id" to "q2", "text" to "Выберите правильный вариант:\n2. This room … (use) only on special occasions.",
                "answers" to listOf(
                    mapOf("text" to "a) used", "isCorrect" to false),
                    mapOf("text" to "b) is used", "isCorrect" to true),
                    mapOf("text" to "c) be used", "isCorrect" to false)
                )),
            mapOf("id" to "q3", "text" to "Выберите правильный вариант:\n3. Why nothing … (to do) about it at the time?",
                "answers" to listOf(
                    mapOf("text" to "a) was be done", "isCorrect" to false),
                    mapOf("text" to "b) was being done", "isCorrect" to true),
                    mapOf("text" to "c) was did", "isCorrect" to false)
                )),
            mapOf("id" to "q4", "text" to "Выберите правильный вариант:\n4. Dictionaries … (may not use) at the examination.",
                "answers" to listOf(
                    mapOf("text" to "a) may used be not", "isCorrect" to false),
                    mapOf("text" to "b) may be not used", "isCorrect" to false),
                    mapOf("text" to "c) may not be used", "isCorrect" to true),
                    mapOf("text" to "d) may are not be used", "isCorrect" to false)
                )),
            mapOf("id" to "q5", "text" to "Выберите правильный вариант:\n5. She promised that nothing … (to do) till he came back.",
                "answers" to listOf(
                    mapOf("text" to "a) will be done", "isCorrect" to false),
                    mapOf("text" to "b) would be done", "isCorrect" to true),
                    mapOf("text" to "c) would been done", "isCorrect" to false),
                    mapOf("text" to "d) would be did", "isCorrect" to false)
                )),
            mapOf("id" to "q6", "text" to "Выберите правильный вариант:\n6. Wherever I went I found evidence that the camp … (to leave) only a short time before we arrived.",
                "answers" to listOf(
                    mapOf("text" to "a) would left", "isCorrect" to false),
                    mapOf("text" to "b) was left", "isCorrect" to false),
                    mapOf("text" to "c) had been left", "isCorrect" to true)
                )),
            mapOf("id" to "q7", "text" to "Выберите правильный вариант:\n7. There's nothing here. Everything … (to take) away.",
                "answers" to listOf(
                    mapOf("text" to "a) has taken", "isCorrect" to false),
                    mapOf("text" to "b) was being taken", "isCorrect" to false),
                    mapOf("text" to "c) has been taken", "isCorrect" to true)
                )),
            mapOf("id" to "q8", "text" to "Выберите правильный вариант:\n8. At lunch nothing … (discuss) but the latest news.",
                "answers" to listOf(
                    mapOf("text" to "a) be discussed", "isCorrect" to false),
                    mapOf("text" to "b) was be discussing", "isCorrect" to false),
                    mapOf("text" to "c) was being discussed", "isCorrect" to true)
                )),
            mapOf("id" to "q9", "text" to "Выберите правильный вариант:\n9. He … (take) to hospital this afternoon, and … (operate on) tomorrow morning.",
                "answers" to listOf(
                    mapOf("text" to "a) has been taken, will be operated on", "isCorrect" to false),
                    mapOf("text" to "b) is taken, will be operated on", "isCorrect" to false),
                    mapOf("text" to "c) was taken, will been operated on", "isCorrect" to true),
                    mapOf("text" to "d) had been taken will be operated on", "isCorrect" to false)
                )),
            mapOf("id" to "q10", "text" to "Выберите правильный вариант:\n10. The damaged buildings … (reconstruct) now, the reconstruction … (finish) by the end of the year.",
                "answers" to listOf(
                    mapOf("text" to "a) are reconstructed, will have been finished", "isCorrect" to false),
                    mapOf("text" to "b) are being reconstructed, will finish", "isCorrect" to false),
                    mapOf("text" to "c) have been reconstructed, will have finished", "isCorrect" to false),
                    mapOf("text" to "d) are being reconstructed, will have been finished", "isCorrect" to true)
                )),
            mapOf("id" to "q11", "text" to "Выберите правильный вариант:\n11. She heard footsteps, she thought she … (follow).",
                "answers" to listOf(
                    mapOf("text" to "a) followed", "isCorrect" to false),
                    mapOf("text" to "b) was followed", "isCorrect" to false),
                    mapOf("text" to "c) had followed", "isCorrect" to false),
                    mapOf("text" to "d) was being followed", "isCorrect" to true)
                )),
            mapOf("id" to "q12", "text" to "Выберите правильный вариант:\n12. Why don't you use your car? - it … (repair) now, I had a bad accident a week ago. - Anybody … (hurt)?",
                "answers" to listOf(
                    mapOf("text" to "a) is being repaired, was anybody hurt", "isCorrect" to true),
                    mapOf("text" to "b) is repaired, anybody was hurt", "isCorrect" to false),
                    mapOf("text" to "c) it has been repaired, did anybody hurt", "isCorrect" to false),
                    mapOf("text" to "d) is being repaired, had anybody hurt", "isCorrect" to false)
                )),

            // Блок 2 – Выберите правильный вариант перевода (вопросы 13–20)
            mapOf("id" to "q13", "text" to "Выберите правильный вариант перевода предложения, стоящего в активном залоге, в пассивный:\n1. An expert is restoring the antique car.",
                "answers" to listOf(
                    mapOf("text" to "a) The antique car is been restored", "isCorrect" to false),
                    mapOf("text" to "b) The antique car is being restored by an expert.", "isCorrect" to true),
                    mapOf("text" to "c) The antique car is been restoring by an expert.", "isCorrect" to false)
                )),
            mapOf("id" to "q14", "text" to "Выберите правильный вариант перевода:\n2. Steven Spielberg has directed a lot of successful films.",
                "answers" to listOf(
                    mapOf("text" to "a) A lot of successful films has been directed by Steven Spielberg.", "isCorrect" to false),
                    mapOf("text" to "b) A lot of successful films have been directed by Steven Spielberg.", "isCorrect" to true),
                    mapOf("text" to "c) By Steven Spielberg have been directed а lot of successful films.", "isCorrect" to false)
                )),
            mapOf("id" to "q15", "text" to "Выберите правильный вариант перевода:\n3. The judge has fined him ?300.",
                "answers" to listOf(
                    mapOf("text" to "a) He was fined ?300 by the judge.", "isCorrect" to false),
                    mapOf("text" to "b) ?300 have been found for him by the judge.", "isCorrect" to false),
                    mapOf("text" to "c) He has been fined ?300 by the judge.", "isCorrect" to true)
                )),
            mapOf("id" to "q16", "text" to "Выберите правильный вариант перевода:\n4. A number of reporters will meet the professor at the airport.",
                "answers" to listOf(
                    mapOf("text" to "a) The professor will be met by a number of reporters at the airport.", "isCorrect" to true),
                    mapOf("text" to "b) A number of reporters will be met by the professor at the airport.", "isCorrect" to false),
                    mapOf("text" to "c) The professor will been meet by a number of reporters at the airport.", "isCorrect" to false)
                )),
            mapOf("id" to "q17", "text" to "Выберите правильный вариант перевода:\n5. A famous designer is going to redecorate the President's house.",
                "answers" to listOf(
                    mapOf("text" to "a) The President's house is being gone to be redecorated by a famous designer.", "isCorrect" to false),
                    mapOf("text" to "b) A famous designer is gone to redecorate the President's house.", "isCorrect" to false),
                    mapOf("text" to "c) The President's house is going to be redecorated by a famous designer.", "isCorrect" to true)
                )),
            mapOf("id" to "q18", "text" to "Выберите правильный вариант перевода:\n6. A nightmare woke Mary up.",
                "answers" to listOf(
                    mapOf("text" to "a) Mary was waken up by a nightmare.", "isCorrect" to true),
                    mapOf("text" to "b) A nightmare was waked Mary up.", "isCorrect" to false),
                    mapOf("text" to "c) A nightmare is woked Mary up.", "isCorrect" to false)
                )),
            mapOf("id" to "q19", "text" to "Выберите правильный вариант перевода:\n7. Muslims celebrate Ramadan.",
                "answers" to listOf(
                    mapOf("text" to "a) Ramadan are celebrated by Muslims.", "isCorrect" to false),
                    mapOf("text" to "b) Ramadan is celebrated by Muslims.", "isCorrect" to true),
                    mapOf("text" to "c) Ramadan is been celebrated by Muslims", "isCorrect" to false)
                )),
            mapOf("id" to "q20", "text" to "Выберите правильный вариант перевода:\n8. Van Gogh painted \"Sunflowers\".",
                "answers" to listOf(
                    mapOf("text" to "a) \"Sunflowers\" were painted by Van Gogh.", "isCorrect" to false),
                    mapOf("text" to "b) By Van Gogh were painted \"Sunflowers\".", "isCorrect" to false),
                    mapOf("text" to "c) \"Sunflowers\" was painted by Van Gogh.", "isCorrect" to true)
                )),

            // Блок 3 – Past Continuous / Past Perfect Passive (вопросы 21–24)
            mapOf("id" to "q21", "text" to "Выберите правильный вариант (Past Continuous Passive или Past Perfect Passive):\n1. They didn't leave the restaurant until the bill ... (pay).",
                "answers" to listOf(
                    mapOf("text" to "a) had paid", "isCorrect" to false),
                    mapOf("text" to "b) was being paid", "isCorrect" to false),
                    mapOf("text" to "c) had been paid", "isCorrect" to true),
                    mapOf("text" to "d) was paid", "isCorrect" to false)
                )),
            mapOf("id" to "q22", "text" to "Выберите правильный вариант:\n2. I couldn't go to my favourite cafe for a drink. It … (redecorate).",
                "answers" to listOf(
                    mapOf("text" to "a) was being redecorated", "isCorrect" to true),
                    mapOf("text" to "b) had been redecorated", "isCorrect" to false),
                    mapOf("text" to "c) had not been redecorated", "isCorrect" to false),
                    mapOf("text" to "d) was been redecorating", "isCorrect" to false)
                )),
            mapOf("id" to "q23", "text" to "Выберите правильный вариант:\n3. He … (take) to the hospital when the ambulance crashed.",
                "answers" to listOf(
                    mapOf("text" to "a) was taking", "isCorrect" to false),
                    mapOf("text" to "b) was being taken", "isCorrect" to true),
                    mapOf("text" to "c) has been taken", "isCorrect" to false),
                    mapOf("text" to "d) was to be had taken", "isCorrect" to false)
                )),
            mapOf("id" to "q24", "text" to "Выберите правильный вариант:\n4. The search was called off. The escaped criminal … (find).",
                "answers" to listOf(
                    mapOf("text" to "a) was not found", "isCorrect" to false),
                    mapOf("text" to "b) was being found", "isCorrect" to false),
                    mapOf("text" to "c) had been found", "isCorrect" to true),
                    mapOf("text" to "d) had not been found", "isCorrect" to false)
                )),

            // Блок 4 – Способы перевода (вопросы 25–28)
            mapOf("id" to "q25", "text" to "Выберите правильный вариант способов перевода активных предложений в пассивные:\n1. They have offered him the job.",
                "answers" to listOf(
                    mapOf("text" to "a) He was offered a job. A job was offered to him.", "isCorrect" to false),
                    mapOf("text" to "b) He has been offered the job. The job has been offered to him.", "isCorrect" to true),
                    mapOf("text" to "c) They have been offered the job. The job has been offered to him.", "isCorrect" to false),
                    mapOf("text" to "d) He have been offered the job by them The have been offered to him by them.", "isCorrect" to false)
                )),
            mapOf("id" to "q26", "text" to "Выберите правильный вариант:\n2. She will send you a fax.",
                "answers" to listOf(
                    mapOf("text" to "a) You will be sent a fax. A fax will be sent to you.", "isCorrect" to true),
                    mapOf("text" to "b) A fax will be sent to her. A fax will be sent to you.", "isCorrect" to false),
                    mapOf("text" to "c) You will be send a fax. A fax will be send to you.", "isCorrect" to false),
                    mapOf("text" to "d) A fax will be send to her. A fax will be send to you.", "isCorrect" to false)
                )),
            mapOf("id" to "q27", "text" to "Выберите правильный вариант:\n3. They are going to show me a new technique.",
                "answers" to listOf(
                    mapOf("text" to "a) I am going to show them a new technique. A new technique is going to show me.", "isCorrect" to false),
                    mapOf("text" to "b) They are going to be shown a new technique. I am going to be shown a new technique.", "isCorrect" to false),
                    mapOf("text" to "c) They are going to be shown a new technique. A new technique is going to be shown to me.", "isCorrect" to false),
                    mapOf("text" to "d) I am going to be shown a new technique. A new technique is going to be shown to me.", "isCorrect" to true)
                )),
            mapOf("id" to "q28", "text" to "Выберите правильный вариант:\n4. They should give the students extra lessons.",
                "answers" to listOf(
                    mapOf("text" to "a) The students are should be given extra lessons. Extra lessons are should be given to students.", "isCorrect" to false),
                    mapOf("text" to "b) The students should be given extra lessons. Extra lessons should be given to students.", "isCorrect" to true),
                    mapOf("text" to "c) The students should are be given extra lessons. Extra lessons should are be given to students.", "isCorrect" to false),
                    mapOf("text" to "d) They should the students be given extra lessons. They should extra lessons be given to students.", "isCorrect" to false)
                )),

            // Блок 5 – Перевод предложений в пассивном залоге (вопросы 29–36)
            mapOf("id" to "q29", "text" to "Выберите правильный вариант перевода в пассивном залоге следующих предложений:\n1. На него нельзя положиться.",
                "answers" to listOf(
                    mapOf("text" to "a) He can't be relied on.", "isCorrect" to true),
                    mapOf("text" to "b) He can't be rely on.", "isCorrect" to false),
                    mapOf("text" to "c) He cannot is relied on.", "isCorrect" to false)
                )),
            mapOf("id" to "q30", "text" to "Выберите правильный вариант:\n2. Он не любит, когда над ним смеются.",
                "answers" to listOf(
                    mapOf("text" to "a) He doesn't like been laughed.", "isCorrect" to false),
                    mapOf("text" to "b) He doesn't like to be laughed at.", "isCorrect" to true),
                    mapOf("text" to "c) To be laughed he doesn't like at.", "isCorrect" to false)
                )),
            mapOf("id" to "q31", "text" to "Выберите правильный вариант:\n3. За доктором послали.",
                "answers" to listOf(
                    mapOf("text" to "a) The doctor is been sent for.", "isCorrect" to false),
                    mapOf("text" to "b) For the doctor has been sent.", "isCorrect" to false),
                    mapOf("text" to "c) The doctor has been sent for.", "isCorrect" to true)
                )),
            mapOf("id" to "q32", "text" to "Выберите правильный вариант:\n4. За машиной хорошо смотрели.",
                "answers" to listOf(
                    mapOf("text" to "a) The car was well looked after.", "isCorrect" to true),
                    mapOf("text" to "b) After the car was well looked.", "isCorrect" to false),
                    mapOf("text" to "c) They were looked after the car well.", "isCorrect" to false)
                )),
            mapOf("id" to "q33", "text" to "Выберите правильный вариант:\n5. Её слушали в тишине.",
                "answers" to listOf(
                    mapOf("text" to "a) She was listened by in silence.", "isCorrect" to false),
                    mapOf("text" to "b) She was being listened to in silence.", "isCorrect" to false),
                    mapOf("text" to "c) She was listened in silence.", "isCorrect" to true)
                )),
            mapOf("id" to "q34", "text" to "Выберите правильный вариант:\n6. В больнице о нем позаботятся.",
                "answers" to listOf(
                    mapOf("text" to "a) They will take care of him in hospital.", "isCorrect" to false),
                    mapOf("text" to "b) He will be taken care of in hospital.", "isCorrect" to true),
                    mapOf("text" to "c) Of him will be taken care of in hospital.", "isCorrect" to false)
                )),
            mapOf("id" to "q35", "text" to "Выберите правильный вариант:\n7. Люди говорят о её небрежности.",
                "answers" to listOf(
                    mapOf("text" to "a) She is spoken about her carelessness by people.", "isCorrect" to true),
                    mapOf("text" to "b) About her carelessness is spoken by people.", "isCorrect" to false),
                    mapOf("text" to "c) Her carelessness is spoken by people about.", "isCorrect" to false)
                )),
            mapOf("id" to "q36", "text" to "Выберите правильный вариант:\n8. Тебя везде ищут.",
                "answers" to listOf(
                    mapOf("text" to "a) You are be looking for everywhere.", "isCorrect" to false),
                    mapOf("text" to "b) You are being looked for everywhere.", "isCorrect" to true),
                    mapOf("text" to "c) You are be looked everywhere for.", "isCorrect" to false)
                ))
        )
    }*/

    // ==================== ТЕСТ 1: DRILLING RIG ====================

    /*private fun uploadDrillingRigTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Drilling Rig")
                    .setMessage("Загружаем тест... Это может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Drilling Rig",
                    "num" to 3,           // номер теста по вашему усмотрению
                    "semester" to 3,
                    "totalScore" to 20,
                    "hasParts" to true
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // 2. Создаём часть теста
                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                // 3. Добавляем вопросы
                val questions = getDrillingRigQuestions()
                for (question in questions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(partId)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Drilling Rig' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: ${questions.size}",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Drilling Rig test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

    private fun getDrillingRigQuestions(): List<Map<String, Any>> {
        return listOf(
            // Task 1 – Matching (8 вопросов)
            mapOf(
                "id" to "q1",
                "text" to "Task 1. Match the given English words and word combinations with their Russian equivalents.\n\n" +
                        "Введите последовательность букв через точку с запятой (пример: c;a;b;e;d;g;f;h)",
                "isManualInput" to true,
                "correctSequence" to "c;a;b;f;d;e;h;g",   // 1-c;2-a;3-b;4-f;5-d;6-e;7-h;8-g
                "maxPoints" to 8
            ),

            // Task 2 – True/False (7 вопросов)
            mapOf(
                "id" to "q2",
                "text" to "Task 2. Think if the given statements are true or false.\n\n" +
                        "Введите последовательность ответов через точку с запятой (T = true, F = false).\n" +
                        "Пример: F;T;T;T;F;F;T",
                "isManualInput" to true,
                "correctSequence" to "F;T;T;T;F;F;T",     // ответы из файла
                "maxPoints" to 7
            )
        )
    }

    // ==================== ТЕСТ 2: GLOBAL FINANCIAL SYSTEMS ====================

    private fun uploadGlobalFinancialSystemsTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Global Financial Systems")
                    .setMessage("Загружаем тест... Это может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                val testData = hashMapOf<String, Any>(
                    "title" to "Global Financial Systems",
                    "num" to 3,
                    "semester" to 3,
                    "totalScore" to 30,      // Task 1 = 8 + Task 2 = 16 пунктов
                    "hasParts" to true
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                val partData = hashMapOf<String, Any>(
                    "title" to "Основная часть",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val partDocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val partId = partDocRef.id
                partDocRef.set(partData).await()

                val questions = getGlobalFinancialSystemsQuestions()
                for (q in questions) {
                    db.collection("tests").document(testId)
                        .collection("parts").document(partId)
                        .collection("questions").document(q["id"] as String)
                        .set(q).await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Global Financial Systems' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: ${questions.size}",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                //progressDialog.dismiss()
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

    private fun getGlobalFinancialSystemsQuestions(): List<Map<String, Any>> {
        return listOf(
            // Task 1 – True/False (8 вопросов)
            mapOf(
                "id" to "q1",
                "text" to "Task 1. Decide whether the given statements are true or false.\n\n" +
                        "Введите последовательность ответов через точку с запятой (T = true, F = false).\n" +
                        "Пример: F;F;T;F;T;T;F;F",
                "isManualInput" to true,
                "correctSequence" to "F;F;T;F;T;T;F;F",   // 1F 2F 3T 4F 5T 6T 7F 8F
                "maxPoints" to 8
            ),

            // Task 2 – Matching (16 пунктов)
            mapOf(
                "id" to "q2",
                "text" to "Task 2. Match the Russian words and word combinations with their English equivalents.\n\n" +
                        "Введите последовательность букв через точку с запятой (пример: c;a;b;f;d;e;h;g;j;i;m;k;l;p;n;o)",
                "isManualInput" to true,
                "correctSequence" to "c;a;b;f;d;e;h;g;j;i;m;k;l;p;n;o",   // согласно вашему ответу
                "maxPoints" to 16
            )
        )
    }*/
}