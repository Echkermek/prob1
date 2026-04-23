// com/example/prob1/ui/profile/ProfileFragment.kt
package com.example.prob1.ui.profile

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
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
import com.example.prob1.data.database.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment<com.example.prob1.databinding.ProfileFragmentBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var userRepository: UserRepository

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): com.example.prob1.databinding.ProfileFragmentBinding {
        return com.example.prob1.databinding.ProfileFragmentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        userRepository = UserRepository(requireContext())

        if (userId == null) {
            showToast("Не авторизован")
            return
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
            val userData = mainViewModel.userData.value
            if (userData != null) {
                showGradeDialog(userData.totalScore, userData.grade)
            }
        }
    }

    private fun setupRefreshButton() {
        binding.refreshButton.setOnClickListener {
            mainViewModel.refreshAllData(userId!!)
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

        // Кнопка с баллами
        updateScoreButton(binding.score, userData.totalScore, userData.grade)
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

    private fun showGradeDialog(totalPoints: Double, grade: Int) {
        val gradeDesc = when (grade) {
            5 -> "Отлично"
            4 -> "Хорошо"
            3 -> "Удовлетворительно"
            2 -> "Неудовлетворительно"
            else -> "Нет данных"
        }

        val message = """
            Ваша оценка: $gradeDesc ($grade)
            
            Всего баллов: ${totalPoints.toInt()}
            
            Критерии оценки:
            • 5 (Отлично): 106 – 125 баллов
            • 4 (Хорошо): 90 – 105 баллов
            • 3 (Удовл.): 76 – 89 баллов
            • 2 (Неуд.): 75 и меньше баллов
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