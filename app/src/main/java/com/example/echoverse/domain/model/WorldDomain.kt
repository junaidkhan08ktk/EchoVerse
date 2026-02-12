package com.example.echoverse.domain.model

enum class WorldCategory {
    CALM,
    ENERGETIC,
    ABSTRACT,
    NATURE,
    FLUID,
    STORY
}

enum class RendererType {
    FLUID,
    PARTICLE,
    GEOMETRIC,
    ORGANIC,
    NARRATIVE
}

// Category-wide behavior rules
data class CategoryBehaviorProfile(
    val audioResponsiveness: Float, // 0.1 (Slow) to 1.0 (Instant)
    val bassWeight: Float,
    val midWeight: Float = 0.5f,
    val highFreqWeight: Float,
    val motionSpeed: Float,
    val gestureImpact: Float,
    val idleIntensity: Float = 0.2f
)

fun WorldCategory.getDefaultProfile(): CategoryBehaviorProfile {
    return when (this) {
        WorldCategory.CALM -> CategoryBehaviorProfile(0.2f, 1.0f, 0.5f, 0.2f, 0.4f, 0.3f, 0.1f)
        WorldCategory.ENERGETIC -> CategoryBehaviorProfile(0.9f, 1.5f, 0.8f, 1.0f, 1.2f, 1.0f, 0.3f)
        WorldCategory.ABSTRACT -> CategoryBehaviorProfile(0.5f, 0.8f, 1.0f, 1.5f, 0.8f, 1.2f, 0.4f)
        WorldCategory.NATURE -> CategoryBehaviorProfile(0.3f, 0.5f, 1.2f, 0.8f, 0.6f, 0.6f, 0.5f)
        WorldCategory.FLUID -> CategoryBehaviorProfile(0.6f, 1.2f, 1.0f, 0.6f, 0.7f, 0.9f, 0.3f)
        WorldCategory.STORY -> CategoryBehaviorProfile(0.4f, 1.0f, 1.0f, 1.0f, 0.5f, 0.8f, 0.2f)
    }
}

enum class TouchReactionType {
    RIPPLE,
    BURST,
    WARP,
    SPAWN,
    FORCE,
    NARRATIVE_STEP
}

enum class FrequencyBand {
    BASS, MID, HIGH, ALL
}
