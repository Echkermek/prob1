package com.example.prob1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.prob1.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            auth = FirebaseAuth.getInstance()
            Log.d("MainActivity", "FirebaseAuth initialized")

            val navView: BottomNavigationView = binding.navView
            navController = findNavController(R.id.nav_host_fragment)
            Log.d("MainActivity", "NavController found")

            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_profile,
                    R.id.navigation_lections,
                    R.id.navigation_tests,
                    R.id.navigation_browse,
                    R.id.navigation_notifications
                )
            )

            setupWithNavController(binding.navView, navController)
            Log.d("MainActivity", "BottomNavigationView setup completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "Setup error: ${e.message}", e)
            Toast.makeText(this, "Initialization error", Toast.LENGTH_LONG).show()
        }
        loadUserCourseAndSubscribe()
    }
    private fun loadUserCourseAndSubscribe() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("usersgroup").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val groupId = snapshot.documents[0].getString("groupId") ?: return@addOnSuccessListener
                    db.collection("courses").whereEqualTo("groupId", groupId).get()
                        .addOnSuccessListener { courses ->
                            if (!courses.isEmpty) {
                                val courseId = courses.documents[0].id
                                FirebaseMessaging.getInstance().subscribeToTopic("course_$courseId")
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d("FCM", "Subscribed to course_$courseId")
                                        }
                                    }
                            }
                        }
                }
            }
    }



}