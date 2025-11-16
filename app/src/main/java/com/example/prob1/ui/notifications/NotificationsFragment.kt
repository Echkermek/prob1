package com.example.prob1.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.data.EventNotification
import com.example.prob1.data.MessageNotification
import com.example.prob1.databinding.ActivityNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {
    private var _binding: ActivityNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: NotificationsAdapter
    private val allNotifications = mutableListOf<Any>()
    private var currentGroupId: String? = null
    private var currentCourseId: String? = null

    private enum class FilterType { ALL, EVENTS, MESSAGES }
    private var currentFilter = FilterType.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupButtons()
        loadUserDataAndNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter { notification ->
            // Обработка клика на уведомление
        }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
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

    private fun updateButtonStyles() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.purple_500)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.purple_200)

        binding.btnAll.setBackgroundColor(
            if (currentFilter == FilterType.ALL) activeColor else inactiveColor
        )
        binding.btnEvents.setBackgroundColor(
            if (currentFilter == FilterType.EVENTS) activeColor else inactiveColor
        )
        binding.btnMessages.setBackgroundColor(
            if (currentFilter == FilterType.MESSAGES) activeColor else inactiveColor
        )
    }

    private fun loadUserDataAndNotifications() {
        binding.progressBar.visibility = View.VISIBLE
        allNotifications.clear()

        val currentUserId = auth.currentUser?.uid ?: run {
            binding.progressBar.visibility = View.GONE
            Log.e("Notifications", "Current user ID is null")
            return
        }

        // 1. Загружаем группу пользователя
        db.collection("usersgroup")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    currentGroupId = document.getString("groupId")
                    Log.d("Notifications", "User group ID: $currentGroupId")

                    // 2. Находим курс по ID группы
                    currentGroupId?.let { groupId ->
                        db.collection("courses")
                            .whereEqualTo("groupId", groupId)
                            .get()
                            .addOnSuccessListener { coursesSnapshot ->
                                if (!coursesSnapshot.isEmpty) {
                                    val courseDoc = coursesSnapshot.documents[0]
                                    currentCourseId = courseDoc.id
                                    Log.d("Notifications", "User course ID: $currentCourseId")
                                }
                                loadNotifications(currentUserId)
                            }
                            .addOnFailureListener { e ->
                                Log.e("Notifications", "Error loading user course", e)
                                loadNotifications(currentUserId)
                            }
                    } ?: run {
                        loadNotifications(currentUserId)
                    }
                } else {
                    loadNotifications(currentUserId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error loading user group", e)
                loadNotifications(currentUserId)
            }
    }

    private fun loadNotifications(userId: String) {
        loadPersonalMessages(userId)
        currentGroupId?.let { loadGroupMessages(it) }
        loadCalendarEvents()
    }

    private fun loadPersonalMessages(userId: String) {
        db.collection("teacher_messages")
            .whereEqualTo("recipientId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val timestamp = when (val ts = doc.get("timestamp")) {
                            is com.google.firebase.Timestamp -> ts.toDate().time
                            is Date -> ts.time
                            is Long -> ts
                            else -> System.currentTimeMillis()
                        }

                        MessageNotification(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            recipientId = doc.getString("recipientId") ?: "",
                            recipientName = doc.getString("recipientName") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            isGroupMessage = false,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        Log.e("Notifications", "Error parsing message", e)
                        null
                    }
                }

                Log.d("Notifications", "Loaded ${messages.size} personal messages")
                allNotifications.addAll(messages)
                updateFilteredNotifications()
                checkEmptyState()
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error loading personal messages", e)
                checkEmptyState()
            }
    }

    private fun loadGroupMessages(groupId: String) {
        db.collection("teacher_messages")
            .whereEqualTo("recipientId", groupId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val timestamp = when (val ts = doc.get("timestamp")) {
                            is com.google.firebase.Timestamp -> ts.toDate().time
                            is Date -> ts.time
                            is Long -> ts
                            else -> System.currentTimeMillis()
                        }

                        MessageNotification(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            recipientId = doc.getString("recipientId") ?: "",
                            recipientName = doc.getString("recipientName") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            isGroupMessage = true,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        Log.e("Notifications", "Error parsing group message", e)
                        null
                    }
                }

                Log.d("Notifications", "Loaded ${messages.size} group messages")
                allNotifications.addAll(messages)
                updateFilteredNotifications()
                checkEmptyState()
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error loading group messages", e)
                checkEmptyState()
            }
    }

    private fun loadCalendarEvents() {
        val query = if (currentCourseId != null) {
            db.collection("calendar_events")
                .whereEqualTo("courseId", currentCourseId)
        } else {
            db.collection("calendar_events")
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val events = snapshot.documents.mapNotNull { doc ->
                    try {
                        val isDeadline = doc.getBoolean("isDeadline") ?: false
                        EventNotification(
                            id = doc.id,
                            title = doc.getString("testTitle") ?: doc.getString("description") ?: "Без названия",
                            description = doc.getString("description") ?: "",
                            date = doc.getString("date"),
                            isDeadline = isDeadline,
                            timestamp = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            courseId = doc.getString("courseId")
                        ).takeIf { shouldShowEvent(it) }
                    } catch (e: Exception) {
                        Log.e("Notifications", "Error parsing event", e)
                        null
                    }
                }

                Log.d("Notifications", "Loaded ${events.size} calendar events")
                allNotifications.addAll(events)
                updateFilteredNotifications()
                checkEmptyState()
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error loading events", e)
                checkEmptyState()
            }
    }

    private fun checkEmptyState() {
        if (!isAdded) return

        binding.progressBar.visibility = View.GONE
        if (allNotifications.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.notificationsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.notificationsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun shouldShowEvent(event: EventNotification): Boolean {
        // Если у события нет courseId, показываем его всем
        if (event.courseId == null) return true

        // Показываем только события для текущего курса пользователя
        if (currentCourseId != null && event.courseId != currentCourseId) {
            return false
        }

        // Остальная логика фильтрации по дате
        if (event.date == null) return true

        return try {
            val eventDate = parseDate(event.date!!)
            val today = Calendar.getInstance()

            val diffInMillis = eventDate.timeInMillis - today.timeInMillis
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

            diffInDays in -7..30
        } catch (e: Exception) {
            Log.e("Notifications", "Error checking event date", e)
            true
        }
    }

    private fun updateFilteredNotifications() {
        if (!isAdded) return

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
        checkEmptyState()
    }

    private fun parseDate(dateStr: String): Calendar {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: Date()
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            Log.e("Notifications", "Error parsing date", e)
            Calendar.getInstance()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}