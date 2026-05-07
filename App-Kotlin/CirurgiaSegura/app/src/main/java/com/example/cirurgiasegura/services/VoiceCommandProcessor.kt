package com.example.cirurgiasegura.services

import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.cirurgiasegura.R//Aqui pode conflitar algo?
import com.example.cirurgiasegura.data.CheckboxQuestion
import com.example.cirurgiasegura.data.CheckboxQuestion2
import com.example.cirurgiasegura.data.MultipleChoiceQuestion
import com.example.cirurgiasegura.data.MultipleChoiceQuestion2
import com.example.cirurgiasegura.data.Question
import com.example.cirurgiasegura.data.SalvaTempo
import com.example.cirurgiasegura.data.TextInputQuestion

class VoiceCommandProcessor {

    /**
     * Processa um comando de voz e atualiza a view.
     * @return True se o comando "próxima pergunta" foi detectado, False caso contrário.
     */
    fun processCommand(command: String, question: Question, view: View): Boolean {
        if (command.contains("próxima pergunta", ignoreCase = true)) {
            return true
        }

        // Começando a edição
        when (question) {
            is TextInputQuestion -> {
                val input = view.findViewById<EditText>(R.id.input_answer)
                val input1 = view.findViewById<EditText>(R.id.input_answer1)

                when {
                    question.title.startsWith("Nome", true) && command.contains("nome", true) ->
                        input?.setText(command.substringAfter("nome ").trim())

                    question.title.startsWith(
                        "Data de Nascimento",
                        true
                    ) && command.contains("nascimento", true) ->
                        input?.setText(command.substringAfter("nascimento ").trim())

                    question.title.startsWith("Prontuário", true) ->
                        input?.setText(command.substringAfter("prontuário ").trim())

                    question.title.startsWith("Sala", true) ->
                        input?.setText(command.substringAfter("sala ").trim())

                    //question.title.startsWith("Verificação da segurança", true) ->
                    question.title.startsWith("Verificação da segurança", true) ->
                        input1?.setText(command.trim())


                    question.title.startsWith("Requisição completa", true) ->
                        input?.setText(command)

                    question.title.startsWith("Comunicado a enfermeira para providenciar", true) ->
                        input?.setText(command)

                    question.title.startsWith("Responsável:", true) ->
                        input?.setText(command.substringAfter("responsável ").trim())

                    question.title.startsWith("Data:", true) ->
                        input?.setText(command.substringAfter("data ").trim())

                    //Novos questionários
                    else ->
                        input?.setText(command.trim())
                }
            }

            is CheckboxQuestion -> {
                marcarCheckboxPorComando(view, command, question)
            }

            is MultipleChoiceQuestion -> {
                marcarRadioPorComando(view, command, question) //Complica para palavras grandes


            }

            is MultipleChoiceQuestion2 -> {
                marcarRadioPorComando2(view, command, question)

            }

            is CheckboxQuestion2 -> {

                marcarCheckboxPorComando2(view, command, question)
            }

            is SalvaTempo -> {

            }
            /*
            else -> {

            }
            */
        }
        return false // Não era "próxima pergunta"

    }

    private fun marcarCheckbox(view: View, texto: String) {
        val container = view.findViewById<LinearLayout>(R.id.checkbox_container) ?: return
        for (i in 0 until container.childCount) {
            val cb = container.getChildAt(i) as? CheckBox
            if (cb?.text.toString().equals(texto, ignoreCase = true)) {
                cb?.isChecked = true
            }
        }
    }

    private fun marcarRadio(view: View, texto: String) {
        val group = view.findViewById<RadioGroup>(R.id.options_group) ?: return
        for (i in 0 until group.childCount) {
            val rb = group.getChildAt(i) as? RadioButton
            if (rb?.text.toString().equals(texto, ignoreCase = true)) {
                rb?.isChecked = true
                break
            }
        }
    }

