// com/example/prob1/ui/lections/LectionsAdapter.kt
package com.example.prob1.ui.lections

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.data.Lection
import com.example.prob1.databinding.ItemLectionBinding
import kotlinx.coroutines.launch

class LectionsAdapter(
    private val onClick: (Lection) -> Unit,
    private val getReadStatus: suspend (String) -> Boolean
) : RecyclerView.Adapter<LectionsAdapter.ViewHolder>() {

    private var lections = emptyList<Lection>()

    inner class ViewHolder(val binding: ItemLectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lection = lections[position]
        with(holder.binding) {
            lectionName.text = lection.name
            lectionNum.text = "Лекция ${lection.num}"
            root.setOnClickListener { onClick(lection) }

            (holder.itemView.context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                val isRead = getReadStatus(lection.id)
                lectionStatus.text = if (isRead) "Прочитано" else ""
            }
        }
    }

    override fun getItemCount() = lections.size

    fun submitList(newList: List<Lection>) {
        lections = newList
        notifyDataSetChanged()
    }
}