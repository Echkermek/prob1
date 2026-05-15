// com/example/prob1/ui/lections/LectionsFragment.kt
package com.example.prob1.ui.lections

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.LectionWebViewActivity
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.Lection
import com.example.prob1.data.database.repository.LectionRepository
import com.example.prob1.data.repository.UserRepository
import com.example.prob1.data.repository.StudentScoreManager
import com.example.prob1.databinding.FragmentLectionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class LectionsFragment : BaseFragment<FragmentLectionsBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var lectionRepository: LectionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var adapter: LectionsAdapter
    private val firestore = Firebase.firestore

    private var currentCourseId: String? = null
    private var userGroupId: String? = null
    private var allLections = mutableListOf<Lection>()

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

        // Загружаем лекции для активного курса
        loadLectionsForActiveCourse()
    }

    private fun loadLectionsForActiveCourse() {
        launchSafe {
            try {
                showLoading(true)

                // 1. Получаем группу пользователя
                val groupSnapshot = firestore.collection("usersgroup")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (groupSnapshot.isEmpty()) {
                    Log.e("LectionsFragment", "User group not found")
                    showEmptyState("Группа не найдена")
                    return@launchSafe
                }

                userGroupId = groupSnapshot.documents[0].getString("groupId")
                Log.d("LectionsFragment", "User groupId: $userGroupId")

                if (userGroupId == null) {
                    showEmptyState("Группа не найдена")
                    return@launchSafe
                }

                // 2. Получаем активный курс для группы
                currentCourseId = getActiveCourseForGroup(userGroupId!!)

                if (currentCourseId == null) {
                    Log.e("LectionsFragment", "No active course found")
                    showEmptyState("Нет активного курса для вашей группы")
                    return@launchSafe
                }

                Log.d("LectionsFragment", "Active courseId: $currentCourseId")

                // 3. Получаем ID лекций для этого курса из lecture_course
                val lectureCourseSnapshot = firestore.collection("lecture_course")
                    .whereEqualTo("courseId", currentCourseId)
                    .get()
                    .await()

                val lectureIds = lectureCourseSnapshot.documents.mapNotNull { it.getString("lectureId") }
                Log.d("LectionsFragment", "Found ${lectureIds.size} lecture IDs for course")

                // 4. Загружаем сами лекции
                val lectionsList = mutableListOf<Lection>()
                for (lectureId in lectureIds) {
                    try {
                        val lectureDoc = firestore.collection("lections")
                            .document(lectureId)
                            .get()
                            .await()

                        if (lectureDoc.exists()) {
                            val lection = Lection(
                                id = lectureDoc.id,
                                name = lectureDoc.getString("name") ?: lectureDoc.getString("title") ?: "Лекция",
                                url = lectureDoc.getString("url") ?: "",
                                num = lectureDoc.getString("num") ?: "0"
                            )
                            lectionsList.add(lection)
                            Log.d("LectionsFragment", "Loaded lecture: ${lection.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("LectionsFragment", "Error loading lecture $lectureId", e)
                    }
                }

                // Сортируем по номеру
                allLections = lectionsList.sortedBy { it.num.toIntOrNull() ?: 0 }.toMutableList()

                // Обновляем адаптер
                adapter.submitList(allLections)

                if (allLections.isEmpty()) {
                    showEmptyState("Лекции не найдены для текущего курса")
                } else {
                    showContent()
                }

            } catch (e: Exception) {
                Log.e("LectionsFragment", "Error loading lections", e)
                showEmptyState("Ошибка загрузки лекций: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun getActiveCourseForGroup(groupId: String): String? {
        try {
            // Получаем все связи группы с курсами
            val courseGroupsSnapshot = firestore.collection("course_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            Log.d("LectionsFragment", "Found ${courseGroupsSnapshot.size()} course(s) for group")

            if (courseGroupsSnapshot.isEmpty()) {
                return null
            }

            // Фильтруем только активные курсы (completed = false)
            val activeCourses = mutableListOf<Pair<String, Date?>>()

            for (doc in courseGroupsSnapshot.documents) {
                val courseId = doc.getString("courseId")
                val assignedAt = doc.getDate("assignedAt")
                val isGroupCourseCompleted = doc.getBoolean("completed") == true

                Log.d("LectionsFragment", "  Checking course: $courseId, completed in course_groups: $isGroupCourseCompleted")

                if (courseId != null && !isGroupCourseCompleted) {
                    // Проверяем статус курса в коллекции courses
                    val courseDoc = firestore.collection("courses")
                        .document(courseId)
                        .get()
                        .await()

                    val isCourseCompleted = courseDoc.getBoolean("completed") == true

                    if (!isCourseCompleted) {
                        activeCourses.add(Pair(courseId, assignedAt))
                        Log.d("LectionsFragment", "  ✅ Active course: $courseId")
                    } else {
                        Log.d("LectionsFragment", "  ❌ Skipping completed course (courses): $courseId")
                    }
                } else {
                    Log.d("LectionsFragment", "  ❌ Skipping completed course (course_groups): $courseId")
                }
            }

            if (activeCourses.isEmpty()) {
                Log.e("LectionsFragment", "No active courses found")
                return null
            }

            // Выбираем самый новый курс
            val selected = activeCourses.maxByOrNull { it.second ?: java.util.Date(0) }
            return selected?.first

        } catch (e: Exception) {
            Log.e("LectionsFragment", "Error getting active course", e)
            return null
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
            loadLectionsForActiveCourse()
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

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.refreshButton.isEnabled = !show
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