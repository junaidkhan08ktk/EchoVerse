package com.example.echoverse.domain.model

data class TouchState(
    val lastX: Float = 0f,
    val lastY: Float = 0f,
    val isPressed: Boolean = false,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val pinchScale: Float = 1f,
    val isLongPress: Boolean = false,
    val lastSwipeDirectionX: Float = 0f,
    val lastSwipeDirectionY: Float = 0f,
    val lastInteractionTime: Long = 0L
)
