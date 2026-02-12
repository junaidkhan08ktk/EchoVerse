package com.example.echoverse.domain.audio

import com.example.echoverse.domain.model.AudioState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class AudioAnalyzer {

    private val FFT_SIZE = 1024
    private val buffer = FloatArray(FFT_SIZE)
    private var bufferIndex = 0
    private var sampleRate = 44100

    private var smoothedBass = 0f
    private var smoothedMid = 0f
    private var smoothedHigh = 0f
    private var smoothedAmplitude = 0f

    // Smoothing factor (0.0 = no smoothing, 1.0 = no change)
    // Higher value = more smoothing (slower reaction)
    private val SMOOTHING_FACTOR = 0.5f 
    private val DECAY_FACTOR = 0.95f

    fun process(pcmData: ByteArray, rate: Int = 44100): AudioState {
        this.sampleRate = rate
        
        // Convert input bytes to floats and fill buffer
        // Note: this is a simple implementation that processes chunks as they come.
        // Ideally we'd use a circular buffer and window overlapping.
        
        // Assuming 16-bit PCM (2 bytes per sample)
        val shortBuffer = ShortArray(pcmData.size / 2)
        ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)
        
        var maxAmp = 0f
        var totalEnergy = 0f
        
        for (sample in shortBuffer) {
            val normalized = sample / 32768f
            maxAmp = max(maxAmp, abs(normalized))
            totalEnergy += normalized * normalized
            
            if (bufferIndex < FFT_SIZE) {
                buffer[bufferIndex++] = normalized
            } else {
                // Buffer full, process FFT?
                // For simplicity, we process the first valid chunk or just overwrite circular buffer style
                // Here we just discard excess for this simplified version, or return last state
                break 
            }
        }
        
        // Calculate RMS amplitude for better representation
        val rmsAmp = if (shortBuffer.isNotEmpty()) {
            sqrt(totalEnergy / shortBuffer.size)
        } else {
            0f
        }
        
        if (bufferIndex >= FFT_SIZE) {
            performFFT()
            bufferIndex = 0 // Reset for next batch
        }

        // Use RMS for smoother amplitude response
        val targetAmp = rmsAmp.coerceIn(0f, 1f)
        smoothedAmplitude = performSmoothing(smoothedAmplitude, targetAmp)
        
        // Boost amplitude slightly for better visual response
        val boostedAmplitude = (smoothedAmplitude * 1.5f).coerceIn(0f, 1f)
        
        // Return current state (smoothed)
        return AudioState(
            amplitude = boostedAmplitude,
            bass = smoothedBass.coerceIn(0f, 1f),
            mid = smoothedMid.coerceIn(0f, 1f),
            high = smoothedHigh.coerceIn(0f, 1f),
            isSilence = boostedAmplitude < 0.01f
        )
    }

    private fun performFFT() {
        // Apply Hanning Window to reduce spectral leakage
        val windowed = FloatArray(FFT_SIZE)
        for (i in 0 until FFT_SIZE) {
            val window = 0.5 * (1 - cos(2 * PI * i / (FFT_SIZE - 1)))
            windowed[i] = buffer[i] * window.toFloat()
        }

        // Real FFT logic (simplified recursive or iterative)
        // Since implementing full complex FFT from scratch is verbose, 
        // we'll use a placeholder logic that calculates approximate energy in bands 
        // using simple filters or partial DFT if libraries aren't allowed.
        // However, for a user request of "FFT Processor", I should implement a basic one.
        
        val spectrum = calculateMagnitudeSpectrum(windowed)
        
        // Split into bands
        // Frequency resolution = SampleRate / FFT_SIZE
        // Bin 0 = 0Hz
        // Bin i = i * Resolution
        val resolution = sampleRate.toFloat() / FFT_SIZE
        
        var bassEnergy = 0f
        var midEnergy = 0f
        var highEnergy = 0f
        var bassCount = 0
        var midCount = 0
        var highCount = 0
        
        for (i in 1 until spectrum.size / 2) { // Spectrum is symmetric, only need first half
            val freq = i * resolution
            val magnitude = spectrum[i] // Magnitude
            
            if (freq < 250) {
                bassEnergy += magnitude
                bassCount++
            } else if (freq < 2000) {
                midEnergy += magnitude
                midCount++
            } else if (freq < 8000) {
                highEnergy += magnitude
                highCount++
            }
        }
        
        // Average energy per band (normalize by bin count)
        if (bassCount > 0) bassEnergy /= bassCount
        if (midCount > 0) midEnergy /= midCount
        if (highCount > 0) highEnergy /= highCount
        
        // Normalize to 0-1 range with scaling for better visual response
        // These scaling factors are empirically determined for music visualization
        bassEnergy = (bassEnergy * 2.0f).coerceIn(0f, 1f)
        midEnergy = (midEnergy * 1.5f).coerceIn(0f, 1f)
        highEnergy = (highEnergy * 1.2f).coerceIn(0f, 1f)

        // Apply smoothing and decay
        smoothedBass = performSmoothing(smoothedBass, bassEnergy)
        smoothedMid = performSmoothing(smoothedMid, midEnergy)
        smoothedHigh = performSmoothing(smoothedHigh, highEnergy)
    }
    
    // Simple basic DFT for demonstration (Slow O(N^2), but OK for small N=512/1024 on modern phones?)
    // Actually O(N^2) is bad for 1024 (1M ops per frame). 
    // We need Cooley-Tukey O(N log N).
    // I will implement a compact Cooley-Tukey in-place.
    
    private fun calculateMagnitudeSpectrum(input: FloatArray): FloatArray {
        // Prepare complex array
        val n = input.size
        val real = input.copyOf()
        val imag = FloatArray(n)
        
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }
        
        // Butterfly
        var l = 2
        while (l <= n) {
            val ang = -2.0 * PI / l
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var uRe = 1.0f
            var uIm = 0.0f
            
            for (m in 0 until l / 2) {
                for (i in m until n step l) {
                    val ip = i + l / 2
                    val tRe = uRe * real[ip] - uIm * imag[ip]
                    val tIm = uRe * imag[ip] + uIm * real[ip]
                    real[ip] = real[i] - tRe
                    imag[ip] = imag[i] - tIm
                    real[i] += tRe
                    imag[i] += tIm
                }
                val tRe = uRe
                uRe = tRe * wRe - uIm * wIm
                uIm = tRe * wIm + uIm * wRe
            }
            l *= 2
        }
        
        // Calculate magnitude
        val magnitude = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            magnitude[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        
        return magnitude
    }

    private fun performSmoothing(current: Float, target: Float): Float {
        // Attack is fast, decay is slow? Or just standard smoothing?
        // Let's implement peak decay
        if (target > current) {
            return current + (target - current) * (1 - SMOOTHING_FACTOR)
        } else {
            return current * DECAY_FACTOR
        }
    }
}
