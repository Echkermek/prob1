package com.example.prob1.ui.tests

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.Test
import com.example.prob1.databinding.FragmentDebtTestsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat

class DebtTestsFragment : BaseFragment<FragmentDebtTestsBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var testsAdapter: TestsAdapter
    private val deadlinesMap = mutableMapOf<String, String>()

    // Данные о долге
    private var debtCourseId: String? = null
    private var debtCourseName: String? = null
    private var debtStudentScore: Double = 0.0
    private var debtRequiredScore: Double = 0.0
    private var debtMin3: Double = 0.0
    private var debtMin4: Double = 0.0
    private var debtMin5: Double = 0.0
    private var testsCompleted: Int = 0
    private var totalTests: Int = 0

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDebtTestsBinding {
        return FragmentDebtTestsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        if (userId == null) {
            showToast("Не авторизован")
            return
        }

        setupRecyclerView()
        setupBackButton()

        // Загружаем данные о долге из Firestore
        loadDebtData()
    }

    private fun setupRecyclerView() {
        testsAdapter = TestsAdapter(deadlinesMap) { testId: String, semester: Int, _: Boolean ->
            openTestByRealStructure(testId, true)
        }

        binding.debtTestsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.debtTestsRecycler.adapter = testsAdapter
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadDebtData() {
        launchSafe {
            try {
                showLoading(true)

                // 1. Находим активный долг пользователя
                val debtSnapshot = firestore.collection("dolg")
                    .whereEqualTo("studentId", userId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                if (debtSnapshot.isEmpty) {
                    showEmptyState("Активные долги не найдены")
                    showLoading(false)
                    return@launchSafe
                }

                val debtDoc = debtSnapshot.documents[0]
                debtCourseId = debtDoc.getString("courseId")
                debtCourseName = debtDoc.getString("courseName") ?: "Курс"
                testsCompleted = debtDoc.getLong("testsCompleted")?.toInt() ?: 0
                totalTests = debtDoc.getLong("totalTests")?.toInt() ?: 0

                // 2. Получаем критерии оценок для курса
                if (debtCourseId != null) {
                    val courseDoc = firestore.collection("courses")
                        .document(debtCourseId!!)
                        .get()
                        .await()

                    debtMin3 = courseDoc.getDouble("min3") ?: 40.0
                    debtMin4 = courseDoc.getDouble("min4") ?: 60.0
                    debtMin5 = courseDoc.getDouble("min5") ?: 80.0
                    debtRequiredScore = debtMin3
                }

                // 3. ПОЛУЧАЕМ БАЛЛ ИЗ student_course_scores (а не из dolg)
                if (debtCourseId != null && userId != null) {
                    val docId = "${userId}_${debtCourseId}"
                    val scoreDoc = firestore.collection("student_course_scores")
                        .document(docId)
                        .get()
                        .await()

                    if (scoreDoc.exists()) {
                        debtStudentScore = scoreDoc.getDouble("totalScore") ?: 0.0
                        Log.d("DebtTestsFragment", "Loaded totalScore from student_course_scores: $debtStudentScore")
                    } else {
                        // Если документа нет, берем из dolg (но обычно должен быть)
                        debtStudentScore = debtDoc.getDouble("avgScore") ?: 0.0
                        Log.d("DebtTestsFragment", "No student_course_scores doc, using avgScore from dolg: $debtStudentScore")
                    }
                } else {
                    debtStudentScore = debtDoc.getDouble("avgScore") ?: 0.0
                }

                // 4. Показываем информацию о долге
                showDebtInfo()

                // 5. Загружаем тесты для этого курса
                loadTestsForCourse()

            } catch (e: Exception) {
                Log.e("DebtTestsFragment", "Error loading debt data", e)
                showEmptyState("Ошибка загрузки: ${e.message}")
                showLoading(false)
            }
        }
    }

    private suspend fun loadTestsForCourse() {
        try {
            if (debtCourseId == null) {
                showEmptyState("ID курса не найден")
                showLoading(false)
                return
            }

            // Загружаем тесты для курса-долга
            val testCourseSnapshot = firestore.collection("test_course")
                .whereEqualTo("courseId", debtCourseId)
                .get()
                .await()

            val currentCourseTestIds = mutableListOf<String>()
            for (doc in testCourseSnapshot.documents) {
                val testId = doc.getString("testId")
                if (testId != null) {
                    currentCourseTestIds.add(testId)
                }
            }

            if (currentCourseTestIds.isEmpty()) {
                showEmptyState("Тесты для сдачи не найдены")
                showLoading(false)
                return
            }

            // Загружаем сами тесты
            val debtTests = mutableListOf<Test>()
            for (testId in currentCourseTestIds) {
                try {
                    val testDoc = firestore.collection("tests")
                        .document(testId)
                        .get()
                        .await()

                    if (testDoc.exists()) {
                        val test = Test(
                            id = testDoc.id,
                            title = testDoc.getString("title") ?: "",
                            semester = testDoc.getLong("semester")?.toInt() ?: 1,
                            num = testDoc.getLong("num")?.toInt() ?: 0,
                            isAvailable = testDoc.getBoolean("isAvailable") ?: true
                        )
                        debtTests.add(test)
                    }
                } catch (e: Exception) {
                    Log.e("DebtTestsFragment", "Error loading test $testId", e)
                }
            }

            testsAdapter.submitList(debtTests.sortedBy { it.num })

            if (debtTests.isEmpty()) {
                showEmptyState("Тесты не найдены")
            } else {
                showContent()
            }

            showLoading(false)

        } catch (e: Exception) {
            Log.e("DebtTestsFragment", "Error loading tests", e)
            showEmptyState("Ошибка загрузки тестов: ${e.message}")
            showLoading(false)
        }
    }

    private fun showDebtInfo() {
        val df = DecimalFormat("#.##")

        val currentGrade = when {
            debtStudentScore >= debtMin5 -> "5 (Отлично)"
            debtStudentScore >= debtMin4 -> "4 (Хорошо)"
            debtStudentScore >= debtMin3 -> "3 (Удовлетворительно)"
            else -> "2 (Неудовлетворительно)"
        }

        val neededPoints = if (debtStudentScore < debtMin3) {
            debtMin3 - debtStudentScore
        } else {
            0.0
        }

        val infoText = """
            Курс: ${debtCourseName ?: "Курс"}
            
            Ваш балл: ${df.format(debtStudentScore)}
            Текущая оценка: $currentGrade
            
            ─────────────────────────
            Критерии оценок:
            5 (Отлично): от ${df.format(debtMin5)} баллов
            4 (Хорошо): от ${df.format(debtMin4)} баллов
            3 (Удовл.): от ${df.format(debtMin3)} баллов
            2 (Неуд.): менее ${df.format(debtMin3)} баллов
            ─────────────────────────
            
            Пройдено тестов: $testsCompleted из $totalTests
            ${if (neededPoints > 0) "\n⚠Необходимо набрать еще ${df.format(neededPoints)} баллов для сдачи" else "\n"}
        """.trimIndent()

        binding.debtInfoText.text = infoText
    }

    private fun openTestByRealStructure(testId: String, isDebtTest: Boolean = true) {
        try {
            Log.d("DebtTestsFragment", "=== OPENING DEBT TEST ===")
            Log.d("DebtTestsFragment", "testId: $testId")
            Log.d("DebtTestsFragment", "debtCourseId: $debtCourseId")

            // Открываем TestPartsFragment, который покажет все части теста
            findNavController().navigate(
                R.id.action_debtTestsFragment_to_testPartsFragment,
                Bundle().apply {
                    putString("testId", testId)
                    putBoolean("isDebtTest", true)
                    // Передаем данные о долге для обновления после прохождения
                    putString("debtCourseId", debtCourseId)
                    putString("debtCourseName", debtCourseName)
                }
            )
        } catch (e: Exception) {
            Log.e("DebtTestsFragment", "Error navigating to test parts", e)
            showToast("Ошибка открытия теста: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Перезагружаем данные о долге при возврате на фрагмент
        if (userId != null) {
            loadDebtData()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(message: String) {
        binding.emptyStateText.text = message
        binding.emptyStateText.visibility = View.VISIBLE
        binding.debtTestsRecycler.visibility = View.GONE
        binding.debtInfoText.visibility = View.GONE
    }

    private fun showContent() {
        binding.emptyStateText.visibility = View.GONE
        binding.debtTestsRecycler.visibility = View.VISIBLE
        binding.debtInfoText.visibility = View.VISIBLE
    }
}