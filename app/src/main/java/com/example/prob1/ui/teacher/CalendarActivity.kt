package com.example.prob1.ui.teacher

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.example.prob1.R
import com.example.prob1.databinding.ActivityCalendarBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarBinding
    private val db = FirebaseFirestore.getInstance()
    private val calendarCollection = "calendar_events"
    private var selectedDate: String = ""
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val eventDates = mutableSetOf<CalendarDay>()
    private val deadlineDates = mutableSetOf<CalendarDay>() // Для дедлайнов
    private val groups = mutableListOf<Group>()
    private var selectedGroupId: String = ""

    data class Group(
        val id: String,
        val name: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedDate = dateFormat.format(Date())
        loadGroups()

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = formatFirestoreDate(date)
            if (selectedGroupId.isNotEmpty()) {
                updateEventsForDate(selectedDate)
            }
        }

        binding.saveEventButton.setOnClickListener {
            saveEventToFirestore()
        }
    }

    private fun loadGroups() {
        db.collection("usersgroup")
            .get()
            .addOnSuccessListener { documents ->
                val uniqueGroups = mutableMapOf<String, String>()

                for (doc in documents) {
                    val groupId = doc.getString("groupId")
                    val groupName = doc.getString("groupName")
                    if (groupId != null && groupName != null) {
                        uniqueGroups[groupId] = groupName
                    }
                }

                groups.clear()
                uniqueGroups.forEach { (groupId, groupName) ->
                    groups.add(Group(groupId, groupName))
                }

                if (groups.isEmpty()) {
                    Toast.makeText(this, "Нет доступных групп", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    groups.map { it.name }
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                binding.courseSpinner.adapter = adapter
                binding.courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedGroupId = groups[position].id
                        loadAllEvents()
                        loadDeadlines() // Загружаем дедлайны
                        updateEventsForDate(selectedDate)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedGroupId = ""
                    }
                }

                // Обновляем заголовок спиннера
                val spinnerLabel = findViewById<TextView>(R.id.spinnerLabel)
                spinnerLabel?.text = "Выберите группу:"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки групп: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatFirestoreDate(date: CalendarDay): String {
        return "${date.year}-${String.format("%02d", date.month)}-${String.format("%02d", date.day)}"
    }

    private fun saveEventToFirestore() {
        if (selectedGroupId.isEmpty()) {
            Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
            return
        }

        val eventDescription = binding.eventEditText.text.toString().trim()

        if (eventDescription.isEmpty()) {
            Toast.makeText(this, "Введите описание события", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedGroup = groups.find { it.id == selectedGroupId }
        val groupName = selectedGroup?.name ?: ""

        val eventData = hashMapOf(
            "date" to selectedDate,
            "description" to eventDescription,
            "createdAt" to System.currentTimeMillis(),
            "groupId" to selectedGroupId,
            "groupName" to groupName
        )

        db.collection(calendarCollection)
            .add(eventData)
            .addOnSuccessListener {
                Toast.makeText(this, "Событие сохранено", Toast.LENGTH_SHORT).show()
                binding.eventEditText.text.clear()
                loadAllEvents()
                updateEventsForDate(selectedDate)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAllEvents() {
        if (selectedGroupId.isEmpty()) return

        db.collection(calendarCollection)
            .whereEqualTo("groupId", selectedGroupId)
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
            }
            .addOnFailureListener { e ->
                showError("Ошибка загрузки событий: ${e.message}")
            }
    }

    private fun loadDeadlines() {
        if (selectedGroupId.isEmpty()) return

        db.collection("deadlines")
            .whereEqualTo("groupId", selectedGroupId)
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
                Log.d("CalendarTeacher", "Загружено ${deadlineDates.size} дедлайнов для группы $selectedGroupId")
            }
            .addOnFailureListener { e ->
                Log.e("CalendarTeacher", "Ошибка загрузки дедлайнов", e)
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
        if (selectedGroupId.isEmpty()) return

        try {
            // Загружаем события
            val eventsQuery = db.collection(calendarCollection)
                .whereEqualTo("date", date)
                .whereEqualTo("groupId", selectedGroupId)
                .orderBy("createdAt", Query.Direction.ASCENDING)

            // Загружаем дедлайны
            val deadlinesQuery = db.collection("deadlines")
                .whereEqualTo("deadline", date)
                .whereEqualTo("groupId", selectedGroupId)

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
        binding.eventsContainer.removeAllViews()

        if ((eventsDocs == null || eventsDocs.isEmpty) && (deadlinesDocs == null || deadlinesDocs.isEmpty)) {
            val textView = TextView(this).apply {
                text = "Событий и дедлайнов на $date нет"
                setTextColor(Color.parseColor("#FFFFFFFF"))
                setPadding(16)
            }
            binding.eventsContainer.addView(textView)
            return
        }

        // Заголовок даты
        val dateHeader = TextView(this).apply {
            text = "Дата: $date"
            textSize = 18f
            setTextColor(Color.parseColor("#FFFFFF"))
            setPadding(16)
        }
        binding.eventsContainer.addView(dateHeader)

        // Отображаем события
        if (eventsDocs != null && !eventsDocs.isEmpty) {
            val eventsHeader = TextView(this).apply {
                text = " События:"
                textSize = 16f
                setTextColor(Color.parseColor("#FFFFFF"))
                setPadding(16, 8, 16, 8)
            }
            binding.eventsContainer.addView(eventsHeader)

            for (document in eventsDocs) {
                val eventView = LayoutInflater.from(this).inflate(
                    R.layout.event_item_layout,
                    binding.eventsContainer,
                    false
                )

                val eventText = eventView.findViewById<TextView>(R.id.eventText)
                val deleteButton = eventView.findViewById<Button>(R.id.deleteButton)

                eventText.text = document.getString("description") ?: "Без описания"
                deleteButton.setOnClickListener {
                    deleteEvent(document.id)
                }

                binding.eventsContainer.addView(eventView)
            }
        }

        // Отображаем дедлайны
        if (deadlinesDocs != null && !deadlinesDocs.isEmpty) {
            val deadlinesHeader = TextView(this).apply {
                text = " Дедлайны:"
                textSize = 16f
                setTextColor(Color.parseColor("#FFFFFF"))
                setPadding(16, 8, 16, 8)
            }
            binding.eventsContainer.addView(deadlinesHeader)

            for (document in deadlinesDocs) {
                val deadlineView = LayoutInflater.from(this).inflate(
                    R.layout.event_item_layout,
                    binding.eventsContainer,
                    false
                )

                val eventText = deadlineView.findViewById<TextView>(R.id.eventText)
                val deleteButton = deadlineView.findViewById<Button>(R.id.deleteButton)

                val testTitle = document.getString("testTitle") ?: "Тест"
                eventText.text = "Дедлайн: $testTitle"

                // Для дедлайнов тоже добавляем возможность удаления
                deleteButton.setOnClickListener {
                    deleteDeadline(document.id)
                }

                binding.eventsContainer.addView(deadlineView)
            }
        }
    }

    private fun loadEventsWithoutSorting(date: String) {
        if (selectedGroupId.isEmpty()) return

        val eventsQuery = db.collection(calendarCollection)
            .whereEqualTo("date", date)
            .whereEqualTo("groupId", selectedGroupId)

        val deadlinesQuery = db.collection("deadlines")
            .whereEqualTo("deadline", date)
            .whereEqualTo("groupId", selectedGroupId)

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
        binding.eventsContainer.removeAllViews()

        if (events.isEmpty() && deadlines.isEmpty()) {
            val textView = TextView(this).apply {
                text = "Событий и дедлайнов на $date нет"
                setTextColor(Color.parseColor("#FFFFFFFF"))
                setPadding(16)
            }
            binding.eventsContainer.addView(textView)
            return
        }

        val dateHeader = TextView(this).apply {
            text = "Дата: $date"
            textSize = 18f
            setTextColor(Color.parseColor("#FFFFFF"))
            setPadding(16)
        }
        binding.eventsContainer.addView(dateHeader)

        // Отображаем события
        if (events.isNotEmpty()) {
            val eventsHeader = TextView(this).apply {
                text = " События:"
                textSize = 16f
                setTextColor(Color.parseColor("#FFFFFF"))
                setPadding(16, 8, 16, 8)
            }
            binding.eventsContainer.addView(eventsHeader)

            for (document in events) {
                val eventView = LayoutInflater.from(this).inflate(
                    R.layout.event_item_layout,
                    binding.eventsContainer,
                    false
                )

                val eventText = eventView.findViewById<TextView>(R.id.eventText)
                val deleteButton = eventView.findViewById<Button>(R.id.deleteButton)

                eventText.text = document.getString("description") ?: "Без описания"
                deleteButton.setOnClickListener {
                    deleteEvent(document.id)
                }

                binding.eventsContainer.addView(eventView)
            }
        }

        // Отображаем дедлайны
        if (deadlines.isNotEmpty()) {
            val deadlinesHeader = TextView(this).apply {
                text = "Дедлайны:"
                textSize = 16f
                setTextColor(Color.parseColor("#FFFFFF"))
                setPadding(16, 8, 16, 8)
            }
            binding.eventsContainer.addView(deadlinesHeader)

            for (document in deadlines) {
                val deadlineView = LayoutInflater.from(this).inflate(
                    R.layout.event_item_layout,
                    binding.eventsContainer,
                    false
                )

                val eventText = deadlineView.findViewById<TextView>(R.id.eventText)
                val deleteButton = deadlineView.findViewById<Button>(R.id.deleteButton)

                val testTitle = document.getString("testTitle") ?: "Тест"
                eventText.text = "Дедлайн: $testTitle"

                deleteButton.setOnClickListener {
                    deleteDeadline(document.id)
                }

                binding.eventsContainer.addView(deadlineView)
            }
        }
    }

    private fun deleteEvent(documentId: String) {
        db.collection(calendarCollection)
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Событие удалено", Toast.LENGTH_SHORT).show()
                loadAllEvents()
                updateEventsForDate(selectedDate)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteDeadline(documentId: String) {
        db.collection("deadlines")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Дедлайн удален", Toast.LENGTH_SHORT).show()
                loadDeadlines()
                updateEventsForDate(selectedDate)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка удаления дедлайна: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showError(message: String?) {
        binding.eventsContainer.removeAllViews()
        val textView = TextView(this).apply {
            text = "Ошибка загрузки событий"
            setPadding(16)
        }
        binding.eventsContainer.addView(textView)
        Toast.makeText(this, message ?: "Неизвестная ошибка", Toast.LENGTH_SHORT).show()
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
            // Для дедлайнов используем большие точки другого цвета
            view.addSpan(DotSpan(10f, color))
        }
    }
}