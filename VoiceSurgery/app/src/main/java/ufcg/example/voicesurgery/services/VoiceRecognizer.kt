package ufcg.example.voicesurgery.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceRecognizer(context: Context) {

    interface Listener {
        fun onReady()
        fun onListening()
        fun onProcessing()
        fun onResult(text: String)
        fun onError(error: String)
    }

    private var speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var listener: Listener? = null

    init {
        setupListener()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private fun setupListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listener?.onReady() }
            override fun onBeginningOfSpeech() { listener?.onListening()}
            override fun onEndOfSpeech() { listener?.onProcessing() }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onResults(results: Bundle?) {
                val palavras = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!palavras.isNullOrEmpty()) {
                    listener?.onResult(palavras.joinToString(" "))
                }
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão insuficiente"
                    SpeechRecognizer.ERROR_NETWORK -> "Erro de rede"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma fala reconhecida"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhum som detectado"
                    else -> "Erro desconhecido: $error"
                }
                listener?.onError(errorMsg)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        }
        speechRecognizer.startListening(intent)
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}