// com/example/prob1/ui/notifications/NotificationsFragment.kt
package com.example.prob1.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.base.BaseFragment
import com.example.prob1.data.EventNotification
import com.example.prob1.data.MessageNotification
import com.example.prob1.databinding.ActivityNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : BaseFragment<ActivityNotificationsBinding>() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: NotificationsAdapter
    private val allNotifications = mutableListOf<Any>()
    private var currentGroupId: String? = null
    private var currentCourseId: String? = null

    private enum class FilterType { ALL, EVENTS, MESSAGES }
    private var currentFilter = FilterType.ALL

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ActivityNotificationsBinding {
        return ActivityNotificationsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreatedSafe(savedInstanceState: Bundle?) {
        if (userId == null) {
            showToast("Не авторизован")
            showEmptyState()
            return
        }

        setupRecyclerView()
        setupFilterButtons()
        setupRefreshButton()

        // Подписываемся на данные пользователя из ViewModel
        observeUserData()

        // Загружаем уведомления
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter { notification ->
            when (notification) {
                is EventNotification -> {
                    Log.d("Notifications", "Clicked event: ${notification.title}")
                    showToast(notification.title)
                }
                is MessageNotification -> {
                    Log.d("Notifications", "Clicked message: ${notification.text}")
                }
            }
        }

        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
    }

    private fun setupFilterButtons() {
        updateButtonStyles()

        binding.btnAll.setOnClickListener {
            currentFilter = FilterType.ALL
            updateButtonStyles()
            updateFilteredNotifications()
        }

        binding.btnEvents.setOnClickListener {
            currentFilter = FilterType.EVENTS
            updateButtonStyles()
            updateFilteredNotifications()
        }

        binding.btnMessages.setOnClickListener {
            currentFilter = FilterType.MESSAGES
            updateButtonStyles()
            updateFilteredNotifications()
        }
    }

    private fun setupRefreshButton() {
        binding.refreshButton.setOnClickListener {
            loadNotifications()
        }
    }

    private fun updateButtonStyles() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.purple_500)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.purple_200)

        binding.btnAll.setBackgroundColor(if (currentFilter == FilterType.ALL) activeColor else inactiveColor)
        binding.btnEvents.setBackgroundColor(if (currentFilter == FilterType.EVENTS) activeColor else inactiveColor)
        binding.btnMessages.setBackgroundColor(if (currentFilter == FilterType.MESSAGES) activeColor else inactiveColor)
    }

    private fun observeUserData() {
        lifecycleScope.launch {
            mainViewModel.userData.collect { userData ->
                userData?.let {
                    currentGroupId = it.groupId
                    currentCourseId = it.courseId
                }
            }
        }
    }

    private fun loadNotifications() {
        launchSafe {
            try {
                showLoading(true)
                allNotifications.clear()

                // Если данных пользователя ещё нет, берём из ViewModel
                if (currentGroupId == null) {
                    mainViewModel.userData.value?.let {
                        currentGroupId = it.groupId
                        currentCourseId = it.courseId
                    }
                }

                Log.d("Notifications", "Loading notifications for groupId: $currentGroupId, courseId: $currentCourseId")

                // Загружаем личные сообщения
                loadPersonalMessages(userId!!)

                // Загружаем групповые сообщения
                currentGroupId?.let { loadGroupMessages(it) }

                // Загружаем события календаря
                loadCalendarEvents()

                // Загружаем дедлайны
                loadDeadlines()

                // Обновляем список
                updateFilteredNotifications()
                checkEmptyState()

            } catch (e: Exception) {
                Log.e("NotificationsFragment", "Error loading notifications", e)
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadPersonalMessages(userId: String) {
        try {
            val snapshot = db.collection("teacher_messages")
                .whereEqualTo("recipientId", userId)
                .get()
                .await()

            Log.d("Notifications", "Personal messages found: ${snapshot.size()}")

            snapshot.documents.forEach { doc ->
                try {
                    val timestamp = when (val ts = doc.get("timestamp")) {
                        is com.google.firebase.Timestamp -> ts.toDate().time
                        is Date -> ts.time
                        is Long -> ts
                        else -> System.currentTimeMillis()
                    }

                    val message = MessageNotification(
                        id = doc.id,
                        text = doc.getString("text") ?: "",
                        recipientId = doc.getString("recipientId") ?: "",
                        recipientName = doc.getString("recipientName") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        isGroupMessage = false,
                        timestamp = timestamp
                    )
                    allNotifications.add(message)
                } catch (e: Exception) {
                    Log.e("Notifications", "Error parsing personal message", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Notifications", "Error loading personal messages", e)
        }
    }

    private suspend fun loadGroupMessages(groupId: String) {
        try {
            val snapshot = db.collection("teacher_messages")
                .whereEqualTo("recipientId", groupId)
                .get()
                .await()

            Log.d("Notifications", "Group messages found: ${snapshot.size()}")

            snapshot.documents.forEach { doc ->
                try {
                    val timestamp = when (val ts = doc.get("timestamp")) {
                        is com.google.firebase.Timestamp -> ts.toDate().time
                        is Date -> ts.time
                        is Long -> ts
                        else -> System.currentTimeMillis()
                    }

                    val message = MessageNotification(
                        id = doc.id,
                        text = doc.getString("text") ?: "",
                        recipientId = doc.getString("recipientId") ?: "",
                        recipientName = doc.getString("recipientName") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        isGroupMessage = true,
                        timestamp = timestamp
                    )
                    allNotifications.add(message)
                } catch (e: Exception) {
                    Log.e("Notifications", "Error parsing group message", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Notifications", "Error loading group messages", e)
        }
    }

    private suspend fun loadCalendarEvents() {
        try {
            val query = if (currentGroupId != null) {
                db.collection("calendar_events")
                    .whereEqualTo("groupId", currentGroupId)
            } else {
                db.collection("calendar_events")
            }

            val snapshot = query.get().await()

            Log.d("Notifications", "Calendar events found: ${snapshot.size()}")

            snapshot.documents.forEach { doc ->
                try {
                    val dateStr = doc.getString("date")
                    val timestamp = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    val event = EventNotification(
                        id = doc.id,
                        title = doc.getString("description") ?: "Событие",
                        description = doc.getString("description") ?: "",
                        date = dateStr,
                        isDeadline = false,
                        timestamp = timestamp,
                        courseId = doc.getString("courseId"),
                        courseName = doc.getString("courseName")
                    )

                    if (shouldShowEvent(event)) {
                        allNotifications.add(event)
                    }
                } catch (e: Exception) {
                    Log.e("Notifications", "Error parsing calendar event", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Notifications", "Error loading calendar events", e)
        }
    }

    private suspend fun loadDeadlines() {
        try {
            if (currentGroupId == null) return

            val snapshot = db.collection("deadlines")
                .whereEqualTo("groupId", currentGroupId)
                .get()
                .await()

            Log.d("Notifications", "Deadlines found: ${snapshot.size()}")

            snapshot.documents.forEach { doc ->
                try {
                    val dateStr = doc.getString("deadline")
                    val timestamp = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    val event = EventNotification(
                        id = doc.id,
                        title = doc.getString("testTitle") ?: "Дедлайн",
                        description = "Срок сдачи: ${doc.getString("testTitle") ?: "Тест"}",
                        date = dateStr,
                        isDeadline = true,
                        timestamp = timestamp,
                        courseId = currentCourseId,
                        courseName = doc.getString("groupName")
                    )

                    if (shouldShowEvent(event)) {
                        allNotifications.add(event)
                    }
                } catch (e: Exception) {
                    Log.e("Notifications", "Error parsing deadline", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Notifications", "Error loading deadlines", e)
        }
    }

    private fun shouldShowEvent(event: EventNotification): Boolean {
        if (event.date == null) return true

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val eventDate = sdf.parse(event.date) ?: return true
            val today = Calendar.getInstance()
            val eventCal = Calendar.getInstance().apply { time = eventDate }

            val diffInMillis = eventCal.timeInMillis - today.timeInMillis
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

            diffInDays in -7..30
        } catch (e: Exception) {
            true
        }
    }

    private fun updateFilteredNotifications() {
        val filteredList = when (currentFilter) {
            FilterType.ALL -> allNotifications
            FilterType.EVENTS -> allNotifications.filterIsInstance<EventNotification>()
            FilterType.MESSAGES -> allNotifications.filterIsInstance<MessageNotification>()
        }

        val sorted = filteredList.sortedByDescending {
            when (it) {
                is EventNotification -> it.timestamp
                is MessageNotification -> it.timestamp
                else -> 0L
            }
        }

        adapter.submitList(sorted)
        Log.d("Notifications", "Showing ${sorted.size} notifications (filter: $currentFilter)")
    }

    private fun checkEmptyState() {
        if (allNotifications.isEmpty()) {
            showEmptyState()
        } else {
            showContent()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.refreshButton.isEnabled = !show
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.notificationsRecyclerView.visibility = View.GONE
    }

    private fun showContent() {
        binding.emptyState.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.VISIBLE
    }
}