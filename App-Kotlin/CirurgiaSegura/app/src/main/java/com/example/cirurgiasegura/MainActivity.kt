package com.example.cirurgiasegura


import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cirurgiasegura.R
import com.example.cirurgiasegura.data.AuthRepository
import com.example.cirurgiasegura.data.CheckboxQuestion
import com.example.cirurgiasegura.data.MultipleChoiceQuestion
import com.example.cirurgiasegura.data.PdfUploadRepository
import com.example.cirurgiasegura.data.Question
import com.example.cirurgiasegura.data.SalvaTempo
import com.example.cirurgiasegura.data.TextInputQuestion
import com.example.cirurgiasegura.services.VoiceCommandProcessor
import com.example.cirurgiasegura.services.VoiceRecognizer
import com.example.cirurgiasegura.ui.AnswerExtractor
import com.example.cirurgiasegura.ui.QuestionViewFactory
import com.example.cirurgiasegura.utils.CsvDataSaver
import com.example.cirurgiasegura.utils.FileUtil
import com.example.cirurgiasegura.utils.PermissionManager
import com.example.cirurgiasegura.viewmodel.QuizStateManager
import com.example.cirurgiasegura.ui.PdfFlowManager
import java.io.File

class MainActivity : AppCompatActivity() {

    // Edições para uso de novo formulário


    annotation class LoginResponse

    //Parou, jogador
    private var campoSelecionado: EditText? = null


    // Gerenciadores e Serviços
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    private var questions: List<String> = emptyList()

    private var stateManager = QuizStateManager()
    private lateinit var viewFactory: QuestionViewFactory
    private val answerExtractor = AnswerExtractor()
    private lateinit var dataSaver: CsvDataSaver
    private lateinit var voiceRecognizer: VoiceRecognizer
    private val commandProcessor = VoiceCommandProcessor()

    // Views da UI
    private lateinit var container: FrameLayout
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button

    private lateinit var btnFalar: Button

    private lateinit var btnHowto: Button
    private lateinit var textView: TextView
    private lateinit var currentQuestionView: View

    private lateinit var jwtToken: String
    private val authRepository = AuthRepository() // Crie uma instância do repositório

    private var formulario_novo: Boolean = false


    private lateinit var pdfManager: PdfFlowManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //questions = intent.getStringArrayListExtra("questions")
        val questionsFromIntent = intent.getStringArrayListExtra("questions") ?: emptyList()
        val nomeForms = intent.getStringExtra("nomeArquivo")

        //val questions: List<String> = questionsFromIntent
        val questions: List<String> = questionsFromIntent

        //val questionsInQuestion: List<TextInputQuestion> =
        //    questionsFromIntent.map { questionText -> TextInputQuestion(questionText) }
        /*
        val questionsInQuestion = questionsFromIntent.map {
            questionText -> if (questionText.contains(":")){
                if (questionText.contains("/")) MultipleChoiceQuestion(
                    questionText,
                    questionText.substringAfter(":").split("/"),
                )
            }
            else TextInputQuestion(questionText)
        }
        */
        val questionsInQuestion: List<Question> = questionsFromIntent.map { text ->
            when {
                text.contains(":") and text.contains("/") ->{
                    MultipleChoiceQuestion(text.substringBefore(":"), text.substringAfter(":").split("/").map {it.trim()})
                }
                /*
                text.contains(":") and text.contains("|") ->{
                    CheckboxQuestion(text.substringBefore(":"), text.substringAfter(":").split("|").map {it.trim()})
                }
                */
                // Para o caso em que haja apenas uma coisa pra marcar(coloca após a que usa o and pra não cair sempre nessa):
                text.contains(":")->{
                    CheckboxQuestion(text.substringBefore(":"), text.substringAfter(":").split("|").map {it.trim()})
                }
                text.startsWith("*") -> {
                    SalvaTempo(text.substringAfter("*"))
                }
                else -> TextInputQuestion(text)
            }
        }


