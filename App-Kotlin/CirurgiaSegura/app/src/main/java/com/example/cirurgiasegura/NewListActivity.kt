package com.example.cirurgiasegura

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import com.example.cirurgiasegura.data.Question
import com.example.cirurgiasegura.data.TextInputQuestion
import com.example.cirurgiasegura.databinding.ActivityNewListBinding
import com.example.cirurgiasegura.ui.CsvDataReader
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

class NewListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewListBinding
    private val vm = QuestionsViewModel()

    //private lateinit var csvPickerManager: CsvDataReader
    val filePicker = CsvDataReader(this) { uri ->
        if (uri == null) {
            println("Seleção cancelada")
        } else {
            println("Arquivo selecionado: $uri")
            processFile(uri)
        }
    }

    val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val formsDir = File(directory, "Template Formulários")

    var formsTemplate = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!formsDir.exists()) {
            formsDir.mkdirs()
        }

        binding = ActivityNewListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            val text = binding.etQuestions.text.toString()

            var file: File
            var fileNumber = 1
            var fileName = ""

            do {
                fileName = "perguntas$fileNumber.csv"
                file = File(formsDir, fileName)
                fileNumber++
            } while (file.exists())

            val fileOutputStream = FileOutputStream(file)

            val writer = PrintWriter(OutputStreamWriter(fileOutputStream, "UTF-8"))


            val questions = text.lines().filter { it.isNotBlank() }

            for (pergunta in questions){
                writer.println(pergunta)
            }

            writer.flush()
            writer.close()


            vm.setQuestions(questions)

            val listaPersonalizada = criacaoNovaLista(questions)

            val intent = Intent(this, MainActivity::class.java)
            //intent.putStringArrayListExtra("questions", ArrayList(questions))
            intent.putStringArrayListExtra("questions", ArrayList(questions))
            intent.putExtra("nomeArquivo", fileName)
            startActivity(intent)
        }
        binding.btnChoice.setOnClickListener {
            filePicker.openDownloads("text/*")
            //Usar Intent ACTION_OPEN_DOCUMENT com URI inicial
        }
    }

    // A ideia é simplesmente mapear cada String para um TextInputQuestion
    fun criacaoNovaLista(questoes: List<String>): List<Question>{
        return questoes.map { TextInputQuestion(it) }
    }
    fun processFile(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val text = inputStream?.bufferedReader().use { it?.readText() }

        val fileName = getFileName(this, uri) ?: "arquivo_desconhecido"

        //println("Conteúdo do arquivo:\n$text")
        // Desta forma dá conflito pois tem uma variável no meio dos 'writer's que é importante ao fluxo do código
        //formsTemplate = true
        //binding.etQuestions.setText(text)
        //binding.btnStart.performClick()
        //return uri
        val questions = text!!.lines().filter { it.isNotBlank() }

        vm.setQuestions(questions)

        val intent = Intent(this, MainActivity::class.java)
        intent.putStringArrayListExtra("questions", ArrayList(questions))
        intent.putExtra("nomeArquivo", fileName)
        startActivity(intent)

    }
    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null

        // Tenta via cursor (método oficial)
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(
                uri, null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }

        // Se vier via file://
        if (name == null && uri.scheme == "file") {
            name = File(uri.path!!).name
        }

        // Última alternativa (funciona para SAF raiz)
        if (name == null) {
            name = uri.lastPathSegment?.substringAfterLast("/")
        }

        name = name?.substringBefore(".")

        return name
    }
}