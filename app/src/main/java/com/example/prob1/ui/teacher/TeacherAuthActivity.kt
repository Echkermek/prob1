package com.example.prob1.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityTeacherAuthBinding
import com.google.firebase.firestore.FieldValue
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

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, TeacherRegisterActivity::class.java))
        }
    }

    private fun checkTeacherCredentials(login: String, password: String) {
        db.collection("teacher")
            .whereEqualTo("login", login)
            .whereEqualTo("password", password)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                } else {
                    val teacherDoc = documents.documents[0]

                    db.collection("teacher")
                        .document(teacherDoc.id)
                        .update("last_login_time", FieldValue.serverTimestamp())

                    startActivity(Intent(this, TeacherMain::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка при проверке данных", Toast.LENGTH_SHORT).show()
            }
    }
}