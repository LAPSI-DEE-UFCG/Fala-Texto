package ufcg.example.voicesurgery

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voicesurgery.R
import ufcg.example.voicesurgery.services.VoiceCommandProcessor
import ufcg.example.voicesurgery.services.VoiceRecognizer
import ufcg.example.voicesurgery.ui.AnswerExtractor
import ufcg.example.voicesurgery.ui.QuestionViewFactory
import ufcg.example.voicesurgery.utils.CsvDataSaver
import ufcg.example.voicesurgery.utils.PermissionManager
import ufcg.example.voicesurgery.viewmodel.QuizStateManager

class MainActivity : AppCompatActivity() {

    // Gerenciadores e Serviços
    private val stateManager = QuizStateManager()
    private lateinit var viewFactory: QuestionViewFactory
    private val answerExtractor = AnswerExtractor()
    private lateinit var dataSaver: CsvDataSaver
    private lateinit var voiceRecognizer: VoiceRecognizer
    private val commandProcessor = VoiceCommandProcessor()

    // Views da UI
    private lateinit var container: FrameLayout
    private lateinit var btnNext: Button
    private lateinit var btnFalar: Button
    private lateinit var textView: TextView
    private lateinit var currentQuestionView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa classes que dependem do Context
        viewFactory = QuestionViewFactory(this)
        dataSaver = CsvDataSaver(this)
        voiceRecognizer = VoiceRecognizer(this)

        // Encontra Views
        textView = findViewById(R.id.textView)
        container = findViewById(R.id.question_container)
        btnNext = findViewById(R.id.btn_next)
        btnFalar = findViewById(R.id.btnFalar)

        // Configura Listeners
        btnNext.setOnClickListener { onNextClicked() }
        btnFalar.setOnClickListener { voiceRecognizer.startListening() }
        setupVoiceListener()

        // Inicia
        PermissionManager.checkAndRequestAudioPermission(this)
        showCurrentQuestion()
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
                val question = stateManager.getCurrentQuestion()
                val shouldGoNext = commandProcessor.processCommand(text, question, currentQuestionView)

                if (shouldGoNext) {
                    btnNext.performClick()
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

    private fun showCurrentQuestion() {
        val question = stateManager.getCurrentQuestion()
        currentQuestionView = viewFactory.createView(question, container)
        container.removeAllViews()
        container.addView(currentQuestionView)
    }

    private fun saveCurrentAnswer() {
        val question = stateManager.getCurrentQuestion()
        val answer = answerExtractor.extractAnswer(question, currentQuestionView)
        stateManager.saveAnswer(answer)
    }

    private fun showQuizFinishedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Fim das perguntas")
            .setMessage("Você completou todas as perguntas!")
            .setPositiveButton("Salvar e Recomeçar") { _, _ ->
                dataSaver.saveAnswers(stateManager.getFormattedAnswers())
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
            if (grantResults.isEmpty() || grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "A permissão de áudio é necessária para o app funcionar", Toast.LENGTH_LONG).show()
                // Você pode desabilitar o botão de falar aqui
                btnFalar.isEnabled = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizer.destroy() // Libera recursos do SpeechRecognizer
    }
}