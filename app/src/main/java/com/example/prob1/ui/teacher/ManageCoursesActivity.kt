package com.example.prob1.ui.teacher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import com.google.firebase.firestore.FirebaseFirestore

class ManageCoursesActivity : AppCompatActivity() {

    private lateinit var courseNameEditText: EditText
    private lateinit var groupSpinner: Spinner
    private lateinit var semesterSpinner: Spinner
    private lateinit var addCourseButton: Button
    private lateinit var coursesRecyclerView: RecyclerView
    private lateinit var backButton: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val coursesAdapter = CoursesAdapter(mutableListOf())
    private val groups = mutableListOf<Group>()

    data class Group(
        var id: String,
        var name: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_courses)

        backButton = findViewById(R.id.backButton)
        courseNameEditText = findViewById(R.id.courseNameEditText)
        groupSpinner = findViewById(R.id.groupSpinner)
        semesterSpinner = findViewById(R.id.semesterSpinner)
        addCourseButton = findViewById(R.id.addCourseButton)
        coursesRecyclerView = findViewById(R.id.coursesRecyclerView)

        backButton.setOnClickListener {
            finish()
        }

        coursesRecyclerView.layoutManager = LinearLayoutManager(this)
        coursesRecyclerView.adapter = coursesAdapter

        ArrayAdapter.createFromResource(
            this,
            R.array.semesters_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            semesterSpinner.adapter = adapter
        }

        loadGroups()
        loadCourses()

        courseNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateForm()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        addCourseButton.setOnClickListener {
            addCourse()
        }
    }

    private fun loadCourses() {
        db.collection("courses")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val courses = mutableListOf<CoursesAdapter.Course>()

                    for (document in task.result) {
                        val name = document.getString("name") ?: ""
                        val groupName = document.getString("groupName") ?: ""
                        val semesterValue = document["semester"]

                        val semester = when (semesterValue) {
                            is Long -> semesterValue.toInt()
                            is String -> semesterValue.toIntOrNull() ?: 1
                            else -> 1
                        }

                        courses.add(CoursesAdapter.Course(name, groupName, semester))
                    }

                    coursesAdapter.updateData(courses)
                } else {
                    Toast.makeText(this, "Ошибка загрузки курсов", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadGroups() {
        db.collection("groups")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    groups.clear()

                    for (document in task.result) {
                        val groupName = document.getString("name") ?: document.id
                        groups.add(Group(document.id, groupName))
                    }

                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        groups.map { it.name }
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }

                    groupSpinner.adapter = adapter
                    validateForm()

                    if (groups.isEmpty()) {
                        Toast.makeText(this, "Нет доступных групп", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ошибка загрузки групп", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateForm() {
        val isValid = courseNameEditText.text.toString().trim().isNotEmpty() &&
                groups.isNotEmpty() &&
                groupSpinner.selectedItemPosition >= 0

        addCourseButton.isEnabled = isValid
    }

    private fun addCourse() {
        if (groups.isEmpty() || groupSpinner.selectedItemPosition !in groups.indices) {
            Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
            return
        }

        val courseName = courseNameEditText.text.toString().trim()
        val selectedGroup = groups[groupSpinner.selectedItemPosition]
        val selectedSemester = semesterSpinner.selectedItem as String
        val semesterInt = selectedSemester.toIntOrNull() ?: 1

        val courseData = hashMapOf(
            "name" to courseName,
            "groupId" to selectedGroup.id,
            "groupName" to selectedGroup.name,
            "semester" to semesterInt
        )

        db.collection("courses")
            .add(courseData)
            .addOnSuccessListener {
                Toast.makeText(this, "Курс успешно добавлен", Toast.LENGTH_SHORT).show()
                courseNameEditText.text.clear()
                loadCourses()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}