package com.ferdaoues.eyesopen.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ferdaoues.eyesopen.R
import com.ferdaoues.eyesopen.databinding.ActivityHomeBinding
import com.ferdaoues.eyesopen.settings.AppSettings
import com.ferdaoues.eyesopen.speech.AndroidSpeech
import com.ferdaoues.eyesopen.speech.SpeechOutput
import kotlin.math.roundToInt

/**
 * Welcome screen, designed for visually impaired users: one very large
 * "Start" button, a spoken welcome message and simple settings.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var settings: AppSettings
    private lateinit var speech: SpeechOutput

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = AppSettings(this)
        speech = AndroidSpeech(this)

        bindSettings()

        binding.startButton.setOnClickListener {
            speech.stop()
            startActivity(Intent(this, DetectorActivity::class.java))
        }

        if (savedInstanceState == null && settings.voiceEnabled) {
            speech.speak(getString(R.string.welcome_spoken))
        }
    }

    private fun bindSettings() = with(binding) {
        voiceSwitch.isChecked = settings.voiceEnabled
        voiceSwitch.setOnCheckedChangeListener { _, checked -> settings.voiceEnabled = checked }

        positionSwitch.isChecked = settings.announcePosition
        positionSwitch.setOnCheckedChangeListener { _, checked -> settings.announcePosition = checked }

        vibrateSwitch.isChecked = settings.vibrate
        vibrateSwitch.setOnCheckedChangeListener { _, checked -> settings.vibrate = checked }

        confidenceSlider.value = (settings.minConfidence * 100).roundToInt().toFloat()
        renderConfidence(confidenceSlider.value)
        confidenceSlider.addOnChangeListener { _, value, _ ->
            settings.minConfidence = value / 100f
            renderConfidence(value)
        }
    }

    private fun renderConfidence(value: Float) {
        binding.confidenceValue.text = getString(R.string.confidence_value, value.roundToInt())
    }

    override fun onDestroy() {
        super.onDestroy()
        speech.shutdown()
    }
}
