package com.example.prob1

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.databinding.ActivityRatingBinding
import com.example.prob1.models.UserRating
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RatingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRatingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val fakeNames = listOf("Утконос", "Ананас", "Банан", "Ежик", "Кролик")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRatingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ratingRecyclerView.layoutManager = LinearLayoutManager(this)
        loadGroupRating()
    }

    private fun loadGroupRating() {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserName = auth.currentUser?.displayName ?: "Вы"

        db.collection("usersgroup")
            .whereEqualTo("userId", currentUserId)
            .limit(1)
            .get()
            .addOnSuccessListener { groupDocs ->
                if (groupDocs.isEmpty) {
                    Log.d("RatingActivity", "No group found for user")
                    return@addOnSuccessListener
                }

                val groupId = groupDocs.documents[0].getString("groupId") ?: ""
                val groupName = groupDocs.documents[0].getString("groupName") ?: ""
                binding.groupNameTextView.text = getString(R.string.group_name_format, groupName)

                db.collection("usersgroup")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .addOnSuccessListener { studentDocs ->
                        val students = mutableListOf<StudentData>()

                        for (doc in studentDocs.documents) {
                            val userId = doc.getString("userId") ?: ""
                            val userName = doc.getString("userName") ?: "Неизвестный"
                            students.add(StudentData(userId, userName))
                        }

                        loadStudentsData(students, currentUserId, currentUserName)
                    }
                    .addOnFailureListener { e ->
                        Log.e("RatingActivity", "Error loading students", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("RatingActivity", "Error loading group", e)
            }
    }

    private fun loadStudentsData(
        students: List<StudentData>,
        currentUserId: String,
        currentUserName: String
    ) {
        val allRatings = mutableListOf<UserRating>()
        var loadedCount = 0

        if (students.isEmpty()) {
            processRatings(allRatings, currentUserId, currentUserName)
            return
        }

        for (student in students) {
            db.collection("users")
                .document(student.userId)
                .get()
                .addOnSuccessListener { userDoc ->

                    val coins = userDoc.getLong("coins") ?: 0L

                    val rating = UserRating(
                        name = student.userName,
                        coins = coins,
                        userId = student.userId
                    )

                    allRatings.add(rating)

                    loadedCount++
                    if (loadedCount == students.size) {
                        processRatings(allRatings, currentUserId, currentUserName)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RatingActivity", "Error loading coins for ${student.userName}", e)

                    allRatings.add(
                        UserRating(
                            name = student.userName,
                            coins = 0L,
                            userId = student.userId
                        )
                    )

                    loadedCount++
                    if (loadedCount == students.size) {
                        processRatings(allRatings, currentUserId, currentUserName)
                    }
                }
        }
    }

    private fun processRatings(
        allRatings: List<UserRating>,
        currentUserId: String,
        currentUserName: String
    ) {
        val sortedRatings = allRatings.sortedByDescending { it.coins }
        val userPosition = sortedRatings.indexOfFirst { it.userId == currentUserId } + 1

        val top5Ratings = sortedRatings.take(5).mapIndexed { index, rating ->
            val isCurrentUser = rating.userId == currentUserId

            val displayName = if (isCurrentUser) {
                getString(
                    R.string.you_with_nickname,
                    fakeNames.getOrElse(index) {
                        getString(R.string.student_format, index + 1)
                    }
                )
            } else {
                fakeNames.getOrElse(index) {
                    getString(R.string.student_format, index + 1)
                }
            }

            rating.copy(name = displayName)
        }

        binding.ratingRecyclerView.adapter =
            RatingAdapter(top5Ratings, { }, false)

        val currentUserRating = sortedRatings.find { it.userId == currentUserId }
        currentUserRating?.let {
            binding.currentUserInfo.text =
                getString(R.string.user_info_format, currentUserName, it.coins, userPosition)
        }
    }

    private data class StudentData(
        val userId: String,
        val userName: String
    )
}