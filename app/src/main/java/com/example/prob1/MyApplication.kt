package com.example.prob1

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        setupFCM()
    }

    private fun setupFCM() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result!!
                    Log.d("FCM", "📱 FCM Token received: ${token.take(10)}...")
                    saveTokenToFirestore(token)
                } else {
                    Log.e("FCM", "❌ Failed to get FCM token", task.exception)
                }
            }

            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FCM", " Subscribed to topic: all_users")
                    } else {
                        Log.e("FCM", " Failed to subscribe to topic", task.exception)
                    }
                }

        } catch (e: Exception) {
            Log.e("FCM", " Error in FCM setup", e)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                // Исправлено: используем явное создание HashMap
                val tokenData = HashMap<String, Any>().apply {
                    put("token", token)
                    put("userId", user.uid)
                    put("email", user.email ?: "")
                    put("timestamp", FieldValue.serverTimestamp())
                    put("device", android.os.Build.MODEL)
                    put("platform", "android")
                }

                FirebaseFirestore.getInstance()
                    .collection("fcm_tokens")
                    .document(user.uid)
                    .set(tokenData)
                    .addOnSuccessListener {
                        Log.d("FCM", " Token saved successfully for user: ${user.uid}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", " Error saving token", e)
                    }
            } ?: run {
                Log.w("FCM", "⚠️ No authenticated user, token not saved")
            }
        } catch (e: Exception) {
            Log.e("FCM", " Error in saveTokenToFirestore", e)
        }
    }
}