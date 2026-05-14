package com.example.prob1.ui.teacher

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import com.example.prob1.FCMTokenManager
import com.example.prob1.databinding.ActivityNotificationTeacherBinding
import com.example.prob1.databinding.DialogSendMessageBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NotificationTeacher : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationTeacherBinding
    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = "teacher_messages"
    private var currentTeacherId: String? = null

    private val groups = mutableListOf<String>()
    private val groupIds = mutableListOf<String>()
    private val students = mutableListOf<String>()
    private val studentIds = mutableListOf<String>()
    private val messages = mutableListOf<Message>()
    private lateinit var messagesAdapter: MessagesAdapter

    // OkHttp клиент для отправки FCM запросов
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        setupUI()
        loadInitialData()
    }

    private fun setupUI() {
        setupSpinners()
        setupRecyclerView()
        setupSendButton()
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            this,
            R.array.recipient_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.recipientTypeSpinner.adapter = adapter
        }

        binding.recipientTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateStudentSpinnerVisibility(position == 1)
                if (position == 1 && binding.groupSpinner.selectedItemPosition != AdapterView.INVALID_POSITION) {
                    loadStudents(groupIds[binding.groupSpinner.selectedItemPosition])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val groupAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groups)
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.groupSpinner.adapter = groupAdapter

        binding.groupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (binding.recipientTypeSpinner.selectedItemPosition == 1) {
                    loadStudents(groupIds[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val studentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, students)
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.studentSpinner.adapter = studentAdapter

        updateStudentSpinnerVisibility(false)
    }

    private fun updateStudentSpinnerVisibility(show: Boolean) {
        binding.studentSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messages)
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationTeacher)
            adapter = messagesAdapter
        }
    }

    private fun setupSendButton() {
        binding.sendMessageButton.setOnClickListener {
            showSendMessageDialog()
        }
    }

    private fun loadInitialData() {
        db.collection("teacher").limit(1).get()
            .addOnSuccessListener { teacherDocs ->
                if (!teacherDocs.isEmpty) {
                    currentTeacherId = teacherDocs.documents[0].id
                    loadGroups()
                } else {
                    Toast.makeText(this, "Данные преподавателя не найдены", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки данных преподавателя", Toast.LENGTH_SHORT).show()
                Log.e("NotificationTeacher", "Ошибка загрузки teacher", e)
            }
    }

    private fun loadMessages() {
        if (currentTeacherId == null) {
            Log.e("NotificationTeacher", "currentTeacherId is null - cannot load messages")
            return
        }

        Log.d("NotificationTeacher", "Loading messages for teacherId: $currentTeacherId")

        db.collection(messagesCollection)
            .whereEqualTo("senderId", currentTeacherId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { querySnapshot ->
                        messages.clear()
                        Log.d("NotificationTeacher", "Found ${querySnapshot.size()} messages")

                        for (document in querySnapshot) {
                            try {
                                Log.d("NotificationTeacher", "Processing document: ${document.id}")

                                // Добавляем проверку типа timestamp
                                val timestamp = when {
                                    document.get("timestamp") is com.google.firebase.Timestamp -> {
                                        document.getTimestamp("timestamp")?.toDate()
                                    }
                                    document.get("timestamp") is Date -> {
                                        document.getDate("timestamp")
                                    }
                                    else -> null
                                } ?: continue

                                messages.add(Message(
                                    id = document.id,
                                    text = document.getString("text") ?: "No text",
                                    recipientId = document.getString("recipientId") ?: "No recipientId",
                                    recipientName = document.getString("recipientName") ?: "No name",
                                    senderId = document.getString("senderId") ?: "No senderId",
                                    timestamp = timestamp
                                ))
                            } catch (e: Exception) {
                                Log.e("NotificationTeacher", "Error parsing document ${document.id}", e)
                            }
                        }

                        runOnUiThread {
                            messagesAdapter.notifyDataSetChanged()
                            Log.d("NotificationTeacher", "Messages list updated with ${messages.size} items")
                        }
                    }
                } else {
                    val error = task.exception
                    Log.e("NotificationTeacher", "Error loading messages", error)

                    when {
                        error?.message?.contains("PERMISSION_DENIED") == true -> {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Permission denied. Check Firestore rules",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        error?.message?.contains("NOT_FOUND") == true -> {
                            Log.d("NotificationTeacher", "Collection not found - may be first run")
                            runOnUiThread {
                                messages.clear()
                                messagesAdapter.notifyDataSetChanged()
                            }
                        }
                        else -> {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Error loading messages: ${error?.message ?: "Unknown error"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
    }

    private fun showSendMessageDialog() {
        val dialogBinding = DialogSendMessageBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle("Отправить сообщение")
            .setView(dialogBinding.root)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Отправить", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val messageText = dialogBinding.etMessage.text.toString().trim()
                if (messageText.isEmpty()) {
                    dialogBinding.etMessage.error = "Введите текст сообщения"
                    return@setOnClickListener
                }

                val recipientType = binding.recipientTypeSpinner.selectedItemPosition
                val recipientId: String
                val recipientName: String

                if (recipientType == 0) { // Группа
                    val pos = binding.groupSpinner.selectedItemPosition
                    if (pos == AdapterView.INVALID_POSITION || pos >= groupIds.size) {
                        Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    recipientId = groupIds[pos]
                    recipientName = groups[pos]
                } else { // Студент
                    val pos = binding.studentSpinner.selectedItemPosition
                    if (pos == AdapterView.INVALID_POSITION || pos >= studentIds.size) {
                        Toast.makeText(this, "Выберите студента", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    recipientId = studentIds[pos]
                    recipientName = students[pos]
                }

                sendMessage(messageText, recipientId, recipientName)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun sendMessage(text: String, recipientId: String, recipientName: String) {
        currentTeacherId?.let { teacherId ->
            val messageData = hashMapOf(
                "text" to text,
                "recipientId" to recipientId,
                "recipientName" to recipientName,
                "senderId" to teacherId,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection(messagesCollection)
                .add(messageData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Сообщение отправлено", Toast.LENGTH_SHORT).show()
                    loadMessages()

                    // Отправляем уведомление после сохранения сообщения
                    sendPushNotification(recipientId, text, documentReference.id, teacherId, recipientName)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Ошибка отправки", e)
                }
        } ?: run {
            Toast.makeText(this, "ID преподавателя не установлен", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPushNotification(recipientId: String, messageText: String, messageId: String, senderId: String, recipientName: String) {
        // Получаем FCM токен получателя
        db.collection("fcm_tokens")
            .document(recipientId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val token = document.getString("token")
                    token?.let {
                        Log.d("FCM", "Sending notification to token: ${token.take(10)}...")
                        // Используем Coroutine для асинхронной отправки
                        CoroutineScope(Dispatchers.IO).launch {
                            sendFCMNotificationV1(token, messageText, messageId, recipientId, senderId, recipientName)
                        }
                    } ?: run {
                        Log.w("FCM", "No FCM token found for user: $recipientId")
                    }
                } else {
                    Log.w("FCM", "No FCM token document for user: $recipientId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error getting FCM token", e)
            }
    }

    private suspend fun sendFCMNotificationV1(token: String, message: String, messageId: String, recipientId: String, senderId: String, recipientName: String) {
        try {
            // Получаем access token
            val accessToken = FCMTokenManager.getAccessToken()

            if (accessToken == null) {
                Log.e("FCM", "Failed to get access token")
                return
            }

            val notificationBody = if (message.length > 50) message.substring(0, 50) + "..." else message

            val fcmMessage = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", token)
                    put("notification", JSONObject().apply {
                        put("title", " Новое сообщение")
                        put("body", notificationBody)
                    })
                    put("data", JSONObject().apply {
                        put("type", "message")
                        put("messageId", messageId)
                        put("recipientId", recipientId)
                        put("senderId", senderId)
                        put("recipientName", recipientName)
                        put("text", message)
                        put("click_action", "FLUTTER_NOTIFICATION_CLICK")
                    })
                    put("android", JSONObject().apply {
                        put("priority", "high")
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = fcmMessage.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/prob1-5c047/messages:send")
                .post(body)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d("FCM", "✅ FCM v1 notification sent successfully")
                Log.d("FCM", "Response: ${response.body?.string()}")
            } else {
                Log.e("FCM", "❌ FCM v1 failed: ${response.code} - ${response.body?.string()}")
            }

        } catch (e: Exception) {
            Log.e("FCM", "💥 Error sending FCM v1 notification", e)
        }
    }

    private fun loadStudents(groupId: String) {
        db.collection("usersgroup")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { documents ->
                students.clear()
                studentIds.clear()

                documents.forEach { doc ->
                    val userId = doc.getString("userId")
                    val userName = doc.getString("userName")
                    if (userId != null && userName != null) {
                        studentIds.add(userId)
                        students.add(userName)
                    }
                }

                (binding.studentSpinner.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("NotificationTeacher", "Ошибка загрузки студентов", e)
                Toast.makeText(this, "Ошибка загрузки студентов", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadGroups() {
        db.collection("usersgroup")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("NotificationTeacher", "Total documents: ${documents.size()}")

                documents.forEach { doc ->
                    Log.d("NotificationTeacher",
                        "Document: groupId=${doc.getString("groupId")}, groupName=${doc.getString("groupName")}")
                }

                val uniqueGroups = mutableMapOf<String, String>()

                for (doc in documents) {
                    val groupId = doc.getString("groupId")
                    val groupName = doc.getString("groupName")
                    if (groupId != null && groupName != null) {
                        uniqueGroups[groupId] = groupName
                    }
                }

                uniqueGroups.forEach { (groupId, groupName) ->
                    groupIds.add(groupId)
                    groups.add(groupName)
                }

                (binding.groupSpinner.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
                loadMessages()
            }
            .addOnFailureListener { e ->
                Log.e("NotificationTeacher", "Ошибка загрузки групп", e)
                Toast.makeText(this, "Ошибка загрузки групп", Toast.LENGTH_SHORT).show()
            }
    }
}

data class Message(
    val id: String = "",
    val text: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val senderId: String = "",
    val timestamp: Date = Date()
)