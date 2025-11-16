package com.example.prob1.ui.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R

class CoursesAdapter(private var courses: List<Course>) :
    RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.courseNameTextView)
        val groupName: TextView = itemView.findViewById(R.id.groupTextView)
        val semester: TextView = itemView.findViewById(R.id.semesterTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.courseName.text = "Название: ${course.name}"
        holder.groupName.text = "Группа: ${course.groupName}"
        holder.semester.text = "Семестр: ${course.semester}"
        holder.courseName.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )
        holder.groupName.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )
        holder.semester.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )
    }

    override fun getItemCount() = courses.size

    data class Course(
        val name: String,
        val groupName: String,
        val semester: Int
    )

    fun updateData(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }
}