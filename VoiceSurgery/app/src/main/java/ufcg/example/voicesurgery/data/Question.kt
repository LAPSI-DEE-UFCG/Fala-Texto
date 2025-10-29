package ufcg.example.voicesurgery.data

sealed class Question(val title: String)

class MultipleChoiceQuestion(title: String, val options: List<String>) : Question(title)
class TextInputQuestion(title: String) : Question(title)
class CheckboxQuestion(title: String, val options: List<String>) : Question(title)
class SalvaTempo(title: String) : Question(title)
