package fr.devslops.quizz

class Question constructor(libelleArg: String, explanationArg: String, answersArg: ArrayList<Reponse>, isAnsweredArg: Boolean = false) {
    val libelle: String = libelleArg
    val explanation: String = explanationArg
    val answers: ArrayList<Reponse> = answersArg
    var isAnswered: Boolean = isAnsweredArg
}