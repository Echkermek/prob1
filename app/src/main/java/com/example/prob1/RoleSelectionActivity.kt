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
/*
        // Кнопка для теста Drilling Rig
        binding.btnUploadGerund.setOnClickListener {   // измените ID кнопки под вашу разметку
            uploadFunctionalPartsTest()
        }*/

        // Кнопка для теста Global Financial Systems
        /*binding.buttonFinance.setOnClickListener {    // измените ID кнопки под вашу разметку
            uploadGlobalFinancialSystemsTest()
        }*/
    }

    // ==================== ТЕСТ: SEQUENCE OF TENSES (Согласование времён) ====================

    // ==================== ТЕСТ: INFINITIVE (Инфинитив) ====================


    // ==================== ТЕСТ: СЛУЖЕБНЫЕ ЧАСТИ РЕЧИ (С ПОДЧАСТЯМИ) ====================

    private fun uploadFunctionalPartsTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Служебные части речи")
                    .setMessage("Загружаем тест с 2 частями (Предлог и Союз)...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста (hasParts = true)
                val testData = hashMapOf<String, Any>(
                    "title" to "Служебные части речи",
                    "num" to 14,  // номер теста (можно изменить)
                    "semester" to 4,  // IV семестр
                    "totalScore" to 52,  // 42 (предлог) + 10 (союз)
                    "hasParts" to true
                )

                val testDocRef = db.collection("tests").document()
                val testId = testDocRef.id
                testDocRef.set(testData).await()

                // ========== ЧАСТЬ 1: ПРЕДЛОГ (42 вопроса) ==========
                val part1Data = hashMapOf<String, Any>(
                    "title" to "Предлог",
                    "num" to 1,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val part1DocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val part1Id = part1DocRef.id
                part1DocRef.set(part1Data).await()

                // Добавляем вопросы для части 1 (Предлог)
                val prepositionQuestions = getPrepositionQuestions()
                for (question in prepositionQuestions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(part1Id)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                // ========== ЧАСТЬ 2: СОЮЗ (10 вопросов) ==========
                val part2Data = hashMapOf<String, Any>(
                    "title" to "Союз",
                    "num" to 2,
                    "enterAnswer" to false,
                    "lecId" to "not"
                )

                val part2DocRef = db.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .document()
                val part2Id = part2DocRef.id
                part2DocRef.set(part2Data).await()

                // Добавляем вопросы для части 2 (Союз)
                val conjunctionQuestions = getConjunctionQuestions()
                for (question in conjunctionQuestions) {
                    db.collection("tests")
                        .document(testId)
                        .collection("parts")
                        .document(part2Id)
                        .collection("questions")
                        .document(question["id"] as String)
                        .set(question)
                        .await()
                }

                progressDialog.dismiss()

                Toast.makeText(
                    this@RoleSelectionActivity,
                    "✅ Тест 'Служебные части речи' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Часть 1 (Предлог): $part1Id - 42 вопроса\n" +
                            "Часть 2 (Союз): $part2Id - 10 вопросов\n" +
                            "Всего вопросов: 52",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Functional Parts test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ ВОПРОСЫ ДЛЯ ЧАСТИ 1: ПРЕДЛОГ (42 вопроса) ====================

    private fun getPrepositionQuestions(): List<Map<String, Any>> {
        return listOf(
            // ========== Упражнение 1 (6 вопросов) ==========
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант:\n\n1. She blamed him … the murder.",
                "answers" to listOf(
                    mapOf("text" to "a) for", "isCorrect" to true),
                    mapOf("text" to "b) to", "isCorrect" to false)
                )),

            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n\n2. They arrived … London at 7:30.",
                "answers" to listOf(
                    mapOf("text" to "a) at", "isCorrect" to false),
                    mapOf("text" to "b) in", "isCorrect" to true)
                )),

            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n\n3. I must apologise … Mary … the delay.",
                "answers" to listOf(
                    mapOf("text" to "a) to; in", "isCorrect" to false),
                    mapOf("text" to "b) to; for", "isCorrect" to true)
                )),

            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n\n4. I am very annoyed … John … being so careless.",
                "answers" to listOf(
                    mapOf("text" to "a) with; for", "isCorrect" to true),
                    mapOf("text" to "b) to; about", "isCorrect" to false)
                )),

            mapOf("id" to "q5", "text" to "1. Выберите правильный вариант:\n\n5. He was accused … being a thief.",
                "answers" to listOf(
                    mapOf("text" to "a) of", "isCorrect" to true),
                    mapOf("text" to "b) for", "isCorrect" to false)
                )),

            mapOf("id" to "q6", "text" to "1. Выберите правильный вариант:\n\n6. He believes … God.",
                "answers" to listOf(
                    mapOf("text" to "a) to", "isCorrect" to false),
                    mapOf("text" to "b) in", "isCorrect" to true)
                )),

            // ========== Упражнение 2 (4 вопроса) ==========
            mapOf("id" to "q7", "text" to "2. Выберите правильный вариант:\n\n1. Everybody congratulated him … passing his exams.",
                "answers" to listOf(
                    mapOf("text" to "a) on", "isCorrect" to true),
                    mapOf("text" to "b) of", "isCorrect" to false)
                )),

            mapOf("id" to "q8", "text" to "2. Выберите правильный вариант:\n\n2. The police have charged him … murder.",
                "answers" to listOf(
                    mapOf("text" to "a) with", "isCorrect" to true),
                    mapOf("text" to "b) of", "isCorrect" to false)
                )),

            mapOf("id" to "q9", "text" to "2. Выберите правильный вариант:\n\n3. What time will you have finished painting your room? I will have finished … 7 o'clock, I hope.",
                "answers" to listOf(
                    mapOf("text" to "a) by", "isCorrect" to true),
                    mapOf("text" to "b) until", "isCorrect" to false)
                )),

            mapOf("id" to "q10", "text" to "2. Выберите правильный вариант:\n\n4. Are you seeing Julie tonight? No, I will have left … the time she gets here.",
                "answers" to listOf(
                    mapOf("text" to "a) by", "isCorrect" to true),
                    mapOf("text" to "b) until", "isCorrect" to false)
                )),

            // ========== Упражнение 3 (8 вопросов) ==========
            mapOf("id" to "q11", "text" to "3. Выберите правильный вариант:\n\n1. Her family couldn't decide … the best place to go for their summer holidays.",
                "answers" to listOf(
                    mapOf("text" to "a) on", "isCorrect" to true),
                    mapOf("text" to "b) at", "isCorrect" to false)
                )),

            mapOf("id" to "q12", "text" to "3. Выберите правильный вариант:\n\n2. The mountain-climbers died … extreme cold.",
                "answers" to listOf(
                    mapOf("text" to "a) of", "isCorrect" to true),
                    mapOf("text" to "b) in", "isCorrect" to false)
                )),

            mapOf("id" to "q13", "text" to "3. Выберите правильный вариант:\n\n3. Sally dreams … being a famous actress.",
                "answers" to listOf(
                    mapOf("text" to "a) about", "isCorrect" to false),
                    mapOf("text" to "b) of", "isCorrect" to true)
                )),

            mapOf("id" to "q14", "text" to "3. Выберите правильный вариант:\n\n4. What's the difference… a rabbit and a hare?",
                "answers" to listOf(
                    mapOf("text" to "a) between", "isCorrect" to true),
                    mapOf("text" to "b) for", "isCorrect" to false)
                )),

            mapOf("id" to "q15", "text" to "3. Выберите правильный вариант:\n\n5. Sam was so disappointed … his birthday present that he burst into tears.",
                "answers" to listOf(
                    mapOf("text" to "a) with", "isCorrect" to true),
                    mapOf("text" to "b) to", "isCorrect" to false)
                )),

            mapOf("id" to "q16", "text" to "3. Выберите правильный вариант:\n\n6. The demand … new cars is low because they are so expensive.",
                "answers" to listOf(
                    mapOf("text" to "a) of", "isCorrect" to false),
                    mapOf("text" to "b) for", "isCorrect" to true)
                )),

            mapOf("id" to "q17", "text" to "3. Выберите правильный вариант:\n\n7. Linda couldn't deal … all the typing, so she hired an assistant to help her.",
                "answers" to listOf(
                    mapOf("text" to "a) with", "isCorrect" to true),
                    mapOf("text" to "b) at", "isCorrect" to false)
                )),

            mapOf("id" to "q18", "text" to "3. Выберите правильный вариант:\n\n8. Now that he has a good job, Paul doesn't depend … his parents for money.",
                "answers" to listOf(
                    mapOf("text" to "a) on", "isCorrect" to true),
                    mapOf("text" to "b) with", "isCorrect" to false)
                )),

            // ========== Упражнение 4 (4 вопроса) ==========
            mapOf("id" to "q19", "text" to "4. Выберите правильный вариант:\n\n1. A footballer's life starts … the weekend.",
                "answers" to listOf(
                    mapOf("text" to "a) at", "isCorrect" to true),
                    mapOf("text" to "b) in", "isCorrect" to false),
                    mapOf("text" to "c) on", "isCorrect" to false)
                )),

            mapOf("id" to "q20", "text" to "4. Выберите правильный вариант:\n\n2. Most people go out … Friday night, but I have to be in bed at 10 o'clock.",
                "answers" to listOf(
                    mapOf("text" to "a) in", "isCorrect" to false),
                    mapOf("text" to "b) at", "isCorrect" to false),
                    mapOf("text" to "c) on", "isCorrect" to true)
                )),

            mapOf("id" to "q21", "text" to "4. Выберите правильный вариант:\n\n3. I go to school every day … 9 o'clock.",
                "answers" to listOf(
                    mapOf("text" to "a) at", "isCorrect" to true),
                    mapOf("text" to "b) in", "isCorrect" to false),
                    mapOf("text" to "c) on", "isCorrect" to false)
                )),

            mapOf("id" to "q22", "text" to "4. Выберите правильный вариант:\n\n4. Lessons start at 9.15 am … Mondays and Tuesdays.",
                "answers" to listOf(
                    mapOf("text" to "a) at", "isCorrect" to false),
                    mapOf("text" to "b) on", "isCorrect" to true),
                    mapOf("text" to "c) in", "isCorrect" to false)
                )),

            // ========== Упражнение 5 (8 вопросов) ==========
            mapOf("id" to "q23", "text" to "5. Выберите правильный вариант:\n\n1. John Barnes has been in the police force … 1980.",
                "answers" to listOf(
                    mapOf("text" to "a) for", "isCorrect" to false),
                    mapOf("text" to "b) since", "isCorrect" to true)
                )),

            mapOf("id" to "q24", "text" to "5. Выберите правильный вариант:\n\n2. Before that he worked in a supermarket … two years, but he found it very boring.",
                "answers" to listOf(
                    mapOf("text" to "a) for", "isCorrect" to true),
                    mapOf("text" to "b) since", "isCorrect" to false)
                )),

            mapOf("id" to "q25", "text" to "5. Выберите правильный вариант:\n\n3. I met my penfriend, Bid, four days ...",
                "answers" to listOf(
                    mapOf("text" to "a) ago", "isCorrect" to true),
                    mapOf("text" to "b) before", "isCorrect" to false)
                )),

            mapOf("id" to "q26", "text" to "5. Выберите правильный вариант:\n\n4. I had never met him …",
                "answers" to listOf(
                    mapOf("text" to "a) before", "isCorrect" to true),
                    mapOf("text" to "b) ago", "isCorrect" to false)
                )),

            mapOf("id" to "q27", "text" to "5. Выберите правильный вариант:\n\n5. We went to a few tropical islands … the summer holiday last year.",
                "answers" to listOf(
                    mapOf("text" to "a) during", "isCorrect" to true),
                    mapOf("text" to "b) while", "isCorrect" to false)
                )),

            mapOf("id" to "q28", "text" to "5. Выберите правильный вариант:\n\n6. My parents spent most of their time in the hotel … I was sunbathing on the beach.",
                "answers" to listOf(
                    mapOf("text" to "a) while", "isCorrect" to true),
                    mapOf("text" to "b) during", "isCorrect" to false)
                )),

            mapOf("id" to "q29", "text" to "5. Выберите правильный вариант:\n\n7. Mr. Savage was driving very fast last night because he wanted to be home … for the late film.",
                "answers" to listOf(
                    mapOf("text" to "a) in time", "isCorrect" to true),
                    mapOf("text" to "b) on time", "isCorrect" to false)
                )),

            mapOf("id" to "q30", "text" to "5. Выберите правильный вариант:\n\n8. He knew it probably wouldn't start … but he didn't want to take any chances.",
                "answers" to listOf(
                    mapOf("text" to "a) on time", "isCorrect" to false),
                    mapOf("text" to "b) in time", "isCorrect" to true)
                )),

            // ========== Упражнение 6 (6 вопросов) ==========
            mapOf("id" to "q31", "text" to "6. Выберите правильный вариант:\n\n1. When I went out last Saturday I told my father I'd be back ... 7 o'clock at the latest.",
                "answers" to listOf(
                    mapOf("text" to "a) by", "isCorrect" to true),
                    mapOf("text" to "b) at", "isCorrect" to false)
                )),

            mapOf("id" to "q32", "text" to "6. Выберите правильный вариант:\n\n2. However, I was having such a good time that I didn't even look at my watch … 2:30!",
                "answers" to listOf(
                    mapOf("text" to "a) at", "isCorrect" to false),
                    mapOf("text" to "b) till", "isCorrect" to true)
                )),

            mapOf("id" to "q33", "text" to "6. Выберите правильный вариант:\n\n3. My father was furious and told me I'd have to be home at 7 o'clock every night of the week … the end of the month!",
                "answers" to listOf(
                    mapOf("text" to "a) by", "isCorrect" to false),
                    mapOf("text" to "b) till", "isCorrect" to true)
                )),

            mapOf("id" to "q34", "text" to "6. Выберите правильный вариант:\n\n4. I'm in trouble with my history teacher. He gave us a project to finish ... a week, and I haven't even started it yet.",
                "answers" to listOf(
                    mapOf("text" to "a) after", "isCorrect" to false),
                    mapOf("text" to "b) within", "isCorrect" to true)
                )),

            mapOf("id" to "q35", "text" to "6. Выберите правильный вариант:\n\n5. I was going to do it … dinner on Thursday, but my friend phoned and invited me out to the cinema.",
                "answers" to listOf(
                    mapOf("text" to "a) afterwards", "isCorrect" to false),
                    mapOf("text" to "b) after", "isCorrect" to true)
                )),

            mapOf("id" to "q36", "text" to "6. Выберите правильный вариант:\n\n6. We stayed at the party … 11 o'clock … 3.00 in the morning.",
                "answers" to listOf(
                    mapOf("text" to "a) from … till", "isCorrect" to true),
                    mapOf("text" to "b) till … from", "isCorrect" to false)
                )),

            // ========== Упражнение 7 (6 вопросов - фразовые глаголы) ==========
            mapOf("id" to "q37", "text" to "7. Выберите правильный вариант употребления предлога или наречия:\n\n1. My car broke … on the motorway and I had to walk to a garage.",
                "answers" to listOf(
                    mapOf("text" to "a) into", "isCorrect" to false),
                    mapOf("text" to "b) down", "isCorrect" to true)
                )),

            mapOf("id" to "q38", "text" to "7. Выберите правильный вариант:\n\n2. The robber broke … the house by smashing a window.",
                "answers" to listOf(
                    mapOf("text" to "a) into", "isCorrect" to true),
                    mapOf("text" to "b) down", "isCorrect" to false)
                )),

            mapOf("id" to "q39", "text" to "7. Выберите правильный вариант:\n\n3. As both her parents had died, she was brought … by her grandparents.",
                "answers" to listOf(
                    mapOf("text" to "a) up", "isCorrect" to true),
                    mapOf("text" to "b) round", "isCorrect" to false)
                )),

            mapOf("id" to "q40", "text" to "7. Выберите правильный вариант:\n\n4. The police held … the fans who were trying to get into the football pitch.",
                "answers" to listOf(
                    mapOf("text" to "a) back", "isCorrect" to true),
                    mapOf("text" to "b) on", "isCorrect" to false)
                )),

            mapOf("id" to "q41", "text" to "7. Выберите правильный вариант:\n\n5. They carried … a survey to find out which TV channel was the most popular.",
                "answers" to listOf(
                    mapOf("text" to "a) with", "isCorrect" to false),
                    mapOf("text" to "b) out", "isCorrect" to true)
                )),

            mapOf("id" to "q42", "text" to "7. Выберите правильный вариант:\n\n6. Could you hold … please? Mrs. Jones' line is engaged at the moment.",
                "answers" to listOf(
                    mapOf("text" to "a) on", "isCorrect" to true),
                    mapOf("text" to "b) up", "isCorrect" to false)
                ))
        )
    }

