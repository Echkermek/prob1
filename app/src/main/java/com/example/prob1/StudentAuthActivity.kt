package com.example.prob1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import com.example.prob1.databinding.ActivityStudentAuthBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentAuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityStudentAuthBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkCurrentUser()

        binding.signButton.setOnClickListener {
            loginUser()
        }

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, StudentRegisterActivity::class.java))
        }

        // Setup password visibility toggle
        setupPasswordVisibilityToggle()
    }

    private fun setupPasswordVisibilityToggle() {
        binding.passwordLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.passwordField.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.passwordLayout.setEndIconDrawable(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                binding.passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.passwordLayout.setEndIconDrawable(android.R.drawable.ic_menu_view)
            }
            binding.passwordField.setSelection(binding.passwordField.text?.length ?: 0)
        }
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        auth.signOut()
                        Toast.makeText(this, "Данные пользователя не найдены", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    auth.signOut()
                    Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loginUser() {
        val email = binding.loginField.text.toString()
        val password = binding.passwordField.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkCurrentUser()
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка авторизации: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}