package com.example.echoverse.domain.world

import com.example.echoverse.domain.model.*
import kotlin.math.max
import kotlin.math.abs

class WorldBehaviorEngine {

    private var currentMode = WorldMode.IDLE
    private val historySize = 60
    private val energyHistory = FloatArray(historySize)
    private var historyIndex = 0

    fun update(
        audioState: AudioState,
        touchState: TouchState,
        category: WorldCategory,
        profile: CategoryBehaviorProfile
    ): Pair<WorldMode, VisualParameters> {
        
        // 1. Calculate weighted energy from audio + touch
        val audioEnergy = (
            audioState.bass * profile.bassWeight + 
            audioState.mid * profile.midWeight + 
            audioState.high * profile.highFreqWeight
        ) * profile.audioResponsiveness
        
        val touchEnergy = if (touchState.isPressed) 0.5f * profile.gestureImpact else 0f
        val swipeEnergy = (abs(touchState.velocityX) + abs(touchState.velocityY)) * 0.001f * profile.gestureImpact
        
        val totalEnergy = audioEnergy + touchEnergy + swipeEnergy
        
        // 2. Update mode
        updateHistory(totalEnergy)
        val avgEnergy = energyHistory.average().toFloat()
        currentMode = determineMode(avgEnergy, category)
        
        // 3. Derive parameters
        val baseParams = getBaseParameters(currentMode, category, profile)
        
        // 4. Merge Audio + Touch + Category Profile
        val finalParams = mergeInputs(baseParams, audioState, touchState, category, profile)
        
        return currentMode to finalParams
    }

    private fun updateHistory(energy: Float) {
        energyHistory[historyIndex] = energy
        historyIndex = (historyIndex + 1) % historySize
    }

    private fun determineMode(avgEnergy: Float, category: WorldCategory): WorldMode {
        val peakThresh = when(category) {
            WorldCategory.CALM -> 4.0f
            WorldCategory.ENERGETIC -> 2.0f
            else -> 3.0f
        }
        val activeThresh = when(category) {
            WorldCategory.CALM -> 1.5f
            WorldCategory.ENERGETIC -> 0.8f
            else -> 1.2f
        }
        
        return when {
            avgEnergy > peakThresh -> WorldMode.PEAK
            avgEnergy > activeThresh -> WorldMode.ACTIVE
            avgEnergy > 0.3f -> WorldMode.CALM
            else -> WorldMode.IDLE
        }
    }

    private fun getBaseParameters(mode: WorldMode, category: WorldCategory, profile: CategoryBehaviorProfile): VisualParameters {
        val speedMult = profile.motionSpeed
        return when (mode) {
            WorldMode.IDLE -> {
                // Enhanced IDLE mode - wallpapers should look good even without audio
                when (category) {
                    WorldCategory.FLUID -> VisualParameters(
                        particleSpeed = 0.8f * speedMult,  // Keep fluid flowing
                        masterScale = 1.0f,                 // Full scale
                        colorIntensity = 0.6f,              // Visible colors
                        distortion = 0.2f,                  // Subtle variation
                        spawnRate = 1.0f                    // Keep particles active
                    )
                    WorldCategory.CALM -> VisualParameters(
                        particleSpeed = 0.5f * speedMult,
                        masterScale = 0.9f,
                        colorIntensity = 0.4f,
                        distortion = 0.05f,
                        spawnRate = 0.8f
                    )
                    WorldCategory.NATURE -> VisualParameters(
                        particleSpeed = 0.6f * speedMult,
                        masterScale = 0.9f,
                        colorIntensity = 0.5f,
                        distortion = 0.1f,
                        spawnRate = 0.9f
                    )
                    else -> VisualParameters(
                        particleSpeed = 0.6f * speedMult,
                        masterScale = 0.9f,
                        colorIntensity = 0.4f,
                        distortion = 0.1f,
                        spawnRate = 0.8f
                    )
                }
            }
            WorldMode.CALM -> VisualParameters(1.0f * speedMult, 0.8f, 0.6f, 0.1f, 1.0f)
            WorldMode.ACTIVE -> VisualParameters(2.0f * speedMult, 2.0f, 1.0f, 0.3f, 1.2f)
            WorldMode.PEAK -> VisualParameters(4.0f * speedMult, 5.0f, 1.5f, 0.8f, 1.5f)
        }
    }

    private fun mergeInputs(
        base: VisualParameters,
        audio: AudioState,
        touch: TouchState,
        category: WorldCategory,
        profile: CategoryBehaviorProfile
    ): VisualParameters {
        val impact = profile.gestureImpact
        
        // Audio contributions
        val audioScale = (audio.bass * profile.bassWeight * 0.2f)
        val audioDistortion = (audio.high * profile.highFreqWeight * 0.5f)
        
        // Gesture contributions
        val pinchMod = max(0.2f, touch.pinchScale)
        val swipeMod = (abs(touch.velocityX) + abs(touch.velocityY)) * 0.0005f * impact
        val pressMod = if (touch.isPressed) 0.2f * impact else 0f
        val longPressMod = if (touch.isLongPress) 0.5f * impact else 0f

        // Category specific gesture logic
        val categoryGestureMod = when(category) {
            WorldCategory.ABSTRACT -> (touch.velocityX + touch.velocityY) * 0.001f // More distortion on swipe
            WorldCategory.ENERGETIC -> pressMod * 2f // More scale on press
            else -> 0f
        }

        return base.copy(
            masterScale = (base.masterScale + audioScale + pressMod + longPressMod) * pinchMod,
            particleSpeed = (base.particleSpeed + swipeMod) * if (touch.isLongPress) 0.5f else 1.0f,
            colorIntensity = base.colorIntensity + (audio.mid * 0.5f) + pressMod,
            distortion = base.distortion + audioDistortion + (if (category == WorldCategory.ABSTRACT) categoryGestureMod else 0f),
            spawnRate = base.spawnRate + (audio.amplitude * 3f) + (swipeMod * 10f)
        )
    }
}