// ==================== ВСЕ ВОПРОСЫ ДЛЯ ЧАСТИ 2: СОЮЗ (10 вопросов) ====================

    private fun getConjunctionQuestions(): List<Map<String, Any>> {
        return listOf(
            mapOf("id" to "c1", "text" to "1. Выберите правильный вариант:\n\n1. He arrived at the office before the others … he could start work early.",
                "answers" to listOf(
                    mapOf("text" to "a) so that", "isCorrect" to true),
                    mapOf("text" to "b) in case", "isCorrect" to false)
                )),

            mapOf("id" to "c2", "text" to "1. Выберите правильный вариант:\n\n2. These tools are mending my car.",
                "answers" to listOf(
                    mapOf("text" to "a) for", "isCorrect" to true),
                    mapOf("text" to "b) to", "isCorrect" to false)
                )),

            mapOf("id" to "c3", "text" to "1. Выберите правильный вариант:\n\n3. She went shopping … to be short of food.",
                "answers" to listOf(
                    mapOf("text" to "a) not to", "isCorrect" to false),
                    mapOf("text" to "b) so as not", "isCorrect" to true)
                )),

            mapOf("id" to "c4", "text" to "1. Выберите правильный вариант:\n\n4. I'll give you my phone number … you need any information.",
                "answers" to listOf(
                    mapOf("text" to "a) in case", "isCorrect" to true),
                    mapOf("text" to "b) in order that", "isCorrect" to false)
                )),

            mapOf("id" to "c5", "text" to "1. Выберите правильный вариант:\n\n5. She worked hard … she could go to university.",
                "answers" to listOf(
                    mapOf("text" to "a) so that", "isCorrect" to true),
                    mapOf("text" to "b) for", "isCorrect" to false)
                )),

            mapOf("id" to "c6", "text" to "1. Выберите правильный вариант:\n\n6. She is saving money … she can go on holiday.",
                "answers" to listOf(
                    mapOf("text" to "a) to", "isCorrect" to false),
                    mapOf("text" to "b) so that", "isCorrect" to true)
                )),

            mapOf("id" to "c7", "text" to "1. Выберите правильный вариант:\n\n7. … the traffic, we made it to school on time.",
                "answers" to listOf(
                    mapOf("text" to "a) despite", "isCorrect" to true),
                    mapOf("text" to "b) although", "isCorrect" to false)
                )),

            mapOf("id" to "c8", "text" to "1. Выберите правильный вариант:\n\n8. … the fact that I didn't study, I passed the exam.",
                "answers" to listOf(
                    mapOf("text" to "a) although", "isCorrect" to false),
                    mapOf("text" to "b) in spite of", "isCorrect" to true)
                )),

            mapOf("id" to "c9", "text" to "1. Выберите правильный вариант:\n\n9. I can't stand classical music, … my mother loves it.",
                "answers" to listOf(
                    mapOf("text" to "a) in spite of", "isCorrect" to false),
                    mapOf("text" to "b) whereas", "isCorrect" to true)
                )),

            mapOf("id" to "c10", "text" to "1. Выберите правильный вариант:\n\n10. Tom loves playing football, … Paul prefers basketball.",
                "answers" to listOf(
                    mapOf("text" to "a) despite", "isCorrect" to false),
                    mapOf("text" to "b) while", "isCorrect" to true)
                ))
        )
    }


    private fun uploadInfinitiveTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Infinitive")
                    .setMessage("Загружаем тест с 42 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Infinitive (Инфинитив)",
                    "num" to 13,  // номер теста (можно изменить)
                    "semester" to 4,  // IV семестр
                    "totalScore" to 42,
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

                // 3. Добавляем все 42 вопроса
                val questions = getInfinitiveQuestions()
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
                    "✅ Тест 'Infinitive (Инфинитив)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 42",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Infinitive test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 42 ВОПРОСА ПО ТЕМЕ INFINITIVE ====================

    private fun getInfinitiveQuestions(): List<Map<String, Any>> {
        return listOf(
            // ========== Упражнение 1 – Выберите правильный вариант (8 вопросов) ==========
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант:\n\n1. He expected … (help) by his friends.",
                "answers" to listOf(
                    mapOf("text" to "a) to help", "isCorrect" to false),
                    mapOf("text" to "b) to be helped", "isCorrect" to true),
                    mapOf("text" to "c) help", "isCorrect" to false)
                )),

            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n\n2. Perhaps it would upset her … (tell) the truth of the matter.",
                "answers" to listOf(
                    mapOf("text" to "a) to be told", "isCorrect" to true),
                    mapOf("text" to "b) to tell", "isCorrect" to false),
                    mapOf("text" to "c) to have told", "isCorrect" to false)
                )),

            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n\n3. I'd like him … (go) to a University but I can't make him … (go).",
                "answers" to listOf(
                    mapOf("text" to "a) go, go", "isCorrect" to false),
                    mapOf("text" to "b) to go, to be gone", "isCorrect" to false),
                    mapOf("text" to "c) to go, go", "isCorrect" to true)
                )),

            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n\n4. Before he let us … (go) he made us … (promise) not to tell anybody what we had seen.",
                "answers" to listOf(
                    mapOf("text" to "a) go, promise", "isCorrect" to true),
                    mapOf("text" to "b) to go, promise", "isCorrect" to false),
                    mapOf("text" to "c) go, to promise", "isCorrect" to false)
                )),

            mapOf("id" to "q5", "text" to "1. Выберите правильный вариант:\n\n5. I hate … (bother) you, but the man is still waiting … (give) a definite answer.",
                "answers" to listOf(
                    mapOf("text" to "a) to bother, to give", "isCorrect" to false),
                    mapOf("text" to "b) bother, be given", "isCorrect" to false),
                    mapOf("text" to "c) to bother, to be given", "isCorrect" to true)
                )),

            mapOf("id" to "q6", "text" to "1. Выберите правильный вариант:\n\n6. She would never miss a chance … (show) her efficiency, she was so anxious (like) and (praise).",
                "answers" to listOf(
                    mapOf("text" to "a) to show, to like, to praise", "isCorrect" to false),
                    mapOf("text" to "b) to show, to be liked, be praised", "isCorrect" to true),
                    mapOf("text" to "c) to show, be liked, be praised", "isCorrect" to false)
                )),

            mapOf("id" to "q7", "text" to "1. Выберите правильный вариант:\n\n7. It seems … (rain) ever since we came here.",
                "answers" to listOf(
                    mapOf("text" to "a) to have been raining", "isCorrect" to true),
                    mapOf("text" to "b) to rain", "isCorrect" to false),
                    mapOf("text" to "c) to be raining", "isCorrect" to false)
                )),

            mapOf("id" to "q8", "text" to "1. Выберите правильный вариант:\n\n8. She was sorry … (be) out when I called and promised … (to wait) for me after the office hours.",
                "answers" to listOf(
                    mapOf("text" to "a) to be, to wait", "isCorrect" to false),
                    mapOf("text" to "b) to have been, to wait", "isCorrect" to true),
                    mapOf("text" to "c) to have been, to be waiting", "isCorrect" to false)
                )),

            // ========== Упражнение 2 – Выберите правильный вариант (8 вопросов) ==========
            mapOf("id" to "q9", "text" to "2. Выберите правильный вариант:\n\n1. It is high time for you ... (go) to bed.",
                "answers" to listOf(
                    mapOf("text" to "a) to go", "isCorrect" to true),
                    mapOf("text" to "b) go", "isCorrect" to false),
                    mapOf("text" to "c) to have gone", "isCorrect" to false)
                )),

            mapOf("id" to "q10", "text" to "2. Выберите правильный вариант:\n\n2. They heard the girl ... (cry) out with joy.",
                "answers" to listOf(
                    mapOf("text" to "a) to cry", "isCorrect" to false),
                    mapOf("text" to "b) cry", "isCorrect" to true),
                    mapOf("text" to "c) be crying", "isCorrect" to false)
                )),

            mapOf("id" to "q11", "text" to "2. Выберите правильный вариант:\n\n3. I would rather ... (stay) at home today.",
                "answers" to listOf(
                    mapOf("text" to "a) stay", "isCorrect" to true),
                    mapOf("text" to "b) to stay", "isCorrect" to false),
                    mapOf("text" to "c) be staying", "isCorrect" to false)
                )),

            mapOf("id" to "q12", "text" to "2. Выберите правильный вариант:\n\n4. You look tired. You had better ... (go) home.",
                "answers" to listOf(
                    mapOf("text" to "a) to go", "isCorrect" to false),
                    mapOf("text" to "b) have gone", "isCorrect" to false),
                    mapOf("text" to "c) go", "isCorrect" to true)
                )),

            mapOf("id" to "q13", "text" to "2. Выберите правильный вариант:\n\n5. I think I shall be able ... (solve) this problem.",
                "answers" to listOf(
                    mapOf("text" to "a) to solve", "isCorrect" to true),
                    mapOf("text" to "b) solve", "isCorrect" to false),
                    mapOf("text" to "c) to be solved", "isCorrect" to false)
                )),

            mapOf("id" to "q14", "text" to "2. Выберите правильный вариант:\n\n6. He would sooner... (die) than ... (betray) his friends.",
                "answers" to listOf(
                    mapOf("text" to "a) to die, betray", "isCorrect" to false),
                    mapOf("text" to "b) die, betray", "isCorrect" to true),
                    mapOf("text" to "c) to have died, have betrayed", "isCorrect" to false)
                )),

            mapOf("id" to "q15", "text" to "2. Выберите правильный вариант:\n\n7. You ought not... (speak) to the Dean like that.",
                "answers" to listOf(
                    mapOf("text" to "a) speak", "isCorrect" to false),
                    mapOf("text" to "b) to speak", "isCorrect" to true),
                    mapOf("text" to "c) have spoken", "isCorrect" to false)
                )),

            mapOf("id" to "q16", "text" to "2. Выберите правильный вариант:\n\n8. Get them ... (come) as early as possible.",
                "answers" to listOf(
                    mapOf("text" to "a) come", "isCorrect" to false),
                    mapOf("text" to "b) be came", "isCorrect" to false),
                    mapOf("text" to "c) to come", "isCorrect" to true)
                )),

            // ========== Упражнение 3 – Перевод с инфинитивом (6 вопросов) ==========
            mapOf("id" to "q17", "text" to "3. Выберите правильный вариант перевода следующих предложений с инфинитивом, употребленным в различных функциях:\n\n1. Бесполезно обсуждать этот вопрос сейчас.",
                "answers" to listOf(
                    mapOf("text" to "a) It is useless discuss this question now.", "isCorrect" to false),
                    mapOf("text" to "b) It is useless this question to be discussed now.", "isCorrect" to false),
                    mapOf("text" to "c) It is useless to discuss this question now.", "isCorrect" to true)
                )),

            mapOf("id" to "q18", "text" to "3. Выберите правильный вариант перевода:\n\n2. Мне жаль, что я отнял у вас столько времени.",
                "answers" to listOf(
                    mapOf("text" to "a) I'm sorry to have taken so much of your time.", "isCorrect" to true),
                    mapOf("text" to "b) I'm sorry to take so much of your time.", "isCorrect" to false),
                    mapOf("text" to "c) I'm sorry that I to have taken so much of your time.", "isCorrect" to false)
                )),

            mapOf("id" to "q19", "text" to "3. Выберите правильный вариант перевода:\n\n3. Я попросил вас придти, чтобы сообщить вам об этом.",
                "answers" to listOf(
                    mapOf("text" to "a) I asked you to come so as to inform you of it.", "isCorrect" to true),
                    mapOf("text" to "b) I asked you to come in order inform you of it.", "isCorrect" to false),
                    mapOf("text" to "c) I asked you to come and inform you of it.", "isCorrect" to false)
                )),

            mapOf("id" to "q20", "text" to "3. Выберите правильный вариант перевода:\n\n4. Встреча, такая как эта, была шансом, который нельзя было упускать.",
                "answers" to listOf(
                    mapOf("text" to "a) A meeting such as this was a chance to be not missed.", "isCorrect" to false),
                    mapOf("text" to "b) A meeting such as this was a chance to not miss.", "isCorrect" to false),
                    mapOf("text" to "c) A meeting such as this was a chance not to be missed.", "isCorrect" to true)
                )),

            mapOf("id" to "q21", "text" to "3. Выберите правильный вариант перевода:\n\n5. Я возьму такси, чтобы не опоздать в аэропорт.",
                "answers" to listOf(
                    mapOf("text" to "a) I will take a taxi not miss the flight.", "isCorrect" to false),
                    mapOf("text" to "b) I will take a taxi not to miss the flight.", "isCorrect" to true),
                    mapOf("text" to "c) I will take a taxi to not miss the flight.", "isCorrect" to false)
                )),

            mapOf("id" to "q22", "text" to "3. Выберите правильный вариант перевода:\n\n6. Я слишком устал, чтобы идти в кино сегодня.",
                "answers" to listOf(
                    mapOf("text" to "a) I am too tired in order to go to the cinema today.", "isCorrect" to false),
                    mapOf("text" to "b) I am too tired for to going to the cinema today.", "isCorrect" to false),
                    mapOf("text" to "c) I am too tired to go to the cinema today.", "isCorrect" to true)
                )),

            // ========== Упражнение 4 – Оборот "For + существительное + инфинитив" (4 вопроса) ==========
            mapOf("id" to "q23", "text" to "4. Выберите правильный вариант перевода следующих предложений с оборотом 'For + существительное (местоимение) + инфинитив':\n\n1. Вам необходимо быть здесь завтра в 5 часов.",
                "answers" to listOf(
                    mapOf("text" to "a) It is necessary for you to be here at 5 o'clock tomorrow.", "isCorrect" to true),
                    mapOf("text" to "b) It is necessary you be here at 5 o'clock tomorrow.", "isCorrect" to false),
                    mapOf("text" to "c) It is necessary for you are here at 5 o'clock tomorrow.", "isCorrect" to false)
                )),

            mapOf("id" to "q24", "text" to "4. Выберите правильный вариант перевода:\n\n2. Ему легко это сделать.",
                "answers" to listOf(
                    mapOf("text" to "a) It is easy for him to do it.", "isCorrect" to true),
                    mapOf("text" to "b) It is easy he do it.", "isCorrect" to false),
                    mapOf("text" to "c) It is easy for him do it.", "isCorrect" to false)
                )),

            mapOf("id" to "q25", "text" to "4. Выберите правильный вариант перевода:\n\n3. Нам трудно сделать эту работу в такой короткий срок.",
                "answers" to listOf(
                    mapOf("text" to "a) It is difficult for we to do this work in such a short term.", "isCorrect" to false),
                    mapOf("text" to "b) It is difficult for us to do this work in such a short term.", "isCorrect" to true),
                    mapOf("text" to "c) It is difficult to for us do this work in such a short term.", "isCorrect" to false)
                )),

            mapOf("id" to "q26", "text" to "4. Выберите правильный вариант перевода:\n\n4. Текст был не такой сложный, чтобы он не смог перевести его без словаря.",
                "answers" to listOf(
                    mapOf("text" to "a) The text was not so difficult for him to translate without a dictionary.", "isCorrect" to false),
                    mapOf("text" to "b) The text was not so difficult for not him to translate without a dictionary.", "isCorrect" to false),
                    mapOf("text" to "c) The text was not so difficult for him not to translate without a dictionary.", "isCorrect" to true)
                )),

            // ========== Упражнение 5 – Оборот "Объектный падеж с инфинитивом" (10 вопросов) ==========
            mapOf("id" to "q27", "text" to "5. Выберите правильный вариант перевода следующих предложений с оборотом 'Объектный падеж с инфинитивом':\n\n1. Он хочет, чтобы мы пришли к нему сегодня.",
                "answers" to listOf(
                    mapOf("text" to "a) He wants us come to him today.", "isCorrect" to false),
                    mapOf("text" to "b) He wants us to come to him today.", "isCorrect" to true),
                    mapOf("text" to "c) He wants we to come to him today.", "isCorrect" to false)
                )),

            mapOf("id" to "q28", "text" to "5. Выберите правильный вариант перевода:\n\n2. Хотите ли вы, чтобы я вам помог?",
                "answers" to listOf(
                    mapOf("text" to "a) Do you want I to help you?", "isCorrect" to false),
                    mapOf("text" to "b) Do you want me to help you?", "isCorrect" to true),
                    mapOf("text" to "c) Do you want me help to you?", "isCorrect" to false)
                )),

            mapOf("id" to "q29", "text" to "5. Выберите правильный вариант перевода:\n\n3. Она любит, чтобы ужин был вовремя.",
                "answers" to listOf(
                    mapOf("text" to "a) She likes that dinner to be in time.", "isCorrect" to false),
                    mapOf("text" to "b) She likes that dinner be in time.", "isCorrect" to false),
                    mapOf("text" to "c) She likes dinner to be in time.", "isCorrect" to true)
                )),

            mapOf("id" to "q30", "text" to "5. Выберите правильный вариант перевода:\n\n4. Я никогда не слышал, как он говорит по-французски.",
                "answers" to listOf(
                    mapOf("text" to "a) I have never heard him speak French.", "isCorrect" to true),
                    mapOf("text" to "b) I have never heard he to speak French.", "isCorrect" to false),
                    mapOf("text" to "c) I have never heard him to have spoken French.", "isCorrect" to false)
                )),

            mapOf("id" to "q31", "text" to "5. Выберите правильный вариант перевода:\n\n5. Она видела, что он вошел в дом и спустилась вниз, чтобы встретить его.",
                "answers" to listOf(
                    mapOf("text" to "a) She saw he come into the house and went downstairs to meet him.", "isCorrect" to false),
                    mapOf("text" to "b) She saw him to come into the house and went downstairs to meet him.", "isCorrect" to false),
                    mapOf("text" to "c) She saw him come into the house and went downstairs to meet him.", "isCorrect" to true)
                )),

            mapOf("id" to "q32", "text" to "5. Выберите правильный вариант перевода:\n\n6. Он заметил, что она очень бледна.",
                "answers" to listOf(
                    mapOf("text" to "a) He noticed that she was very pale.", "isCorrect" to true),
                    mapOf("text" to "b) He noticed her to be very pale.", "isCorrect" to false),
                    mapOf("text" to "c) He noticed her be very pale.", "isCorrect" to false)
                )),

            mapOf("id" to "q33", "text" to "5. Выберите правильный вариант перевода:\n\n7. Никто не заметил, что она вышла из комнаты.",
                "answers" to listOf(
                    mapOf("text" to "a) Nobody noticed her to leave the room.", "isCorrect" to false),
                    mapOf("text" to "b) Nobody noticed her leave the room.", "isCorrect" to true),
                    mapOf("text" to "c) Nobody noticed she to leave the room.", "isCorrect" to false)
                )),

            mapOf("id" to "q34", "text" to "5. Выберите правильный вариант перевода:\n\n8. Я знаю, что он очень опытный преподаватель.",
                "answers" to listOf(
                    mapOf("text" to "a) I know he to be a very experienced teacher.", "isCorrect" to false),
                    mapOf("text" to "b) I know him be a very experienced teacher.", "isCorrect" to false),
                    mapOf("text" to "c) I know him to be a very experienced teacher.", "isCorrect" to true)
                )),

            mapOf("id" to "q35", "text" to "5. Выберите правильный вариант перевода:\n\n9. Я ожидал, что меня пригласят туда.",
                "answers" to listOf(
                    mapOf("text" to "a) I expect to be invited there.", "isCorrect" to true),
                    mapOf("text" to "b) I expect be invited there.", "isCorrect" to false),
                    mapOf("text" to "c) I expect that I to be invited there.", "isCorrect" to false)
                )),

            mapOf("id" to "q36", "text" to "5. Выберите правильный вариант перевода:\n\n10. Я считаю, что я прав.",
                "answers" to listOf(
                    mapOf("text" to "a) I consider I to be right.", "isCorrect" to false),
                    mapOf("text" to "b) I consider myself to be right.", "isCorrect" to true),
                    mapOf("text" to "c) I consider myself I be right.", "isCorrect" to false)
                )),

            // ========== Упражнение 6 – Оборот "Именительный падеж с инфинитивом" (6 вопросов) ==========
            mapOf("id" to "q37", "text" to "6. Выберите правильный вариант перевода следующих предложений при помощи оборота 'Именительный падеж с инфинитивом':\n\n1. Известно, что он придерживается другого мнения по этому вопросу.",
                "answers" to listOf(
                    mapOf("text" to "a) He is known to have a different opinion on this question.", "isCorrect" to true),
                    mapOf("text" to "b) It is known him to have a different opinion on this question.", "isCorrect" to false),
                    mapOf("text" to "c) It is known he to have a different opinion on this question.", "isCorrect" to false)
                )),

            mapOf("id" to "q38", "text" to "6. Выберите правильный вариант перевода:\n\n2. Суть эксперимента, который считают, оказался успешным, будет представлен на конференции.",
                "answers" to listOf(
                    mapOf("text" to "a) The idea of the experiment, which is believed to have proved to be successful, will be represented at the conference.", "isCorrect" to true),
                    mapOf("text" to "b) The idea of the experiment, which is believed to be proved to be successful, will be represented at the conference.", "isCorrect" to false),
                    mapOf("text" to "c) The idea of the experiment, which is believed to have been proved to be successful, will be represented at the conference.", "isCorrect" to false)
                )),

            mapOf("id" to "q39", "text" to "6. Выберите правильный вариант перевода:\n\n3. Кажется, его провал ничуть его не расстроил.",
                "answers" to listOf(
                    mapOf("text" to "a) He does not seem to be discouraged by his failure.", "isCorrect" to false),
                    mapOf("text" to "b) He does not seem has been discouraged by his failure.", "isCorrect" to false),
                    mapOf("text" to "c) He does not seem to have been discouraged by his failure.", "isCorrect" to true)
                )),

            mapOf("id" to "q40", "text" to "6. Выберите правильный вариант перевода:\n\n4. Он не казался удивленным, услышав эти новости.",
                "answers" to listOf(
                    mapOf("text" to "a) He did not appear surprised at this news.", "isCorrect" to false),
                    mapOf("text" to "b) He did not appear to be surprised at this news.", "isCorrect" to true),
                    mapOf("text" to "c) He did not appear that he to be surprised at this news.", "isCorrect" to false)
                )),

            mapOf("id" to "q41", "text" to "6. Выберите правильный вариант перевода:\n\n5. Вероятно, они примут участие в этой работе.",
                "answers" to listOf(
                    mapOf("text" to "a) It is likely they are to take part in this work.", "isCorrect" to false),
                    mapOf("text" to "b) They are likely will take part in this work.", "isCorrect" to false),
                    mapOf("text" to "c) They are likely to take part in this work.", "isCorrect" to true)
                )),

            mapOf("id" to "q42", "text" to "6. Выберите правильный вариант перевода:\n\n6. Ему несомненно понравится на вечеринке.",
                "answers" to listOf(
                    mapOf("text" to "a) He is sure to enjoy himself at the party.", "isCorrect" to true),
                    mapOf("text" to "b) He to be sure to enjoy himself at the party.", "isCorrect" to false),
                    mapOf("text" to "c) He is sure to be enjoyed himself at the party.", "isCorrect" to false)
                ))
        )
    }


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
    }

    // ==================== ТЕСТ: GERUND (Герундий) ====================

    private fun uploadGerundTest() {
        coroutineScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Загрузка теста Gerund")
                    .setMessage("Загружаем тест с 24 вопросами...\nЭто может занять несколько секунд")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // 1. Создаём документ теста
                val testData = hashMapOf<String, Any>(
                    "title" to "Gerund (Герундий)",
                    "num" to 12,  // номер теста (можно изменить)
                    "semester" to 4,  // IV семестр
                    "totalScore" to 24,
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

                // 3. Добавляем все 24 вопроса
                val questions = getGerundQuestions()
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
                    "✅ Тест 'Gerund (Герундий)' успешно загружен!\n\n" +
                            "Test ID: $testId\n" +
                            "Part ID: $partId\n" +
                            "Вопросов: 24",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("UploadTest", "Gerund test uploaded successfully. TestID: $testId")

            } catch (e: Exception) {
                AlertDialog.Builder(this@RoleSelectionActivity)
                    .setTitle("Ошибка загрузки теста")
                    .setMessage("Произошла ошибка:\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
                e.printStackTrace()
            }
        }
    }

