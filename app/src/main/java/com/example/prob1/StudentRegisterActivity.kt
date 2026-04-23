package com.example.prob1

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.databinding.ActivityStudentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class StudentRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityStudentRegisterBinding
    private val groupsList = mutableListOf<String>()
    private val groupIds = mutableMapOf<String, String>()
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadGroups()

        binding.group.setOnClickListener {
            showGroupSelectionDialog()
        }

        binding.submitRegister.setOnClickListener {
            registerStudent()
        }

        // Add text watchers for name and surname fields
        setupNameAndSurnameValidation()

        // Setup password visibility toggles
        setupPasswordVisibilityToggles()
    }

    private fun setupPasswordVisibilityToggles() {
        // Password field toggle
        binding.passLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.pass.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.passLayout.setEndIconDrawable(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                binding.pass.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.passLayout.setEndIconDrawable(android.R.drawable.ic_menu_view)
            }
            binding.pass.setSelection(binding.pass.text?.length ?: 0)
        }

        // Confirm password field toggle
        binding.passConfirmLayout.setEndIconOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                binding.passConfirm.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.passConfirmLayout.setEndIconDrawable(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                binding.passConfirm.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.passConfirmLayout.setEndIconDrawable(android.R.drawable.ic_menu_view)
            }
            binding.passConfirm.setSelection(binding.passConfirm.text?.length ?: 0)
        }
    }

    private fun setupNameAndSurnameValidation() {
        binding.Name.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && !isValidName(text)) {
                    binding.Name.error = "Имя может содержать только буквы, пробелы и дефисы"
                    binding.Name.removeTextChangedListener(this)
                    val cleanText = text.filter { it.isLetter() || it == ' ' || it == '-' }
                    binding.Name.setText(cleanText)
                    binding.Name.setSelection(cleanText.length)
                    binding.Name.addTextChangedListener(this)
                } else {
                    binding.Name.error = null
                }
            }
        })

        binding.Surname.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && !isValidName(text)) {
                    binding.Surname.error = "Фамилия может содержать только буквы, пробелы и дефисы"
                    binding.Surname.removeTextChangedListener(this)
                    val cleanText = text.filter { it.isLetter() || it == ' ' || it == '-' }
                    binding.Surname.setText(cleanText)
                    binding.Surname.setSelection(cleanText.length)
                    binding.Surname.addTextChangedListener(this)
                } else {
                    binding.Surname.error = null
                }
            }
        })
    }

    private fun isValidName(name: String): Boolean {
        val namePattern = Regex("^[A-Za-zА-Яа-я\\s\\-]+$")
        return namePattern.matches(name)
    }

    private fun loadGroups() {
        db.collection("groups")
            .get()
            .addOnSuccessListener { documents ->
                groupsList.clear()
                groupIds.clear()
                for (document in documents) {
                    document.getString("name")?.let {
                        groupsList.add(it)
                        groupIds[it] = document.id
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки списка групп", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showGroupSelectionDialog() {
        if (groupsList.isEmpty()) {
            Toast.makeText(this, "Нет доступных групп. Попробуйте позже", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Выберите группу")
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, groupsList)) { dialog, which ->
                binding.group.setText(groupsList[which])
                binding.group.error = null
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun registerStudent() {
        val name = binding.Name.text.toString().trim()
        val surname = binding.Surname.text.toString().trim()
        val email = binding.mail.text.toString().trim()
        val password = binding.pass.text.toString().trim()
        val confirmPassword = binding.passConfirm.text.toString().trim()
        val groupName = binding.group.text.toString().trim()

        if (!validateInputs(name, surname, email, password, confirmPassword, groupName)) {
            return
        }

        val groupId = groupIds[groupName]
        if (groupId == null) {
            Toast.makeText(this, "Ошибка: группа не найдена", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveUserData(name, surname, email, groupName, groupId)
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка регистрации: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun validateInputs(
        name: String,
        surname: String,
        email: String,
        password: String,
        confirmPassword: String,
        group: String
    ): Boolean {
        var isValid = true

        if (TextUtils.isEmpty(name)) {
            binding.Name.error = "Введите имя"
            isValid = false
        } else if (!isValidName(name)) {
            binding.Name.error = "Имя может содержать только буквы"
            isValid = false
        } else {
            binding.Name.error = null
        }

        if (TextUtils.isEmpty(surname)) {
            binding.Surname.error = "Введите фамилию"
            isValid = false
        } else if (!isValidName(surname)) {
            binding.Surname.error = "Фамилия может содержать только буквы"
            isValid = false
        } else {
            binding.Surname.error = null
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.mail.error = "Введите корректный email"
            isValid = false
        } else {
            binding.mail.error = null
        }

        if (TextUtils.isEmpty(password) || password.length < 6) {
            binding.pass.error = "Пароль должен содержать минимум 6 символов"
            isValid = false
        } else {
            binding.pass.error = null
        }

        if (password != confirmPassword) {
            binding.passConfirm.error = "Пароли не совпадают"
            isValid = false
        } else {
            binding.passConfirm.error = null
        }

        if (TextUtils.isEmpty(group)) {
            binding.group.error = "Выберите группу из списка"
            isValid = false
        } else {
            binding.group.error = null
        }

        return isValid
    }

    private fun saveUserData(name: String, surname: String, email: String, groupName: String, groupId: String) {
        auth.currentUser?.let { user ->
            val studentData = hashMapOf(
                "name" to name,
                "surname" to surname,
                "email" to email,
                "group" to groupName,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("users")
                .document(user.uid)
                .set(studentData)
                .addOnSuccessListener {
                    val userGroupData = hashMapOf(
                        "userId" to user.uid,
                        "groupId" to groupId,
                        "userName" to "$name $surname",
                        "groupName" to groupName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("usersgroup")
                        .add(userGroupData)
                        .addOnSuccessListener {
                            val coinsData = hashMapOf(
                                "userId" to user.uid,
                                "coins" to 100,
                                "credit" to 0
                            )

                            db.collection("user_coins")
                                .document(user.uid)
                                .set(coinsData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Ошибка создания данных о монетах: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Ошибка создания связи с группой: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Ошибка сохранения данных: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}