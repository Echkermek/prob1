package com.example.prob1.ui.teacher

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prob1.R
import com.example.prob1.databinding.ActivityGroupBinding
import com.google.firebase.firestore.FirebaseFirestore

class GroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupBinding
    private val db = FirebaseFirestore.getInstance()
    private val groupsList = mutableListOf<String>()
    private lateinit var adapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GroupAdapter(groupsList) { groupName ->
            val intent = Intent(this, GroupRatingActivity::class.java)
            intent.putExtra("GROUP_NAME", groupName)
            startActivity(intent)
        }

        setupGroupsList()
        loadGroups()

        binding.addGroupButton.setOnClickListener {
            showAddGroupDialog()
        }
    }


    private fun setupGroupsList() {
        binding.groupsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.groupsRecyclerView.adapter = adapter
    }

    private fun loadGroups() {
        db.collection("groups")
            .get()
            .addOnSuccessListener { documents ->
                groupsList.clear()
                for (document in documents) {
                    document.getString("name")?.let { groupsList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showAddGroupDialog() {
        val input = EditText(this).apply {
            hint = "Введите название группы"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Добавить группу")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    addGroup(groupName)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.light_blue))
        }
        dialog.setOnShowListener {
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.light_blue))
        }

        dialog.show()
    }

    private fun addGroup(groupName: String) {
        val groupData = hashMapOf(
            "name" to groupName
        )

        db.collection("groups")
            .add(groupData)
            .addOnSuccessListener { documentReference ->
                groupsList.add(groupName)
                adapter.notifyItemInserted(groupsList.size - 1)
            }
    }
}