// ==================== ВСЕ 24 ВОПРОСА ПО ТЕМЕ GERUND ====================

    private fun getGerundQuestions(): List<Map<String, Any>> {
        return listOf(
            // ========== Упражнение 1 – Выберите правильный вариант (Indefinite/Perfect Active/Passive Gerund) ==========
            mapOf("id" to "q1", "text" to "1. Выберите правильный вариант, используя Indefinite/Perfect Active/Passive Gerund:\n\n1. He remembered … (cross) the road, but he didn't remember … (knock down).",
                "answers" to listOf(
                    mapOf("text" to "a) crossing, being knocked down", "isCorrect" to true),
                    mapOf("text" to "b) having crossed, having been knocked down", "isCorrect" to false),
                    mapOf("text" to "c) having crossed, being knocked down", "isCorrect" to false)
                )),

            mapOf("id" to "q2", "text" to "1. Выберите правильный вариант:\n\n2. I am still hungry in spite of … (eat) four sandwiches.",
                "answers" to listOf(
                    mapOf("text" to "a) eating", "isCorrect" to false),
                    mapOf("text" to "b) having eaten", "isCorrect" to true),
                    mapOf("text" to "c) have eating", "isCorrect" to false)
                )),

            mapOf("id" to "q3", "text" to "1. Выберите правильный вариант:\n\n3. He got into the house by … (climb) through a window, without … (see) by anyone.",
                "answers" to listOf(
                    mapOf("text" to "a) having climbed, having seen", "isCorrect" to false),
                    mapOf("text" to "b) climbing, being seen", "isCorrect" to true),
                    mapOf("text" to "c) having climbed, being seen", "isCorrect" to false)
                )),

            mapOf("id" to "q4", "text" to "1. Выберите правильный вариант:\n\n4. He woke up at 7 a.m. in spite of … (work) late.",
                "answers" to listOf(
                    mapOf("text" to "a) having worked", "isCorrect" to false),
                    mapOf("text" to "b) have working", "isCorrect" to false),
                    mapOf("text" to "c) working", "isCorrect" to true)
                )),

            mapOf("id" to "q5", "text" to "1. Выберите правильный вариант:\n\n5. He complained of … (give) a very small room at the back of the hotel.",
                "answers" to listOf(
                    mapOf("text" to "a) being given", "isCorrect" to true),
                    mapOf("text" to "b) having given", "isCorrect" to false),
                    mapOf("text" to "c) having been given", "isCorrect" to false)
                )),

            mapOf("id" to "q6", "text" to "1. Выберите правильный вариант:\n\n6. The little girl never gets tired of … (ask) her mother questions, but her mother often gets tired of … (ask) so many questions.",
                "answers" to listOf(
                    mapOf("text" to "a) being asked, asking", "isCorrect" to false),
                    mapOf("text" to "b) asking, having asked", "isCorrect" to false),
                    mapOf("text" to "c) asking, being asked", "isCorrect" to true)
                )),

            mapOf("id" to "q7", "text" to "1. Выберите правильный вариант:\n\n7. He enjoyed ... (need).",
                "answers" to listOf(
                    mapOf("text" to "a) needing", "isCorrect" to false),
                    mapOf("text" to "b) having needed", "isCorrect" to false),
                    mapOf("text" to "c) being needed", "isCorrect" to true)
                )),

            mapOf("id" to "q8", "text" to "1. Выберите правильный вариант:\n\n8. I don't like ... (interfere) with.",
                "answers" to listOf(
                    mapOf("text" to "a) interfering", "isCorrect" to false),
                    mapOf("text" to "b) being interfered", "isCorrect" to true),
                    mapOf("text" to "c) having been interfered", "isCorrect" to false)
                )),

            // ========== Упражнение 2 – Выберите правильный вариант перевода с герундием ==========
            mapOf("id" to "q9", "text" to "2. Выберите правильный вариант перевода на английский язык с помощью герундия части предложения, стоящей в скобках:\n\n1. How proud I was of... (что изобрел это устройство).",
                "answers" to listOf(
                    mapOf("text" to "a) having invented this device", "isCorrect" to true),
                    mapOf("text" to "b) inventing", "isCorrect" to false),
                    mapOf("text" to "c) have inventing", "isCorrect" to false)
                )),

            mapOf("id" to "q10", "text" to "2. Выберите правильный вариант перевода:\n\n2. They accused him ... (в том, что он предал своих друзей).",
                "answers" to listOf(
                    mapOf("text" to "a) of having betrayed", "isCorrect" to false),
                    mapOf("text" to "b) of betraying his friends", "isCorrect" to true),
                    mapOf("text" to "c) betraying his friends", "isCorrect" to false)
                )),

            mapOf("id" to "q11", "text" to "2. Выберите правильный вариант перевода:\n\n3. I can't recall ... (чтобы меня с ним когда-нибудь знакомили). I even don't remember... (что видел его).",
                "answers" to listOf(
                    mapOf("text" to "a) of having been introduced to him, having seen him", "isCorrect" to true),
                    mapOf("text" to "b) of being introduced to him, seeing him", "isCorrect" to false),
                    mapOf("text" to "c) of having introduced to him, having seen him", "isCorrect" to false)
                )),

            mapOf("id" to "q12", "text" to "2. Выберите правильный вариант перевода:\n\n4. He couldn't get used ... (к левостороннему движению/водить машину по левой стороне).",
                "answers" to listOf(
                    mapOf("text" to "a) to driving on the lefthand side of the road", "isCorrect" to true),
                    mapOf("text" to "b) driving on the lefthand side of the road", "isCorrect" to false),
                    mapOf("text" to "c) having driven on the lefthand side of the road", "isCorrect" to false)
                )),

            mapOf("id" to "q13", "text" to "2. Выберите правильный вариант перевода:\n\n5. Excuse me ... (что я вошел не постучав).",
                "answers" to listOf(
                    mapOf("text" to "a) entering the room without knocking", "isCorrect" to false),
                    mapOf("text" to "b) of having entered the room without knocking", "isCorrect" to false),
                    mapOf("text" to "c) for entering the room without knocking", "isCorrect" to true)
                )),

            mapOf("id" to "q14", "text" to "2. Выберите правильный вариант перевода:\n\n6. In the morning she was ashamed of herself for ... (что была так груба вчера вечером).",
                "answers" to listOf(
                    mapOf("text" to "a) have being been so rude the night before", "isCorrect" to false),
                    mapOf("text" to "b) having been so rude the night before", "isCorrect" to true),
                    mapOf("text" to "c) being so rude the night before", "isCorrect" to false)
                )),

            mapOf("id" to "q15", "text" to "2. Выберите правильный вариант перевода:\n\n7. Why do you avoid ... (смотреть на меня)?",
                "answers" to listOf(
                    mapOf("text" to "a) having looked at me", "isCorrect" to false),
                    mapOf("text" to "b) of looking at me", "isCorrect" to false),
                    mapOf("text" to "c) looking at me", "isCorrect" to true)
                )),

            mapOf("id" to "q16", "text" to "2. Выберите правильный вариант перевода:\n\n8. He's merely used to ... (что за ним ухаживают).",
                "answers" to listOf(
                    mapOf("text" to "a) being taken care of", "isCorrect" to true),
                    mapOf("text" to "b) having been taken care of", "isCorrect" to false),
                    mapOf("text" to "c) taking care of", "isCorrect" to false)
                )),

            // ========== Упражнение 3 – Выберите правильный вариант (предлоги и фразы с герундием) ==========
            mapOf("id" to "q17", "text" to "3. Выберите правильный вариант:\n\n1. I have no intention ... (stay) here any longer.",
                "answers" to listOf(
                    mapOf("text" to "a) staying", "isCorrect" to false),
                    mapOf("text" to "b) of staying", "isCorrect" to true),
                    mapOf("text" to "c) for staying", "isCorrect" to false)
                )),

            mapOf("id" to "q18", "text" to "3. Выберите правильный вариант:\n\n2. She insisted ... (help) me.",
                "answers" to listOf(
                    mapOf("text" to "a) on helping", "isCorrect" to true),
                    mapOf("text" to "b) at helping", "isCorrect" to false),
                    mapOf("text" to "c) with helping", "isCorrect" to false)
                )),

            mapOf("id" to "q19", "text" to "3. Выберите правильный вариант:\n\n3. There is no possibility ... (find) his address.",
                "answers" to listOf(
                    mapOf("text" to "a) finding", "isCorrect" to false),
                    mapOf("text" to "b) of finding", "isCorrect" to true),
                    mapOf("text" to "c) being found", "isCorrect" to false)
                )),

            mapOf("id" to "q20", "text" to "3. Выберите правильный вариант:\n\n4. There is little chance ... (see) her today.",
                "answers" to listOf(
                    mapOf("text" to "a) seeing", "isCorrect" to false),
                    mapOf("text" to "b) for seeing", "isCorrect" to false),
                    mapOf("text" to "c) of seeing", "isCorrect" to true)
                )),

            mapOf("id" to "q21", "text" to "3. Выберите правильный вариант:\n\n5. We have the pleasure ... (send) you our catalogues.",
                "answers" to listOf(
                    mapOf("text" to "a) of sending", "isCorrect" to true),
                    mapOf("text" to "b) of having sent", "isCorrect" to false),
                    mapOf("text" to "c) having sent", "isCorrect" to false)
                )),

            mapOf("id" to "q22", "text" to "3. Выберите правильный вариант:\n\n6. He is afraid ... (catch) cold.",
                "answers" to listOf(
                    mapOf("text" to "a) for catching", "isCorrect" to false),
                    mapOf("text" to "b) having caught", "isCorrect" to false),
                    mapOf("text" to "c) of catching", "isCorrect" to true)
                )),

            mapOf("id" to "q23", "text" to "3. Выберите правильный вариант:\n\n7. I can't help … (think) about this.",
                "answers" to listOf(
                    mapOf("text" to "a) thinking", "isCorrect" to true),
                    mapOf("text" to "b) of thinking", "isCorrect" to false),
                    mapOf("text" to "c) to thinking", "isCorrect" to false)
                )),

            mapOf("id" to "q24", "text" to "3. Выберите правильный вариант:\n\n8. They had much difficulty ... (to find) the house.",
                "answers" to listOf(
                    mapOf("text" to "a) with finding", "isCorrect" to false),
                    mapOf("text" to "b) in finding", "isCorrect" to true),
                    mapOf("text" to "c) of finding", "isCorrect" to false)
                ))
        )
    }
}