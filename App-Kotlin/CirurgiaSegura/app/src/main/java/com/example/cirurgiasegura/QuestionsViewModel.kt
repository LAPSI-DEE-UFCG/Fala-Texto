package com.example.cirurgiasegura

class QuestionsViewModel {

    var questions: List<String> = emptyList()
        private set

    fun setQuestions(list: List<String>) {
        questions = list
    }
}