package com.example.prob1

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RulesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rules)

        val mySwitch: Switch = findViewById(R.id.switch1)
        val namepage: TextView = findViewById(R.id.namepage)
        val namecard1: TextView = findViewById(R.id.namecard1)
        val namecard2: TextView = findViewById(R.id.namecard2)
        val namecard3: TextView = findViewById(R.id.namecard3)
        val namecard4: TextView = findViewById(R.id.namecard4)
        val namecard5: TextView = findViewById(R.id.namecard5)
        val textcard1: TextView = findViewById(R.id.textcard1)
        val textcard2: TextView = findViewById(R.id.textcard2)
        val textcard3: TextView = findViewById(R.id.textcard3)
        val textcard4: TextView = findViewById(R.id.textcard4)
        val textcard5: TextView = findViewById(R.id.textcard5)

        namepage.text = "Rules"
        namecard1.text = "Coin receiving system"
        namecard2.text = "Levels"
        namecard3.text = "Penalty system"
        namecard4.text = "You are run out of money?"
        namecard5.text = "Do you have debts from the previous term?"
        textcard1.text = "Doing correctly any 5 grammatical tasks in the 1-st term block you can earn 3 coins.\n" +
                "If done correctly, any task in the 2-d and the 3-d term blocks lets you earn 1 coin.\n" +
                "If done correctly, every 3 tasks in the tests checking text reading comprehension let you earn 2 coins.\n" +
                "If you finish doing the all the tasks in every block one week ahead the deadline you can earn 5 extra coins."
        textcard2.text = "Do the tasks correctly and earn coins!!! \n" +
                "Pay attention to your avatar picture! Its' changes will show your game status, which depends on the amount of coins you have earned."
        textcard3.text = "A repeated consultation costs you 2 coins.\n" +
                "A poorly done task costs you 2 coins.\n" +
                "Being one month behind the schedule you lose 10 coins.\n" +
                "Being more than a month behind the schedule causes the loss of 20 coins."
        textcard4.text = "Our bank is ready to support you with an interest free loan of 75 coins.\n" +
                "As soon as you've earned more than 120coins, your credit will be deducted from your account.\n" +
                "Dear client! Our company provides only paid consultations. So if you have got a bank loan, one consultation here costs 1 coin."
        textcard5.text = "We are sorry to tell you that the bonus program of our bank is not available for you as you have got debts from the previous term."

        mySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                namepage.text = "Правила"
                namecard1.text = "Система выдачи монет"
                namecard2.text = "Уровни"
                namecard3.text = "Система штрафов"
                namecard4.text = "У вас закончились монеты?"
                namecard5.text = "У вас есть долг с прошлого семестра?"
                textcard1.text = "Правильное выполнение любых пяти заданий в упражнениях по грамматике позволяет получить 3 монеты (I семестр). \n" +
                        "Во втором и третьем семестре за выполнение каждого задания начисляется по 1 монете.\n" +
                        "Каждые 3 правильно выполненных задания в тестах по текстам позволяют получить 2 монеты.\n" +
                        "За досрочное выполнение заданий в каждом блоке участник получает 5 монеты (если выполнено за неделю до крайнего срока)."
                textcard2.text = "Выполняйте задания успешно и получайте монеты. \n" +
                        "Следите за своей аватаркой! Она меняется в зависимости от собранных вами монет."
                textcard3.text = "Повторная консультация стоит 2 монеты.\n" +
                        "Плохо пройденное задание - потеря 2 монет.\n" +
                        "Просрочка задания на месяц - потеря 10 монет.\n" +
                        "Просрочка задания более чем на месяц — потеря 20 монет."
                textcard4.text = "Банк может предоставить вам беспроцентный кредит (75 монет). \n" +
                        "После того, как вы соберете более 120 монет, кредит будет автоматически списан со счета. \n" +
                        "*При наличии кредита стоимость консультаций - 1 монета."
                textcard5.text = "Бонусная программа нашего банка для Вас недоступна."
            } else {
                namepage.text = "Rules"
                namecard1.text = "Coin receiving system"
                namecard2.text = "Levels"
                namecard3.text = "Penalty system"
                namecard4.text = "You are run out of money?"
                namecard5.text = "Do you have debts from the previous term?"
                textcard1.text = "Doing correctly any 5 grammatical tasks in the 1-st term block you can earn 3 coins.\n" +
                        "If done correctly, any task in the 2-d and the 3-d term blocks lets you earn 1 coin.\n" +
                        "If done correctly, every 3 tasks in the tests checking text reading comprehension let you earn 2 coins.\n" +
                        "If you finish doing the all the tasks in every block one week ahead the deadline you can earn 5 extra coins."
                textcard2.text = "Do the tasks correctly and earn coins!!! \n" +
                        "Pay attention to your avatar picture! Its' changes will show your game status, which depends on the amount of coins you have earned."
                textcard3.text = "A repeated consultation costs you 2 coins.\n" +
                        "A poorly done task costs you 2 coins.\n" +
                        "Being one month behind the schedule you lose 10 coins.\n" +
                        "Being more than a month behind the schedule causes the loss of 20 coins."
                textcard4.text = "Our bank is ready to support you with an interest free loan of 75 coins.\n" +
                        "As soon as you've earned more than 120coins, your credit will be deducted from your account.\n" +
                        "Dear client! Our company provides only paid consultations. So if you have got a bank loan, one consultation here costs 1 coin."
                textcard5.text = "We are sorry to tell you that the bonus program of our bank is not available for you as you have got debts from the previous term."
            }
        }
    }

    fun exit(view: android.view.View) {
        finish()
    }
}