        if(questions.isEmpty()){
            stateManager = QuizStateManager()
            formulario_novo = false
        }
        else{
            stateManager = QuizStateManager(questionsInQuestion)
            formulario_novo = true
        }

        // Inicializa classes que dependem do Context
        viewFactory = QuestionViewFactory(this)
        if(!formulario_novo)    dataSaver = CsvDataSaver(this, null)
        else    dataSaver = CsvDataSaver(this, nomeForms)
        voiceRecognizer = VoiceRecognizer(this)
        // Passamos 'this' (activity), o repositório e uma função lambda para atualizar o texto
        val uploadRepository = PdfUploadRepository(this)

        pdfManager = PdfFlowManager(
            activity = this,
            uploadRepository = uploadRepository,
            onStatusUpdate = { mensagem ->
                // Esta função roda sempre que o Manager quiser falar algo
                textView.text = mensagem
            }
        )

        // Encontra Views
        textView = findViewById(R.id.textView)
        container = findViewById(R.id.question_container)
        btnNext = findViewById(R.id.btn_next)
        btnPrevious = findViewById(R.id.btn_previous)
        btnFalar = findViewById(R.id.btnFalar)
        btnHowto = findViewById(R.id.btnHowto)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        // Configura Listeners
        btnNext.setOnClickListener { onNextClicked() }
        btnPrevious.setOnClickListener { onPreviousClicked() }
        btnFalar.setOnClickListener { voiceRecognizer.startListening() }
        btnHowto.setOnClickListener { mostraInstrucoes() }

        fazerLogin {
            // Este código aqui (onSuccess) só roda
            // depois que o login for bem-sucedido
            textView.text = "Login feito com sucesso!"
            //Log.d("Login", "Login OK, token pronto para uso.")
            // Você pode, por exemplo, habilitar botões aqui
        }

        setupVoiceListener()

