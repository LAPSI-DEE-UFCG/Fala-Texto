package com.example.cirurgiasegura.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Usamos um 'object' (Singleton) pois não precisamos de múltiplas instâncias
object FileUtil {

    /**
     * Copia o conteúdo de uma Uri (do seletor de arquivos) para um
     * arquivo temporário no armazenamento privado do app.
     */
    fun copyPdfToAppStorage(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Cria um nome de arquivo único no diretório de downloads *privado* do app
        val nomeArquivo = "selecionado_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), nomeArquivo)

        return try {
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            file // Retorna o arquivo copiado
        } catch (e: IOException) {
            e.printStackTrace()
            null // Retorna nulo em caso de erro
        } finally {
            inputStream.close()
        }
    }
}