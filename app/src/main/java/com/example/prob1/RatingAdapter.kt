package com.example.prob1

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.models.UserRating

class RatingAdapter(
    private val userRatings: List<UserRating>,
    private val onStudentClick: (UserRating) -> Unit,
    private val isTeacher: Boolean = false // Добавляем флаг для определения роли
) : RecyclerView.Adapter<RatingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val coinsTextView: TextView = itemView.findViewById(R.id.coinsTextView)
        val positionTextView: TextView = itemView.findViewById(R.id.positionTextView)
        val testScoresTextView: TextView = itemView.findViewById(R.id.testScoresTextView) // Добавляем TextView для баллов
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_rating, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userRating = userRatings[position]
        holder.nameTextView.text = userRating.name
        holder.coinsTextView.text = "${userRating.coins} монет"
        holder.positionTextView.text = "${position + 1}."

        // Для преподавателя показываем баллы за тесты, для студентов скрываем
        if (isTeacher) {
            holder.testScoresTextView.visibility = View.VISIBLE
            holder.testScoresTextView.text = "${userRating.totalTestScore} баллов"
        } else {
            holder.testScoresTextView.visibility = View.GONE
        }

        holder.positionTextView.setTextColor(Color.WHITE)
        holder.nameTextView.setTextColor(Color.WHITE)
        holder.coinsTextView.setTextColor(Color.WHITE)
        holder.testScoresTextView.setTextColor(Color.WHITE)

        holder.itemView.setOnClickListener {
            onStudentClick(userRating)
        }
    }

    override fun getItemCount() = userRatings.size
}