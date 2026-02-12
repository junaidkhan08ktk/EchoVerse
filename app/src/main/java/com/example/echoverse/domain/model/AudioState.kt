package com.example.echoverse.domain.model

data class AudioState(
    val amplitude: Float = 0f,
    val bass: Float = 0f,
    val mid: Float = 0f,
    val high: Float = 0f,
    val isSilence: Boolean = true
)
