package com.example.cirurgiasegura.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class CsvDataReader(
    private val activity: ComponentActivity,
    private val onFileSelected: (Uri?) -> Unit
) {
    // Launcher para receber o arquivo escolhido
    private val filePickerLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val dataUri = result.data?.data
                onFileSelected(dataUri)
            } else {
                onFileSelected(null)
            }
        }
    /*
     * Abre o diretório de Downloads para o usuário selecionar um arquivo.
    */
    fun openDownloads(mimeType: String = "*/*") {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType

                // URI inicial: Downloads
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                )
            }

            filePickerLauncher.launch(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            onFileSelected(null)
        }
    }
}