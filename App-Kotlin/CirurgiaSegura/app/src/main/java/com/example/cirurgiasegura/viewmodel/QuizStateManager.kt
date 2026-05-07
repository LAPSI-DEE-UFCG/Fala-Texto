package com.example.cirurgiasegura.viewmodel

import com.example.cirurgiasegura.data.Question
import com.example.cirurgiasegura.data.QuestionRepository
import com.example.cirurgiasegura.data.ResponseItem

//Edições Gepeto
class QuizStateManager(
    private val questions: List<Question> = QuestionRepository.getQuestions()
) {


    private var currentIndex = 0
    private val answers = mutableMapOf<Question, String>()

    fun getCurrentQuestion(): Question = questions[currentIndex]

    fun getTotalQuestions(): Int = questions.size

    fun getCurrentIndex(): Int = currentIndex

    fun isQuizFinished(): Boolean = currentIndex >= questions.size - 1

    fun moveToNextQuestion() {
        if (!isQuizFinished()) {
            currentIndex++
        }
    }

    fun moveToPreviousQuestion() {
        if(currentIndex > 0)   currentIndex--
    }

    fun saveAnswer(answer: List<ResponseItem>) {
        answers[getCurrentQuestion()] = answer.toString()
    }

    // Retorna um map de Título da Pergunta -> Resposta
    fun getFormattedAnswers(): Map<String, String> {
        return answers.mapKeys { it.key.title }
    }

    // Função para recuperar uma resposta específica
    fun getSavedAnswer(question: Question): String? {
        return answers[question]
    }

    fun reset() {
        currentIndex = 0
        answers.clear()
    }
}