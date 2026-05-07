package com.example.cirurgiasegura.data

sealed class Question(val title: String)

class MultipleChoiceQuestion(title: String, val options: List<String>) : Question(title)
class TextInputQuestion(title: String) : Question(title)
class CheckboxQuestion(title: String, val options: List<String>) : Question(title)
class SalvaTempo(title: String) : Question(title)


// --- NOVA CLASSE (o "nó composto") ---
// Ela é uma Question, mas tem uma lista de 'subQuestions' dentro

class CheckboxQuestion2(
    title: String,
    val options: List<String>,
    val subQuestions: List<Question>
) : Question(title)

class MultipleChoiceQuestion2(
    title: String,
    val options: List<String>,
    val subQuestions: List<Question>
) : Question(title)

data class ResponseItem(val title: String, val answer: String)
