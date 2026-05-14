package com.example.prob1.ui.teacher

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.databinding.ActivityDeadlinesBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DeadlinesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeadlinesBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var testsAdapter: TeacherTestAdapter

    private val groups = mutableListOf<Group>()
    private val tests = mutableListOf<Test>()
    private val deadlinesMap = mutableMapOf<String, String>()
    private var selectedGroupId: String = ""
    private var selectedGroupName: String = ""
    private var currentCourseId: String = ""

    data class Group(
        val id: String,
        val name: String
    ) {
        override fun toString(): String = name
    }

    data class Test(
        val id: String,
        val title: String,
        val courseId: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeadlinesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupButtons()
        loadGroups()
    }

    private fun setupRecyclerView() {
        testsAdapter = TeacherTestAdapter(tests, deadlinesMap) { test ->
            showDatePicker(test)
        }

        binding.testsRecycler.layoutManager = LinearLayoutManager(this)
        binding.testsRecycler.adapter = testsAdapter
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            finish()
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

                val adapter = object : ArrayAdapter<Group>(
                    this,
                    android.R.layout.simple_spinner_item,
                    groups
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        val textView = view as TextView
                        textView.setTextColor(Color.WHITE)
                        return view
                    }

                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent)
                        val textView = view as TextView
                        textView.setTextColor(Color.BLACK)
                        return view
                    }
                }
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.courseSpinner.adapter = adapter

                binding.courseSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position in groups.indices) {
                            selectedGroupId = groups[position].id
                            selectedGroupName = groups[position].name
                            Log.d("Deadlines", "Выбрана группа: ${groups[position].name}, ID: $selectedGroupId")
                            // Сначала получаем курс группы, потом тесты
                            getCourseForGroup()
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                        selectedGroupId = ""
                        tests.clear()
                        deadlinesMap.clear()
                        updateTestsRecycler()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки групп: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Получаем курс для выбранной группы из коллекции course_groups
    private fun getCourseForGroup() {
        db.collection("course_groups")
            .whereEqualTo("groupId", selectedGroupId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Группа не привязана к курсу", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val courseId = documents.documents[0].getString("courseId")
                if (!courseId.isNullOrEmpty()) {
                    currentCourseId = courseId
                    Log.d("Deadlines", "Найден courseId: $currentCourseId для группы $selectedGroupName")
                    loadTestsForCourse()
                    loadDeadlinesForGroup()
                } else {
                    Toast.makeText(this, "Ошибка: courseId не найден", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Deadlines", "Ошибка загрузки курса для группы", e)
                Toast.makeText(this, "Ошибка загрузки курса: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTestsForCourse() {
        db.collection("test_course")
            .whereEqualTo("courseId", currentCourseId)
            .get()
            .addOnSuccessListener { testCourseDocs ->
                val testIds = mutableListOf<String>()

                for (doc in testCourseDocs) {
                    val testId = doc.getString("testId")
                    if (!testId.isNullOrEmpty()) {
                        // Проверяем, что это не пустая строка и не "null"
                        if (testId != "null" && testId.isNotBlank()) {
                            testIds.add(testId)
                            Log.d("Deadlines", "Найден testId: $testId")
                        }
                    }
                }

                if (testIds.isEmpty()) {
                    tests.clear()
                    updateTestsRecycler()
                    Toast.makeText(this@DeadlinesActivity, "Нет тестов для этого курса", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Удаляем дубликаты testIds
                val uniqueTestIds = testIds.distinct()

                // Загружаем информацию о тестах по их ID
                loadTestDetails(uniqueTestIds)
            }
            .addOnFailureListener { e ->
                Log.e("Deadlines", "Ошибка загрузки test_course", e)
                tests.clear()
                updateTestsRecycler()
                Toast.makeText(this@DeadlinesActivity, "Ошибка загрузки тестов курса", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTestDetails(testIds: List<String>) {
        tests.clear()

        // Загружаем каждый тест по ID
        var loadedCount = 0
        for (testId in testIds) {
            db.collection("tests").document(testId)
                .get()
                .addOnSuccessListener { testDoc ->
                    // Проверяем, существует ли документ и есть ли у него title
                    if (testDoc.exists()) {
                        val title = testDoc.getString("title")
                        if (!title.isNullOrEmpty() && title != "null") {
                            tests.add(Test(testId, title, currentCourseId))
                            Log.d("Deadlines", "Загружен тест: $title (ID: $testId)")
                        } else {
                            Log.e("Deadlines", "Тест $testId не имеет названия, пропускаем")
                        }
                    } else {
                        Log.e("Deadlines", "Тест $testId не существует в коллекции tests, пропускаем")
                    }
                    loadedCount++

                    if (loadedCount == testIds.size) {
                        // Все тесты загружены
                        Log.d("Deadlines", "Загружено тестов: ${tests.size}")
                        updateTestsRecycler()

                        if (tests.isEmpty()) {
                            Toast.makeText(this@DeadlinesActivity, "Нет доступных тестов", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Deadlines", "Ошибка загрузки теста $testId", e)
                    loadedCount++
                    if (loadedCount == testIds.size) {
                        updateTestsRecycler()
                    }
                }
        }
    }

    private fun loadDeadlinesForGroup() {
        if (selectedGroupId.isEmpty()) return

        db.collection("deadlines")
            .whereEqualTo("groupId", selectedGroupId)
            .get()
            .addOnSuccessListener { documents ->
                deadlinesMap.clear()
                for (doc in documents) {
                    val testId = doc.getString("testId")
                    val deadline = doc.getString("deadline")
                    if (testId != null && deadline != null) {
                        deadlinesMap[testId] = deadline
                        Log.d("Deadlines", "Дедлайн для теста $testId: $deadline")
                    }
                }
                updateTestsRecycler()
            }
            .addOnFailureListener { e ->
                Log.e("Deadlines", "Ошибка загрузки дедлайнов", e)
            }
    }

    private fun updateTestsRecycler() {
        testsAdapter.updateTests(tests, deadlinesMap)

        if (tests.isEmpty()) {
            binding.emptyTestsText.visibility = View.VISIBLE
            binding.testsRecycler.visibility = View.GONE
            binding.emptyTestsText.text = "Нет доступных тестов"
        } else {
            binding.emptyTestsText.visibility = View.GONE
            binding.testsRecycler.visibility = View.VISIBLE
        }
    }

    private fun showDatePicker(test: Test) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                setDeadline(
                    test = test,
                    year = selectedYear,
                    month = selectedMonth,
                    day = selectedDay
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun setDeadline(
        test: Test,
        year: Int,
        month: Int,
        day: Int
    ) {
        if (selectedGroupId.isEmpty()) {
            Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(year, month, day)
        }
        val formattedDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(calendar.time)

        val deadlineData: Map<String, Any> = mapOf(
            "groupId" to selectedGroupId,
            "groupName" to selectedGroupName,
            "testId" to test.id,
            "testTitle" to test.title,
            "deadline" to formattedDate,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("deadlines")
            .whereEqualTo("groupId", selectedGroupId)
            .whereEqualTo("testId", test.id)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    db.collection("deadlines")
                        .add(deadlineData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Срок сдачи \"${test.title}\" установлен",
                                Toast.LENGTH_SHORT
                            ).show()
                            deadlinesMap[test.id] = formattedDate
                            updateTestsRecycler()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val docId = documents.documents[0].id
                    db.collection("deadlines").document(docId)
                        .update(deadlineData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Срок сдачи  теста \"${test.title}\" обновлен",
                                Toast.LENGTH_SHORT
                            ).show()
                            deadlinesMap[test.id] = formattedDate
                            updateTestsRecycler()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка проверки дедлайна: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}