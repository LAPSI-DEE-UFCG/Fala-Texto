package ufcg.example.voicesurgery.services

import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.voicesurgery.R//Aqui pode conflitar algo?
import ufcg.example.voicesurgery.data.Question

class VoiceCommandProcessor {

    /**
     * Processa um comando de voz e atualiza a view.
     * @return True se o comando "próxima pergunta" foi detectado, False caso contrário.
     */
    fun processCommand(command: String, question: Question, view: View): Boolean {
        if (command.contains("próxima pergunta", ignoreCase = true)) {
            return true
        }

        val questionTitle = question.title
        val input = view.findViewById<EditText>(R.id.input_answer)

        when {
            questionTitle.startsWith("Nome", true) && command.contains("nome", true) ->
                input?.setText(command.substringAfter("nome ").trim())

            questionTitle.startsWith("Prontuário", true) ->
                input?.setText(command.substringAfter("prontuário ").trim())

            questionTitle.startsWith("Sala", true) ->
                input?.setText(command.substringAfter("sala ").trim())

            questionTitle.startsWith("Paciente confirmou", true) -> {
                if (command.contains("identidade", true)) marcarCheckbox(view, "Identidade")
                if (command.contains("sítio cirúrgico", true)) marcarCheckbox(view, "Sítio Cirúrgico correto")
                if (command.contains("procedimento", true)) marcarCheckbox(view, "Procedimento")
                if (command.contains("consentimento", true)) marcarCheckbox(view, "Consentimento")
            }

            questionTitle.startsWith("sítio demarcado", true) -> {
                if (command.contains("não se aplica", true)) marcarRadio(view, "Não se aplica")
                else if (command.contains("não", true)) marcarRadio(view, "Não")
                else if (command.contains("sim", true)) marcarRadio(view, "Sim")
            }

            // ... (Adicionar TODOS os outros blocos if/else aqui) ...
            // Ex:
            questionTitle.startsWith("Via aérea difícil", true) -> {
                if (command.contains("não", true)) marcarRadio(view, "Não")
                else if (command.contains("sim", true)) marcarRadio(view, "Sim e equipamento/assistência disponíveis")
            }

            questionTitle.endsWith("Qual?:", true) ->
                input?.setText(command)

            questionTitle.startsWith("Responsável:", true) ->
                input?.setText(command.substringAfter("responsável ").trim())

            questionTitle.startsWith("Data:", true) ->
                input?.setText(command.substringAfter("data ").trim())
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
}