    fun marcarCheckboxPorComando(view: View, command: String, question: CheckboxQuestion) {
        val container = view.findViewById<LinearLayout>(R.id.checkbox_container)
        val texto = command.lowercase()

        for (opcao in question.options) {
            //val nomeOpcao = opcao.lowercase()

            // Normalização simples: remove acentos
            //val opcaoNorm = nomeOpcao.normalize()
            //val textoNorm = texto.normalize()

            if (command.lowercase().contains(opcao.lowercase())) {
                marcarCheckbox(view, opcao)
                //return// Aqui tá ferrando no 'Não'/'Não se aplica'
            }

            when {
                question.title.startsWith("Acesso Venoso", true) -> {
                    if (command.contains("providenciado", true)) marcarCheckbox(view, "Providenciado na SO")
                }
            }
        }
    }
    fun marcarRadioPorComando(view: View, command: String, question: MultipleChoiceQuestion) {
        //val textoNorm = command.lowercase().normalize()

        for (opcao in question.options) {
            //val opcaoNorm = opcao.lowercase().normalize()

            if (command.lowercase().contains(opcao.lowercase())) {
                marcarRadio(view, opcao)
                //return// Aqui tá ferrando no 'Não'/'Não se aplica'
            }
        }

        when {
            question.title.startsWith("Via aérea difícil", true) -> {
                if (command.contains("sim", true)) marcarRadio(view, "Sim e equipamento/assistência disponíveis")
                //if (command.contains("não", true)) marcarRadio(view, "Não")
                //else if (command.contains("sim", true)) marcarRadio(view, "Sim e equipamento/assistência disponíveis")
            }
        }
    }
    fun marcarCheckboxPorComando2(view: View, command: String, question: CheckboxQuestion2) {
        val container = view.findViewById<LinearLayout>(R.id.checkbox_container)
        //val texto = command.lowercase()


        val input1 = view.findViewById<EditText>(R.id.input_answer1)
        val input2 = view.findViewById<EditText>(R.id.input_answer2)
        val input3 = view.findViewById<EditText>(R.id.input_answer3)

        for (opcao in question.options) {
            if (command.lowercase().contains(opcao.lowercase())) {
                marcarCheckbox(view, opcao)
                //return// Aqui tá ferrando no 'Não'/'Não se aplica'
            }

        } // ^ Basicamente irrelevante aqui, são dois casos, um com palavras bem extensas, outro nem coisa pra marcar tem
        when {
            question.title.endsWith("segurança anestésica:", true) -> {
                if (command.contains("montagem da so", ignoreCase = true) or command.contains("de acordo com o procedimento", ignoreCase = true)) marcarCheckbox(view,"Montagem da SO de acordo com o procedimento")
                else if (command.contains("Material anestésico disponível", ignoreCase = true)) marcarCheckbox(view, "Material anestésico disponível, revisados e funcionantes")
                else input1?.setText(command.trim())
            }

            question.title.startsWith("Recomendações importantes na", true) ->{
                if (command.contains("cirurgião")) {
                    input1?.setText(command.substringAfter("cirurgião ").trim())
                }
                if (command.contains("anestesista")) {
                    //Dá pra fazer uma gambiarra pra captar se vem plural. Eu vejo isso?
                    if (command.contains("anestesistas"))    input2?.setText(command.substringAfter("anestesistas ").trim())
                    else    input2?.setText(command.substringAfter("anestesista ").trim())
                }
                if (command.contains("enfermagem")) {
                    input3?.setText(command.substringAfter("enfermagem ").trim())
                }

            }
        }
    }
    fun marcarRadioPorComando2(view: View, command: String, question: MultipleChoiceQuestion2) {
        //val textoNorm = command.lowercase().normalize()

        val input1 = view.findViewById<EditText>(R.id.input_answer1)
        val input2 = view.findViewById<EditText>(R.id.input_answer2)

        for (opcao in question.options) {
            //val opcaoNorm = opcao.lowercase().normalize()

            if (command.lowercase().contains(opcao.lowercase())) {
                marcarRadio(view, opcao)
                //return// Aqui tá ferrando no 'Não'/'Não se aplica'
            }

        }
        when {
            question.title.startsWith("Histórico de reação", true) -> {
                if (!command.contains("não", true) and !command.contains("sim", true)) {
                    input1?.setText(command)
                }
            }

            question.title.startsWith("Contagem de compressas", true) -> {
                if (command.contains("entregues", true)) {
                    //val input1 = view.findViewById<EditText>(R.id.input_answer1)
                    input1?.setText(command.substringAfter("entregues ").trim())
                }
                if (command.contains("conferidas", true)) {
                    //val input2 = view.findViewById<EditText>(R.id.input_answer2)
                    input2?.setText(command.substringAfter("conferidas ").trim())
                }
            }

            question.title.startsWith("Contagem de instrumentos", true) -> {
                if (command.contains("entregues", true)) {
                    //val input1 = view.findViewById<EditText>(R.id.input_answer1)
                    input1?.setText(command.substringAfter("entregues ").trim())
                }
                if (command.contains("conferidos", true)) {
                    //val input2 = view.findViewById<EditText>(R.id.input_answer2)
                    input2?.setText(command.substringAfter("conferidos ").trim())
                }
            }

            question.title.startsWith("Contagem de agulhas", true) -> {
                if (command.contains("entregues", true)) {
                    //val input1 = view.findViewById<EditText>(R.id.input_answer1)
                    input1?.setText(command.substringAfter("entregues ").trim())
                }
                if (command.contains("conferidas", true)) {
                    //val input2 = view.findViewById<EditText>(R.id.input_answer2)
                    input2?.setText(command.substringAfter("conferidas ").trim())
                }
            }

            question.title.startsWith("Amostra cirúrgica identificada adequadamente", true) -> {
                if (!command.contains("não", true) and !command.contains("sim", true))
                    input1?.setText(command)
            }

            question.title.startsWith("Problema com equipamentos que", true) -> {
                if (!command.contains("não", true) and !command.contains("sim", true))
                    input1?.setText(command)

            }
        }
    }

}