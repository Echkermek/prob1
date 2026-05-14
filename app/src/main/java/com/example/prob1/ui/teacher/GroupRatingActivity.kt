package com.example.prob1.ui.teacher

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import com.example.prob1.RatingAdapter
import com.example.prob1.models.UserRating
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import android.app.AlertDialog

class GroupRatingActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var deleteGroupButton: Button
    private lateinit var groupNameTextView: TextView
    private var currentGroupName: String = ""
    private var currentGroupId: String = ""
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_rating)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.ratingRecyclerView)
        deleteGroupButton = findViewById(R.id.deleteGroupButton)
        groupNameTextView = findViewById(R.id.groupNameTextView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        currentGroupName = intent.getStringExtra("GROUP_NAME") ?: ""
        groupNameTextView.text = getString(R.string.group_name_format, currentGroupName)

        loadGroupRating()

        deleteGroupButton.setOnClickListener {
            showDeleteGroupDialog(currentGroupName)
        }
    }

    private fun loadGroupRating() {
        db.collection("groups")
            .whereEqualTo("name", currentGroupName)
            .limit(1)
            .get()
            .addOnSuccessListener { groupDocs ->
                if (groupDocs.isEmpty) {
                    Log.d("GroupRating", "No group found")
                    return@addOnSuccessListener
                }

                currentGroupId = groupDocs.documents[0].id
                Log.d("GroupRating", "Group ID: $currentGroupId")

                db.collection("usersgroup")
                    .whereEqualTo("groupId", currentGroupId)
                    .get()
                    .addOnSuccessListener { studentDocs ->
                        Log.d("GroupRating", "Found ${studentDocs.size()} students")

                        if (studentDocs.isEmpty) {
                            recyclerView.adapter = RatingAdapter(emptyList(), { }, true)
                            return@addOnSuccessListener
                        }

                        val students = mutableListOf<StudentData>()
                        for (doc in studentDocs.documents) {
                            val userId = doc.getString("userId") ?: ""
                            val userName = doc.getString("userName") ?: "Неизвестный"
                            students.add(StudentData(userId, userName))
                        }

                        loadStudentsData(students)
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupRating", "Error loading students", e)
                        recyclerView.adapter = RatingAdapter(emptyList(), { }, true)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("GroupRating", "Error loading group", e)
                recyclerView.adapter = RatingAdapter(emptyList(), { }, true)
            }
    }

    private fun loadStudentsData(students: List<StudentData>) {
        val userRatings = mutableListOf<UserRating>()
        var loadedCount = 0

        if (students.isEmpty()) {
            recyclerView.adapter = RatingAdapter(emptyList(), { }, true)
            return
        }

        for (student in students) {
            // Загружаем монеты
            db.collection("users")
                .whereEqualTo("userId", student.userId)
                .limit(1)
                .get()
                .addOnSuccessListener { coinsDocs ->
                    val coins = if (!coinsDocs.isEmpty) {
                        coinsDocs.documents[0].getLong("coins") ?: 0L
                    } else {
                        0L
                    }

                    // Загружаем баллы за тесты
                    loadStudentTestScores(student.userId) { testScores ->
                        val totalScore = testScores.values.sum()

                        userRatings.add(UserRating(
                            name = student.userName,
                            coins = coins,
                            userId = student.userId,
                            testScores = testScores,
                            totalTestScore = totalScore
                        ))

                        Log.d("GroupRating", "Loaded student: ${student.userName}, coins: $coins, testScore: $totalScore")

                        loadedCount++
                        if (loadedCount == students.size) {
                            val sortedRatings = userRatings.sortedByDescending { it.totalTestScore }
                            recyclerView.adapter = RatingAdapter(sortedRatings, { rating ->
                                showDeleteStudentDialog(rating)
                            }, true)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("GroupRating", "Error loading coins for ${student.userName}", e)
                    // В случае ошибки загрузки монет, загружаем только баллы за тесты
                    loadStudentTestScores(student.userId) { testScores ->
                        val totalScore = testScores.values.sum()

                        userRatings.add(UserRating(
                            name = student.userName,
                            coins = 0L,
                            userId = student.userId,
                            testScores = testScores,
                            totalTestScore = totalScore
                        ))

                        loadedCount++
                        if (loadedCount == students.size) {
                            val sortedRatings = userRatings.sortedByDescending { it.totalTestScore }
                            recyclerView.adapter = RatingAdapter(sortedRatings, { rating ->
                                showDeleteStudentDialog(rating)
                            }, true)
                        }
                    }
                }
        }
    }

    private fun loadStudentTestScores(userId: String, callback: (Map<String, Int>) -> Unit) {
        db.collection("test_grades")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { testGradeDocs ->
                val testScores = mutableMapOf<String, Int>()

                if (testGradeDocs.isEmpty) {
                    callback(emptyMap())
                    return@addOnSuccessListener
                }

                for (testGradeDoc in testGradeDocs.documents) {
                    val testId = testGradeDoc.getString("testId") ?: continue
                    val bestScore = testGradeDoc.getDouble("bestScore") ?: 0.0
                    val scoreInt = bestScore.toInt()
                    testScores[testId] = scoreInt
                }

                callback(testScores)
            }
            .addOnFailureListener { e ->
                Log.e("GroupRating", "Error loading test scores for user $userId", e)
                callback(emptyMap())
            }
    }

    private fun showDeleteStudentDialog(rating: UserRating) {
        AlertDialog.Builder(this)
            .setTitle("Удаление студента")
            .setMessage("Вы уверены, что хотите удалить студента ${rating.name}?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteStudent(rating.userId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteStudent(userId: String) {
        // First get student document
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val batch = db.batch()
                batch.delete(db.collection("users").document(userId))

                // Delete user coins
                db.collection("users")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { coinsDocs ->
                        for (doc in coinsDocs.documents) {
                            batch.delete(doc.reference)
                        }

                        db.collection("usersgroup")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener { userGroupDocs ->
                                for (doc in userGroupDocs.documents) {
                                    batch.delete(doc.reference)
                                }


                                db.collection("test_grades")
                                    .whereEqualTo("userId", userId)
                                    .get()
                                    .addOnSuccessListener { testGradeDocs ->
                                        for (doc in testGradeDocs.documents) {
                                            batch.delete(doc.reference)
                                        }

                                        batch.commit()
                                            .addOnSuccessListener {
                                                Log.d("DeleteStudent", "Firestore documents deleted successfully")
                                                loadGroupRating()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("DeleteStudent", "Error deleting documents", e)
                                            }
                                    }
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteStudent", "Error getting user document", e)
            }
    }

    private fun showDeleteGroupDialog(groupName: String) {
        AlertDialog.Builder(this)
            .setTitle("Удаление группы")
            .setMessage("Вы уверены, что хотите удалить группу $groupName?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteGroup(groupName)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteGroup(groupName: String) {
        db.collection("groups")
            .whereEqualTo("name", groupName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents.documents) {
                    db.collection("groups")
                        .document(document.id)
                        .delete()

                    db.collection("usersgroup")
                        .whereEqualTo("groupId", document.id)
                        .get()
                        .addOnSuccessListener { userGroups ->
                            for (userGroup in userGroups.documents) {
                                db.collection("usersgroup")
                                    .document(userGroup.id)
                                    .delete()
                            }
                        }
                }
                finish()
            }
    }

    private data class StudentData(val userId: String, val userName: String)
}