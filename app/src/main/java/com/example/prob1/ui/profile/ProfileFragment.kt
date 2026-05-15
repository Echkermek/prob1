// com/example/prob1/ui/profile/ProfileFragment.kt
package com.example.prob1.ui.profile

import android.animation.ObjectAnimator
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
import androidx.lifecycle.lifecycleScope
import com.example.prob1.BankActivity
import com.example.prob1.CalendarStudent
import com.example.prob1.LevelsActivity
import com.example.prob1.R
import com.example.prob1.RatingActivity
import com.example.prob1.RulesActivity
import com.example.prob1.StudentAuthActivity
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.database.entities.UserDataEntity
import com.example.prob1.data.repository.UserRepository
import com.example.prob1.data.repository.StudentScoreManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : BaseFragment<com.example.prob1.databinding.ProfileFragmentBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var userRepository: UserRepository
    private val firestore = Firebase.firestore

    // Данные для отображения
    private var currentTotalScore = 0.0
    private var currentGrade = 0
    private var currentSemester = 1
    private var gradeInfo = GradeInfo(0.0, 0.0, 0.0)

    data class GradeInfo(
        val min3: Double,
        val min4: Double,
        val min5: Double
    )

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): com.example.prob1.databinding.ProfileFragmentBinding {
        return com.example.prob1.databinding.ProfileFragmentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        userRepository = UserRepository(requireContext())

        if (userId == null) {
            showToast("Не авторизован")
            return
        }

        // Синхронизируем баллы с текущим курсом
        lifecycleScope.launch {
            val scoreManager = StudentScoreManager()
            scoreManager.syncStudentScoreWithCurrentCourse(userId!!)
            // Загружаем данные после синхронизации
            loadStudentScoreAndGrade()
        }

        setupClickListeners()
        setupRefreshButton()

        // Подписываемся на данные из ViewModel
        observeViewModel()

        // Если данные не загружены - загружаем
        if (mainViewModel.userData.value == null) {
            mainViewModel.loadUserData(userId!!)
        }
    }

    private suspend fun loadStudentScoreAndGrade() {
        try {
            val uid = userId ?: return

            Log.d("ProfileFragment", "=== Loading student score and grade ===")

            // 1. Получаем текущий курс студента
            val scoreManager = StudentScoreManager()
            val currentCourse = scoreManager.getCurrentCourseInfo(uid)

            if (currentCourse != null) {
                Log.d("ProfileFragment", "Current course ID: ${currentCourse.courseId}")
                Log.d("ProfileFragment", "Current course name: ${currentCourse.courseName}")
                Log.d("ProfileFragment", "Semester: ${currentCourse.semester}")

                // 2. Получаем баллы из коллекции student_course_scores
                val scoreDoc = firestore.collection("student_course_scores")
                    .document("${uid}_${currentCourse.courseId}")
                    .get()
                    .await()

                currentTotalScore = scoreDoc.getDouble("totalScore") ?: 0.0
                currentSemester = currentCourse.semester

                Log.d("ProfileFragment", "Loaded score: $currentTotalScore")

                // 3. Получаем критерии оценок из course_grades
                // ИСПРАВЛЕНО: используем "courseid" с маленькой буквы
                val gradeSnapshot = firestore.collection("course_grades")
                    .whereEqualTo("courseid", currentCourse.courseId)  // ← ИСПРАВЛЕНО!
                    .limit(1)
                    .get()
                    .await()

                Log.d("ProfileFragment", "Grade snapshot size: ${gradeSnapshot.size()}")

                if (!gradeSnapshot.isEmpty) {
                    val doc = gradeSnapshot.documents[0]
                    Log.d("ProfileFragment", "Grade doc data: ${doc.data}")

                    // Получаем значения, обрабатывая как числа (могут быть Long, Double или String)
                    gradeInfo = GradeInfo(
                        min3 = getDoubleFromAny(doc.get("min3")) ?: 0.0,
                        min4 = getDoubleFromAny(doc.get("min4")) ?: 0.0,
                        min5 = getDoubleFromAny(doc.get("min5")) ?: 0.0
                    )

                    Log.d("ProfileFragment", "Grade from Firestore - min3: ${gradeInfo.min3}, min4: ${gradeInfo.min4}, min5: ${gradeInfo.min5}")
                } else {
                    // Если не нашли по courseid, пробуем другие варианты
                    Log.d("ProfileFragment", "No grade found with 'courseid', trying alternatives...")

                    // Пробуем "courseId" с большой буквы
                    val altSnapshot = firestore.collection("course_grades")
                        .whereEqualTo("courseId", currentCourse.courseId)
                        .limit(1)
                        .get()
                        .await()

                    if (!altSnapshot.isEmpty) {
                        val doc = altSnapshot.documents[0]
                        gradeInfo = GradeInfo(
                            min3 = getDoubleFromAny(doc.get("min3")) ?: 0.0,
                            min4 = getDoubleFromAny(doc.get("min4")) ?: 0.0,
                            min5 = getDoubleFromAny(doc.get("min5")) ?: 0.0
                        )
                        Log.d("ProfileFragment", "Grade from Firestore (alternative) - min3: ${gradeInfo.min3}, min4: ${gradeInfo.min4}, min5: ${gradeInfo.min5}")
                    } else {
                        // Дефолтные значения по семестру
                        gradeInfo = when (currentSemester) {
                            1 -> GradeInfo(76.0, 90.0, 106.0)
                            2 -> GradeInfo(98.0, 112.0, 128.0)
                            3 -> GradeInfo(43.0, 57.0, 73.0)
                            else -> GradeInfo(50.0, 60.0, 70.0)
                        }
                        Log.d("ProfileFragment", "Using default grade info for semester $currentSemester")
                    }
                }

                // 4. Вычисляем текущую оценку
                currentGrade = when {
                    currentTotalScore >= gradeInfo.min5 -> 5
                    currentTotalScore >= gradeInfo.min4 -> 4
                    currentTotalScore >= gradeInfo.min3 -> 3
                    else -> 2
                }

                Log.d("ProfileFragment", "Final grade: $currentGrade (min3=${gradeInfo.min3}, min4=${gradeInfo.min4}, min5=${gradeInfo.min5})")

                // Обновляем UI с новыми данными
                updateScoreButton(binding.score, currentTotalScore, currentGrade)
            } else {
                Log.e("ProfileFragment", "Could not find current course")
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error loading student score and grade", e)
        }
    }

    // Вспомогательная функция для получения Double из любого типа
    private fun getDoubleFromAny(value: Any?): Double? {
        return when (value) {
            is Double -> value
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun setupClickListeners() {
        binding.btBank.setOnClickListener {
            startActivity(Intent(requireContext(), BankActivity::class.java))
        }

        binding.exit.setOnClickListener {
            showExitConfirmationDialog()
        }

        binding.rating.setOnClickListener {
            startActivity(Intent(requireContext(), RatingActivity::class.java))
        }

        binding.kalendar.setOnClickListener {
            startActivity(Intent(requireContext(), CalendarStudent::class.java))
        }

        binding.rules.setOnClickListener {
            startActivity(Intent(requireContext(), RulesActivity::class.java))
        }

        binding.avatar.setOnClickListener {
            val coins = mainViewModel.userData.value?.coins ?: 0
            val intent = Intent(requireContext(), LevelsActivity::class.java).apply {
                putExtra("coins", coins)
            }
            startActivity(intent)
        }

        binding.score.setOnClickListener {
            showGradeDialog(currentTotalScore, currentGrade, currentSemester, gradeInfo)
        }
    }

    private fun setupRefreshButton() {
        binding.refreshButton.setOnClickListener {
            mainViewModel.refreshAllData(userId!!)
            // Обновляем баллы и оценки
            lifecycleScope.launch {
                val scoreManager = StudentScoreManager()
                scoreManager.syncStudentScoreWithCurrentCourse(userId!!)
                loadStudentScoreAndGrade()
            }
        }
    }

    private fun observeViewModel() {
        // Наблюдаем за данными пользователя
        lifecycleScope.launch {
            mainViewModel.userData.collect { userData: UserDataEntity? ->
                if (isUiSafe && userData != null) {
                    updateUI(userData)
                }
            }
        }

        // Наблюдаем за состоянием загрузки
        lifecycleScope.launch {
            mainViewModel.isLoading.collect { isLoading: Boolean ->
                if (isUiSafe) {
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.refreshButton.isEnabled = !isLoading
                }
            }
        }
    }

    private fun updateUI(userData: UserDataEntity) {
        // Имя и фамилия
        val fullName = "${userData.name ?: ""} ${userData.surname ?: ""}".trim()
        binding.nameSurname.text = fullName.ifEmpty { "Студент" }

        // Группа
        binding.groupName.text = "Группа: ${userData.groupName ?: "Не указана"}"

        // Монеты с анимацией
        updateCoinsWithAnimation(binding.buttonCoins, userData.coins)

        // Аватар
        updateAvatar(binding.avatar, userData.coins)

        // Кнопка с баллами (используем загруженные данные)
        updateScoreButton(binding.score, currentTotalScore, currentGrade)
    }

    private fun updateCoinsWithAnimation(view: TextView, coins: Int) {
        val anim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        anim.duration = 300
        anim.start()
        view.text = coins.toString()
    }

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

    private fun updateScoreButton(button: Button, totalPoints: Double, grade: Int) {
        val pointsText = if (totalPoints > 0) "${totalPoints.toInt()} баллов" else "Нет баллов"
        button.text = pointsText
        button.tag = mapOf("points" to totalPoints, "grade" to grade)
    }

    private fun showGradeDialog(totalPoints: Double, grade: Int, semester: Int, gradeInfo: GradeInfo) {
        val gradeDesc = when (grade) {
            5 -> "Отлично"
            4 -> "Хорошо"
            3 -> "Удовлетворительно"
            2 -> "Неудовлетворительно"
            else -> "Нет данных"
        }

        val criteria = """
            • 5 (Отлично): от ${gradeInfo.min5.toInt()} баллов
            • 4 (Хорошо): от ${gradeInfo.min4.toInt()} баллов
            • 3 (Удовл.): от ${gradeInfo.min3.toInt()} баллов
            • 2 (Неуд.): менее ${gradeInfo.min3.toInt()} баллов
        """.trimIndent()

        val message = """
        Ваша оценка: $gradeDesc ($grade)
        
        Семестр: $semester
        
        Всего баллов: ${"%.2f".format(totalPoints)}
        
        Критерии оценки:
        $criteria
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Успеваемость")
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти из профиля?")
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Выйти") { _, _ -> logoutUser() }
            .show()
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        mainViewModel.clearData()
        launchSafe {
            userRepository.clearUserDataCache(userId!!)
        }
        startActivity(Intent(requireContext(), StudentAuthActivity::class.java))
        requireActivity().finish()
    }
}