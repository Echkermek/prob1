package com.example.prob1

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityCalendarStudentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarStudent : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarStudentBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val calendarCollection = "calendar_events"
    private val eventDates = mutableSetOf<CalendarDay>()
    private val deadlineDates = mutableSetOf<CalendarDay>() // Для дедлайнов
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentGroupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserDataAndEvents()

        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            val selectedDate = formatFirestoreDate(date)
            updateEventsForDate(selectedDate)
        }
    }

    private fun loadUserDataAndEvents() {
        val currentUserId = auth.currentUser?.uid ?: run {
            showError("Пользователь не авторизован")
            return
        }

        db.collection("usersgroup")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    currentGroupId = document.getString("groupId")
                    Log.d("Calendar", "User group ID: $currentGroupId")
                    loadAllEvents()
                    loadDeadlines() // Загружаем дедлайны
                } else {
                    showError("Студент не состоит в группе")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Calendar", "Error loading user group", e)
                showError("Ошибка загрузки данных группы")
            }
    }

    private fun formatFirestoreDate(date: CalendarDay): String {
        return "${date.year}-${String.format("%02d", date.month)}-${String.format("%02d", date.day)}"
    }

    private fun loadAllEvents() {
        if (currentGroupId == null) return

        db.collection(calendarCollection)
            .whereEqualTo("groupId", currentGroupId)
            .get()
            .addOnSuccessListener { documents ->
                eventDates.clear()
                for (document in documents) {
                    val dateStr = document.getString("date") ?: continue
                    val parts = dateStr.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        val day = parts[2].toInt()
                        eventDates.add(CalendarDay.from(year, month, day))
                    }
                }
                updateCalendarDecorators()
                updateEventsForDate(dateFormat.format(Date()))
            }
            .addOnFailureListener { e ->
                showError("Ошибка загрузки событий: ${e.message}")
            }
    }

    private fun loadDeadlines() {
        if (currentGroupId == null) return

        db.collection("deadlines")
            .whereEqualTo("groupId", currentGroupId)
            .get()
            .addOnSuccessListener { documents ->
                deadlineDates.clear()
                for (document in documents) {
                    val dateStr = document.getString("deadline") ?: continue
                    val parts = dateStr.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        val day = parts[2].toInt()
                        deadlineDates.add(CalendarDay.from(year, month, day))
                    }
                }
                updateCalendarDecorators()
                Log.d("Calendar", "Загружено ${deadlineDates.size} дедлайнов")
            }
            .addOnFailureListener { e ->
                Log.e("Calendar", "Ошибка загрузки дедлайнов", e)
            }
    }

    private fun updateCalendarDecorators() {
        binding.calendarView.removeDecorators()

        // Красные точки для обычных событий
        if (eventDates.isNotEmpty()) {
            binding.calendarView.addDecorator(EventDecorator(Color.RED, eventDates))
        }

        // Синие точки для дедлайнов
        if (deadlineDates.isNotEmpty()) {
            binding.calendarView.addDecorator(DeadlineDecorator(Color.BLUE, deadlineDates))
        }
    }

    private fun updateEventsForDate(date: String) {
        if (currentGroupId == null) return

        try {
            // Загружаем события
            val eventsQuery = db.collection(calendarCollection)
                .whereEqualTo("date", date)
                .whereEqualTo("groupId", currentGroupId)
                .orderBy("createdAt", Query.Direction.ASCENDING)

            // Загружаем дедлайны
            val deadlinesQuery = db.collection("deadlines")
                .whereEqualTo("deadline", date)
                .whereEqualTo("groupId", currentGroupId)

            eventsQuery.get()
                .addOnSuccessListener { eventsDocs ->
                    deadlinesQuery.get()
                        .addOnSuccessListener { deadlinesDocs ->
                            displayEventsAndDeadlines(date, eventsDocs, deadlinesDocs)
                        }
                        .addOnFailureListener { e ->
                            displayEventsAndDeadlines(date, eventsDocs, null)
                        }
                }
                .addOnFailureListener { e ->
                    if (e.message?.contains("index") == true) {
                        loadEventsWithoutSorting(date)
                    } else {
                        showError("Ошибка загрузки: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            showError("Ошибка: ${e.localizedMessage}")
        }
    }

    private fun displayEventsAndDeadlines(date: String, eventsDocs: com.google.firebase.firestore.QuerySnapshot?, deadlinesDocs: com.google.firebase.firestore.QuerySnapshot?) {
        val eventsText = StringBuilder()

        // Отображаем события
        if (eventsDocs != null && !eventsDocs.isEmpty) {
            eventsText.append(" События на $date:\n\n")
            for (document in eventsDocs) {
                val description = document.getString("description") ?: "Без описания"
                eventsText.append("• $description\n")
            }
            eventsText.append("\n")
        }

        // Отображаем дедлайны
        if (deadlinesDocs != null && !deadlinesDocs.isEmpty) {
            eventsText.append("Дедлайны на $date:\n\n")
            for (document in deadlinesDocs) {
                val testTitle = document.getString("testTitle") ?: "Тест"
                eventsText.append("• $testTitle\n")
            }
        }

        if (eventsText.isEmpty()) {
            binding.eventsTextView.text = "На $date событий и дедлайнов нет"
        } else {
            binding.eventsTextView.text = eventsText.toString()
        }
    }

    private fun loadEventsWithoutSorting(date: String) {
        if (currentGroupId == null) return

        val eventsQuery = db.collection(calendarCollection)
            .whereEqualTo("date", date)
            .whereEqualTo("groupId", currentGroupId)

        val deadlinesQuery = db.collection("deadlines")
            .whereEqualTo("deadline", date)
            .whereEqualTo("groupId", currentGroupId)

        eventsQuery.get()
            .addOnSuccessListener { eventsDocs ->
                deadlinesQuery.get()
                    .addOnSuccessListener { deadlinesDocs ->
                        val sortedEvents = eventsDocs.documents
                            .sortedBy { it.getLong("createdAt") ?: 0 }

                        displayEventsAndDeadlinesWithoutSorting(date, sortedEvents, deadlinesDocs.documents)
                    }
                    .addOnFailureListener { e ->
                        displayEventsAndDeadlinesWithoutSorting(date, eventsDocs.documents, emptyList())
                    }
            }
            .addOnFailureListener { e ->
                showError("Ошибка загрузки: ${e.message}")
            }
    }

    private fun displayEventsAndDeadlinesWithoutSorting(date: String, events: List<com.google.firebase.firestore.DocumentSnapshot>, deadlines: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val eventsText = StringBuilder()

        // Отображаем события
        if (events.isNotEmpty()) {
            eventsText.append(" События на $date:\n\n")
            for (document in events) {
                val description = document.getString("description") ?: "Без описания"
                eventsText.append("• $description\n")
            }
            eventsText.append("\n")
        }

        // Отображаем дедлайны
        if (deadlines.isNotEmpty()) {
            eventsText.append(" Дедлайны на $date:\n\n")
            for (document in deadlines) {
                val testTitle = document.getString("testTitle") ?: "Тест"
                eventsText.append("• $testTitle\n")
            }
        }

        if (eventsText.isEmpty()) {
            binding.eventsTextView.text = "На $date событий и дедлайнов нет"
        } else {
            binding.eventsTextView.text = eventsText.toString()
        }
    }

    private fun showError(message: String) {
        binding.eventsTextView.text = message
    }

    inner class EventDecorator(
        private val color: Int,
        private val dates: Collection<CalendarDay>
    ) : DayViewDecorator {

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f, color))
        }
    }

    inner class DeadlineDecorator(
        private val color: Int,
        private val dates: Collection<CalendarDay>
    ) : DayViewDecorator {

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            // Для дедлайнов используем другой стиль точки (больше или другой цвет)
            view.addSpan(DotSpan(10f, color))
        }
    }
}