// com/example/prob1/ui/tests/DebtTestsFragment.kt
package com.example.prob1.ui.tests

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

    private var debtCourseId: String? = null
    private var debtCourseName: String? = null
    private var debtStudentScore: Double = 0.0
    private var debtRequiredScore: Double = 0.0
    private var debtMin3: Double = 0.0
    private var debtMin4: Double = 0.0
    private var debtMin5: Double = 0.0
    private var testsCompleted: Int = 0
    private var totalTests: Int = 0

    private var currentCourseTestIds = mutableListOf<String>()

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

        // Получаем данные о долге из аргументов
        debtCourseId = arguments?.getString("debtCourseId")
        debtCourseName = arguments?.getString("debtCourseName")
        debtStudentScore = arguments?.getDouble("debtStudentScore") ?: 0.0
        debtRequiredScore = arguments?.getDouble("debtRequiredScore") ?: 0.0
        debtMin3 = arguments?.getDouble("debtMin3") ?: 0.0
        debtMin4 = arguments?.getDouble("debtMin4") ?: 0.0
        debtMin5 = arguments?.getDouble("debtMin5") ?: 0.0
        testsCompleted = arguments?.getInt("testsCompleted") ?: 0
        totalTests = arguments?.getInt("totalTests") ?: 0

        loadDebtTests()
        showDebtInfo()
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

    private fun showDebtInfo() {
        val df = DecimalFormat("#.##")

        val currentGrade = when {
            debtStudentScore >= debtMin5 -> "5 (Отлично)"
            debtStudentScore >= debtMin4 -> "4 (Хорошо)"
            debtStudentScore >= debtMin3 -> "3 (Удовлетворительно)"
            else -> "2 (Неудовлетворительно)"
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
        """.trimIndent()

        binding.debtInfoText.text = infoText
    }

    private fun loadDebtTests() {
        launchSafe {
            try {
                showLoading(true)

                if (debtCourseId == null) {
                    showEmptyState("ID курса не найден")
                    showLoading(false)
                    return@launchSafe
                }

                // Загружаем тесты для курса-долга
                val testCourseSnapshot = firestore.collection("test_course")
                    .whereEqualTo("courseId", debtCourseId)
                    .get()
                    .await()

                currentCourseTestIds.clear()
                for (doc in testCourseSnapshot.documents) {
                    val testId = doc.getString("testId")
                    if (testId != null) {
                        currentCourseTestIds.add(testId)
                    }
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

                testsAdapter.submitList(debtTests)

                if (debtTests.isEmpty()) {
                    showEmptyState("Тесты для сдачи не найдены")
                } else {
                    showContent()
                }

                showLoading(false)

            } catch (e: Exception) {
                Log.e("DebtTestsFragment", "Error loading debt tests", e)
                showEmptyState("Ошибка загрузки: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun openTestByRealStructure(testId: String, isDebtTest: Boolean = true) {
        launchSafe {
            try {
                val progressDialog = android.app.ProgressDialog(requireContext())
                progressDialog.setMessage("Загрузка теста...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                val partsSnapshot = firestore.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .get()
                    .await()

                progressDialog.dismiss()

                // Открываем TestActivity напрямую (это проще и надёжнее)
                val intent = Intent(requireContext(), TestActivity::class.java).apply {
                    putExtra("testId", testId)
                    putExtra("partId", testId)
                    putExtra("isManual", false)
                    putExtra("isDebtTest", true)
                }
                startActivity(intent)

            } catch (e: Exception) {
                Log.e("DebtTestsFragment", "Error opening test", e)
                showToast("Ошибка открытия теста")
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(message: String) {
        binding.emptyStateText.text = message
        binding.emptyStateText.visibility = View.VISIBLE
        binding.debtTestsRecycler.visibility = View.GONE
    }

    private fun showContent() {
        binding.emptyStateText.visibility = View.GONE
        binding.debtTestsRecycler.visibility = View.VISIBLE
    }
}