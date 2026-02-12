package com.example.echoverse.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

class AudioSourceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    // Flag to control capture loop
    @Volatile
    private var isCapturing = false
    
    // Track current audio source mode
    private enum class AudioSourceMode {
        MUSIC,      // Using Visualizer for system audio
        MICROPHONE  // Using microphone
    }

    /**
     * Intelligent audio stream that switches between music (Visualizer) and microphone.
     * Uses Visualizer when music is playing, switches to microphone when music stops.
     */
    fun getAudioStream(): Flow<ByteArray> = flow {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AudioSourceManager", "Permission not granted")
            return@flow
        }

        isCapturing = true
        var currentMode: AudioSourceMode? = null
        var visualizer: Visualizer? = null
        var audioRecord: AudioRecord? = null
        var lastModeChangeTime = 0L
        val MODE_CHANGE_COOLDOWN = 2000L // 2 seconds cooldown between mode changes
        
        try {
            while (coroutineContext.isActive && isCapturing) {
                val isMusicActive = audioManager.isMusicActive
                val desiredMode = if (isMusicActive) AudioSourceMode.MUSIC else AudioSourceMode.MICROPHONE
                val currentTime = System.currentTimeMillis()
                
                // Check if we need to switch modes
                if (currentMode != desiredMode && (currentTime - lastModeChangeTime) > MODE_CHANGE_COOLDOWN) {
                    Log.d("AudioSourceManager", "Switching from ${currentMode?.name ?: "NONE"} to ${desiredMode.name}")
                    
                    // Clean up current source
                    try {
                        visualizer?.enabled = false
                        visualizer?.release()
                        visualizer = null
                    } catch (e: Exception) {
                        Log.e("AudioSourceManager", "Error releasing visualizer: ${e.message}")
                    }
                    
                    try {
                        audioRecord?.stop()
                        audioRecord?.release()
                        audioRecord = null
                    } catch (e: Exception) {
                        Log.e("AudioSourceManager", "Error releasing audioRecord: ${e.message}")
                    }
                    
                    currentMode = desiredMode
                    lastModeChangeTime = currentTime
                    
                    // Initialize new source
                    when (desiredMode) {
                        AudioSourceMode.MUSIC -> {
                            visualizer = initializeVisualizer()
                            if (visualizer == null) {
                                Log.w("AudioSourceManager", "Failed to initialize Visualizer, using microphone")
                                currentMode = AudioSourceMode.MICROPHONE
                                audioRecord = initializeMicrophone()
                            } else {
                                Log.d("AudioSourceManager", "Using Visualizer - capturing music playback")
                            }
                        }
                        AudioSourceMode.MICROPHONE -> {
                            audioRecord = initializeMicrophone()
                            Log.d("AudioSourceManager", "Using Microphone - no music playing")
                        }
                    }
                }
                
                // Emit data from current source
                when (currentMode) {
                    AudioSourceMode.MUSIC -> {
                        visualizer?.let { viz ->
                            if (viz.enabled) {
                                val waveform = ByteArray(viz.captureSize)
                                val result = viz.getWaveForm(waveform)
                                if (result == Visualizer.SUCCESS) {
                                    // Convert waveform to proper format
                                    val converted = ByteArray(waveform.size * 2)
                                    for (i in waveform.indices) {
                                        val sample = (waveform[i].toInt() and 0xFF) - 128
                                        val scaled = (sample * 256).toShort()
                                        converted[i * 2] = (scaled.toInt() and 0xFF).toByte()
                                        converted[i * 2 + 1] = ((scaled.toInt() shr 8) and 0xFF).toByte()
                                    }
                                    emit(converted)
                                }
                            }
                        }
                    }
                    AudioSourceMode.MICROPHONE -> {
                        audioRecord?.let { record ->
                            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                                val buffer = ByteArray(record.bufferSizeInFrames * 2)
                                val readCount = record.read(buffer, 0, buffer.size)
                                if (readCount > 0) {
                                    emit(buffer.copyOf(readCount))
                                }
                            }
                        }
                    }
                    null -> {
                        // Initialize with default mode
                        Log.d("AudioSourceManager", "Initial mode selection...")
                        delay(100)
                    }
                }
                
                delay(50) // ~20 Hz update rate
            }
        } finally {
            // Cleanup
            try {
                visualizer?.enabled = false
                visualizer?.release()
            } catch (e: Exception) {
                Log.e("AudioSourceManager", "Error releasing visualizer: ${e.message}")
            }
            
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                Log.e("AudioSourceManager", "Error releasing audioRecord: ${e.message}")
            }
        }
    }

    private fun initializeVisualizer(): Visualizer? {
        return try {
            val viz = Visualizer(0)
            val captureSize = Visualizer.getCaptureSizeRange()[1]
            viz.captureSize = captureSize.coerceIn(128, 1024)
            viz.enabled = true
            viz
        } catch (e: Exception) {
            Log.e("AudioSourceManager", "Failed to initialize Visualizer: ${e.message}")
            null
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initializeMicrophone(): AudioRecord? {
        return try {
            sampleRate = getValidSampleRate()
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 2048)
            
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record.startRecording()
                record
            } else {
                Log.e("AudioSourceManager", "AudioRecord initialization failed")
                record.release()
                null
            }
        } catch (e: Exception) {
            Log.e("AudioSourceManager", "Failed to initialize microphone: ${e.message}")
            null
        }
    }

    private fun getValidSampleRate(): Int {
        for (rate in intArrayOf(44100, 48000, 22050, 16000, 11025, 8000)) {
            val bufferSize = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize > 0) {
                return rate
            }
        }
        return 44100 // Default
    }
    
    fun stopCapture() {
        isCapturing = false
    }
}
