package com.ferdaoues.eyesopen.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** Something that can speak a sentence (TextToSpeech in the app, a fake in tests). */
interface SpeechOutput {
    fun speak(text: String, interrupt: Boolean = false)
    fun stop()
    fun shutdown()
}

/** Android TextToSpeech, in the device language (falls back to French, then English). */
class AndroidSpeech(context: Context, private val rate: Float = 1.0f) : SpeechOutput {

    private var ready = false
    private val pending = mutableListOf<String>()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            configureLanguage()
            ready = true
            pending.forEach { speakNow(it, interrupt = false) }
            pending.clear()
        } else {
            Log.e(TAG, "Initialisation de la synthèse vocale impossible ($status)")
        }
    }

    private fun configureLanguage() {
        val candidates = listOf(Locale.getDefault(), Locale.FRENCH, Locale.ENGLISH)
        candidates.firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
            ?.let { tts.language = it }
        tts.setSpeechRate(rate)
    }

    override fun speak(text: String, interrupt: Boolean) {
        if (!ready) {
            pending += text
            return
        }
        speakNow(text, interrupt)
    }

    private fun speakNow(text: String, interrupt: Boolean) {
        val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(text, mode, null, text.hashCode().toString())
    }

    override fun stop() {
        tts.stop()
    }

    override fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "AndroidSpeech"
    }
}
