// com/example/prob1/ui/lections/LectionsFragment.kt
package com.example.prob1.ui.lections

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.LectionWebViewActivity
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.Lection
import com.example.prob1.data.database.repository.LectionRepository
import com.example.prob1.data.database.repository.UserRepository
import com.example.prob1.databinding.FragmentLectionsBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LectionsFragment : BaseFragment<FragmentLectionsBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var lectionRepository: LectionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var adapter: LectionsAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLectionsBinding {
        return FragmentLectionsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        lectionRepository = LectionRepository(requireContext())
        userRepository = UserRepository(requireContext())

        if (userId == null) {
            showToast("Не авторизован")
            return
        }

        setupRecyclerView()
        setupRefreshButton()

        // Подписываемся на данные из ViewModel
        observeViewModel()

        // Если данные не загружены - загружаем
        if (mainViewModel.lections.value.isEmpty() && mainViewModel.userData.value == null) {
            mainViewModel.loadUserData(userId!!)
        }
    }

    private fun setupRecyclerView() {
        adapter = LectionsAdapter(
            onClick = { lection: Lection -> checkAndOpenLection(lection) },
            getReadStatus = { lectionId: String -> lectionRepository.isLectionRead(userId!!, lectionId) }
        )

        binding.lectionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.lectionsRecyclerView.adapter = adapter
    }

    private fun setupRefreshButton() {
        binding.refreshButton.setOnClickListener {
            mainViewModel.refreshAllData(userId!!)
        }
    }

    private fun observeViewModel() {
        // Наблюдаем за лекциями
        lifecycleScope.launch {
            mainViewModel.lections.collect { lections: List<Lection> ->
                if (isUiSafe) {
                    adapter.submitList(lections)

                    if (lections.isEmpty() && !mainViewModel.isLoading.value) {
                        showEmptyState("Лекции не найдены")
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
    }

    private fun checkAndOpenLection(lection: Lection) {
        launchSafe {
            val readCount = lectionRepository.getReadCount(userId!!, lection.id)
            val needPay = readCount > 0

            if (needPay) {
                showPayDialog { confirmed: Boolean ->
                    if (confirmed) {
                        launchSafe {
                            if (userRepository.deductCoin(userId!!, 1)) {
                                lectionRepository.markLectionAsRead(userId!!, lection.id)
                                val currentCoins = mainViewModel.userData.value?.coins ?: 0
                                mainViewModel.updateCoins(currentCoins - 1)
                                openLection(lection)
                            } else {
                                showToast("Недостаточно монет")
                            }
                        }
                    }
                }
            } else {
                lectionRepository.markLectionAsRead(userId!!, lection.id)
                openLection(lection)
            }
        }
    }

    private fun showPayDialog(onConfirm: (Boolean) -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Повторное чтение")
            .setMessage("Списать 1 монету за повторное чтение лекции?")
            .setPositiveButton("Да") { _, _ -> onConfirm(true) }
            .setNegativeButton("Нет") { _, _ -> onConfirm(false) }
            .show()
    }

    private fun openLection(lection: Lection) {
        val intent = Intent(requireContext(), LectionWebViewActivity::class.java).apply {
            putExtra("url", lection.url)
            putExtra("lectionId", lection.id)
        }
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun showEmptyState(message: String) {
        binding.emptyStateText.text = message
        binding.emptyStateText.visibility = View.VISIBLE
        binding.lectionsRecyclerView.visibility = View.GONE
    }

    private fun showContent() {
        binding.emptyStateText.visibility = View.GONE
        binding.lectionsRecyclerView.visibility = View.VISIBLE
    }

    companion object {
        private const val REQUEST_CODE = 101
    }
}