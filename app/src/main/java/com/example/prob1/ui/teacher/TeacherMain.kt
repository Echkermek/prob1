package com.example.prob1.ui.teacher

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.prob1.databinding.ActivityTeacherDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherMain : AppCompatActivity() {
    private lateinit var binding: ActivityTeacherDashboardBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.groupsButton.setOnClickListener {
            startActivity(Intent(this, GroupActivity::class.java))
        }

        binding.calendarButton.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        binding.notificationsButton.setOnClickListener {
            startActivity(Intent(this, NotificationTeacher::class.java))
        }

        binding.deadlinesButton.setOnClickListener {
            startActivity(Intent(this, DeadlinesActivity::class.java))
        }

        /*binding.coursesButton.setOnClickListener {
            startActivity(Intent(this, ManageCoursesActivity::class.java))
        }*/
        binding.manualAnswersButton.setOnClickListener {
            startActivity(Intent(this, ManualAnswersActivity::class.java))
        }
        binding.btGrades.setOnClickListener {
            startActivity(Intent(this, CheckGrades::class.java))
        }

    }
}