package com.example.quizzy

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import fr.devslops.quizz.Quizz
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var quizz: Quizz
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Load last score
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        score_tw.text = "Dernier score: ${sharedPreferences.getString("score", "")}"

        init()
    }

    private fun init() {
        //Set default values to difficuty spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.difficulty_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            difficulty_spn!!.adapter = adapter
        }
        //Set default values to questions number spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.nbr_questions_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            nbr_questions_spn!!.adapter = adapter
        }

        //Start game button
        go_btn.setOnClickListener {
            quizz = Quizz(
                difficulty_spn?.selectedItem.toString(),
                nbr_questions_spn?.selectedItem.toString().toInt(),
                this
            )
            quizz.startQuizz()
        }
    }

    fun makeAnswer(view: View) = quizz.answerQuestion(findViewById<Button>(view.id).text.toString())

}