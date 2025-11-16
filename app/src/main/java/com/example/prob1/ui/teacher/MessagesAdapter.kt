package com.example.prob1.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import java.text.SimpleDateFormat
import java.util.Locale

class MessagesAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecipient: TextView = itemView.findViewById(R.id.tvRecipient)
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvRecipient.text = "Кому: ${message.recipientName}"
        holder.tvMessage.text = message.text
        holder.tvTimestamp.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(message.timestamp)
    }

    override fun getItemCount() = messages.size
}