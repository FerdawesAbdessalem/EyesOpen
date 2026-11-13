package com.ferdaoues.eyesopen.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ferdaoues.eyesopen.R
import com.ferdaoues.eyesopen.databinding.ActivityDetectorBinding
import com.ferdaoues.eyesopen.detection.Detection
import com.ferdaoues.eyesopen.detection.ObjectDetector
import com.ferdaoues.eyesopen.detection.TfliteObjectDetector
import com.ferdaoues.eyesopen.detection.isClose
import com.ferdaoues.eyesopen.detection.position
import com.ferdaoues.eyesopen.settings.AppSettings
import com.ferdaoues.eyesopen.speech.AndroidSpeech
import com.ferdaoues.eyesopen.speech.Announcer
import com.ferdaoues.eyesopen.speech.Phrases
import com.ferdaoues.eyesopen.speech.Sighting
import com.ferdaoues.eyesopen.speech.SpeechOutput
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Live camera screen: CameraX preview + on-device detection on a background
 * thread (the first version ran the model on the UI thread), boxes drawn on an
 * overlay, and spoken announcements that do not repeat endlessly.
 */
class DetectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetectorBinding
    private lateinit var settings: AppSettings
    private lateinit var speech: SpeechOutput
    private lateinit var announcer: Announcer
    private lateinit var analysisExecutor: ExecutorService

    @Volatile private var detector: ObjectDetector? = null
    private var camera: Camera? = null
    private var torchOn = false

    @Volatile private var latest: List<Detection> = emptyList()

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = AppSettings(this)
        speech = AndroidSpeech(this)
        announcer = Announcer(
            Phrases(
                left = getString(R.string.position_left),
                ahead = getString(R.string.position_ahead),
                right = getString(R.string.position_right),
                veryClose = getString(R.string.very_close),
                nothing = getString(R.string.nothing_detected),
                and = getString(R.string.and_word),
            )
        )
        analysisExecutor = Executors.newSingleThreadExecutor()

        try {
            detector = TfliteObjectDetector(this)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.model_error, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setUpControls()

        if (hasCameraPermission()) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun setUpControls() = with(binding) {
        toolbar.setNavigationOnClickListener { finish() }

        describeButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            speech.speak(announcer.describeScene(latest.toSightings(), settings.announcePosition), interrupt = true)
        }

        renderVoiceButton()
        voiceButton.setOnClickListener {
            settings.voiceEnabled = !settings.voiceEnabled
            if (!settings.voiceEnabled) speech.stop() else announcer.reset()
            renderVoiceButton()
        }

        torchButton.setOnClickListener {
            torchOn = !torchOn
            camera?.cameraControl?.enableTorch(torchOn)
            torchButton.setIconResource(if (torchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
            torchButton.contentDescription = getString(if (torchOn) R.string.torch_off else R.string.torch_on)
        }

        retryPermissionButton.setOnClickListener { cameraPermission.launch(Manifest.permission.CAMERA) }
    }

    private fun renderVoiceButton() {
        val on = settings.voiceEnabled
        binding.voiceButton.setIconResource(if (on) R.drawable.ic_volume_on else R.drawable.ic_volume_off)
        binding.voiceButton.text = getString(if (on) R.string.voice_on else R.string.voice_off)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionDenied() {
        binding.permissionPanel.visibility = View.VISIBLE
        speech.speak(getString(R.string.permission_needed), interrupt = true)
    }

    private fun startCamera() {
        binding.permissionPanel.visibility = View.GONE
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(analysisExecutor, ::analyze) }

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                binding.torchButton.visibility =
                    if (camera?.cameraInfo?.hasFlashUnit() == true) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this, R.string.camera_error, Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /** Runs on [analysisExecutor], never on the UI thread. */
    private fun analyze(image: ImageProxy) {
        image.use { proxy ->
            val bitmap = proxy.toBitmap().rotate(proxy.imageInfo.rotationDegrees)
            val results = detector?.detect(bitmap, settings.minConfidence).orEmpty()
            latest = results

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.overlay.show(results, bitmap.width, bitmap.height)
                renderSummary(results)
                announce(results)
            }
        }
    }

    private fun renderSummary(results: List<Detection>) {
        binding.summaryText.text = if (results.isEmpty()) {
            getString(R.string.scanning)
        } else {
            results.distinctBy { it.label }.take(3).joinToString(" · ") { it.label }
        }
    }

    private fun announce(results: List<Detection>) {
        if (!settings.voiceEnabled) return
        val sentence = announcer.onFrame(results.toSightings(), settings.announcePosition) ?: return
        speech.speak(sentence)
        if (settings.vibrate) binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun List<Detection>.toSightings() = map { Sighting(it.label, it.position(), it.isClose()) }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    override fun onPause() {
        super.onPause()
        speech.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the model on the analysis thread, after any frame still being processed.
        val toClose = detector
        detector = null
        analysisExecutor.execute { toClose?.close() }
        analysisExecutor.shutdown()
        speech.shutdown()
    }
}
