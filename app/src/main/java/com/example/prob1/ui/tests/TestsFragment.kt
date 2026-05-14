package com.example.prob1.ui.tests

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.Test
import com.example.prob1.data.database.repository.TestRepository
import com.example.prob1.ui.tests.TestWithDeadline
import com.example.prob1.databinding.FragmentTestsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat

class TestsFragment : BaseFragment<FragmentTestsBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var testRepository: TestRepository
    private lateinit var testsAdapter: TestsAdapter
    private val deadlinesMap = mutableMapOf<String, String>()

    private var currentSemester = 1
    private var currentCourseId: String? = null
    private var userGroupId: String? = null
    private var currentCourseName: String? = null

    // Список ID тестов текущего курса
    private var currentCourseTestIds = mutableListOf<String>()

    // Данные для долгов
    private var debtInfoList = mutableListOf<DebtInfo>()
    private val debtTestsByCourse = mutableMapOf<String, MutableList<TestWithDeadline>>()

    data class DebtInfo(
        val courseId: String,
        val courseName: String,
        val studentScore: Double,  // Реальный балл студента
        val requiredScore: Double,  // Минимальный балл для удовлетворительно
        val testsCompleted: Int,
        val totalTests: Int
    )

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTestsBinding {
        return FragmentTestsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        testRepository = TestRepository(requireContext())

        if (userId == null) {
            showToast(getString(R.string.not_authorized))
            return
        }

        setupRecyclerView()
        setupButtons()
        observeViewModel()

        // Загружаем все данные
        loadAllData()
    }

    private fun loadAllData() {
        lifecycleScope.launch {
            // 1. Загружаем группу пользователя
            loadUserGroup()

            // 2. Загружаем тесты текущего курса
            if (currentCourseId != null) {
                loadTestsForCurrentCourse()
            }

            // 3. Загружаем тесты через ViewModel
            mainViewModel.loadUserData(userId!!)

            // 4. Проверяем долги
            checkDebts()
        }
    }

    private fun loadUserGroup() {
        lifecycleScope.launch {
            try {
                val groupSnapshot = firestore.collection("usersgroup")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (groupSnapshot.isEmpty()) {
                    Log.e("TestsFragment", "User group not found")
                    return@launch
                }

                userGroupId = groupSnapshot.documents[0].getString("groupId")
                Log.d("TestsFragment", "User groupId: $userGroupId")

                if (userGroupId != null) {
                    loadGroupCourse()
                }

                loadDeadlines()
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error loading user group", e)
            }
        }
    }

    private suspend fun loadGroupCourse() {
        try {
            val groupSnapshot = firestore.collection("groups")
                .document(userGroupId!!)
                .get()
                .await()

            currentCourseId = groupSnapshot.getString("courseId")
            currentCourseName = groupSnapshot.getString("name")
            Log.d("TestsFragment", "Current courseId: $currentCourseId, name: $currentCourseName")
        } catch (e: Exception) {
            Log.e("TestsFragment", "Error loading group course", e)
        }
    }

    private suspend fun loadTestsForCurrentCourse() {
        try {
            // Получаем все тесты, привязанные к текущему курсу из коллекции test_course
            val testCourseSnapshot = firestore.collection("test_course")
                .whereEqualTo("courseId", currentCourseId)
                .get()
                .await()

            currentCourseTestIds.clear()
            for (doc in testCourseSnapshot.documents) {
                val testId = doc.getString("testId")
                if (testId != null) {
                    currentCourseTestIds.add(testId)
                }
            }

            Log.d("TestsFragment", "Found ${currentCourseTestIds.size} tests for current course")
        } catch (e: Exception) {
            Log.e("TestsFragment", "Error loading tests for course", e)
        }
    }

    private fun loadDeadlines() {
        if (userGroupId == null) {
            return
        }

        lifecycleScope.launch {
            try {
                val deadlinesSnapshot = firestore.collection("deadlines")
                    .whereEqualTo("groupId", userGroupId)
                    .get()
                    .await()

                deadlinesMap.clear()
                deadlinesSnapshot.documents.forEach { doc ->
                    val testId = doc.getString("testId")
                    val deadline = doc.getString("deadline")
                    if (testId != null && deadline != null) {
                        val formattedDate = try {
                            val parts = deadline.split("-")
                            if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else deadline
                        } catch (e: Exception) {
                            deadline
                        }
                        deadlinesMap[testId] = formattedDate
                    }
                }

                if (::testsAdapter.isInitialized) {
                    testsAdapter.updateDeadlines(deadlinesMap)
                }
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error loading deadlines", e)
            }
        }
    }

    private fun setupRecyclerView() {
        testsAdapter = TestsAdapter(deadlinesMap) { testId: String, semester: Int, _: Boolean ->
            if (semester > currentSemester) {
                showToast(getString(R.string.test_available_from_semester, semester))
            } else {
                openTestByRealStructure(testId, false)
            }
        }

        binding.recyclerTests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTests.adapter = testsAdapter
    }

    private fun openTestByRealStructure(testId: String, isDebtTest: Boolean = false) {
        launchSafe {
            try {
                val progressDialog = android.app.ProgressDialog(requireContext())
                progressDialog.setMessage(getString(R.string.loading_test))
                progressDialog.setCancelable(false)
                progressDialog.show()

                val partsSnapshot = firestore.collection("tests")
                    .document(testId)
                    .collection("parts")
                    .get()
                    .await()

                progressDialog.dismiss()

                if (!partsSnapshot.isEmpty) {
                    findNavController().navigate(
                        R.id.action_navigation_tests_to_testPartsFragment,
                        Bundle().apply {
                            putString("testId", testId)
                            putBoolean("isDebtTest", isDebtTest)
                        }
                    )
                } else {
                    val intent = Intent(requireContext(), TestActivity::class.java).apply {
                        putExtra("testId", testId)
                        putExtra("partId", testId)
                        putExtra("isManual", false)
                        putExtra("isDebtTest", isDebtTest)
                    }
                    startActivity(intent)
                }

            } catch (e: Exception) {
                Log.e("TestsFragment", "Error opening test", e)
                showToast(getString(R.string.test_load_error))
            }
        }
    }

    private fun setupButtons() {
        binding.debtButton.setOnClickListener {
            showDebtsDialogWithTests()
        }

        binding.refreshButton.setOnClickListener {
            loadAllData()
            checkTestsCompletionStatus()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            mainViewModel.userData.collect { userData ->
                userData?.let {
                    currentSemester = it.semester
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.tests.collect { tests: List<Test> ->
                if (isUiSafe) {
                    // Фильтруем тесты: показываем только те, которые принадлежат текущему курсу
                    val filteredTests = if (currentCourseTestIds.isNotEmpty()) {
                        tests.filter { it.id in currentCourseTestIds }
                    } else {
                        tests
                    }

                    testsAdapter.submitList(filteredTests)

                    if (filteredTests.isEmpty() && !mainViewModel.isLoading.value) {
                        showEmptyState(getString(R.string.tests_not_found))
                    } else {
                        showContent()
                    }
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.isLoading.collect { isLoading: Boolean ->
                if (isUiSafe) {
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.refreshButton.isEnabled = !isLoading
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.error.collect { error: String? ->
                if (error != null && isUiSafe) {
                    showEmptyState(error)
                    mainViewModel.clearError()
                }
            }
        }
    }

    private fun checkTestsCompletionStatus() {
        lifecycleScope.launch {
            try {
                val tests = mainViewModel.tests.value
                if (tests.isEmpty()) return@launch

                val completionStatus = mutableMapOf<String, Boolean>()

                for (test in tests) {
                    val isCompleted = isTestFullyCompleted(test.id)
                    completionStatus[test.id] = isCompleted
                }

                testsAdapter.updateTestCompletionStatus(completionStatus)
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error checking tests completion", e)
            }
        }
    }

    private suspend fun isTestFullyCompleted(testId: String): Boolean {
        try {
            val partsSnapshot = firestore.collection("tests")
                .document(testId)
                .collection("parts")
                .get()
                .await()

            if (partsSnapshot.isEmpty) {
                val attemptsSnapshot = firestore.collection("test_attempts")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("testId", testId)
                    .whereEqualTo("status", "completed")
                    .whereEqualTo("isPassed", true)
                    .get()
                    .await()
                return attemptsSnapshot.documents.any { it.getBoolean("isPassed") == true }
            }

            val partIds = partsSnapshot.documents.mapNotNull { it.id }
            if (partIds.isEmpty()) return false

            val allGrades = firestore.collection("test_grades")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val completedParts = allGrades.documents
                .filter { doc ->
                    val docPartId = doc.getString("partId")
                    val bestScore = doc.getDouble("bestScore") ?: 0.0
                    docPartId in partIds && bestScore > 0
                }
                .mapNotNull { it.getString("partId") }
                .toSet()

            return completedParts.size == partIds.size
        } catch (e: Exception) {
            Log.e("TestsFragment", "Error checking test completion for $testId", e)
            return false
        }
    }

    // Проверяем долги - рассчитываем реальный балл студента
    private fun checkDebts() {
        lifecycleScope.launch {
            try {
                // Получаем все завершенные курсы студента
                val completedCoursesSnapshot = firestore.collection("user_courses")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("completed", true)
                    .get()
                    .await()

                debtInfoList.clear()
                debtTestsByCourse.clear()

                for (courseDoc in completedCoursesSnapshot.documents) {
                    val courseId = courseDoc.getString("courseId") ?: continue
                    val courseName = courseDoc.getString("name") ?: "Курс"

                    // Получаем общий балл студента за этот курс из user_courses
                    val studentTotalScore = courseDoc.getDouble("totalScore") ?: 0.0

                    // Получаем пороговые значения для оценки "удовлетворительно" из коллекции course_grades
                    val passingScore = getPassingScoreForCourse(courseId)

                    // Если оценка неудовлетворительная (2) - это долг
                    if (studentTotalScore < passingScore) {
                        // Получаем тесты этого курса
                        val testCourseSnapshot = firestore.collection("test_course")
                            .whereEqualTo("courseId", courseId)
                            .get()
                            .await()

                        val testIds = testCourseSnapshot.documents.mapNotNull { it.getString("testId") }
                        val courseTests = mainViewModel.tests.value.filter { it.id in testIds }
                        if (courseTests.isEmpty()) continue

                        debtInfoList.add(
                            DebtInfo(
                                courseId = courseId,
                                courseName = courseName,
                                studentScore = studentTotalScore,
                                requiredScore = passingScore,
                                testsCompleted = 0, // Можно рассчитать из test_grades
                                totalTests = courseTests.size
                            )
                        )

                        // Сохраняем тесты этого курса для отображения в диалоге
                        debtTestsByCourse.getOrPut(courseId) { mutableListOf() }.addAll(courseTests)
                    }
                }

                if (isUiSafe) {
                    if (debtInfoList.isNotEmpty()) {
                        binding.debtButton.visibility = View.VISIBLE
                        binding.debtButton.text = getString(R.string.debts_count, debtInfoList.size)
                    } else {
                        binding.debtButton.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error checking debts", e)
                if (isUiSafe) {
                    binding.debtButton.visibility = View.GONE
                }
            }
        }
    }

    // Получаем проходной балл для курса (минимальный для "удовлетворительно")
    private suspend fun getPassingScoreForCourse(courseId: String): Double {
        return try {
            // Пытаемся получить из коллекции course_grades
            val gradeSnapshot = firestore.collection("course_grades")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("grade", 3) // Удовлетворительно
                .limit(1)
                .get()
                .await()

            if (!gradeSnapshot.isEmpty) {
                gradeSnapshot.documents[0].getDouble("minScore") ?: 0.0
            } else {
                // Дефолтные значения по семестрам
                when (currentSemester) {
                    1 -> 76.0
                    2 -> 98.0
                    3 -> 43.0
                    else -> 50.0
                }
            }
        } catch (e: Exception) {
            Log.e("TestsFragment", "Error getting passing score", e)
            50.0
        }
    }

    private fun showDebtsDialogWithTests() {
        if (debtInfoList.isEmpty()) {
            showToast(getString(R.string.no_active_debts))
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_debts_with_tests, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.debtTestsRecycler)
        val messageText = dialogView.findViewById<TextView>(R.id.debtMessageText)

        val debtInfo = debtInfoList.first()
        val debtTests = debtTestsByCourse[debtInfo.courseId].orEmpty()
        val df = DecimalFormat("#.##")
        val infoMessage = """
            Курс: ${debtInfo.courseName}
            Ваш балл: ${df.format(debtInfo.studentScore)}
            Требуется: ${df.format(debtInfo.requiredScore)} баллов для удовлетворительной оценки
            
            Список тестов, которые необходимо сдать:
        """.trimIndent()

        messageText.text = infoMessage

        val debtTestsAdapter = DebtTestsAdapter(debtTests.distinctBy { it.id }) { testId ->
            launchSafe {
                openTestByRealStructure(testId, true)
            }
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = debtTestsAdapter

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.your_debts))
            .setView(dialogView)
            .setPositiveButton("OK") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setCancelable(true)
            .create()

        dialog.show()
    }

    private fun showEmptyState(message: String) {
        binding.emptyStateText.text = message
        binding.emptyStateText.visibility = View.VISIBLE
        binding.recyclerTests.visibility = View.GONE
    }

    private fun showContent() {
        binding.emptyStateText.visibility = View.GONE
        binding.recyclerTests.visibility = View.VISIBLE
    }
}