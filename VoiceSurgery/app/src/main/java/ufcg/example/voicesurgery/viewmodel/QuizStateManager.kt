package ufcg.example.voicesurgery.viewmodel

import ufcg.example.voicesurgery.data.Question
import ufcg.example.voicesurgery.data.QuestionRepository

class QuizStateManager {

    private val questions: List<Question> = QuestionRepository.getQuestions()
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

    fun saveAnswer(answer: String) {
        answers[getCurrentQuestion()] = answer
    }

    // Retorna um map de Título da Pergunta -> Resposta
    fun getFormattedAnswers(): Map<String, String> {
        return answers.mapKeys { it.key.title }
    }

    fun reset() {
        currentIndex = 0
        answers.clear()
    }
}