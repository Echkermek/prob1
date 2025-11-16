package com.example.prob1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val db = Firebase.firestore
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "messages_channel"
        private const val NOTIFICATION_ID = 1001
    }

    /**
     * Вызывается когда приходит новое FCM сообщение
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "📨 Message received from: ${remoteMessage.from}")

        // Обработка данных уведомления
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "📊 Message data: ${remoteMessage.data}")
        }

        // Обработка уведомления
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "📢 Notification received: ${notification.title} - ${notification.body}")
            showNotification(
                title = notification.title ?: "Новое сообщение",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }

        // Также можно обработать только data-сообщение
        if (remoteMessage.notification == null && remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Новое сообщение"
            val body = remoteMessage.data["body"] ?: ""
            showNotification(title, body, remoteMessage.data)
        }
    }

    /**
     * Вызывается когда обновляется FCM токен
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("user_tokens").document(userId)
            .set(mapOf("token" to token))
            .addOnSuccessListener { Log.d("FCM", "Token saved") }
    }

    /**
     * Показывает уведомление в системной шторке
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Создаем канал уведомлений (для Android 8.0+)
            createNotificationChannel(notificationManager)

            // Создаем Intent для открытия приложения при нажатии на уведомление
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Передаем данные из уведомления
                putExtra("notification_data", data.toString())
                putExtra("messageId", data["messageId"])
                putExtra("type", data["type"])
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Строим уведомление
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp) // Создайте этот icon
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            // Показываем уведомление
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "✅ Notification shown: $title")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification", e)
        }
    }

    /**
     * Создает канал для уведомлений (требуется для Android 8.0+)
     */
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Сообщения",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для уведомлений о сообщениях"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Сохраняет FCM токен в Firestore
     */
    private fun saveTokenToFirestore(token: String) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val tokenData = hashMapOf(
                    "token" to token,
                    "userId" to user.uid,
                    "email" to user.email,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "device" to android.os.Build.MODEL,
                    "platform" to "android"
                )

                FirebaseFirestore.getInstance()
                    .collection("fcm_tokens")
                    .document(user.uid)
                    .set(tokenData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Token saved/updated in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error saving token to Firestore", e)
                    }
            } ?: run {
                Log.w(TAG, "⚠️ No user logged in, token not saved")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error in saveTokenToFirestore", e)
        }
    }
}