package com.example.prob1.ui.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import com.example.prob1.databinding.ActivityCheckGradesBinding
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.CoroutineContext

class CheckGrades : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityCheckGradesBinding
    private val db = FirebaseFirestore.getInstance()

    private val groups = mutableListOf<String>()
    private val groupIds = mutableListOf<String>()
    private val students = mutableListOf<String>()
    private val studentIds = mutableListOf<String>()
    private val grades = mutableListOf<StudentGrade>()
    private lateinit var gradesAdapter: GradesAdapter

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckGradesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadGroups()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun setupUI() {
        setupSpinners()
        setupRecyclerView()
    }

    private fun setupSpinners() {
        // Адаптер для групп
        val groupAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groups)
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.groupSpinner.adapter = groupAdapter

        // Адаптер для студентов
        val studentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, students)
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.studentSpinner.adapter = studentAdapter

        // Обработчик выбора группы
        binding.groupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position != AdapterView.INVALID_POSITION && position < groupIds.size) {
                    loadStudents(groupIds[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Обработчик выбора студента
        binding.studentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position != AdapterView.INVALID_POSITION && position < studentIds.size) {
                    val studentId = studentIds[position]
                    loadStudentGrades(studentId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRecyclerView() {
        gradesAdapter = GradesAdapter(grades)
        binding.gradesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CheckGrades)
            adapter = gradesAdapter
        }
    }

    private fun loadGroups() {
        binding.progressBar.visibility = View.VISIBLE

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
                groupIds.clear()

                uniqueGroups.forEach { (groupId, groupName) ->
                    groupIds.add(groupId)
                    groups.add(groupName)
                }

                (binding.groupSpinner.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

                if (groups.isNotEmpty()) {
                    binding.groupSpinner.setSelection(0)
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.emptyState.text = "Нет доступных групп"
                }
            }
            .addOnFailureListener { e ->
                Log.e("CheckGrades", "Ошибка загрузки групп", e)
                Toast.makeText(this, "Ошибка загрузки групп", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.emptyState.text = "Ошибка загрузки групп"
            }
    }

    private fun loadStudents(groupId: String) {
        binding.progressBar.visibility = View.VISIBLE
        students.clear()
        studentIds.clear()
        (binding.studentSpinner.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
        showEmptyState()

        db.collection("usersgroup")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("CheckGrades", "Found ${documents.size()} documents in usersgroup for group $groupId")

                if (documents.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.emptyState.text = "В группе нет студентов"
                    return@addOnSuccessListener
                }

                // Собираем все userId из группы
                val userIds = documents.mapNotNull { it.getString("userId") }

                Log.d("CheckGrades", "User IDs found: $userIds")

                if (userIds.isNotEmpty()) {
                    // Загружаем информацию о пользователях
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), userIds)
                        .get()
                        .addOnSuccessListener { userDocuments ->
                            Log.d("CheckGrades", "Found ${userDocuments.size()} user documents")

                            userDocuments.forEach { userDoc ->
                                try {
                                    val userId = userDoc.id
                                    val name = userDoc.getString("name") ?: ""
                                    val surname = userDoc.getString("surname") ?: ""
                                    val userName = "$name $surname".trim()

                                    if (userName.isNotEmpty()) {
                                        studentIds.add(userId)
                                        students.add(userName)
                                        Log.d("CheckGrades", "Added student: $userName (ID: $userId)")
                                    }
                                } catch (e: Exception) {
                                    Log.e("CheckGrades", "Error processing user document", e)
                                }
                            }

                            Log.d("CheckGrades", "Total students loaded: ${students.size}")

                            (binding.studentSpinner.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

                            if (students.isNotEmpty()) {
                                binding.studentSpinner.setSelection(0)
                                binding.progressBar.visibility = View.GONE
                            } else {
                                binding.progressBar.visibility = View.GONE
                                binding.emptyState.visibility = View.VISIBLE
                                binding.emptyState.text = "В группе нет студентов"
                                Toast.makeText(this, "В группе нет студентов", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("CheckGrades", "Error loading users", e)
                            binding.progressBar.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                            binding.emptyState.text = "Ошибка загрузки студентов"
                            Toast.makeText(this, "Ошибка загрузки студентов", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.emptyState.text = "В группе нет студентов"
                    Toast.makeText(this, "В группе нет студентов", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("CheckGrades", "Ошибка загрузки студентов группы", e)
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.emptyState.text = "Ошибка загрузки студентов"
                Toast.makeText(this, "Ошибка загрузки студентов", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadStudentGrades(studentId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.gradesRecyclerView.visibility = View.GONE

        Log.d("CheckGrades", "Loading grades for student: $studentId")

        launch {
            try {
                grades.clear()

                // Загружаем оценки студента
                val gradeDocuments = withContext(Dispatchers.IO) {
                    db.collection("test_grades")
                        .whereEqualTo("userId", studentId)
                        .get()
                        .await()
                }

                Log.d("CheckGrades", "Found ${gradeDocuments.size()} grade documents")

                if (gradeDocuments.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.emptyState.text = "Нет оценок для выбранного студента"
                    return@launch
                }

                // Собираем все testId и partId
                val testIds = gradeDocuments.mapNotNull { it.getString("testId") }.distinct()
                val partIds = gradeDocuments.mapNotNull { it.getString("partId") }.distinct()

                Log.d("CheckGrades", "Test IDs: $testIds")
                Log.d("CheckGrades", "Part IDs: $partIds")

                // Загружаем информацию о тестах
                val testMap = withContext(Dispatchers.IO) {
                    db.collection("tests")
                        .whereIn(FieldPath.documentId(), testIds)
                        .get()
                        .await()
                        .associateBy { it.id }
                }

                Log.d("CheckGrades", "Found ${testMap.size} test documents")

                // Загружаем информацию о частях тестов из подколлекций
                val partMap = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()

                for (testId in testIds) {
                    try {
                        val parts = withContext(Dispatchers.IO) {
                            db.collection("tests").document(testId).collection("parts")
                                .whereIn(FieldPath.documentId(), partIds)
                                .get()
                                .await()
                        }

                        for (partDoc in parts) {
                            partMap[partDoc.id] = partDoc
                        }
                    } catch (e: Exception) {
                        Log.e("CheckGrades", "Error loading parts for test $testId", e)
                    }
                }

                Log.d("CheckGrades", "Found ${partMap.size} part documents")

                // Формируем список оценок
                for (gradeDoc in gradeDocuments) {
                    try {
                        val testId = gradeDoc.getString("testId") ?: continue
                        val partId = gradeDoc.getString("partId") ?: continue

                        val testDoc = testMap[testId]
                        val partDoc = partMap[partId]

                        val bestScore = gradeDoc.getDouble("bestScore") ?: 0.0
                        val timestamp = gradeDoc.getTimestamp("timestamp")?.toDate() ?: Date()
                        val isManual = gradeDoc.getBoolean("isManual") ?: false

                        val testName = testDoc?.getString("title") ?: "Неизвестный тест"
                        val partName = partDoc?.getString("title") ?: "Неизвестная часть"

                        val grade = StudentGrade(
                            testName = testName,
                            partName = partName,
                            score = bestScore,
                            timestamp = timestamp,
                            isManual = isManual
                        )

                        grades.add(grade)
                        Log.d("CheckGrades", "Added grade: $testName - $partName - $bestScore")
                    } catch (e: Exception) {
                        Log.e("CheckGrades", "Error processing grade document", e)
                    }
                }

                // Сортируем по дате (новые сверху)
                grades.sortByDescending { it.timestamp }

                binding.progressBar.visibility = View.GONE

                if (grades.isNotEmpty()) {
                    binding.emptyState.visibility = View.GONE
                    binding.gradesRecyclerView.visibility = View.VISIBLE
                    gradesAdapter.notifyDataSetChanged()
                    Log.d("CheckGrades", "Displaying ${grades.size} grades")
                } else {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.gradesRecyclerView.visibility = View.GONE
                    binding.emptyState.text = "Нет оценок для отображения"
                }

            } catch (e: Exception) {
                Log.e("CheckGrades", "Error loading student grades", e)
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.emptyState.text = "Ошибка загрузки оценок"
                Toast.makeText(this@CheckGrades, "Ошибка загрузки оценок", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState() {
        grades.clear()
        gradesAdapter.notifyDataSetChanged()
        binding.emptyState.visibility = View.VISIBLE
        binding.gradesRecyclerView.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }
}

data class StudentGrade(
    val testName: String,
    val partName: String,
    val score: Double,
    val timestamp: Date,
    val isManual: Boolean = false
)

class GradesAdapter(private val grades: List<StudentGrade>) :
    RecyclerView.Adapter<GradesAdapter.GradeViewHolder>() {

    class GradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val testName: TextView = itemView.findViewById(R.id.testName)
        val partName: TextView = itemView.findViewById(R.id.partName)
        val grade: TextView = itemView.findViewById(R.id.gradeValue)
        val date: TextView = itemView.findViewById(R.id.dateText)
        val type: TextView = itemView.findViewById(R.id.typeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_grade, parent, false)
        return GradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val gradeItem = grades[position]

        holder.testName.text = gradeItem.testName
        holder.partName.text = gradeItem.partName
        holder.grade.text = "%.1f".format(gradeItem.score)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.date.text = dateFormat.format(gradeItem.timestamp)

        holder.type.text = if (gradeItem.isManual) "Ручная проверка" else ""
    }

    override fun getItemCount() = grades.size
}