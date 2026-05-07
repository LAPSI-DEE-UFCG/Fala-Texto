package com.example.cirurgiasegura.ui

import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.cirurgiasegura.R
import com.example.cirurgiasegura.data.*
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.compareTo
import kotlin.text.get

class AnswerExtractor {

    fun extractAnswer(question: Question, view: View): List<ResponseItem> {
        return when (question) {
            is MultipleChoiceQuestion -> {
                val group = view.findViewById<RadioGroup>(R.id.options_group)
                val selectedId = group?.checkedRadioButtonId ?: -1

                val answer = if (selectedId != -1) {
                    val selectedRadioButton = view.findViewById<RadioButton>(selectedId)
                    selectedRadioButton?.text?.toString() ?: ""
                } else {
                    ""
                }

                // Retorna a resposta única como List<ResponseItem>
                listOf(ResponseItem(question.title, answer))
            }
            is TextInputQuestion -> {
                val editText = view.findViewById<EditText>(R.id.input_answer)
                val answer = editText?.text?.toString() ?: ""
                // Retorna uma lista com um único item
                listOf(ResponseItem(question.title, answer))
            }
            is CheckboxQuestion -> {
                val container = view.findViewById<LinearLayout>(R.id.checkbox_container)
                val selectedOptions = mutableListOf<String>()

                for (i in 0 until (container?.childCount ?: 0)) {
                    val child = container!!.getChildAt(i)
                    if (child is CheckBox && child.isChecked) {
                        selectedOptions.add(child.text.toString())
                    }
                }
                val answer = selectedOptions.joinToString("; ")

                // Retorna a resposta única (seleções combinadas) como List<ResponseItem>
                listOf(ResponseItem(question.title, answer))
            }

            is CheckboxQuestion2 -> {
                val responses = mutableListOf<ResponseItem>()

                // 1. EXTRAÇÃO DA RESPOSTA PRINCIPAL (CHECKBOXES)
                val container = view.findViewById<LinearLayout>(R.id.checkbox_container)
                val selectedOptions = mutableListOf<String>()

                // Só processa se o container existir
                container?.let {
                    for (i in 0 until container.childCount) {
                        val child = container.getChildAt(i)
                        if (child is CheckBox && child.isChecked) {
                            selectedOptions.add(child.text.toString())
                        }
                    }
                }
                // Adiciona a resposta dos Checkboxes como o primeiro item da lista
                //responses.add(ResponseItem(question.title, selectedOptions.joinToString("; ")))
                if (question.options.isNotEmpty() && selectedOptions.isNotEmpty()) {
                    responses.add(ResponseItem(question.title, selectedOptions.joinToString("; ")))
                }

                // 2. EXTRAÇÃO DAS RESPOSTAS DE SUBPERGUNTAS (TEXT INPUTS)
                val subQuestionsList = question.subQuestions

                // IDs fixos das caixas de texto no layout
                val inputIds = listOf(R.id.input_answer1, R.id.input_answer2, R.id.input_answer3)
                //val inputIds = listOf(R.id.input_answer1, R.id.input_answer2)

                for (i in subQuestionsList.indices) {
                    if (i < inputIds.size) {
                        // Garante que a subpergunta tenha um ID fixo correspondente (<= 2) editado
                        val subQuestionTitle = subQuestionsList[i].title
                        val subAnswer = extractText(view, inputIds[i])

                        // Adiciona apenas se houver uma resposta de texto
                        if (subAnswer.isNotEmpty()) {
                            responses.add(ResponseItem(subQuestionTitle, subAnswer))
                        }
                    }
                }

                // Retorna a lista completa de itens de resposta
                responses
            }
            is MultipleChoiceQuestion2 -> {
                val responses = mutableListOf<ResponseItem>()

                val group = view.findViewById<RadioGroup>(R.id.options_group)
                val selectedId = group?.checkedRadioButtonId ?: -1

                val answer = if (selectedId != -1) {
                    val selectedRadioButton = view.findViewById<RadioButton>(selectedId)
                    selectedRadioButton?.text?.toString() ?: ""
                } else {
                    ""
                }

                // Retorna a resposta única como List<ResponseItem>
                //listOf(ResponseItem(question.title, answer))
                responses.add(ResponseItem(question.title, answer))

                // 2. EXTRAÇÃO DAS RESPOSTAS DE SUBPERGUNTAS (TEXT INPUTS)
                val subQuestionsList = question.subQuestions

                // IDs fixos das caixas de texto no layout
                val inputIds = listOf(R.id.input_answer1, R.id.input_answer2, R.id.input_answer3)
                //val inputIds = listOf(R.id.input_answer1, R.id.input_answer2)

                for (i in subQuestionsList.indices) {
                    if (i < inputIds.size) {
                        // Garante que a subpergunta tenha um ID fixo correspondente (<= 2) editado
                        val subQuestionTitle = subQuestionsList[i].title
                        val subAnswer = extractText(view, inputIds[i])

                        // Adiciona apenas se houver uma resposta de texto
                        if (subAnswer.isNotEmpty()) {
                            responses.add(ResponseItem(subQuestionTitle, subAnswer))
                        }
                    }
                }

                // Retorna a lista completa de itens de resposta
                responses

            }
            is SalvaTempo -> {
                val input = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
                //input
                listOf(ResponseItem(question.title, input))
            }
            //else -> emptyList() // Retorna uma lista vazia se for um tipo de pergunta desconhecido
        }
    }
    // Adicione esta função auxiliar na sua MainActivity
    private fun extractText(view: View, id: Int): String {
        return view.findViewById<EditText>(id)?.text?.toString() ?: ""
    }
    /*
                is SalvaTempo -> {
                //val input = view.findViewById<EditText>(R.id.input_answer)
                val input = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
                input
            }
     */
}