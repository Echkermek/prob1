// com/example/prob1/ui/tests/TestsFragment.kt
package com.example.prob1.ui.tests

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.Test
import com.example.prob1.data.database.repository.TestRepository
import com.example.prob1.databinding.FragmentTestsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TestsFragment : BaseFragment<FragmentTestsBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var testRepository: TestRepository
    private lateinit var testsAdapter: TestsAdapter

    private var currentSemester = 1
    private var currentCourseId: String? = null
    private var hasDebts = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTestsBinding {
        return FragmentTestsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        testRepository = TestRepository(requireContext())

        if (userId == null) {
            showToast("Не авторизован")
            return
        }

        setupRecyclerView()
        setupButtons()

        // Подписываемся на данные из ViewModel
        observeViewModel()

        // Если данные не загружены - загружаем
        if (mainViewModel.tests.value.isEmpty() && mainViewModel.userData.value == null) {
            mainViewModel.loadUserData(userId!!)
        }

        // Проверяем долги
        checkDebts()
    }

    private fun setupRecyclerView() {
        testsAdapter = TestsAdapter { testId: String, semester: Int, hasParts: Boolean ->
            if (semester > currentSemester && !hasDebts) {
                showToast("Этот тест доступен только с $semester семестра")
            } else {
                if (hasParts) {
                    findNavController().navigate(
                        R.id.action_navigation_tests_to_testPartsFragment,
                        Bundle().apply {
                            putString("testId", testId)
                            putBoolean("hasParts", true)
                        }
                    )
                } else {
                    startTestWithoutParts(testId)
                }
            }
        }

        binding.recyclerTests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTests.adapter = testsAdapter
    }

    private fun setupButtons() {
        binding.debtButton.setOnClickListener {
            showDebtsDialog()
        }

        binding.refreshButton.setOnClickListener {
            mainViewModel.refreshAllData(userId!!)
            checkDebts()
        }
    }

    private fun observeViewModel() {
        // Наблюдаем за данными пользователя
        lifecycleScope.launch {
            mainViewModel.userData.collect { userData ->
                userData?.let {
                    currentSemester = it.semester
                    currentCourseId = it.courseId
                }
            }
        }

        // Наблюдаем за тестами
        lifecycleScope.launch {
            mainViewModel.tests.collect { tests: List<Test> ->
                if (isUiSafe) {
                    testsAdapter.submitList(tests)

                    if (tests.isEmpty() && !mainViewModel.isLoading.value) {
                        showEmptyState("Тесты не найдены")
                    } else {
                        showContent()
                    }
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

        // Наблюдаем за ошибками
        lifecycleScope.launch {
            mainViewModel.error.collect { error: String? ->
                if (error != null && isUiSafe) {
                    showEmptyState(error)
                    mainViewModel.clearError()
                }
            }
        }
    }

    private fun checkDebts() {
        launchSafe {
            try {
                val debtsSnapshot = firestore.collection("dolg")
                    .whereEqualTo("studentId", userId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                hasDebts = !debtsSnapshot.isEmpty

                if (isUiSafe) {
                    if (hasDebts) {
                        binding.debtButton.visibility = View.VISIBLE
                        binding.debtButton.text = "Долги (${debtsSnapshot.size()})"
                    } else {
                        binding.debtButton.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("TestsFragment", "Error checking debts", e)
                hasDebts = false
                if (isUiSafe) {
                    binding.debtButton.visibility = View.GONE
                }
            }
        }
    }

    private fun startTestWithoutParts(testId: String) {
        launchSafe {
            try {
                val progressDialog = android.app.ProgressDialog(requireContext()).apply {
                    setMessage("Загрузка теста...")
                    setCancelable(false)
                    show()
                }

                testRepository.preloadFullTest(testId)

                progressDialog.dismiss()

                val intent = Intent(requireContext(), TestActivity::class.java).apply {
                    putExtra("testId", testId)
                    putExtra("partId", testId)
                    putExtra("isManual", false)
                    putExtra("hasParts", false)
                }
                startActivity(intent)

            } catch (e: Exception) {
                Log.e("TestsFragment", "Error loading test", e)
                showToast("Ошибка загрузки теста")
            }
        }
    }

    private fun showDebtsDialog() {
        launchSafe {
            try {
                val debts = firestore.collection("dolg")
                    .whereEqualTo("studentId", userId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                if (debts.isEmpty) {
                    showToast("У вас нет активных долгов")
                    return@launchSafe
                }

                val debtDetails = debts.documents.map { doc ->
                    val courseName = doc.getString("courseName") ?: "Неизвестный курс"
                    val avgScore = doc.getDouble("avgScore") ?: 0.0
                    val testsCompleted = doc.getLong("testsCompleted")?.toInt() ?: 0
                    val totalTests = doc.getLong("totalTests")?.toInt() ?: 0

                    "• $courseName\n  Средний балл: ${"%.1f".format(avgScore)} (выполнено $testsCompleted из $totalTests тестов)"
                }

                if (isUiSafe) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Ваши долги")
                        .setMessage(debtDetails.joinToString("\n\n"))
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                showToast("Ошибка загрузки долгов")
            }
        }
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