package com.example.prob1.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
data class EventNotification(
    val id: String,
    val title: String,
    val description: String,
    val date: String?,
    val isDeadline: Boolean,
    val timestamp: Long,
    val courseId: String? = null,
    val courseName: String? = null
) {
    override fun equals(other: Any?): Boolean = other is EventNotification && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

data class MessageNotification(
    var id: String = "",
    var text: String = "",
    var recipientId: String = "",
    var recipientName: String = "",
    var senderId: String = "",
    var isGroupMessage: Boolean = false,
    var timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean = other is MessageNotification && id == other.id
    override fun hashCode(): Int = id.hashCode()
}
