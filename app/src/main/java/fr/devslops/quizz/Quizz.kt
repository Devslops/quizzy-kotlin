package fr.devslops.quizz

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import com.android.volley.Request
import com.android.volley.toolbox.*
import com.example.quizzy.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class Quizz(difficultySetting: String, nbrQuestionsSetting: Int, mainActivity: MainActivity) {
    private val token: String = "TOKEN_HERE";
    private var ctx: MainActivity = mainActivity

    private val difficulty: String = difficultySetting
    private val nbrQuestions: Int = nbrQuestionsSetting

    var questions: ArrayList<Question> = ArrayList() //questions from the current game
    var currentQuestion: Question ?= null //Question displayed in user screen
    var score = 0

    fun startQuizz() {
        GlobalScope.launch(Dispatchers.IO) {
            val responseApi = async { getQuestionApi() }
            val questionsApi: ArrayList<Question> = parseJsonToQuestions(JSONArray(responseApi.await()))
            questions.addAll(questionsApi)
            nextQuestion()
        }
    }

    //answer a question: display the correction and increase the score
    fun answerQuestion(answer: String) {
        toggleButtonActivation()
         //Move the portion of the background task that updates the UI onto the main thread
            GlobalScope.launch(Dispatchers.IO) {
                val clickedAnswer = currentQuestion!!.answers.find { it.libelle ==  answer} //Get clicked answer object
                if(clickedAnswer!!.isCorrect)
                    score++

                val goodAnswer: String = currentQuestion!!.answers.find { it.isCorrect }!!.libelle
                ctx.runOnUiThread {ctx.question_tw.text = "Bonne réponse : $goodAnswer"}

                ctx.explanation_tw.text = currentQuestion?.explanation
                delay(5000)
                ctx.runOnUiThread {ctx.explanation_tw.text = ""}
                nextQuestion()
                toggleButtonActivation()
            }
    }

    //Lock / unlock button click
    private fun toggleButtonActivation() {
        ctx.reply1_btn.isClickable = !ctx.reply1_btn.isClickable
        ctx.reply2_btn.isClickable = !ctx.reply2_btn.isClickable
        ctx.reply3_btn.isClickable = !ctx.reply3_btn.isClickable
        ctx.reply4_btn.isClickable = !ctx.reply4_btn.isClickable
        ctx.reply5_btn.isClickable = !ctx.reply5_btn.isClickable
        ctx.reply6_btn.isClickable = !ctx.reply6_btn.isClickable
    }

    //Generate next quizz question
    private fun nextQuestion() {
        currentQuestion = questions.find { !it.isAnswered } //Retrieve a question not yet answered

        if(currentQuestion != null) {
            val answers: ArrayList<Reponse> = currentQuestion!!.answers; //Retrieve answers of question

            ctx.question_tw.text = currentQuestion!!.libelle

            when(answers.size) {
                1 -> generateAnswerButton(answers, View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE)
                2 -> generateAnswerButton(answers, View.VISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE)
                3 -> generateAnswerButton(answers, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE)
                4 -> generateAnswerButton(answers, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE)
                5 -> generateAnswerButton(answers, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.INVISIBLE)
                6 -> generateAnswerButton(answers, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE)
                else -> println("Aucune question !")
            }

            questions.find {it.libelle == currentQuestion!!.libelle}!!.isAnswered = true
        } else {
            generateAnswerButton(null, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE)

            ctx.runOnUiThread {
                ctx.score_tw.text = "Dernier score: $score/$nbrQuestions"
                ctx.question_tw.text =""
            }

            //Persisting data
            val sharedPreferences: SharedPreferences = ctx.getPreferences(Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("score", "$score/$nbrQuestions")
            editor.apply()

        }

    }

    //Make answer in each button
    private fun generateAnswerButton(answers: ArrayList<Reponse>?, visibility1: Int, visibility2: Int, visibility3: Int, visibility4: Int, visibility5: Int, visibility6: Int): Unit {
        ctx.runOnUiThread { //Move the portion of the background task that updates the UI onto the main thread
            ctx.reply1_btn.visibility = visibility1 //Display first answer button
            if (visibility1 == View.VISIBLE) ctx.reply1_btn.text = answers!![0].libelle //Set first answer to the first button
            ctx.reply2_btn.visibility = visibility2
            if (visibility2 == View.VISIBLE) ctx.reply2_btn.text = answers!![1].libelle
            ctx.reply3_btn.visibility = visibility3
            if (visibility3 == View.VISIBLE) ctx.reply3_btn.text = answers!![2].libelle
            ctx.reply4_btn.visibility = visibility4
            if (visibility4 == View.VISIBLE) ctx.reply4_btn.text = answers!![3].libelle
            ctx.reply5_btn.visibility = visibility5
            if (visibility5 == View.VISIBLE) ctx.reply5_btn.text = answers!![4].libelle
            ctx.reply6_btn.visibility = visibility6
            if (visibility6 == View.VISIBLE) ctx.reply6_btn.text = answers!![5].libelle
        }
    }


    //Get questions filter by settings filter
    private fun getQuestionApi(): String {
        val queue = Volley.newRequestQueue(ctx)
        val url = "https://quizapi.io/api/v1/questions?apiKey=$token&difficulty=$difficulty&limit=$nbrQuestions"

        val future: RequestFuture<String> = RequestFuture.newFuture();
        val request = StringRequest(Request.Method.GET, url, future, future)
        queue.add(request);

        var result: String = future.get();
        return result
    }

    private fun parseJsonToQuestions(response: JSONArray): ArrayList<Question> {
        val newQuestions: ArrayList<Question> = ArrayList()
        for (i in 0 until response.length()) {
            val q: JSONObject = response.getJSONObject(i)
            val qLibelle: String = q.getString("question")
            val answersObject: JSONObject = q.getJSONObject("answers")
            val correctAnswers: JSONObject = q.getJSONObject("correct_answers")
            val qExplanation: String = if(q.getString("explanation")=="null") "" else q.getString("explanation")

            val anwsersIterator: Iterator<String> = answersObject.keys()
            var index = 0;
            val responses: ArrayList<Reponse> = ArrayList()

            //Possible answers
            for (answerLibelleKey in anwsersIterator) {
                var answerLibelle: String = answersObject.getString(answerLibelleKey)
                if (answerLibelle != "null") {
                    var isCorrect: String = correctAnswers.getString(answerLibelleKey + "_correct")
                    val answer = Reponse(
                        answersObject.getString(answerLibelleKey),
                        isCorrect.toBoolean()
                    )
                    responses.add(answer)
                    index++
                }
            }

            val question = Question(qLibelle, qExplanation, responses)
            newQuestions.add(question)
        }
        return newQuestions
    }
}