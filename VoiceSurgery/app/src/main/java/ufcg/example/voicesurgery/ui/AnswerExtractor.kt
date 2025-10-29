package ufcg.example.voicesurgery.ui

import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.voicesurgery.R
import ufcg.example.voicesurgery.data.*
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AnswerExtractor {

    fun extractAnswer(question: Question, view: View): String {
        return when (question) {
            is MultipleChoiceQuestion -> {
                val group = view.findViewById<RadioGroup>(R.id.options_group)
                val selectedId = group.checkedRadioButtonId
                if (selectedId != -1) {
                    view.findViewById<RadioButton>(selectedId).text.toString()
                } else {
                    ""
                }
            }
            is TextInputQuestion -> {
                view.findViewById<EditText>(R.id.input_answer).text.toString()
            }
            is CheckboxQuestion -> {
                val checkboxContainer = view.findViewById<LinearLayout>(R.id.checkbox_container)
                val selected = mutableListOf<String>()
                for (i in 0 until checkboxContainer.childCount) {
                    val checkBox = checkboxContainer.getChildAt(i) as? CheckBox
                    if (checkBox?.isChecked == true) {
                        selected.add(checkBox.text.toString())
                    }
                }
                selected.joinToString("; ")
            }

            is SalvaTempo -> {
                //val input = view.findViewById<EditText>(R.id.input_answer)
                val input = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
                input
            }
        }
    }
}