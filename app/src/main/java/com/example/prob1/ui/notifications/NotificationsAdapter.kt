package com.example.prob1.ui.notifications

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import com.example.prob1.data.MessageNotification
import com.example.prob1.data.EventNotification
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val onItemClick: (Any) -> Unit = {}
) : ListAdapter<Any, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_EVENT = 1
        private const val VIEW_TYPE_MESSAGE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EventNotification -> VIEW_TYPE_EVENT
            is MessageNotification -> VIEW_TYPE_MESSAGE
            else -> throw IllegalArgumentException("Invalid notification type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_EVENT -> EventViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_event_notification, parent, false),
                onItemClick
            )
            VIEW_TYPE_MESSAGE -> MessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_notification, parent, false),
                onItemClick
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder -> holder.bind(getItem(position) as EventNotification)
            is MessageViewHolder -> holder.bind(getItem(position) as MessageNotification)
        }
    }

    class EventViewHolder(
        itemView: View,
        private val onItemClick: (Any) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.notificationTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.notificationDescription)
        private val dateTextView: TextView = itemView.findViewById(R.id.notificationDate)
        private val typeTextView: TextView = itemView.findViewById(R.id.notificationType)

        fun bind(notification: EventNotification) {
            itemView.setOnClickListener { onItemClick(notification) }

            titleTextView.text = if (notification.isDeadline) {
                "Дедлайн: ${notification.title}"
            } else {
                "Событие: ${notification.title}"
            }

            descriptionTextView.text = notification.description
            typeTextView.text = if (notification.isDeadline) "Срок сдачи" else "Календарное событие"

            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateTextView.text = sdf.format(Date(notification.timestamp))

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = notification.timestamp

            val today = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

            var extraText = ""
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                extraText = " (Сегодня)"
            } else if (calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
                extraText = " (Завтра)"
            }

            titleTextView.text = if (notification.isDeadline) {
                "Дедлайн: ${notification.title}$extraText"
            } else {
                "Событие: ${notification.title}$extraText"
            }
        }
    }

    class MessageViewHolder(
        itemView: View,
        private val onItemClick: (Any) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.notificationTitle)
        private val messageTextView: TextView = itemView.findViewById(R.id.notificationMessage)
        private val dateTextView: TextView = itemView.findViewById(R.id.notificationDate)
        private val sourceTextView: TextView = itemView.findViewById(R.id.notificationSource)

        fun bind(notification: MessageNotification) {
            itemView.setOnClickListener { onItemClick(notification) }

            titleTextView.text = if (notification.isGroupMessage) {
                "Групповое сообщение"
            } else {
                "Личное сообщение"
            }

            messageTextView.text = notification.text
            sourceTextView.text = if (notification.isGroupMessage) "Для вашей группы" else "От преподавателя"

            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            dateTextView.text = sdf.format(Date(notification.timestamp))
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is EventNotification && newItem is EventNotification -> oldItem.id == newItem.id
            oldItem is MessageNotification && newItem is MessageNotification -> oldItem.id == newItem.id
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }
}