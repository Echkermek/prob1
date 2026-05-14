package com.example.prob1

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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
        val backButton: ImageView = findViewById(R.id.backButton)


        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        btnExitBank.setOnClickListener {
            finish()
        }

        loadUserData()
    }

    private fun loadUserData() {
        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .addSnapshotListener { document, _ ->
                    if (document != null && document.exists()) {

                        val oldCoins = currentCoins
                        val oldCredit = currentCredit

                        currentCoins = document.getLong("coins")?.toInt() ?: 0
                        currentCredit = document.getLong("credit")?.toInt() ?: 0

                        updateUI()

                        if (oldCredit > 0 && currentCredit == 0 && oldCoins > currentCoins) {
                            val paidAmount = oldCredit
                            Toast.makeText(
                                this,
                                "Кредит $paidAmount монет списан с баланса",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        checkAutoPayment()
                    }
                }
        }
    }

    private fun updateUI() {
        tvBankStatus.text = "Баланс: $currentCoins coins"
        tvCreditStatus.text = "Кредит: $currentCredit coins"

        if (currentCredit > 0) {
            btnTakeLoan.isEnabled = false
            btnTakeLoan.text = "Уже есть кредит"
        } else {
            btnTakeLoan.isEnabled = true
            btnTakeLoan.text = "Взять кредит ($LOAN_AMOUNT coins)"
            btnTakeLoan.setOnClickListener {
                showLoanConfirmationDialog()
            }
        }
    }

    private fun showLoanConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Кредит")
            .setMessage("""
                Взять кредит в размере $LOAN_AMOUNT монет.
                
                Кредит будет автоматически списан при достижении $CREDIT_THRESHOLD монет.
            """.trimIndent())
            .setPositiveButton("Взять кредит") { _, _ ->
                takeLoan()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun checkAutoPayment() {
        if (currentCredit > 0 && currentCoins >= CREDIT_THRESHOLD) {
            payCreditAutomatically()
        }
    }

    private fun takeLoan() {
        auth.currentUser?.let { user ->

            val newCoins = currentCoins + LOAN_AMOUNT
            val newCredit = currentCredit + LOAN_AMOUNT

            db.collection("users").document(user.uid)
                .update(
                    mapOf(
                        "coins" to newCoins,
                        "credit" to newCredit
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Кредит $LOAN_AMOUNT монет получен", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Не удалось взять кредит", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun payCreditAutomatically() {
        auth.currentUser?.let { user ->

            val amountToPay = currentCredit
            val newCoins = currentCoins - amountToPay

            db.collection("users").document(user.uid)
                .update(
                    mapOf(
                        "coins" to newCoins,
                        "credit" to 0
                    )
                )
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка списания кредита", Toast.LENGTH_SHORT).show()
                }
        }
    }
}