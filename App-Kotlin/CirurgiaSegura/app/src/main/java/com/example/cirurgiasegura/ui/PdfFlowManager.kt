package com.example.cirurgiasegura.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.cirurgiasegura.data.PdfUploadRepository
import com.example.cirurgiasegura.utils.FileUtil
import java.io.File

class PdfFlowManager(
    private val activity: ComponentActivity, // Precisamos da Activity para registrar o launcher
    private val uploadRepository: PdfUploadRepository, // Para fazer o upload
    private val onStatusUpdate: (String) -> Unit // Callback para atualizar o TextView na Main
){

    // Variáveis de estado necessárias para o upload
    var currentJwtToken: String = ""
    var currentCsvFile: File? = null

    // O Launcher é registrado assim que a classe é instanciada na MainActivity
    private val pdfLauncher: ActivityResultLauncher<String> = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handlePdfSelection(uri)
    }

    /**
     * Função pública para iniciar o processo.
     * Substitui o antigo 'abrirSeletorDePdf'
     */
    fun startPdfSelection() {
        if (currentCsvFile == null) {
            onStatusUpdate("Erro: Arquivo CSV não definido antes da seleção.")
            return
        }
        if (currentJwtToken.isEmpty()) {
            onStatusUpdate("Erro: Token de login inválido.")
            return
        }

        try {
            pdfLauncher.launch("application/pdf")
        } catch (e: Exception) {
            onStatusUpdate("Erro ao abrir seletor: ${e.message}")
        }
    }

    /**
     * Lógica interna que processa o arquivo escolhido
     */
    private fun handlePdfSelection(uri: Uri?) {
        if (uri == null) {
            onStatusUpdate("Seleção de PDF cancelada.")
            return
        }

        onStatusUpdate("Processando PDF...")

        // Usa o FileUtil que já criamos
        val pdfFile = FileUtil.copyPdfToAppStorage(activity, uri)

        if (pdfFile == null) {
            onStatusUpdate("Erro ao copiar o PDF para o armazenamento interno.")
            return
        }

        // Usa o Repository que já criamos
        onStatusUpdate("Enviando arquivos...")

        // currentCsvFile!! é seguro aqui pois checamos no startPdfSelection
        uploadRepository.uploadFiles(currentJwtToken, currentCsvFile!!, pdfFile, object : PdfUploadRepository.UploadCallback {
            override fun onSuccess(filePath: String) {
                onStatusUpdate(filePath) // Sucesso!
            }

            override fun onError(message: String) {
                onStatusUpdate(message) // Erro!
            }
        })
    }
}