package com.example.cirurgiasegura.utils

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.cirurgiasegura.data.ResponseItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

class CsvDataSaver(private val context: Context,
                   private val originalCsvName: String?) {
    fun saveAnswers(answers: Map<String, String>, onComplete: (File?) -> Unit) {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val csvDir = File(directory, "Arquivos CSV")
        /*
        val csvDir = context.getExternalFilesDir("Arquivos CSV") // Cria uma pasta dentro da área do seu App.
        if (!csvDir?.exists()!!) {
            csvDir.mkdirs()
        }
         */


        if (!csvDir.exists()) {
            csvDir.mkdirs()
        }
        //var fileNumber = 1
        var file: File

        // Padrão regex para capturar o valor após 'answer='
        // O grupo de captura é \w+ (uma ou mais letras, números, ou underscore)
        val pattern = Regex("answer=(\\w+)")

        //var sujeito = answers["Nome:"]?.replace(" ", "_") ?: "respostas"
        // Alterando pra considerar o primeiro item da lista, e não especificamente àquele associado com 'Nome:'
        var sujeito = answers.values.firstOrNull()?.replace(" ", "_") ?: "respostas"

        // Ajuste pra pegar o item de resposta mesmo, e não a variável 'por extenso'
        val matchResult = sujeito?.let { pattern.find(it) }
        sujeito = matchResult?.groups?.get(1)?.value?.replace(" ", "_") ?: "respostas"

        //val sujeito = answers["Nome:"]?.replace(" ", "_")?.takeIf { it.isNotEmpty() } ?: "respostas"

        var fileName: String
        if (originalCsvName == null)    fileName = "CirurgiaSegura_$sujeito.csv"
        else    fileName = "${originalCsvName}_$sujeito.csv"
        file = File(csvDir, fileName)
        /*
        do {
            val fileName = "respostas$fileNumber.csv"
            file = File(directory, fileName)
            fileNumber++
        } while (file.exists())
         */

        try {
            val fileOutputStream = FileOutputStream(file)
            val writer = PrintWriter(OutputStreamWriter(fileOutputStream, "UTF-8"))

            writer.println("Pergunta,Resposta")

            // A regex vai buscar por:
            // title= (seguido por qualquer coisa exceto vírgula ou parêntese),
            // answer= (seguido por qualquer coisa exceto parêntese de fechamento)
            val pattern = Regex("""ResponseItem\(title=(.+?), answer=(.+?)\)""")

            answers.forEach { (pergunta, resposta) ->
                //Log.d("CSV_DATA_SAVER", "Pergunta: ${pergunta.length} caracteres. Resposta: ${resposta.length}")
                //writer.println("\"$pergunta\",\"$resposta\"")
                val separados = parseResponseItems(resposta)
                separados.forEach { item ->
                    val novaPergunta = when(item.title){
                        "Cirurgião:" -> "Recomendações Cirurgião:"
                        "Anestesista:" -> "Recomendações Anestesista:"
                        "Enfermagem:" -> "Recomendações Enfermagem:"
                        "Antes da Indução Anestésica" -> "parte 1"
                        "Antes da Incisão Cirúrgica" -> "parte 2"
                        "Antes da Saída do Paciente da Sala de Cirurgia" -> "parte 3"
                        else -> item.title
                    }

                    writer.println("\"$novaPergunta\",\"${item.answer}\"") }
            }


            writer.flush()
            writer.close()
            //Toast.makeText(context, "Salvo em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            Toast.makeText(context, "Arquivo '$fileName' salvo em 'Downloads/Arquivos CSV'", Toast.LENGTH_LONG).show()

            // Copiar para a pasta Downloads
            //copyToDownloads(context, file)

            // CHAMADA DE SUCESSO: Notifica o chamador que o arquivo foi salvo
            onComplete(file)
        }

        catch (e: Exception) {
            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            //Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()

            // CHAMADA DE FALHA: Notifica o chamador que houve um erro
            onComplete(null)
        }
    }

    fun parseResponseItems(responseString: String): List<ResponseItem> {

        // A regex busca por:
        // title= (seguido por qualquer coisa exceto vírgula ou parêntese),
        // answer= (seguido por qualquer coisa exceto parêntese de fechamento)
        val pattern = Regex("""ResponseItem\(title=(.+?), answer=(.+?)\)""")

        val matches = pattern.findAll(responseString)

        return matches.map { matchResult ->
            val titleWithColon = matchResult.groupValues[1].trim()
            val answer = matchResult.groupValues[2].trim()

            // Remove a vírgula final se for o último item antes do parêntese fechado e limpa as aspas
            val cleanTitle = titleWithColon.removeSuffix(",").trim()
            val cleanAnswer = answer.removeSuffix(")").trim()

            ResponseItem(
                // O título deve ser limpo de aspas externas se existirem
                title = cleanTitle.removeSurrounding("\""),
                answer = cleanAnswer.removeSurrounding("\"")
            )
        }.toList()
    }

    private fun copyToDownloads(context: Context, sourceFile: File) {
        // Caminho da pasta "Arquivos CSV" dentro do diretório público Downloads
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val csvDir = File(downloadsDir, "Arquivos CSV") // Subpasta "Arquivos CSV"

        // Verifica se a pasta "Arquivos CSV" existe, se não, cria
        if (!csvDir.exists()) {
            val success = csvDir.mkdirs() // Tenta criar a pasta
            if (success) {
                Toast.makeText(context, "Pasta 'Arquivos CSV' criada.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erro ao criar a pasta 'Arquivos CSV'.", Toast.LENGTH_SHORT).show()
            }
        }

        // Agora, verifica se a pasta existe e copia o arquivo para lá
        if (csvDir.exists()) {
            // Define o destino final para o arquivo CSV
            val destinationFile = File(csvDir, sourceFile.name)

            try {
                // Copia o arquivo para a pasta "Arquivos CSV"
                sourceFile.copyTo(destinationFile, overwrite = true)

                // Notificação de sucesso
                Toast.makeText(context, "Arquivo copiado para 'Arquivos CSV'.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Caso ocorra algum erro na cópia do arquivo
                e.printStackTrace()
                Toast.makeText(context, "Erro ao copiar o arquivo.", Toast.LENGTH_LONG).show()
            }
        } else {
            // Caso a pasta não exista e não tenha sido criada com sucesso
            Toast.makeText(context, "Não foi possível encontrar ou criar a pasta.", Toast.LENGTH_LONG).show()
        }
    }
    /*
    fun saveAnswers(answers: Map<String, String>, onComplete: (File?) -> Unit) {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val csvDir = File(directory, "Arquivos CSV")
        if (!csvDir.exists()) {
            csvDir.mkdirs()
        }

        //var fileNumber = 1
        var file: File
        var sujeito = answers["Nome:"]?.replace(" ", "_") ?: "respostas"
        do {
            val fileName = "respostas$sujeito.csv"
            file = File(csvDir, fileName)
            //fileNumber++
        } while (file.exists())

        try {
            val fileOutputStream = FileOutputStream(file)
            val writer = PrintWriter(OutputStreamWriter(fileOutputStream, "UTF-8"))

            writer.println("Pergunta,Resposta")
            // Processa as perguntas e respostas
            answers.forEach { (pergunta, resposta) ->
                // Substitui perguntas específicas por nomes curtos
                val novaPergunta = when (pergunta) {
                    "Antes da Indução Anestésica" -> "parte 1"
                    "Antes da Incisão Cirúrgica" -> "parte 2"
                    "Antes da Saída do Paciente da Sala de Cirurgia" -> "parte 3"
                    else -> pergunta
                }

                // Escreve a pergunta e resposta no arquivo
                writer.println("\"$novaPergunta\",\"$resposta\"")
            }

            /*
            answers.forEach { (pergunta, resposta) ->
                if(pergunta == "Antes da Indução Anestésica"){
                    pergunta = "parte 1"
                }
                else if(pergunta == "Antes da Incisão Cirúrgica"){
                    pergunta = "parte 2"
                }
                else if(pergunta == "Antes da Saída do Paciente da Sala de Cirurgia"){
                    pergunta = "parte 3"
                }
                val resposta = answers[pergunta] ?: ""
                writer.println("\"$pergunta\",\"$resposta\"")
                //writer.println("\"$pergunta\",\"$resposta\"")
            }
            */

            writer.flush()
            writer.close()
            Toast.makeText(context, "Salvo em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            onComplete(file) // SUCESSO: Retorna o arquivo salvo

        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            onComplete(null) // FALHA: Retorna nulo
        }
    }
    */
}