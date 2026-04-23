package com.example.prob1

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.R

class LevelsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        val coins = intent.getIntExtra("coins", 0)

        val level1 = findViewById<ImageView>(R.id.level1)
        val level2 = findViewById<ImageView>(R.id.level2)
        val level3 = findViewById<ImageView>(R.id.level3)
        val level4 = findViewById<ImageView>(R.id.level4)
        val level5 = findViewById<ImageView>(R.id.level5)
        val level6 = findViewById<ImageView>(R.id.level6)
        val level7 = findViewById<ImageView>(R.id.level7)
        val level8 = findViewById<ImageView>(R.id.level8)

        // Настройка обработчиков для уровней
        level2.setOnClickListener { showToast("Сollect 130 coins") }
        level3.setOnClickListener { showToast("Сollect 170 coins") }
        level4.setOnClickListener { showToast("Сollect 240 coins") }
        level5.setOnClickListener { showToast("Сollect 285 coins") }
        level6.setOnClickListener { showToast("Сollect 325 coins") }
        level7.setOnClickListener { showToast("Сollect 400 coins") }
        level8.setOnClickListener { showToast("Сollect 460 coins") }

        // Блокировка уровней в зависимости от количества монет
        when {
            coins < 130 -> {
                lockLevels(level2, level3, level4, level5, level6, level7, level8)
            }
            coins < 170 -> {
                lockLevels(level3, level4, level5, level6, level7, level8)
            }
            coins < 240 -> {
                lockLevels(level4, level5, level6, level7, level8)
            }
            coins < 285 -> {
                lockLevels(level5, level6, level7, level8)
            }
            coins < 325 -> {
                lockLevels(level6, level7, level8)
            }
            coins < 400 -> {
                lockLevels(level7, level8)
            }
            coins < 460 -> {
                lockLevels(level8)
            }
        }
    }

    // Метод для кнопки назад (вызывается из XML)
    fun exit(view: View) {
        finish() // Закрывает текущую активность
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun lockLevels(vararg levels: ImageView) {
        levels.forEach { level ->
            level.setImageResource(R.mipmap.lock)
        }
    }
}