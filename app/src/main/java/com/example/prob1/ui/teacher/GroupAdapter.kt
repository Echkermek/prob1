package com.example.prob1.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R

class GroupAdapter(
    private val groups: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(android.R.id.text1)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.groupName.text = groups[position]
        holder.itemView.setOnClickListener {
            onItemClick(groups[position])
        }
        holder.groupName.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )
    }

    override fun getItemCount() = groups.size
}