package com.example.cirurgiasegura.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.cirurgiasegura.network.ApiClient // IMPORTANTE: Use o ApiClient singleton!
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfUploadRepository(private val context: Context) {

    // Interface de callback para notificar a MainActivity sobre o resultado
    interface UploadCallback {
        fun onSuccess(filePath: String)
        fun onError(message: String)
    }

    fun uploadFiles(token: String, csvFile: File, pdfFile: File, callback: UploadCallback) {

        // 1. Prepara os arquivos para o upload
        val csvRequest = csvFile.asRequestBody("text/csv".toMediaTypeOrNull())
        val csvPart = MultipartBody.Part.createFormData("files", csvFile.name, csvRequest)

        val pdfRequest = pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
        val pdfPart = MultipartBody.Part.createFormData("files", pdfFile.name, pdfRequest)

        val parts = listOf(csvPart, pdfPart)

        // 2. Extrai o nome do CSV (lógica de negócio)
        val nomeDoSujeito = parseNameFromCsv(csvFile)

        // 3. Faz a chamada de rede USANDO O ApiClient EXISTENTE
        // (Não crie um novo Retrofit.Builder aqui!)
        ApiClient.apiService.uploadArquivos(token, parts).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        // 4. Salva o arquivo de resposta
                        val savedFilePath = saveResponseBodyToFile(body, nomeDoSujeito)
                        if (savedFilePath != null) {
                            callback.onSuccess("PDF salvo em: $savedFilePath")
                        } else {
                            callback.onError("Erro ao salvar o PDF recebido")
                        }
                    } ?: run {
                        callback.onError("Resposta vazia do servidor")
                    }
                } else {
                    callback.onError("Erro na resposta: ${response.message()} (${response.code()})")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onError("Falha na conexão: ${t.message}")
            }
        })
    }

    /**
     * Extrai o nome do paciente do arquivo CSV.
     */
    private fun parseNameFromCsv(csvFile: File): String {
        return try {
            val linhas = csvFile.readLines()
            val tabela = linhas.map { it.split(",") }
            val item11 = tabela.getOrNull(1)?.getOrNull(1) // Pega linha 1, coluna 1 (Nome:)
            item11?.replace(Regex("[^A-Za-z0-9]"), "") ?: "desconhecido"
        } catch (e: Exception) {
            "desconhecido"
        }
    }

    /**
     * Salva o corpo da resposta (o PDF preenchido) no armazenamento público.
     */
    private fun saveResponseBodyToFile(body: ResponseBody, nomeDoSujeito: String): String? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pdfDir = File(downloadsDir, "Arquivos PDF") // Cria uma subpasta
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val outputFile = File(pdfDir, "preenchido - $nomeDoSujeito.pdf")

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Notifica a galeria de mídia sobre o novo arquivo
            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null, null)
            outputFile.absolutePath // Retorna o caminho do arquivo salvo
        } catch (e: IOException) {
            e.printStackTrace()
            null // Retorna nulo em caso de erro
        }
    }
}