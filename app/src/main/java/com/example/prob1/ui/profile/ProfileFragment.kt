package com.example.prob1.ui.profile

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.fragment.app.Fragment
import com.example.prob1.BankActivity
import com.example.prob1.CalendarStudent
import com.example.prob1.LevelsActivity
import com.example.prob1.R
import com.example.prob1.RatingActivity
import com.example.prob1.RulesActivity
import com.example.prob1.StudentAuthActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentGroupId: String? = null
    private var currentGroupName: String? = null
    private var coinsListener: ListenerRegistration? = null
    private var gradesListener: ListenerRegistration? = null
    private lateinit var scoreButton: Button

    // Переменные для баллов и оценки
    private var totalBestScore: Double = 0.0
    private var currentGrade: Int = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.profile_fragment, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameSurname: TextView = root.findViewById(R.id.nameSurname)
        val groupName: TextView = root.findViewById(R.id.groupName)
        val coinsProfile: TextView = root.findViewById(R.id.buttonCoins)
        val btbank: Button = root.findViewById(R.id.btBank)
        scoreButton = root.findViewById(R.id.score)
        val exitButton: Button = root.findViewById(R.id.exit)
        val ratingButton: Button = root.findViewById(R.id.rating)
        val calendarButton: Button = root.findViewById(R.id.kalendar)
        val rulesButton: Button = root.findViewById(R.id.rules)
        val avatar = root.findViewById<ImageFilterView>(R.id.avatar)

        // Инициализация кнопки оценки
        scoreButton.text = "Нет данных"

        auth.currentUser?.let { user ->
            // Загрузка имени пользователя
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nameSurname.text = "${document.getString("name")} ${document.getString("surname")}"
                    }
                }

            // Загрузка группы
            db.collection("usersgroup")
                .whereEqualTo("userId", user.uid)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.documents[0]
                        currentGroupId = document.getString("groupId")
                        currentGroupName = document.getString("groupName")
                        groupName.text = "Группа: ${currentGroupName}"
                    }
                }

            // Слушатель для монет
            coinsListener = db.collection("user_coins").document(user.uid)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e("ProfileFragment", "Listen failed.", error)
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        val coins = document.getLong("coins")?.toInt() ?: 0
                        updateCoinsWithAnimation(coinsProfile, coins.toString())
                        updateAvatar(avatar, coins)
                    }
                }

            // Слушатель для оценок (лучшие результаты)
            gradesListener = db.collection("test_grades")
                .whereEqualTo("userId", user.uid)
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        Log.e("ProfileFragment", "Listen for grades failed.", error)
                        scoreButton.text = "Нет данных"
                        return@addSnapshotListener
                    }

                    // Считаем сумму bestScore
                    totalBestScore = 0.0
                    querySnapshot?.documents?.forEach { doc ->
                        val bestScore = doc.getDouble("bestScore") ?: 0.0
                        totalBestScore += bestScore
                    }

                    // Загружаем критерии из коллекции `rating`
                    loadRatingCriteriaAndUpdate(user.uid)
                }
        }

        // Кнопки
        rulesButton.setOnClickListener {
            startActivity(Intent(activity, RulesActivity::class.java))
        }

        calendarButton.setOnClickListener {
            startActivity(Intent(activity, CalendarStudent::class.java))
        }

        scoreButton.setOnClickListener {
            val data = scoreButton.tag as? Map<*, *> ?: return@setOnClickListener
            val points = (data["points"] as? Double) ?: 0.0
            val grade = (data["grade"] as? Int) ?: 0
            showGradeDialog(points, grade)
        }

        avatar.setOnClickListener {
            auth.currentUser?.let { user ->
                db.collection("user_coins").document(user.uid).get()
                    .addOnSuccessListener { document ->
                        val coins = document.getLong("coins")?.toInt() ?: 0
                        val intent = Intent(activity, LevelsActivity::class.java).apply {
                            putExtra("coins", coins)
                        }
                        startActivity(intent)
                    }
            }
        }

        ratingButton.setOnClickListener {
            auth.currentUser?.let { user ->
                db.collection("usersgroup")
                    .whereEqualTo("userId", user.uid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            startActivity(Intent(activity, RatingActivity::class.java))
                        } else {
                            Log.d("ProfileFragment", "No group found for user")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileFragment", "Error loading user group", e)
                    }
            }
        }

        exitButton.setOnClickListener {
            showExitConfirmationDialog()
        }

        btbank.setOnClickListener {
            startActivity(Intent(activity, BankActivity::class.java))
        }

        return root
    }

    // Загрузка критериев из `rating` и обновление кнопки
    private fun loadRatingCriteriaAndUpdate(userId: String) {
        db.collection("usersgroup")
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { groupDocs ->
                if (groupDocs.isEmpty) {
                    updateScoreButton(0.0, 0)
                    return@addOnSuccessListener
                }

                val groupId = groupDocs.documents[0].getString("groupId") ?: return@addOnSuccessListener

                db.collection("courses")
                    .whereEqualTo("groupId", groupId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { courseDocs ->
                        val currentSemester = if (!courseDocs.isEmpty) {
                            courseDocs.documents[0].getLong("semester")?.toInt() ?: 1
                        } else 1

                        // Ищем документ в `rating` с нужным semestr
                        db.collection("rating")
                            .whereEqualTo("semestr", currentSemester)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { ratingDocs ->
                                if (ratingDocs.isEmpty) {
                                    updateScoreButton(totalBestScore, 0)
                                    return@addOnSuccessListener
                                }

                                val criteria = ratingDocs.documents[0]
                                val min3 = criteria.getLong("min3")?.toInt() ?: 100
                                val min4 = criteria.getLong("min4")?.toInt() ?: 150
                                val min5 = criteria.getLong("min5")?.toInt() ?: 200

                                currentGrade = when {
                                    totalBestScore >= min5 -> 5
                                    totalBestScore >= min4 -> 4
                                    totalBestScore >= min3 -> 3
                                    else -> 2
                                }

                                updateScoreButton(totalBestScore, currentGrade)
                            }
                            .addOnFailureListener {
                                updateScoreButton(totalBestScore, 0)
                            }
                    }
            }
            .addOnFailureListener {
                updateScoreButton(totalBestScore, 0)
            }
    }

    // Обновление кнопки с баллами и оценкой
    private fun updateScoreButton(totalPoints: Double, grade: Int) {
        val pointsText = if (totalPoints > 0) "${totalPoints.toInt()} баллов" else "Нет баллов"
        val gradeText = when (grade) {
            5 -> "5 (Отлично)"
            4 -> "4 (Хорошо)"
            3 -> "3 (Удовл.)"
            2 -> "2 (Неуд.)"
            else -> "Нет данных"
        }


        scoreButton.text = "$pointsText"
        scoreButton.tag = mapOf("points" to totalPoints, "grade" to grade)
    }

    // Диалог с подробной информацией
    private fun showGradeDialog(totalPoints: Double, grade: Int) {
        val gradeDesc = when (grade) {
            5 -> "Отлично"
            4 -> "Хорошо"
            3 -> "Удовлетворительно"
            2 -> "Неудовлетворительно"
            else -> "Нет данных"
        }

        val message = if (totalPoints > 0) {
            """
                Ваша оценка: $gradeDesc ($grade)
                
                Всего баллов: ${totalPoints.toInt()}                
                
                • 2: менее 100 баллов
                • 3: 100–149 баллов
                • 4: 150–199 баллов
                • 5: 200+ баллов
            """.trimIndent()
        } else {
            """
                У вас пока нет баллов.
            """.trimIndent()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Успеваемость")
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    // Анимация монет
    private fun updateCoinsWithAnimation(view: TextView, newValue: String) {
        val anim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        anim.duration = 300
        anim.start()
        view.text = newValue
    }

    // Обновление аватара
    private fun updateAvatar(avatar: ImageFilterView, coinsValue: Int) {
        when {
            coinsValue < 130 -> avatar.setImageResource(R.mipmap.level1)
            coinsValue < 170 -> avatar.setImageResource(R.mipmap.level2)
            coinsValue < 240 -> avatar.setImageResource(R.mipmap.level3)
            coinsValue < 285 -> avatar.setImageResource(R.mipmap.level4)
            coinsValue < 325 -> avatar.setImageResource(R.mipmap.level5)
            coinsValue < 400 -> avatar.setImageResource(R.mipmap.level6)
            coinsValue < 460 -> avatar.setImageResource(R.mipmap.level7)
            else -> avatar.setImageResource(R.mipmap.level8)
        }
    }

    // Диалог выхода
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход")
            .setMessage("Данные для входа будут удалены. Выйти из профиля?")
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Выйти") { _, _ -> logoutUser() }
            .show()
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(activity, StudentAuthActivity::class.java))
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coinsListener?.remove()
        gradesListener?.remove()
    }
}