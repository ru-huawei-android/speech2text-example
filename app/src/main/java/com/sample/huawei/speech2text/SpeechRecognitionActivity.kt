package com.sample.huawei.speech2text

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.huawei.hms.mlsdk.asr.MLAsrConstants
import com.huawei.hms.mlsdk.asr.MLAsrListener
import com.huawei.hms.mlsdk.asr.MLAsrRecognizer
import com.huawei.hms.mlsdk.common.MLApplication
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.sample.huawei.speech2text.databinding.ActivitySpeechRecognitionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class SpeechRecognitionActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySpeechRecognitionBinding.inflate(layoutInflater)
    }
    private val speechRecognizer = MLAsrRecognizer.createAsrRecognizer(this)
    private var isMicButtonPressed: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        requestPermissionsIfNeeded()
        MLApplication.getInstance().apiKey = API_KEY
        setMicButtonTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setMicButtonTouchListener() {
        speechRecognizer.setAsrListener(SpeechRecognitionListener())

        val speechRecognizerIntent = Intent(MLAsrConstants.ACTION_HMS_ASR_SPEECH)
        speechRecognizerIntent.putExtra(MLAsrConstants.LANGUAGE, "ru")

        with(binding) {
            mic.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mic.alpha = 1f
                        recognizedText.text = ". . ."
                        speechRecognizer.startRecognizing(speechRecognizerIntent)
                        isMicButtonPressed = true
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        isMicButtonPressed = false
                        true
                    }
                    MotionEvent.ACTION_BUTTON_PRESS -> false
                    else -> false
                }
            }
        }
    }

    internal inner class SpeechRecognitionListener : MLAsrListener {
        override fun onStartListening() {
            logd("Started Listening")
        }

        override fun onStartingOfSpeech() {
            logd("Started Speaking")
        }

        // Return the original PCM stream and audio power to the user.
        // This API is not running in the main thread, and the return
        // result is processed in the sub-thread.
        override fun onVoiceDataReceived(data: ByteArray, energy: Float, bundle: Bundle) { }

        // Called each time when ML model suggests new result
        // This API is not running in the main thread, and the return
        // result is processed in the sub-thread.
        override fun onRecognizingResults(partialResults: Bundle) {
            val text = partialResults.getString(MLAsrRecognizer.RESULTS_RECOGNIZING) ?: ""
            showRecognizedText(text)
        }

        // Final ASR results
        // This API is not running in the main thread, and the return
        // result is processed in the sub-thread.
        override fun onResults(results: Bundle) {
            val text = results.getString(MLAsrRecognizer.RESULTS_RECOGNIZED) ?: ""
            showRecognizedText(text)
            if(!isMicButtonPressed) {
                speechRecognizer.destroy()
                with(binding) {
                    mic.alpha = .5f
                    stateTextView.text = getString(R.string.hint)
                }
            }
        }

        // This API is not running in the main thread,
        // and the return result is processed in the sub-thread.
        override fun onError(error: Int, errorMessage: String) =
            logd("Error $error: $errorMessage")

        // Notify the app status change.
        // This API is not running in the main thread,
        // and the return result is processed in the sub-thread.
        override fun onState(state: Int, params: Bundle) {
            val message = when(state) {
                MLAsrConstants.STATE_LISTENING -> "Listening..."
                MLAsrConstants.STATE_NO_NETWORK -> "No network"
                MLAsrConstants.STATE_NO_SOUND -> "No sound"
                MLAsrConstants.STATE_NO_SOUND_TIMES_EXCEED -> "Silence"
                MLAsrConstants.STATE_NO_UNDERSTAND-> "Not recognized"
                else -> ""
            }
            logd("state: $message")
        }

        private fun logd(message: String) {
            CoroutineScope(Main).launch {
                Log.d(TAG, message)
                binding.stateTextView.text = message
            }
        }

        private fun showRecognizedText(text: String) {
            CoroutineScope(Main).launch {
                Log.d(TAG, "Text recognized: $text")
                binding.recognizedText.text = text
            }
        }
    }


    private fun requestPermissionsIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO
        )
        if(!checkPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(!checkPermissions(permissions)) {
                requestPermissionsIfNeeded()
            }
        }
    }

    private fun checkPermissions(permissions: Array<out String>): Boolean {
        for(permission in permissions) {
            if(ActivityCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }


    companion object {
        const val TAG = "SpeechRecognition"
        const val API_KEY = "CgB6e3x9n4OP0BrZO/rk6xVNRc/RRUa0i7JaPIAu8FeFH8/6QPbscKxjjlCmOxBI7PsRczBPg1gUiChgVe9CLVBe"
        const val PERMISSION_REQUEST_CODE = 102323
    }
}