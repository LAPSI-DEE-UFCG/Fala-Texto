package com.example.faturamento21

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlin.random.Random
import org.apache.commons.text.similarity.LevenshteinDistance
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var btnFuzz: Button
    private lateinit var btnFalar: Button
    private lateinit var btnNovoTermo: Button
    private lateinit var btnRelatorio: Button
    private lateinit var inputTexto: TextView  // Agora é TextView
    private lateinit var resultadoTexto: TableLayout
    private lateinit var textoSorteado: TextView
    //private lateinit var tableLayout: TableLayout

    private lateinit var parteNumericaGlobal: String
    private lateinit var restanteGlobal: String


    private var itemSorteado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializações
        btnFuzz = findViewById(R.id.btnFuzz)
        btnFuzz.visibility = View.INVISIBLE
        btnFalar = findViewById(R.id.btnGrava)
        inputTexto = findViewById(R.id.input_text)
        resultadoTexto = findViewById(R.id.tableLayout)
        textoSorteado = findViewById(R.id.data)
        btnNovoTermo = findViewById((R.id.btnAlterna))
        btnRelatorio = findViewById(R.id.btnrelatorio)

        //mostrar a variavel como 'global'
        var tabela_final = mutableMapOf<String, String>()
        //var results: List<Pair<String, Int>> = emptyList()
        var results: List<Pair<Pair<String, String>, Int>> = emptyList()
        //tentativa de criar uma lista 'dados' similar à usada em 'HelloTable2'
        var dadosTabela = mutableListOf(listOf("Descrição", "Valor"))
        var termosSimilares: MutableList<List<String>>
        var quantidades_final: MutableList<String> = mutableListOf("Qtd.")

        val limiteCaracteres = arrayOf(20, 15)

        solicitarPermissoes()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        configurarReconhecimento()

        val descricoes = mutableListOf<Pair<String, String>>()

        // Lê o arquivo CSV
        assets.open("tabela_consulta2.csv").bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line -> // .drop(1) para ignorar o cabeçalho "Descricao,Valor"
                val row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()) // regex que respeita aspas
                if (row.size >= 2) {
                    val descricao = row[0].replace("\"", "").trim()
                    val valor = row[1].replace("\"", "").trim()
                    descricoes.add(descricao to valor)
                }
            }
        }

        // Sorteia um item
        if (descricoes.isNotEmpty()) {
            val item = descricoes[Random.nextInt(descricoes.size)]
            itemSorteado = item.first
            //itemSorteado = descricoes[Random.nextInt(descricoes.size)]
            textoSorteado.text = itemSorteado
        } else {
            textoSorteado.text = "Nenhuma descrição encontrada."
        }

        // Ação do botão de similaridade
        btnFuzz.setOnClickListener {
            termosSimilares = mutableListOf(listOf("Termo", "Similaridade"))

            //val entrada = inputTexto.text.toString()
            val entrada = restanteGlobal
            if (entrada.isBlank()) {
                textoSorteado.text = "Digite algo para comparar"
            }
            else {
                results = descricoes.map { it to (levenshteinSimilarity(entrada, it.first) * 100).toInt() }
                    .sortedByDescending { it.second }
                    .take(3) // top 3 mais parecidos

                for((item, score) in results){
                    termosSimilares.add(listOf( item.first, score.toString()))
                }

                //val resultText = results.joinToString("\n\n") { (item, score) -> "${item.first}: $score%" }

                //textoSorteado.text = "Mais semelhantes:\n$resultText"
                //resultadoTexto.text = "Mais semelhantes:\n$resultText"
                //resultadoTexto.text = "Mais semelhantes:\n${results[1].first}"
            }

            resultadoTexto.removeAllViews()
            mostraTabela(termosSimilares, quantidades_final, false)

        }

        btnFalar.setOnClickListener {
            iniciarReconhecimentoVoz()
        }
        btnRelatorio.setOnClickListener {
            resultadoTexto.removeAllViews()

            mostraTabela(dadosTabela, quantidades_final, true)
            salvarDadosCSV(dadosTabela, quantidades_final)

            resultadoTexto.visibility = View.VISIBLE
        }


        btnNovoTermo.setOnClickListener { view ->
            val popupMenu = PopupMenu(this@MainActivity, view)
            popupMenu.inflate(R.menu.popup_menu_item)
            val format = NumberFormat.getInstance(Locale("pt", "BR"))

            fun processarSelecao(index: Int) {
                if (index in results.indices) {
                    val selecionado = results[index]
                    val descricao = selecionado.first.first
                    val valor = selecionado.first.second
                    val score = selecionado.second

                    Toast.makeText(this@MainActivity, selecionado.first.toString(), Toast.LENGTH_LONG).show()
                    //tabela_final.put(descricao, score.toString())
                    //tabela_final.put(descricao, valor)
                    //val valor_att2 = format.parse(valor).toDouble().times(format.parse(parteNumericaGlobal).toDouble()).toString()
                    //val valor_atualizado = (format.parse(valor).toDouble()*format.parse(parteNumericaGlobal).toDouble()).toString()
                    //var valor_atualizado = valor.replace(".","").replace(",", ".")
                    var valor_atualizado = valor.replace(".","").replace(",", "")
                        .toDouble()
                        .times(parteNumericaGlobal.toDouble())
                        .toString()


                    //format.parse(valorStr)?.toDouble()
                    // Formatar valor final com vírgula como separador decimal
                    val formatador = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))

                    tabela_final.put(descricao, valor_atualizado)
                    dadosTabela.add(listOf(descricao, valor_atualizado))
                    quantidades_final.add(parteNumericaGlobal)

                    val novoItem = descricoes.random()
                    itemSorteado = novoItem.first
                    textoSorteado.text = "${novoItem.first}"
                    //textoSorteado.text = "${novoItem.first} - R$ ${novoItem.second}"
                    inputTexto.setText("Fala reconhecida aparecerá aqui")
                    //resultadoTexto.text = ""
                    //textoSorteado.text = itemSorteado
                    //inputTexto.text = "Fala reconhecida aparecerá aqui"
                    //resultadoTexto.text = ""
                }
                resultadoTexto.removeAllViews()
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when(menuItem.itemId){
                    R.id.item1 ->{
                        processarSelecao(0)
                        true
                    }

                    R.id.item2 ->{
                        processarSelecao(1)
                        true
                    }

                    R.id.item3 ->{
                        processarSelecao(2)
                        true
                    }

                    else ->{
                        false
                    }
                }
            }
            popupMenu.show()
        }
        /*
        btnNovoTermo.setOnClickListener {
            itemSorteado = descricoes[Random.nextInt(descricoes.size)]
            textoSorteado.text = ""
            //textoSorteado.text = itemSorteado
        }
        */
    }

    val brNumberSystem = mapOf(
        "ZERO" to 0, "UM" to 1, "UMA" to 1,
        "DOIS" to 2, "DUAS" to 2, "TRÊS" to 3,
        "QUATRO" to 4, "CINCO" to 5, "SEIS" to 6,
        "SETE" to 7, "OITO" to 8, "NOVE" to 9,
        "DEZ" to 10, "ONZE" to 11, "DOZE" to 12,
        "TREZE" to 13, "CATORZE" to 14, "QUINZE" to 15,
        "DEZESSEIS" to 16, "DEZESSETE" to 17, "DEZOITO" to 18,
        "DEZENOVE" to 19, "VINTE" to 20, "TRINTA" to 30,
        "QUARENTA" to 40, "CINQUENTA" to 50, "SESSENTA" to 60,
        "SETENTA" to 70, "OITENTA" to 80, "NOVENTA" to 90,
        "CEM" to 100, "CENTO" to 100
    )

    private fun solicitarPermissoes() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun configurarReconhecimento() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onResults(results: Bundle?) {
                val palavras = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!palavras.isNullOrEmpty()) {
                    val textoCapturado = palavras.joinToString(" ").uppercase()
                    var (parteNumerica, restante) =dividirFrase(textoCapturado)

                    if (parteNumerica is String){
                        parteNumerica = formarNumero(parteNumerica.split(" ")).toString()
                    }

                    parteNumericaGlobal = parteNumerica
                    restanteGlobal = restante

                    //dividir a string
                    inputTexto.text = textoCapturado + "..\n" + parteNumericaGlobal + "..\n" + restanteGlobal

                    btnFuzz.performClick()
                }
            }

            override fun onError(error: Int) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun iniciarReconhecimentoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        }
        speechRecognizer.startListening(intent)
    }

    private fun levenshteinSimilarity(a: String, b: String): Double {
        val distance = LevenshteinDistance().apply(a, b)
        val maxLen = maxOf(a.length, b.length)
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }

    private fun mostraTabela(infocoluna1: MutableList<List<String>>, infocoluna2: MutableList<String>, tabelaFinal: Boolean) {
        val limiteCaracteres = arrayOf(20, 15)
        //val format = NumberFormat.getInstance(Locale("pt", "BR"))
        val format = DecimalFormat("#,##0.00")
        val somatorio = infocoluna1
            .drop(1)
            .sumOf {
                val valorStr = it.last()
                try {
                    //formatadorBR.parse(valorStr)?.toDouble() ?: 0.0
                    format.parse(valorStr)?.toDouble()?.div(100) ?: 0.0//gambiarra celular
                    //gambiarra do krl
                } catch (e: Exception) {
                    0.0
                }
            }
        //.sumOf { it.last().toDoubleOrNull() ?: 0.0 }


        for ((linhaIndex, linha) in infocoluna1.withIndex()) {
            val tableRow = TableRow(this)

            for ((colunaIndex, coluna) in linha.withIndex()) {
                val cell = TextView(this)
                val limite = limiteCaracteres.getOrNull(colunaIndex) ?: coluna.length

                // Trunca texto e adiciona "..." se necessário
                /*
                val textoExibido = if (coluna.length > limite && tabelaFinal) {
                    coluna.take(limite) + "…"
                } else {
                    coluna
                }
                 */
                //código do gepeto
                val textoExibido = if (colunaIndex == linha.lastIndex && tabelaFinal && linhaIndex != 0) {
                    // Se for a última coluna (valor), e não for a linha de cabeçalho
                    try {
                        val valor = format.parse(coluna)?.toDouble()?.div(100) ?: 0.0//gambiarra celular
                        format.format(valor)
                    } catch (e: Exception) {
                        coluna
                    }
                } else if (coluna.length > limite && tabelaFinal) {
                    coluna.take(limite) + "…"
                } else {
                    coluna
                }

                cell.text = textoExibido
                cell.setPadding(16, 16, 16, 16)
                cell.setBackgroundResource(android.R.drawable.editbox_background)

                if (colunaIndex == 0) {
                    val widthInDp = 100
                    val scale = resources.displayMetrics.density
                    val widthInPx = (widthInDp * scale + 0.5f).toInt()
                    cell.layoutParams = TableRow.LayoutParams(widthInPx, TableRow.LayoutParams.WRAP_CONTENT)
                } else {
                    cell.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                }

                tableRow.addView(cell)

                // ✅ INSERE a coluna "Qtd" logo após a coluna 0 ("Descrição")
                if (colunaIndex == 0 && tabelaFinal && linhaIndex < infocoluna2.size) {
                    val qtdCell = TextView(this)
                    qtdCell.text = infocoluna2[linhaIndex]
                    qtdCell.setPadding(16, 16, 16, 16)
                    qtdCell.setBackgroundResource(android.R.drawable.editbox_background)
                    qtdCell.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                    tableRow.addView(qtdCell)
                }
            }

            resultadoTexto.addView(tableRow)
        }

        val linhaTotais = TableRow(this)
        for (i in infocoluna1[0].indices) {
            val cell = TextView(this)
            cell.setPadding(16, 16, 16, 16)

            if(tabelaFinal){
                when (i) {
                    infocoluna1[0].lastIndex - 1 -> cell.text = "Total:"
                    infocoluna1[0].lastIndex -> cell.text = "%.2f".format(somatorio)
                    else -> cell.text = ""
                }

                cell.setTypeface(null, Typeface.BOLD_ITALIC)
                cell.setBackgroundColor(Color.parseColor("#E0F7FA"))
            }
            if (i == 0) {
                val widthInDp = 100
                val scale = resources.displayMetrics.density
                val widthInPx = (widthInDp * scale + 0.5f).toInt()
                cell.layoutParams = TableRow.LayoutParams(widthInPx, TableRow.LayoutParams.WRAP_CONTENT)
            } else {
                cell.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }

            linhaTotais.addView(cell)
        }

        resultadoTexto.addView(linhaTotais)
        //resultadoTexto[resultadoTexto.inde - 1] = "Total:
    }

    fun formarNumero(palavras: List<String>): Int {
        var soma = 0
        for (palavra in palavras) {
            if (palavra == "E") continue
            val numero = brNumberSystem[palavra] ?: palavra.toIntOrNull()
            if (numero != null) soma += numero
        }
        return soma
    }

    fun dividirFrase(frase: String): Pair<String, String> {
        val palavras = frase.uppercase().split(" ", "-")
        var excluir = 0

        for ((i, palavra) in palavras.withIndex()) {
            if (palavra == "e") continue

            // Verifica se é número por extenso ou algarismo
            if (brNumberSystem.containsKey(palavra) || palavra.toIntOrNull() != null) {
                excluir = i + 1
            } else {
                break
            }
        }

        val parteNumerica = palavras.take(excluir).joinToString(" ")
        val restante = palavras.drop(excluir).joinToString(" ")

        return Pair(parteNumerica, restante)
    }

    private fun salvarDadosCSV(
        infocoluna1: MutableList<List<String>>,
        infocoluna2: MutableList<String>,
    ) {
        val format = DecimalFormat("#,##0.00")
        val somatorio = infocoluna1
            .drop(1)
            .sumOf {
                val valorStr = it.last()
                try {
                    //formatadorBR.parse(valorStr)?.toDouble() ?: 0.0
                    format.parse(valorStr)?.toDouble()?.div(100) ?: 0.0//gambiarra celular
                    //gambiarra do krl
                } catch (e: Exception) {
                    0.0
                }
            }

        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val csvDir = File(directory, "Arquivos CSV")
        if (!csvDir.exists()) {
            csvDir.mkdirs()
        }

        var fileNumber = 1
        var file: File
        do {
            val fileName = "Faturamento$fileNumber.csv"
            file = File(csvDir, fileName)
            fileNumber++
        } while (file.exists())

        try {
            val arquivo = File(csvDir, "nomeArquivo.csv")
            FileWriter(file).use { writer ->
                for ((i, linha) in infocoluna1.withIndex()) {
                    // Pegamos a quantidade da infocoluna2 (se existir para essa linha)
                    val qtd = if (i < infocoluna2.size) infocoluna2[i] else ""

                    // Criamos uma linha CSV unindo as duas fontes
                    val descricao = linha[0]
                    val valor = linha[1].toDoubleOrNull()?.div(100.0)?.let { String.format("%.2f", it).replace(',', '.') } ?: linha[1]

                    val linhaCompleta = listOf(descricao, valor, qtd)

                        // Escrevemos no arquivo, separando por vírgula
                    writer.append(linhaCompleta.joinToString(","))
                    writer.append("\n")
                }
                writer.append("Total:,$somatorio, -")
            }
            println("Arquivo CSV salvo em: ${arquivo.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

}
