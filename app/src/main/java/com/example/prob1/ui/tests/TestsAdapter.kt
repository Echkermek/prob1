package com.example.prob1.ui.tests

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.data.Test
import com.example.prob1.databinding.ItemTestBinding

class TestsAdapter(private val onTestClick: (String, Int) -> Unit) :
    RecyclerView.Adapter<TestsAdapter.TestViewHolder>() {

    private var tests = emptyList<Test>()

    inner class TestViewHolder(val binding: ItemTestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val binding = ItemTestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        val test = tests[position]
        holder.binding.testTitle.text = test.title
        holder.binding.testNumber.text = "Тест ${test.num} (Семестр ${test.semester})"

        if (!test.isAvailable) {
            holder.binding.testTitle.setTextColor(Color.GRAY)
            holder.binding.testNumber.setTextColor(Color.GRAY)
        } else {
            holder.binding.testTitle.setTextColor(Color.BLACK)
            holder.binding.testNumber.setTextColor(Color.BLACK)
        }

        holder.itemView.setOnClickListener {
            onTestClick(test.id, test.semester)
        }
    }

    override fun getItemCount() = tests.size

    fun submitList(newList: List<Test>) {
        tests = newList.sortedBy { it.num }
        notifyDataSetChanged()
    }
}