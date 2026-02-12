package com.example.echoverse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.echoverse.domain.model.*
import com.example.echoverse.ui.components.*
import com.example.echoverse.ui.models.SampleWorlds
import com.example.echoverse.ui.theme.*
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun PreviewScreen(
    worldId: String,
    onApply: () -> Unit,
    onCustomize: () -> Unit,
    onBack: () -> Unit
) {
    val world = SampleWorlds.find { it.id == worldId } ?: SampleWorlds.first()
    val profile = world.behaviorProfile

    // Dynamic State for Preview Interaction
    var pinchScale by remember { mutableStateOf(1f) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var isLongPress by remember { mutableStateOf(false) }

    // Fluid Renderer Integration
    val fluidRenderer = remember { com.example.echoverse.presentation.renderers.FluidRenderer() }
    var lastFrameTime by remember { mutableStateOf(0L) }

    EchoScreen {
        val infiniteTransition = rememberInfiniteTransition(label = "audio")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2 * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing)
            ),
            label = "phase"
        )

        // Simulated Audio State (to fulfill "No fake previews" requirement)
        val simAudio = AudioState(
            bass = (0.5f + 0.5f * sin(phase)).coerceIn(0f, 1f),
            mid = (0.3f + 0.7f * sin(phase * 0.5f)).coerceIn(0f, 1f),
            high = (0.1f + 0.9f * abs(cos(phase * 2f))).coerceIn(0f, 1f),
            amplitude = 0.5f
        )

        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { 
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        isLongPress = false
                    },
                    onLongPress = { isLongPress = true }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        touchPos = change.position
                        velocity = dragAmount
                    },
                    onDragEnd = { velocity = Offset.Zero }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    pinchScale = (pinchScale * zoom).coerceIn(0.5f, 3f)
                }
            }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val cx = width / 2f
                val cy = height / 2f
                val color = world.thumbnailColor
                
                // Behavior Logic Merge (Simplified for Compose)
                val masterScale = (1f + simAudio.bass * profile.bassWeight * 0.2f + (if(isPressed) 0.2f else 0f)) * pinchScale
                val distortion = simAudio.high * profile.highFreqWeight + (if(world.category == WorldCategory.ABSTRACT) abs(velocity.x)*0.01f else 0f)

                // Background
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Black, color.copy(alpha = 0.2f + simAudio.amplitude * 0.1f), Color.Black))
                )

                // Category Specific Drawing
                val now = System.nanoTime()
                val dt = if (lastFrameTime == 0L) 0.016f else (now - lastFrameTime) / 1_000_000_000f
                lastFrameTime = now

                if (world.category == WorldCategory.FLUID) {
                    drawIntoCanvas { canvas ->
                        fluidRenderer.setWorld(world.id)
                        fluidRenderer.setSurfaceSize(width, height, density)
                        fluidRenderer.update(dt.coerceIn(0.001f, 0.1f), simAudio, 
                            TouchState(
                                isPressed = isPressed,
                                lastX = touchPos.x, lastY = touchPos.y,
                                velocityX = velocity.x, velocityY = velocity.y,
                                pinchScale = pinchScale,
                                lastInteractionTime = System.currentTimeMillis()
                            )
                        )
                        fluidRenderer.render(canvas.nativeCanvas)
                    }
                } else {
                    when (world.category) {
                        WorldCategory.CALM -> {
                        val rBase = 300f * masterScale
                        for (i in 1..4) {
                            val r = rBase * (1f + i * 0.2f + sin(phase + i) * 0.05f)
                            drawCircle(color, radius = r, style = Stroke(2f), alpha = 0.5f / i)
                        }
                        drawCircle(Brush.radialGradient(listOf(color.copy(alpha=0.4f), Color.Transparent)), radius = rBase)
                    }
                    WorldCategory.ENERGETIC -> {
                        val path = Path()
                        val points = 30
                        for (i in 0..points) {
                            val x = (i.toFloat() / points) * width
                            val y = cy + sin(i * 0.8f + phase * 6f) * (simAudio.bass * 200f + abs(velocity.y))
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color, style = Stroke(8f))
                        rotate(phase * 100f, pivot = Offset(cx, cy)) {
                            drawRect(color, topLeft = Offset(cx - 100f, cy - 100f), size = androidx.compose.ui.geometry.Size(200f * masterScale, 200f * masterScale), alpha = 0.7f)
                        }
                    }
                    WorldCategory.ABSTRACT -> {
                        val path = Path()
                        val segs = 12
                        val r0 = 250f * masterScale
                        for (i in 0 until segs) {
                            val a = (i.toFloat() / segs) * 2 * PI
                            val r = r0 * (1f + sin(a.toFloat() * 4 + phase * 2) * (distortion + 0.2f))
                            val x = cx + cos(a.toFloat()) * r
                            val y = cy + sin(a.toFloat()) * r
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color, alpha = 0.6f)
                    }
                    com.example.echoverse.domain.model.WorldCategory.NATURE -> {
                        when (world.id) {
                            "n1" -> { // Forest
                                // Light Rays
                                repeat(3) { i ->
                                    val angle = (i - 1) * 15f
                                    val rayAlpha = 0.1f + 0.05f * sin(phase + i)
                                    val path = Path()
                                    path.moveTo(cx + angle * 10 - 50, 0f)
                                    path.lineTo(cx + angle * 10 + 50, 0f)
                                    path.lineTo(cx + angle * 20 + 100, height)
                                    path.lineTo(cx + angle * 20 - 100, height)
                                    path.close()
                                    drawPath(path, Color.White, alpha = rayAlpha)
                                }
                                // Swaying trees
                                val sway = sin(phase) * 15f
                                repeat(5) { i ->
                                    val x = (i / 4f) * width
                                    drawLine(color, start = Offset(x + sway, height), end = Offset(x + sway * 1.5f, height * 0.4f), strokeWidth = 30f, alpha = 0.3f)
                                }
                            }
                            "n2" -> { // Fireflies
                                repeat(25) { i ->
                                    val px = (sin(i * 7.7f + phase * 0.4f) * 0.45f + 0.5f) * width + velocity.x * 0.1f
                                    val py = (cos(i * 5.3f + phase * 0.25f) * 0.45f + 0.5f) * height + velocity.y * 0.1f
                                    val glow = (0.4f + 0.6f * abs(sin(phase + i))).coerceIn(0f, 1f)
                                    drawCircle(color, radius = 6f * glow * masterScale, center = Offset(px, py), alpha = glow)
                                }
                            }
                            "n3" -> { // Rain
                                val gravity = 20f + simAudio.amplitude * 20f
                                repeat(40) { i ->
                                    val rx = (abs(sin(i * 133f)) * width)
                                    val ry = (phase * 1000f + i * 200f) % height
                                    drawLine(color, start = Offset(rx, ry), end = Offset(rx, ry + 30f), strokeWidth = 2f, alpha = 0.4f)
                                    if (ry > height - 50) {
                                        drawCircle(color, radius = (1f - (height - ry)/50f) * 40f, center = Offset(rx, height), style = Stroke(1f), alpha = 0.2f)
                                    }
                                }
                            }
                            "n4" -> { // Wind
                                repeat(10) { i ->
                                    val path = Path()
                                    val yOffset = (i / 9f) * height
                                    for (x in 0..width.toInt() step 50) {
                                        val py = yOffset + sin(x * 0.01f + phase + i) * 80f
                                        if (x == 0) path.moveTo(x.toFloat(), py) else path.lineTo(x.toFloat(), py)
                                    }
                                    drawPath(path, color, alpha = 0.2f, style = Stroke(1f))
                                }
                                // Flying leaves
                                repeat(10) { i ->
                                    val lx = (phase * 300f + i * 400f) % width
                                    val ly = (sin(lx * 0.01f + i) * 100f) + height/2
                                    rotate(lx + i, pivot = Offset(lx, ly)) {
                                        drawOval(color, topLeft = Offset(lx - 10f, ly - 5f), size = androidx.compose.ui.geometry.Size(20f, 10f), alpha = 0.6f)
                                    }
                                }
                            }
                            "n5" -> { // Aurora
                                repeat(2) { l ->
                                    val path = Path()
                                    val lp = phase + l * 2f
                                    for (x in 0..width.toInt() step 20) {
                                        val nx = x / width
                                        val py = height * 0.2f + sin(nx * 4 + lp) * 100f
                                        if (x == 0) path.moveTo(x.toFloat(), py) else path.lineTo(x.toFloat(), py)
                                    }
                                    drawPath(path, if(l==0) color else Color.White, alpha = 0.3f + simAudio.amplitude * 0.3f, style = Stroke(100f))
                                }
                                // Stars
                                repeat(20) { i ->
                                    val sx = (abs(cos(i * 99f)) * width)
                                    val sy = (abs(sin(i * 77f)) * height * 0.6f)
                                    drawCircle(Color.White, radius = 2f, center = Offset(sx, sy), alpha = 0.5f + 0.5f * sin(phase + i))
                                }
                            }
                        }
                    }
                    WorldCategory.FLUID -> {
                        when (world.id) {
                            "f1" -> { // Liquid Dream - Flowing metaballs
                                repeat(8) { i ->
                                    val px = cx + cos(phase * 0.5f + i * 0.8f) * 200f * masterScale
                                    val py = cy + sin(phase * 0.3f + i * 0.6f) * 200f * masterScale
                                    val size = 80f + 40f * sin(phase + i) * masterScale
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(color.copy(alpha = 0.6f), Color.Transparent)
                                        ),
                                        radius = size,
                                        center = Offset(px, py)
                                    )
                                }
                            }
                            "f2" -> { // Vapor Flow - Smoke particles
                                repeat(30) { i ->
                                    val px = (sin(i * 3.7f + phase * 0.2f) * 0.5f + 0.5f) * width
                                    val py = (cos(i * 2.3f + phase * 0.15f) * 0.5f + 0.5f) * height
                                    val size = 40f + 30f * abs(sin(phase + i * 0.5f))
                                    val alpha = 0.15f + 0.15f * abs(cos(phase + i))
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(color.copy(alpha = alpha), Color.Transparent)
                                        ),
                                        radius = size * masterScale,
                                        center = Offset(px, py)
                                    )
                                }
                            }
                            "f3" -> { // Plasma Wave - Flowing wave patterns
                                val path = Path()
                                for (x in 0..width.toInt() step 10) {
                                    val nx = x / width
                                    val y1 = cy + sin(nx * 8 + phase) * 150f * masterScale
                                    if (x == 0) path.moveTo(x.toFloat(), y1) else path.lineTo(x.toFloat(), y1)
                                }
                                drawPath(path, color, alpha = 0.5f, style = Stroke(60f))
                                
                                val path2 = Path()
                                for (x in 0..width.toInt() step 10) {
                                    val nx = x / width
                                    val y2 = cy + sin(nx * 6 + phase * 1.5f) * 120f * masterScale
                                    if (x == 0) path2.moveTo(x.toFloat(), y2) else path2.lineTo(x.toFloat(), y2)
                                }
                                drawPath(path2, Color.White, alpha = 0.3f, style = Stroke(40f))
                            }
                            "f4" -> { // Mercury Dance - Liquid droplets
                                repeat(12) { i ->
                                    val px = (sin(i * 5.5f + phase * 0.6f) * 0.4f + 0.5f) * width
                                    val py = (cos(i * 4.2f + phase * 0.4f) * 0.4f + 0.5f) * height
                                    val baseSize = 50f + 30f * sin(phase * 0.8f + i)
                                    val size = baseSize * masterScale
                                    
                                    // Metallic effect
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.8f),
                                                color.copy(alpha = 0.6f),
                                                Color.Gray.copy(alpha = 0.3f)
                                            )
                                        ),
                                        radius = size,
                                        center = Offset(px, py)
                                    )
                                }
                            }
                            "f5" -> { // Oil Canvas - Swirling paint
                                repeat(6) { i ->
                                    val path = Path()
                                    val centerAngle = i * 60f
                                    for (a in 0..360 step 10) {
                                        val adjustedAngle = a + phase * 30f + centerAngle
                                        val radius = 150f + 50f * sin(a * 0.05f + phase + i) * masterScale
                                        val px = cx + cos(Math.toRadians(adjustedAngle.toDouble())).toFloat() * radius
                                        val py = cy + sin(Math.toRadians(adjustedAngle.toDouble())).toFloat() * radius
                                        if (a == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    }
                                    path.close()
                                    val swirl = if (i % 2 == 0) color else color.copy(alpha = 0.5f)
                                    drawPath(path, swirl, alpha = 0.4f)
                                }
                            }
                        }
                    }
                    WorldCategory.STORY -> {
                        drawCircle(Color.White, radius = 10f * masterScale, center = Offset(cx, cy))
                        for (i in 1..4) {
                            val p = (phase + i * 1.5f) % (2 * PI.toFloat()) / (2 * PI.toFloat())
                            val r = p * 600f * masterScale
                            drawCircle(Color.White, radius = r, style = Stroke(2f), alpha = 1f - p)
                        }
                        if (isLongPress) {
                            // Narrative hint
                            drawCircle(Color.Red, radius = 20f, center = Offset(cx, cy + 200f), alpha = 0.5f)
                        }
                    }
                    else -> {}
                }
            }
                
                // Interaction Feedback (Ripple)
                if (isPressed) {
                    drawCircle(color, radius = 100f, center = touchPos, alpha = 0.3f)
                }
            }
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                 IconButton(onClick = onBack) {
                     Icon(
                         imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                         contentDescription = "Back",
                         tint = Color.White
                     )
                 }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = world.name, style = MaterialTheme.typography.displaySmall, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = world.category.name, style = MaterialTheme.typography.labelMedium, color = AccentSecondary)
                    if (world.isPremium) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "PREMIUM", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = world.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha=0.7f))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCustomize,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.5f))
                    ) {
                        Text("Customize")
                    }
                    EchoButton(text = "Apply Wallpaper", onClick = onApply, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
