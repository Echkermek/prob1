package com.example.prob1.ui.lections

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.LectionWebViewActivity
import com.example.prob1.R
import com.example.prob1.databinding.FragmentLectionsBinding
import com.example.prob1.databinding.ItemLectionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LectionsFragment : Fragment() {

    private var _binding: FragmentLectionsBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var adapter: LectionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (userId == null) {
            Toast.makeText(requireContext(), "Не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        adapter = LectionsAdapter(
            onClick = { lection -> checkAndOpenLection(lection) },
            getReadStatus = { lectionId -> isLectureRead(lectionId) }
        )

        binding.lectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LectionsFragment.adapter
        }

        loadLections()
    }

    private fun checkAndOpenLection(lection: Lection) {
        viewLifecycleOwner.lifecycleScope.launch {
            val readCount = getReadCount(lection.id)
            val needPay = readCount > 0
            if (needPay) {
                showPayDialog { confirmed ->
                    if (confirmed) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            if (checkCoins()) {
                                deductCoin()
                                markAsRead(lection.id)
                                openLection(lection)
                            } else {
                                Toast.makeText(requireContext(), "Недостаточно монет", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                markAsRead(lection.id)
                openLection(lection)
            }
        }
    }

    private fun markAsRead(lectionId: String) {
        if (userId == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val snap = db.collection("user_lections")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("lectionId", lectionId)
                    .get().await()

                if (snap.isEmpty) {
                    // Первое прочтение
                    db.collection("user_lections").add(
                        hashMapOf(
                            "userId" to userId,
                            "lectionId" to lectionId,
                            "readCount" to 1,
                            "lastReadTimestamp" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    // Повторное прочтение - увеличиваем счетчик
                    val document = snap.documents[0]
                    document.reference.update(
                        "readCount", FieldValue.increment(1L),
                        "lastReadTimestamp", FieldValue.serverTimestamp()
                    ).await()
                }
                Toast.makeText(requireContext(), "Лекция засчитана", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка засчитывания лекции: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPayDialog(onConfirm: (Boolean) -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Повторное чтение")
            .setMessage("Списать 1 монету за повторное чтение лекции?")
            .setPositiveButton("Да") { _, _ -> onConfirm(true) }
            .setNegativeButton("Нет") { _, _ -> onConfirm(false) }
            .show()
    }

    private fun openLection(lection: Lection) {
        val intent = Intent(requireContext(), LectionWebViewActivity::class.java).apply {
            putExtra("url", lection.url)
            putExtra("lectionId", lection.id)
        }
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            adapter.notifyDataSetChanged()
        }
    }

    private suspend fun getReadCount(lectionId: String): Int {
        val snap = db.collection("user_lections")
            .whereEqualTo("userId", userId)
            .whereEqualTo("lectionId", lectionId)
            .get().await()
        return if (snap.isEmpty) 0 else snap.documents[0].getLong("readCount")?.toInt() ?: 0
    }

    private suspend fun isLectureRead(lectionId: String): Boolean {
        return getReadCount(lectionId) > 0
    }

    private suspend fun checkCoins(): Boolean {
        val snap = db.collection("user_coins")
            .whereEqualTo("userId", userId)
            .get().await()
        return !snap.isEmpty && (snap.documents[0].getLong("coins") ?: 0) >= 1
    }

    private fun deductCoin() {
        db.collection("user_coins")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    snap.documents[0].reference.update("coins", FieldValue.increment(-1L))
                }
            }
    }

    private fun loadLections() {
        db.collection("lections")
            .get()
            .addOnSuccessListener { result ->
                val lections = result.documents.mapNotNull { doc ->
                    Lection(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        num = doc.getString("num") ?: "0",
                        url = doc.getString("url") ?: return@mapNotNull null
                    )
                }.sortedBy { it.num.toFloatOrNull() ?: 0f }

                adapter.submitList(lections)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки лекций", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE = 101
    }
}

data class Lection(
    val id: String,
    val name: String,
    val num: String,
    val url: String
)

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