package com.example.prob1.ui.teacher

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityDeadlinesBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeadlinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeadlinesBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var datePickerDialog: DatePickerDialog
    private val groups = mutableListOf<Group>()
    private val tests = mutableListOf<Test>()
    private var selectedGroupId: String = ""

    data class Group(
        val id: String,
        val name: String
    )

    data class Test(
        val id: String,
        val title: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeadlinesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        loadGroups()

        binding.setDeadlineButton.setOnClickListener {
            if (selectedGroupId.isEmpty()) {
                Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
            } else if (tests.isEmpty()) {
                Toast.makeText(this, "Нет доступных тестов", Toast.LENGTH_SHORT).show()
            } else {
                showDatePicker()
            }
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

                binding.courseSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedGroupId = groups[position].id
                        Log.d("Deadlines", "Выбрана группа: ${groups[position].name}, ID: $selectedGroupId")
                        loadTests()
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                        selectedGroupId = ""
                        tests.clear()
                        updateTestSpinner()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки групп", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTests() {
        Log.d("Deadlines", "Загружаем все тесты")

        // ИСПРАВЛЕНИЕ: используем правильное название коллекции "tests"
        db.collection("tests")
            .get()
            .addOnSuccessListener { testDocs ->
                tests.clear()

                if (testDocs.isEmpty) {
                    Log.d("Deadlines", "Нет доступных тестов в коллекции 'tests'")
                    updateTestSpinner()
                    return@addOnSuccessListener
                }

                Log.d("Deadlines", "Найдено ${testDocs.size()} тестов")

                for (testDoc in testDocs) {
                    val title = testDoc.getString("title") ?: "Без названия"
                    tests.add(Test(testDoc.id, title))
                    Log.d("Deadlines", "Добавлен тест: $title (ID: ${testDoc.id})")
                }

                updateTestSpinner()
            }
            .addOnFailureListener { e ->
                Log.e("Deadlines", "Ошибка загрузки тестов из коллекции 'tests': ${e.message}", e)
                tests.clear()
                updateTestSpinner()
            }
    }

    private fun updateTestSpinner() {
        val testTitles = if (tests.isEmpty()) {
            listOf("Нет доступных тестов")
        } else {
            tests.map { it.title }
        }

        Log.d("Deadlines", "Обновление спиннера тестов: ${testTitles.size} элементов")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, testTitles).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.testSpinner.adapter = adapter

        binding.setDeadlineButton.isEnabled = tests.isNotEmpty()


    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedTest = tests[binding.testSpinner.selectedItemPosition]
                val selectedGroup = groups.find { it.id == selectedGroupId }
                setDeadline(
                    testTitle = selectedTest.title,
                    year = selectedYear,
                    month = selectedMonth,
                    day = selectedDay,
                    group = selectedGroup,
                    testId = selectedTest.id
                )
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun setDeadline(testTitle: String, year: Int, month: Int, day: Int, group: Group?, testId: String) {
        if (group == null) {
            Toast.makeText(this, "Ошибка: группа не найдена", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance().apply { set(year, month, day) }
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val deadlineData = hashMapOf(
            "groupId" to group.id,
            "groupName" to group.name,
            "testId" to testId,
            "testTitle" to testTitle,
            "deadline" to formattedDate,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("deadlines")
            .add(deadlineData)
            .addOnSuccessListener {
                Toast.makeText(this, "Срок сдачи установлен для группы ${group.name}", Toast.LENGTH_SHORT).show()
                Log.d("Deadlines", "Дедлайн установлен: группа=${group.name}, тест=$testTitle, дата=$formattedDate")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Deadlines", "Ошибка установки дедлайна", e)
            }
    }
}