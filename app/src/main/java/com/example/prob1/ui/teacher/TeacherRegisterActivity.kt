package com.example.prob1.ui.teacher

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityTeacherRegisterBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TeacherRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherRegisterBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener {
            registerTeacher()
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun registerTeacher() {
        val name = binding.nameInput.text.toString().trim()
        val surname = binding.surnameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val passwordConfirm = binding.passwordConfirmInput.text.toString().trim()
        val inviteCode = binding.inviteCodeInput.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() ||
            password.isEmpty() || passwordConfirm.isEmpty() || inviteCode.isEmpty()
        ) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректную почту", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть минимум 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        binding.registerButton.isEnabled = false

        db.collection("teacher")
            .whereEqualTo("login", email)
            .limit(1)
            .get()
            .addOnSuccessListener { teacherDocs ->
                if (!teacherDocs.isEmpty) {
                    binding.registerButton.isEnabled = true
                    Toast.makeText(this, "Такой преподаватель уже зарегистрирован", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                checkInviteCodeAndCreateTeacher(
                    name = name,
                    surname = surname,
                    email = email,
                    password = password,
                    inviteCode = inviteCode
                )
            }
            .addOnFailureListener { e ->
                binding.registerButton.isEnabled = true
                Toast.makeText(this, "Ошибка проверки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkInviteCodeAndCreateTeacher(
        name: String,
        surname: String,
        email: String,
        password: String,
        inviteCode: String
    ) {
        db.collection("invite_codes")
            .whereEqualTo("code", inviteCode)
            .whereEqualTo("used", false)
            .limit(1)
            .get()
            .addOnSuccessListener { codeDocs ->
                if (codeDocs.isEmpty) {
                    binding.registerButton.isEnabled = true
                    Toast.makeText(this, "Код приглашения неверный или уже использован", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val inviteDoc = codeDocs.documents[0]
                val teacherRef = db.collection("teacher").document()
                val inviteRef = db.collection("invite_codes").document(inviteDoc.id)

                val teacherData = hashMapOf(
                    "name" to name,
                    "surname" to surname,
                    "login" to email,
                    "password" to password,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "last_login_time" to FieldValue.serverTimestamp()
                )

                val batch = db.batch()

                batch.set(teacherRef, teacherData)

                batch.update(
                    inviteRef,
                    mapOf(
                        "used" to true,
                        "usedAt" to FieldValue.serverTimestamp(),
                        "usedBy" to teacherRef.id
                    )
                )

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.registerButton.isEnabled = true
                        Toast.makeText(this, "Ошибка регистрации: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.registerButton.isEnabled = true
                Toast.makeText(this, "Ошибка проверки кода: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}