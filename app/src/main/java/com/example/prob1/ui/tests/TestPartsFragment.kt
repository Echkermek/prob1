// com/example/prob1/ui/tests/TestPartsFragment.kt
package com.example.prob1.ui.tests

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.Part
import com.example.prob1.data.database.repository.LectionRepository
import com.example.prob1.data.database.repository.TestRepository
import com.example.prob1.databinding.FragmentTestPartsBinding
import com.google.firebase.auth.FirebaseAuth

class TestPartsFragment : BaseFragment<FragmentTestPartsBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var testRepository: TestRepository
    private lateinit var lectionRepository: LectionRepository

    private var testId: String? = null
    private var hasParts: Boolean = true

    private lateinit var partsAdapter: PartsAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTestPartsBinding {
        return FragmentTestPartsBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testId = arguments?.getString("testId")
        hasParts = arguments?.getBoolean("hasParts") ?: true
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        testRepository = TestRepository(requireContext())
        lectionRepository = LectionRepository(requireContext())

        setupRecyclerView()
        setupBackButton()
        setupRefreshButton()

        if (userId == null) {
            showToast("Не авторизован")
            return
        }

        if (!hasParts) {
            startTestDirectly()
        } else {
            loadParts()
        }
    }

    private fun setupRecyclerView() {
        partsAdapter = PartsAdapter { partId, _, attempts, isManual ->
            launchSafe {
                checkAndStartPart(partId, attempts, isManual)
            }
        }

        binding.partsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.partsRecycler.adapter = partsAdapter
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRefreshButton() {
        binding.refreshButton.setOnClickListener {
            loadParts(forceRefresh = true)
        }
    }

    private suspend fun checkAndStartPart(partId: String, attempts: Int, isManual: Boolean) {
        try {
            val part = testRepository.getPartsForTest(testId!!).find { it.id == partId }
            val lecId = part?.lecId

            val hasNoRealLecture = lecId.isNullOrEmpty() || lecId == "not" || lecId == "-"

            if (hasNoRealLecture) {
                startTest(partId, isManual)
                return
            }

            val readCount = lectionRepository.getReadCount(userId!!, lecId!!)

            if (readCount > attempts) {
                startTest(partId, isManual)
            } else {
                val lection = lectionRepository.getLectionById(lecId)
                val lectionTitle = lection?.name ?: "лекцию"
                val lectionNum = lection?.num ?: ""

                if (isUiSafe) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Лекция не прочитана")
                        .setMessage("Прочитайте $lectionTitle $lectionNum перед прохождением теста")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e("TestPartsFragment", "Error checking part access", e)
            showToast("Ошибка проверки доступа")
        }
    }

    private fun startTestDirectly() {
        launchSafe {
            try {
                showLoading(true)

                testRepository.preloadFullTest(testId!!)

                showLoading(false)

                val intent = Intent(requireActivity(), TestActivity::class.java).apply {
                    putExtra("testId", testId)
                    putExtra("partId", testId)
                    putExtra("isManual", false)
                    putExtra("hasParts", false)
                }
                startActivity(intent)

            } catch (e: Exception) {
                showLoading(false)
                Log.e("TestPartsFragment", "Error loading test", e)
                showToast("Ошибка загрузки теста: ${e.message}")
            }
        }
    }

    private fun loadParts(forceRefresh: Boolean = false) {
        launchSafe {
            try {
                showLoading(true)

                val partEntities = testRepository.getPartsForTest(testId!!, forceRefresh)

                val parts = partEntities.map { entity ->
                    Part(
                        id = entity.id,
                        title = entity.title,
                        num = entity.num,
                        enterAnswer = entity.enterAnswer,
                        idLectures = entity.lecId ?: ""
                    )
                }.sortedBy { it.num }

                partsAdapter.submitList(parts)

                if (parts.isEmpty()) {
                    showEmptyState("Части теста не найдены")
                } else {
                    showContent()
                }

            } catch (e: Exception) {
                Log.e("TestPartsFragment", "Error loading parts", e)
                showEmptyState("Ошибка загрузки: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun startTest(partId: String, isManual: Boolean) {
        val intent = Intent(requireActivity(), TestActivity::class.java).apply {
            putExtra("partId", partId)
            putExtra("testId", testId)
            putExtra("isManual", isManual)
            putExtra("hasParts", true)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.refreshButton.isEnabled = !show
    }

    private fun showEmptyState(message: String) {
        binding.emptyStateText.text = message
        binding.emptyStateText.visibility = View.VISIBLE
        binding.partsRecycler.visibility = View.GONE
    }

    private fun showContent() {
        binding.emptyStateText.visibility = View.GONE
        binding.partsRecycler.visibility = View.VISIBLE
    }
}