// DebtLectionsAdapter.kt
package com.example.prob1.ui.tests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R

data class LectionWithInfo(
    val lectionId: String,
    val title: String,
    val num: Int
)

class DebtLectionsAdapter(
    private val lections: List<LectionWithInfo>,
    private val onLectionClick: (String) -> Unit
) : RecyclerView.Adapter<DebtLectionsAdapter.DebtLectionViewHolder>() {

    inner class DebtLectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lectionTitle: TextView = itemView.findViewById(R.id.lectionTitle)
        val lectionNumber: TextView = itemView.findViewById(R.id.lectionNumber)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLectionClick(lections[position].lectionId)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtLectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt_lection, parent, false)
        return DebtLectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtLectionViewHolder, position: Int) {
        val lection = lections[position]
        holder.lectionTitle.text = lection.title
        holder.lectionNumber.text = "Лекция ${lection.num}"
    }

    override fun getItemCount(): Int = lections.size
}