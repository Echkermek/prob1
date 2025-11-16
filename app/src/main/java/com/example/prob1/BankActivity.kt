package com.example.prob1

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BankActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvBankStatus: TextView
    private lateinit var tvCreditStatus: TextView
    private lateinit var btnTakeLoan: Button
    private var currentCoins = 0
    private var currentCredit = 0
    private val CREDIT_THRESHOLD = 130
    private val LOAN_AMOUNT = 75

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvBankStatus = findViewById(R.id.textViewBankStatus)
        tvCreditStatus = findViewById(R.id.textViewCreditStatus)
        btnTakeLoan = findViewById(R.id.buttonTakeLoan)
        val btnExitBank: Button = findViewById(R.id.buttonExitBank)

        btnExitBank.setOnClickListener { finish() }

        loadUserData()
    }

    private fun loadUserData() {
        auth.currentUser?.let { user ->
            db.collection("user_coins").document(user.uid)
                .addSnapshotListener { document, _ ->
                    if (document != null && document.exists()) {
                        currentCoins = document.getLong("coins")?.toInt() ?: 0
                        currentCredit = document.getLong("credit")?.toInt() ?: 0

                        updateUI()
                        checkAutoPayment()
                    }
                }
        }
    }

    private fun updateUI() {
        tvBankStatus.text = "Баланс  $currentCoins coins"
        tvCreditStatus.text = "Кредит: $currentCredit coins"

        if (currentCredit > 0) {
            btnTakeLoan.isEnabled = false
            btnTakeLoan.text = "Уже есть кредит"
        } else {
            btnTakeLoan.isEnabled = true
            btnTakeLoan.text = "Взять кредит ($LOAN_AMOUNT coins)"
            btnTakeLoan.setOnClickListener { takeLoan() }
        }
    }

    private fun checkAutoPayment() {
        if (currentCredit > 0 && currentCoins > CREDIT_THRESHOLD) {
            payCreditAutomatically()
        }
    }

    private fun takeLoan() {
        auth.currentUser?.let { user ->
            val newCoins = currentCoins + LOAN_AMOUNT
            val newCredit = currentCredit + LOAN_AMOUNT

            db.collection("user_coins").document(user.uid)
                .update(mapOf(
                    "coins" to newCoins,
                    "credit" to newCredit
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, " $LOAN_AMOUNT Получены", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Не удалось взять кредит", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun payCreditAutomatically() {
        auth.currentUser?.let { user ->
            val newCoins = currentCoins - currentCredit

            db.collection("user_coins").document(user.uid)
                .update(mapOf(
                    "coins" to newCoins,
                    "credit" to 0
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, "Кредит $currentCredit ", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "не удалось получить", Toast.LENGTH_SHORT).show()
                }
        }
    }
}