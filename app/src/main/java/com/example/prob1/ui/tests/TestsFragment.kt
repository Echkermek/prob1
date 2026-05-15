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
import com.example.prob1.LectionWebViewActivity
import com.example.prob1.R
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.Lection
import com.example.prob1.data.Test
import com.example.prob1.data.database.repository.TestRepository
import com.example.prob1.data.repository.StudentScoreManager
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
    private lateinit var studentScoreManager: StudentScoreManager

    private var currentSemester = 1
    private var currentCourseId: String? = null
    private var userGroupId: String? = null
    private var currentCourseName: String? = null

    private var currentCourseTestIds = mutableListOf<String>()

    // Только для отображения кнопки долгов
    private var hasDebt = false

    data class DebtInfo(
        val courseId: String,
        val courseName: String,
        val studentScore: Double,
        val requiredScore: Double,
        val min3: Double,
        val min4: Double,
        val min5: Double,
        val testsCompleted: Int,
        val totalTests: Int,
        val lections: List<LectionWithInfo> = emptyList()
    )

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTestsBinding {
        return FragmentTestsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        testRepository = TestRepository(requireContext())
        studentScoreManager = StudentScoreManager()

        if (userId == null) {
            showToast(getString(R.string.not_authorized))
            return
        }

        setupRecyclerView()
        setupButtons()
        observeViewModel()

        loadAllData()
    }

    override fun onResume() {
        super.onResume()
        loadAllData()
    }

    private fun loadAllData() {
        launchSafe {
            showLoading(true)
            loadUserGroup()
        }
    }

    private fun loadUserGroup() {
        launchSafe {
            try {
                val groupSnapshot = firestore.collection("usersgroup")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (groupSnapshot.isEmpty()) {
                    Log.e("TestsFragment", "User group not found")
                    showEmptyState("Группа не найдена")
                    showLoading(false)
                    return@launchSafe
                }

                userGroupId = groupSnapshot.documents[0].getString("groupId")
                Log.d("TestsFragment", "User groupId: $userGroupId")

                if (userGroupId != null) {
                    loadGroupCourse()
                } else {
                    showLoading(false)
                }

                loadDeadlines()
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error loading user group", e)
                showEmptyState("Ошибка загрузки: ${e.message}")
                showLoading(false)
            }
        }
    }

    private suspend fun loadGroupCourse() {
        try {
            Log.d("TestsFragment", "=== loadGroupCourse ===")
            Log.d("TestsFragment", "userGroupId: $userGroupId")

            if (userGroupId == null) {
                Log.e("TestsFragment", "userGroupId is null")
                showLoading(false)
                return
            }

            // Прямая проверка: ищем курс с completed = false
            val activeCoursesSnapshot = firestore.collection("course_groups")
                .whereEqualTo("groupId", userGroupId)
                .whereEqualTo("completed", false)
                .get()
                .await()

            Log.d("TestsFragment", "Active courses (completed=false): ${activeCoursesSnapshot.size()}")

            for (doc in activeCoursesSnapshot.documents) {
                val courseId = doc.getString("courseId")
                val completed = doc.getBoolean("completed")
                Log.d("TestsFragment", "  courseId: $courseId, completed: $completed")
            }

            if (activeCoursesSnapshot.documents.isNotEmpty()) {
                val courseGroupDoc = activeCoursesSnapshot.documents[0]
                val courseId = courseGroupDoc.getString("courseId")

                if (courseId != null) {
                    val courseDoc = firestore.collection("courses").document(courseId).get().await()
                    currentCourseId = courseId
                    currentCourseName = courseDoc.getString("name") ?: "Курс"
                    Log.d("TestsFragment", "Active course found: $currentCourseId - $currentCourseName")

                    loadTestsForCurrentCourse()
                    mainViewModel.refreshAllData(userId!!)
                    checkDebtsOnly()
                    showLoading(false)
                    return
                }
            }

            // Если нет активных курсов с completed=false, ищем обычные
            Log.d("TestsFragment", "No courses with completed=false, checking all courses...")

            val allCourseGroupsSnapshot = firestore.collection("course_groups")
                .whereEqualTo("groupId", userGroupId)
                .get()
                .await()

            Log.d("TestsFragment", "Found ${allCourseGroupsSnapshot.size()} courses for group")

            if (allCourseGroupsSnapshot.documents.isEmpty()) {
                showEmptyState("Курсы не найдены для вашей группы")
                showLoading(false)
                return
            }

            var foundActiveCourse = false

            for (courseGroupDoc in allCourseGroupsSnapshot.documents) {
                val courseId = courseGroupDoc.getString("courseId")
                if (courseId == null) continue

                val courseDoc = firestore.collection("courses").document(courseId).get().await()
                val isCourseCompleted = courseDoc.getBoolean("completed") ?: false

                Log.d("TestsFragment", "Course: $courseId, completed: $isCourseCompleted")

                if (!isCourseCompleted) {
                    currentCourseId = courseId
                    currentCourseName = courseDoc.getString("name") ?: "Курс"
                    Log.d("TestsFragment", "Active course found: $currentCourseId")
                    foundActiveCourse = true
                    break
                }
            }

            if (!foundActiveCourse) {
                Log.d("TestsFragment", "All courses completed!")
                showEmptyState("Все курсы успешно завершены! 🎉")
                showLoading(false)
                return
            }

            Log.d("TestsFragment", "Current courseId: '$currentCourseId'")

            if (currentCourseId != null) {
                loadTestsForCurrentCourse()
                mainViewModel.refreshAllData(userId!!)
            }

            checkDebtsOnly()
            showLoading(false)

        } catch (e: Exception) {
            Log.e("TestsFragment", "Error loading group course", e)
            showEmptyState("Ошибка загрузки курса: ${e.message}")
            showLoading(false)
        }
    }

    private suspend fun loadTestsForCurrentCourse() {
        try {
            Log.d("TestsFragment", "=== loadTestsForCurrentCourse ===")
            Log.d("TestsFragment", "currentCourseId: $currentCourseId")

            if (currentCourseId == null) {
                Log.d("TestsFragment", "currentCourseId is null, skipping")
                return
            }

            currentCourseTestIds.clear()

            val testCourseSnapshot = firestore.collection("test_course")
                .whereEqualTo("courseId", currentCourseId)
                .get()
                .await()

            Log.d("TestsFragment", "test_course documents found: ${testCourseSnapshot.size()}")

            for (doc in testCourseSnapshot.documents) {
                val testId = doc.getString("testId")
                if (testId != null) {
                    currentCourseTestIds.add(testId)
                    Log.d("TestsFragment", "Found testId: $testId")
                }
            }

            Log.d("TestsFragment", "Final currentCourseTestIds: $currentCourseTestIds")

        } catch (e: Exception) {
            Log.e("TestsFragment", "Error loading tests for course", e)
        }
    }

    private fun loadDeadlines() {
        if (userGroupId == null) return

        launchSafe {
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
            if (hasDebt) {
                // Открываем фрагмент с долгами
                findNavController().navigate(
                    R.id.action_navigation_tests_to_debtTestsFragment,
                    Bundle().apply {
                        putString("userId", userId)
                    }
                )
            } else {
                showToast("Нет активных долгов")
            }
        }

        binding.refreshButton.setOnClickListener {
            loadAllData()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            mainViewModel.userData.collect { userData ->
                if (isAdded && userData != null) {
                    currentSemester = userData.semester
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.tests.collect { tests: List<Test> ->
                if (isAdded) {
                    updateTestsDisplay(tests)
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.isLoading.collect { isLoading: Boolean ->
                if (isAdded) {
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.refreshButton.isEnabled = !isLoading
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.error.collect { error: String? ->
                if (isAdded && error != null) {
                    showEmptyState(error)
                    mainViewModel.clearError()
                }
            }
        }
    }

    private fun updateTestsDisplay(tests: List<Test> = mainViewModel.tests.value) {
        Log.d("TestsFragment", "=== updateTestsDisplay ===")
        Log.d("TestsFragment", "currentCourseId: $currentCourseId")
        Log.d("TestsFragment", "currentCourseTestIds: $currentCourseTestIds")
        Log.d("TestsFragment", "All tests from ViewModel: ${tests.size}")

        for (test in tests) {
            Log.d("TestsFragment", "  Test: ${test.id} - ${test.title}")
        }

        val filteredTests = if (currentCourseTestIds.isNotEmpty()) {
            tests.filter { it.id in currentCourseTestIds }
        } else {
            emptyList()
        }

        Log.d("TestsFragment", "Filtered tests: ${filteredTests.size}")
        for (test in filteredTests) {
            Log.d("TestsFragment", "  Filtered: ${test.id} - ${test.title}")
        }

        testsAdapter.submitList(filteredTests)

        if (filteredTests.isEmpty() && !mainViewModel.isLoading.value) {
            if (currentCourseId != null) {
                showEmptyState("Тесты не найдены для текущего курса")
            } else {
                showEmptyState("Нет активного курса")
            }
        } else {
            showContent()
        }
    }

    // Только проверяем наличие долгов, не загружаем их данные
    private suspend fun checkDebtsOnly() {
        try {
            val uid = userId ?: return
            Log.d("TestsFragment", "=== CHECK DEBTS ONLY for userId: $uid ===")

            val debtSnapshot = firestore.collection("dolg")
                .whereEqualTo("studentId", uid)
                .whereEqualTo("status", "active")
                .get()
                .await()

            hasDebt = debtSnapshot.documents.isNotEmpty()

            if (hasDebt) {
                binding.debtButton.visibility = View.VISIBLE
                binding.debtButton.text = "Долги"
                Log.d("TestsFragment", "Has debts, showing button")
            } else {
                binding.debtButton.visibility = View.GONE
                Log.d("TestsFragment", "No debts")
            }

        } catch (e: Exception) {
            Log.e("TestsFragment", "Error checking debts", e)
            binding.debtButton.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.refreshButton.isEnabled = !show
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