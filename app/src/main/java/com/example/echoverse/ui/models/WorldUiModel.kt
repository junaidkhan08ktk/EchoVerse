package com.example.echoverse.ui.models

import androidx.compose.ui.graphics.Color
import com.example.echoverse.domain.model.*

data class WorldUiModel(
    val id: String,
    val name: String,
    val description: String,
    val category: WorldCategory,
    val rendererType: RendererType,
    val behaviorProfile: CategoryBehaviorProfile,
    val thumbnailColor: Color,
    val isPremium: Boolean = false
)

val SampleWorlds = listOf(
    // CALM (5)
    WorldUiModel("c1", "Echo Ocean", "A deep, breathing underwater abyss.", WorldCategory.CALM, RendererType.FLUID, WorldCategory.CALM.getDefaultProfile(), Color(0xFF00E5FF)),
    WorldUiModel("c2", "Zen Garden", "Minimal ripples and geometric peace.", WorldCategory.CALM, RendererType.FLUID, WorldCategory.CALM.getDefaultProfile(), Color(0xFF81C784)),
    WorldUiModel("c3", "Mist Flow", "Vague shapes moving in the fog.", WorldCategory.CALM, RendererType.FLUID, WorldCategory.CALM.getDefaultProfile(), Color(0xFFB0BEC5)),
    WorldUiModel("c4", "Night Tide", "The moon's reflection on sound waves.", WorldCategory.CALM, RendererType.FLUID, WorldCategory.CALM.getDefaultProfile(), Color(0xFF3F51B5)),
    WorldUiModel("c5", "Silent Horizon", "Endless calm at the edge of the world.", WorldCategory.CALM, RendererType.FLUID, WorldCategory.CALM.getDefaultProfile(), Color(0xFFE1F5FE)),

    // ENERGETIC (5)
    WorldUiModel("e1", "Pulse City", "The electric heartbeat of the machine.", WorldCategory.ENERGETIC, RendererType.GEOMETRIC, WorldCategory.ENERGETIC.getDefaultProfile(), Color(0xFFFF0055)),
    WorldUiModel("e2", "Neon Storm", "High voltage lightning and thunder.", WorldCategory.ENERGETIC, RendererType.GEOMETRIC, WorldCategory.ENERGETIC.getDefaultProfile(), Color(0xFF00E5FF)),
    WorldUiModel("e3", "Sonic Core", "The explosive center of the beat.", WorldCategory.ENERGETIC, RendererType.GEOMETRIC, WorldCategory.ENERGETIC.getDefaultProfile(), Color(0xFFFFD600)),
    WorldUiModel("e4", "Bass Reactor", "Heavy vibrations and structural shifts.", WorldCategory.ENERGETIC, RendererType.GEOMETRIC, WorldCategory.ENERGETIC.getDefaultProfile(), Color(0xFFFF5722)),
    WorldUiModel("e5", "Electro Grid", "Digital veins pumping pure energy.", WorldCategory.ENERGETIC, RendererType.GEOMETRIC, WorldCategory.ENERGETIC.getDefaultProfile(), Color(0xFF76FF03)),

    // ABSTRACT (5)
    WorldUiModel("a1", "Nebula Dreams", "Molding cosmic dust with sound.", WorldCategory.ABSTRACT, RendererType.GEOMETRIC, WorldCategory.ABSTRACT.getDefaultProfile(), Color(0xFF6C63FF)),
    WorldUiModel("a2", "Glitch Matrix", "Digital distortion and chaotic patterns.", WorldCategory.ABSTRACT, RendererType.GEOMETRIC, WorldCategory.ABSTRACT.getDefaultProfile(), Color(0xFF00FF00)),
    WorldUiModel("a3", "Fractal Bloom", "Infinite patterns growing from noise.", WorldCategory.ABSTRACT, RendererType.GEOMETRIC, WorldCategory.ABSTRACT.getDefaultProfile(), Color(0xFFFF4081)),
    WorldUiModel("a4", "Liquid Chaos", "Melting reality in a swirl of colors.", WorldCategory.ABSTRACT, RendererType.GEOMETRIC, WorldCategory.ABSTRACT.getDefaultProfile(), Color(0xFFFF9100)),
    WorldUiModel("a5", "Quantum Ink", "Particles existing in multiple states.", WorldCategory.ABSTRACT, RendererType.GEOMETRIC, WorldCategory.ABSTRACT.getDefaultProfile(), Color(0xFF7C4DFF)),

    // NATURE (5)
    WorldUiModel("n1", "Forest Whisper", "The rustling of leaves in the wind.", WorldCategory.NATURE, RendererType.PARTICLE, WorldCategory.NATURE.getDefaultProfile(), Color(0xFF4CAF50)),
    WorldUiModel("n2", "Firefly Field", "Nature's lights dancing to the wind.", WorldCategory.NATURE, RendererType.PARTICLE, WorldCategory.NATURE.getDefaultProfile(), Color(0xFFFFD600)),
    WorldUiModel("n3", "Rain Meadow", "Soft raindrops creating visual echoes.", WorldCategory.NATURE, RendererType.PARTICLE, WorldCategory.NATURE.getDefaultProfile(), Color(0xFF2196F3)),
    WorldUiModel("n4", "Wind Valley", "Invisible forces shaping the landscape.", WorldCategory.NATURE, RendererType.PARTICLE, WorldCategory.NATURE.getDefaultProfile(), Color(0xFF8BC34A)),
    WorldUiModel("n5", "Aurora Sky", "Dancing lights in the upper atmosphere.", WorldCategory.NATURE, RendererType.PARTICLE, WorldCategory.NATURE.getDefaultProfile(), Color(0xFF00E676)),

    // FLUID (5)
    WorldUiModel("f1", "Liquid Dream", "Smooth flowing colors merging and separating.", WorldCategory.FLUID, RendererType.FLUID, WorldCategory.FLUID.getDefaultProfile(), Color(0xFF00BCD4)),
    WorldUiModel("f2", "Vapor Flow", "Ethereal mist swirling and dancing.", WorldCategory.FLUID, RendererType.FLUID, WorldCategory.FLUID.getDefaultProfile(), Color(0xFF9C27B0)),
    WorldUiModel("f3", "Plasma Wave", "Electric currents flowing through liquid.", WorldCategory.FLUID, RendererType.FLUID, WorldCategory.FLUID.getDefaultProfile(), Color(0xFFFF00FF)),
    WorldUiModel("f4", "Mercury Dance", "Metallic droplets flowing freely.", WorldCategory.FLUID, RendererType.FLUID, WorldCategory.FLUID.getDefaultProfile(), Color(0xFFC0C0C0)),
    WorldUiModel("f5", "Oil Canvas", "Vibrant paint swirling on water.", WorldCategory.FLUID, RendererType.FLUID, WorldCategory.FLUID.getDefaultProfile(), Color(0xFFFF9800)),

    // STORY (5)
    WorldUiModel("s1", "Lone Voyager", "Space is cold, but the signal stays alive.", WorldCategory.STORY, RendererType.NARRATIVE, WorldCategory.STORY.getDefaultProfile(), Color(0xFFFFFFFF), isPremium = true),
    WorldUiModel("s2", "Silent City", "The echoes of a world left behind.", WorldCategory.STORY, RendererType.NARRATIVE, WorldCategory.STORY.getDefaultProfile(), Color(0xFFB0BEC5), isPremium = true),
    WorldUiModel("s3", "Ember Path", "Following the light in the darkness.", WorldCategory.STORY, RendererType.NARRATIVE, WorldCategory.STORY.getDefaultProfile(), Color(0xFFFF6F00), isPremium = true),
    WorldUiModel("s4", "Forgotten World", "Relics of a future that never was.", WorldCategory.STORY, RendererType.NARRATIVE, WorldCategory.STORY.getDefaultProfile(), Color(0xFF4E342E), isPremium = true),
    WorldUiModel("s5", "Last Signal", "A flickering beacon in the void.", WorldCategory.STORY, RendererType.NARRATIVE, WorldCategory.STORY.getDefaultProfile(), Color(0xFFF44336), isPremium = true)
)
