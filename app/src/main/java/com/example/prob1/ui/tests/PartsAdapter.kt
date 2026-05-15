package com.example.prob1.ui.tests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prob1.R
import com.example.prob1.data.Part
import com.example.prob1.databinding.ItemPartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PartsAdapter(
    private val onPartClick: (String, Boolean, Int, Boolean) -> Unit,
    private val isDebtTest: Boolean = false  // ДОБАВЛЕНО: флаг для тестов-долгов
) : RecyclerView.Adapter<PartsAdapter.PartViewHolder>() {

    private var parts = emptyList<Part>()
    private val db = Firebase.firestore
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    inner class PartViewHolder(val binding: ItemPartBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val part = parts[adapterPosition]

                    // ИСПРАВЛЕНАЯ ПРОВЕРКА с учётом isDebtTest
                    val hasNoLecture = part.idLectures.isNullOrEmpty() ||
                            part.idLectures == "not" ||
                            part.idLectures == "-"

                    // Если это тест-долг - пропускаем проверку лекции
                    if (hasNoLecture || isDebtTest) {
                        // Сразу запускаем без проверки попыток и лекций
                        onPartClick(part.id, false, 0, part.enterAnswer)
                    } else {
                        // Есть реальная лекция — проверяем статус
                        checkTestStatus(part.id, part.enterAnswer) { isPassed, attempts ->
                            onPartClick(part.id, isPassed, attempts, part.enterAnswer)
                        }
                    }
                }
            }
        }
    }

    private fun checkTestStatus(partId: String, isManual: Boolean, callback: (Boolean, Int) -> Unit) {
        if (userId == null) {
            callback(false, 0)
            return
        }

        db.collection("test_attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("partId", partId)
            .get()
            .addOnSuccessListener { snapshot ->
                var isPassed = false
                val attempts = snapshot.documents.size
                snapshot.documents.forEach { doc ->
                    if (doc.getBoolean("isPassed") == true) isPassed = true
                }
                callback(isPassed, attempts)
            }
            .addOnFailureListener { callback(false, 0) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val binding = ItemPartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PartViewHolder(binding)
    }

    override fun onViewRecycled(holder: PartViewHolder) {
        super.onViewRecycled(holder)
        (holder.itemView.getTag(R.id.manual_answers_listener) as? ListenerRegistration)?.remove()
        (holder.itemView.getTag(R.id.test_attempts_listener) as? ListenerRegistration)?.remove()
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        val part = parts[position]
        holder.binding.partTitle.text = part.title
        holder.binding.partNumber.text = "Часть ${part.num}"

        val hasNoLecture = part.idLectures.isNullOrEmpty() ||
                part.idLectures == "not" ||
                part.idLectures == "-"

        if (userId == null) {
            holder.binding.partStatus.text = "Не авторизован"
            return
        }

        // === ИСПРАВЛЕННАЯ ЛОГИКА ===
        if (part.enterAnswer) {
            // Ручной тест (enterAnswer = true)
            db.collection("test_grades")
                .whereEqualTo("userId", userId)
                .whereEqualTo("partId", part.id)
                .whereEqualTo("isManual", true)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        holder.binding.partStatus.text = "Не пройден"
                    } else {
                        val bestScore = snapshot.documents[0].getDouble("bestScore") ?: 0.0
                        holder.binding.partStatus.text = "Оценка: ${"%.1f".format(bestScore)}"
                    }
                }
                .addOnFailureListener {
                    holder.binding.partStatus.text = "Ошибка"
                }

        } else {
            // Обычный тест (включая тесты с lecId = "not")
            val listener = db.collection("test_grades")
                .whereEqualTo("userId", userId)
                .whereEqualTo("partId", part.id)
                .addSnapshotListener { gradeSnapshot, error ->
                    if (error != null) {
                        holder.binding.partStatus.text = "Ошибка"
                        return@addSnapshotListener
                    }

                    if (gradeSnapshot?.isEmpty == true) {
                        holder.binding.partStatus.text = if (hasNoLecture) "Начать тест" else "Не пройдено"
                        return@addSnapshotListener
                    }

                    val bestScore = gradeSnapshot!!.documents[0].getDouble("bestScore") ?: 0.0
                    val isPassed = bestScore > 0

                    // Показываем попытки
                    db.collection("test_attempts")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("partId", part.id)
                        .get()
                        .addOnSuccessListener { attemptsSnapshot ->
                            val attempts = attemptsSnapshot.size()

                            holder.binding.partStatus.text = when {
                                isPassed -> "Пройдено (${"%.1f".format(bestScore)} баллов)"
                                attempts > 0 -> "Не пройдено"
                                else -> if (hasNoLecture) "Начать тест" else "Не пройдено"
                            }
                        }
                        .addOnFailureListener {
                            holder.binding.partStatus.text = "Ошибка"
                        }
                }

            holder.itemView.setTag(R.id.test_attempts_listener, listener)
        }
    }

    override fun getItemCount() = parts.size

    fun submitList(newList: List<Part>) {
        parts = newList.sortedBy { it.num }
        notifyDataSetChanged()
    }
}