package com.example.prob1.ui.teacher

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.prob1.databinding.ActivityTeacherAuthBinding
import com.google.firebase.firestore.FirebaseFirestore

class TeacherAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherAuthBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val login = binding.loginInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkTeacherCredentials(login, password)
        }
    }

    private fun checkTeacherCredentials(login: String, password: String) {
        db.collection("teacher")
            .whereEqualTo("login", login)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(this, TeacherMain::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка при проверке данных", Toast.LENGTH_SHORT).show()
            }
    }
}