        // Inicia
        PermissionManager.checkAndRequestAudioPermission(this)
        showCurrentQuestion()
        //mostraInstrucoes()
    }

    private fun fazerLogin(onSuccess: () -> Unit) {
        // Mostra o status para o usuário ANTES de começar
        textView.text = "Autenticando..."

        authRepository.login(object : AuthRepository.AuthCallback {

            override fun onSuccess(token: String) {
                // SUCESSO!
                jwtToken = token // Salva o token na Activity
                textView.text = "Login automático realizado" // Atualiza a UI
                onSuccess() // Chama o callback original (ex: habilitar botões)
            }

            override fun onError(message: String) {
                // ERRO!
                textView.text = message // Mostra o erro na UI
            }
        })
    }

    private fun setupVoiceListener() {
        voiceRecognizer.setListener(object : VoiceRecognizer.Listener {
            override fun onReady() { textView.text = "Fale agora..." }
            override fun onListening() { textView.text = "Ouvindo..." }
            override fun onProcessing() { textView.text = "Processando..." }
            override fun onError(error: String) {
                textView.text = error
                // Opcional: reiniciar automaticamente
                // voiceRecognizer.startListening()
            }

            override fun onResult(text: String) {
                textView.text = "Você disse: $text"

                // 1. PRIORIDADE: Se o usuário clicou num campo, preenche direto
                if (campoSelecionado != null) {
                    campoSelecionado?.setText(text)
                    campoSelecionado = null // Limpa para a próxima interação
                    return // Encerra aqui, não processa comandos
                }

                // 2. DEFAULT: Botão falar, usa a lógica de comandos de voz
                val question = stateManager.getCurrentQuestion()
                val shouldGoNext = commandProcessor.processCommand(text, question, currentQuestionView)

                if (shouldGoNext) {
                    //btnNext.performClick()
                    onNextClicked() //Evita dar o balão
                }
            }
        })
    }

    private fun onNextClicked() {
        saveCurrentAnswer()

        if (stateManager.isQuizFinished()) {
            showQuizFinishedDialog()
        } else {
            stateManager.moveToNextQuestion()
            showCurrentQuestion()
        }
    }
    private fun onPreviousClicked() {

        saveCurrentAnswer()
        stateManager.moveToPreviousQuestion()
        showCurrentQuestion()

        /*if (stateManager.isQuizFinished()) {
            showQuizFinishedDialog()
        } else {
            stateManager.moveToNextQuestion()
            showCurrentQuestion()
        }
         */
    }

    // Atualize a função que exibe a questão
    private fun showCurrentQuestion() {
        val question = stateManager.getCurrentQuestion()

        // Pegamos a resposta da questão principal e a função para buscar das subperguntas
        val savedValue = stateManager.getSavedAnswer(question)

        currentQuestionView = viewFactory.createView(
            question,
            container,
            savedValue,
            { subQ -> stateManager.getSavedAnswer(subQ) } // Provedor de respostas para filhos
        )

        container.removeAllViews()
        container.addView(currentQuestionView)
        configurarCliquesNosInputs(currentQuestionView)
        val current = stateManager.getCurrentIndex() + 1
        val total = stateManager.getTotalQuestions()
        progressBar.max = total
        progressBar.progress = current
        progressText.text = "$current/$total"
    }

    private fun saveCurrentAnswer() {
        val question = stateManager.getCurrentQuestion()
        val answer = answerExtractor.extractAnswer(question, currentQuestionView)
        stateManager.saveAnswer(answer)
    }

    private fun showQuizFinishedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Fim das perguntas")
            .setMessage("Você completou todas as perguntas!\nSalve o formulário para anexar o PDF.")
            .setPositiveButton("Salvar e Anexar PDF") { _, _ ->

                dataSaver.saveAnswers(stateManager.getFormattedAnswers()) { savedFile ->
                    if (savedFile != null) {

                        // 3. Configure o Manager com os dados atuais
                        pdfManager.currentCsvFile = savedFile
                        pdfManager.currentJwtToken = jwtToken

                        // 4. Inicie a seleção (Isso substitui abrirSeletorDePdf)
                        if(!formulario_novo){
                            pdfManager.startPdfSelection()
                        }

                    } else {
                        textView.text = "Falha ao salvar CSV. Processo cancelado."
                    }
                }

                //dataSaver.saveAnswers(stateManager.getFormattedAnswers())

                stateManager.reset()
                showCurrentQuestion()
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManager.AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "A permissão de áudio é necessária para o app funcionar", Toast.LENGTH_LONG).show()
                // Você pode desabilitar o botão de falar aqui
                btnFalar.isEnabled = false
            }
        }
    }

    private fun mostraInstrucoes() {
        val alert = AlertDialog.Builder(this)
            .setTitle("Como Utilizar o aplicativo")
            .setMessage("Aperte o botão 'Falar' e pronuncie a informação referente ao campo mostrado na parte superior da tela.\n" +
                    "Caso necessário, utilize o teclado virtual para eventuais correções ou marcações para campos objetivos")
            .setPositiveButton("Fechar") { _, _ ->

            }
            //.setNegativeButton("Não enviar agora", null)
            .create()

        alert.show()
    }
    private fun configurarCliquesNosInputs(view: View) {
        // Se a view for um EditText, configura o clique
        if (view is EditText) {
            view.setOnLongClickListener {
                campoSelecionado = view // Marca este campo como o alvo da voz
                voiceRecognizer.startListening()
                //Toast.makeText(this, "Ouvindo para: ${view.hint ?: "este campo"}", Toast.LENGTH_SHORT).show()

                true
                //Para o view.setOnLongClickListener: 'Type mismatch: inferred type is 'Unit', but 'Boolean' was expected.'
            }
        }
        // Se for um container (LinearLayout/Group), procura dentro dele (Recursivo)
        else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                configurarCliquesNosInputs(view.getChildAt(i))
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizer.destroy() // Libera recursos do SpeechRecognizer
    }
}