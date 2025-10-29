package ufcg.example.voicesurgery.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

class CsvDataSaver(private val context: Context) {

    fun saveAnswers(answers: Map<String, String>) {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var fileNumber = 1
        var file: File
        do {
            val fileName = "respostas$fileNumber.csv"
            file = File(directory, fileName)
            fileNumber++
        } while (file.exists())

        try {
            val fileOutputStream = FileOutputStream(file)
            val writer = PrintWriter(OutputStreamWriter(fileOutputStream, "UTF-8"))

            writer.println("Pergunta,Resposta")
            answers.forEach { (pergunta, resposta) ->
                writer.println("\"$pergunta\",\"$resposta\"")
            }

            writer.flush()
            writer.close()
            Toast.makeText(context, "Salvo em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}