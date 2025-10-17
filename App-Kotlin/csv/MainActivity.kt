package com.example.csv_basico

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.FileOutputStream
//pode parar o ctrl z
import retrofit2.converter.gson.GsonConverterFactory
import org.json.JSONObject
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    //Estruturação das variáveis
    private lateinit var textView: TextView
    private lateinit var btnFalar: Button
    private lateinit var btnSalvacsv: Button
    private lateinit var etA : EditText
    private lateinit var etB : EditText

    private lateinit var selectedPdfFile: File
    private lateinit var jwtToken: String

    //Validando o arquivo .pdf
    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            processarPdfSelecionado(uri)
        } else {
            textView.text = "Nenhum arquivo selecionado"
        }
    }
    //Conexão com o servidor
    data class LoginResponse(
        val access_token: String
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        //Puxando as variáveis do xml associadas aos componentes
        textView = findViewById(R.id.textView)
        btnSalvacsv = findViewById(R.id.btnSalvacsv)
        btnFalar = findViewById(R.id.btnGambiarra)
        etA = findViewById(R.id.et_a)
        etB = findViewById(R.id.et_b)

        //Desabilita o botão para envio dos arquivos 
        btnFalar.isEnabled = false

        btnSalvacsv.setOnClickListener {
            //Chama a função e inverte a lógica dos botões disponíveis
            abrirSeletorDePdf()
            btnSalvacsv.isEnabled = false
            btnFalar.isEnabled = true
        }

        btnFalar.setOnClickListener {
            salvarDadosCSV()
        }
        fazerLogin {
            // Código a ser executado após login bem-sucedido
            textView.text = "Login feito com sucesso!"
        }


    }

    private fun abrirSeletorDePdf() {
        //Launcher para especificação do formato de arquivo
        pdfPickerLauncher.launch("application/pdf")
    }

    private fun processarPdfSelecionado(uri: Uri) {
        //Validação do formulário após seleção pelo usuário
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val nomeArquivo = "selecionado_${System.currentTimeMillis()}.pdf"
        selectedPdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), nomeArquivo)

        try {
            FileOutputStream(selectedPdfFile).use { output ->
                inputStream.copyTo(output)
            }
            textView.text = "Arquivo selecionado"

        } catch (e: IOException) {
            textView.text = "Erro ao processar PDF: ${e.message}"
        }
    }

    private fun salvarDadosCSV() {
        //Comunicação com o elementos do xml, a estrutura da interface
        val nome = etA.text.toString()
        val idade = etB.text.toString()

        //Acesso à pasta com os arquivos .csv da aplicação em questão
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val csvDir = File(directory, "Arquivos CSV")
        if (!csvDir.exists()) {
            csvDir.mkdirs()
        }

        var fileNumber = 1
        var file: File
        do {
            val fileName = "testee$fileNumber.csv"
            file = File(csvDir, fileName)
            fileNumber++
        } while (file.exists()) // Continua até encontrar um nome de arquivo que não existe

        try {
            //Chamada da função que escreve o arquivo, colocando já os termos em questão
            val writer = FileWriter(file)
            writer.append("Nome,Idade\n")
            writer.append("Nome:,$nome\n")
            writer.append("Data de nascimento:,$idade\n")
            writer.flush()
            writer.close()
            MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)
            textView.text = "Dado salvos em ${csvDir}"

            enviarCSV(file,selectedPdfFile)
        } catch (e: IOException) {
            textView.text = "Erro ao salvar arquivo"
        }

    }
    private fun enviarCSV(csvFile: File, pdfFile: File) {

        //Comunicação com o servidor
        val csvRequest = csvFile.asRequestBody("text/csv".toMediaTypeOrNull())
        val csvPart = MultipartBody.Part.createFormData("files", csvFile.name, csvRequest)

        val pdfRequest = pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
        val pdfPart = MultipartBody.Part.createFormData("files", pdfFile.name, pdfRequest)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://processarpdffalatex.zapto.org") // IP Máquina servidor
            .addConverterFactory(ScalarsConverterFactory.create())

            .build()

        val api = retrofit.create(ApiService::class.java)
        val parts = listOf(csvPart, pdfPart)

        api.uploadArquivos(jwtToken, parts).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        try {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val pdfDir = File(downloadsDir, "Arquivos PDF")
                            if (!pdfDir.exists()) {
                                pdfDir.mkdirs()
                            }
                            val outputFile = File(pdfDir, "preenchido_${System.currentTimeMillis()}.pdf")
                            val inputStream = body.byteStream()
                            val outputStream = FileOutputStream(outputFile)

                            inputStream.use { input ->
                                outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(outputFile.absolutePath), null, null)

                            textView.text = "PDF preenchido salvo em: ${outputFile.absolutePath}"
                        } catch (e: IOException) {
                            textView.text = "Erro ao salvar PDF: ${e.message}"
                        }
                    } ?: run {
                        textView.text = "Resposta vazia do servidor"
                    }
                } else {
                    textView.text = "Erro na resposta: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                textView.text = "Falha na conexão: ${t.message}"
            }
        })

    }
    private fun fazerLogin(onSuccess: () -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://processarpdffalatex.zapto.org")
            .addConverterFactory(GsonConverterFactory.create()) // Usa Scalar pois não estamos usando Gson
            .build()

        val api = retrofit.create(ApiService::class.java)

        val loginData = HashMap<String, String>()
        loginData["username"] = "Fala-texto"
        loginData["password"] = "Transcrição_de_fala_em_texto_api"

        api.login(loginData).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    try {
                        val token = response.body()?.access_token
                        if (token != null) {
                            jwtToken = "Bearer $token"
                            textView.text = "Login automático realizado"
                            onSuccess()
                        } else {
                            textView.text = "Token não encontrado na resposta"
                        }
                    } catch (e: Exception) {
                        textView.text = "Erro ao interpretar token: ${e.message}"
                    }
                } else {
                    textView.text = "Erro no login: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                textView.text = "Falha na conexão: ${t.message}"
            }
        })
    }
}
