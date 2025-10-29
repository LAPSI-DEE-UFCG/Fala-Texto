package ufcg.example.voicesurgery.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.voicesurgery.R
import ufcg.example.voicesurgery.data.*

class QuestionViewFactory(private val context: Context) {

    fun createView(question: Question, container: ViewGroup): View {
        val inflater = LayoutInflater.from(context)

        val layoutId = when (question) {
            is TextInputQuestion -> R.layout.question_text_input
            is MultipleChoiceQuestion -> R.layout.question_multiple_choice
            is CheckboxQuestion -> R.layout.question_checkbox
            is SalvaTempo -> R.layout.pontos_pausa
        }

        val view = inflater.inflate(layoutId, container, false)
        val title = view.findViewById<TextView>(R.id.question_title)
        title.text = question.title

        when (question) {
            is MultipleChoiceQuestion -> setupMultipleChoice(view, question)
            is CheckboxQuestion -> setupCheckbox(view, question)
            is TextInputQuestion -> { /* Nenhuma configuração extra necessária */ }
            is SalvaTempo -> { /* Nenhuma configuração extra necessária???? */ } //TODO()
        }

        return view
    }

    private fun setupMultipleChoice(view: View, question: MultipleChoiceQuestion) {
        val group = view.findViewById<RadioGroup>(R.id.options_group)
        group.removeAllViews()
        for (option in question.options) {
            val rb = RadioButton(context)
            rb.text = option
            group.addView(rb)
        }
    }

    private fun setupCheckbox(view: View, question: CheckboxQuestion) {
        val container = view.findViewById<LinearLayout>(R.id.checkbox_container)
        container.removeAllViews()
        for (option in question.options) {
            val cb = CheckBox(context)
            cb.text = option
            container.addView(cb)
        }
    }
}