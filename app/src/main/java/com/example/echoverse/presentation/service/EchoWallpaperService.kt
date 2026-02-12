package com.example.echoverse.presentation.service

import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import com.example.echoverse.data.audio.AudioSourceManager
import com.example.echoverse.domain.audio.AudioAnalyzer
import com.example.echoverse.domain.model.*
import com.example.echoverse.ui.models.SampleWorlds
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

data class Ripple(val x: Float, val y: Float, var radius: Float, var alpha: Int, val color: Int)
sealed class NatureEntity {
    data class Firefly(var x: Float, var y: Float, var vx: Float, var vy: Float, var brightness: Float, val seed: Float) : NatureEntity()
    data class RainDrop(var x: Float, var y: Float, var speed: Float, var length: Float) : NatureEntity()
    data class GroundRipple(val x: Float, val y: Float, var radius: Float, var alpha: Int) : NatureEntity()
    data class Leaf(var x: Float, var y: Float, var vx: Float, var vy: Float, var rot: Float, var vRot: Float) : NatureEntity()
    data class LightRay(val angle: Float, val width: Float, var intensity: Float) : NatureEntity()
}
sealed class FluidEntity {
    data class FluidBlob(var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float, val seed: Float) : FluidEntity()
    data class FluidParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var alpha: Float, val seed: Float) : FluidEntity()
}
class EchoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return EchoEngine()
    }

    inner class EchoEngine : Engine(), GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

        private val audioSourceManager = AudioSourceManager(applicationContext)
        private val audioAnalyzer = AudioAnalyzer()
        private val worldEngine = com.example.echoverse.domain.world.WorldBehaviorEngine()
        private val userPreferences = com.example.echoverse.data.preferences.UserPreferences(applicationContext)
        
        private var currentAudioState = AudioState()
        private var currentTouchState = TouchState()
        private var currentMode = WorldMode.IDLE
        private var currentParams = VisualParameters()
        
        private var activeWorld = SampleWorlds.first()
        private val ripples = mutableListOf<Ripple>()

        // User Settings
        private var sensitivity = 1.0f
        private var isMicEnabled = true
        private var selectedWorldId = "c1"

        // Nature Persisted States
        private val natureEntities = mutableListOf<NatureEntity>()

        private var natureInitDone = ""

        // Fluid Persisted States
        private val fluidEntities = mutableListOf<FluidEntity>()
        private val fluidRenderer = com.example.echoverse.presentation.renderers.FluidRenderer()
        private var lastFrameTime = 0L
        private var surfaceWidth = 0f
        private var surfaceHeight = 0f
        
        private var fluidInitDone = ""

        private var renderJob: Job? = null
        private var audioJob: Job? = null
        private var prefsJob: Job? = null
        
        private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        private val gestureDetector = GestureDetector(applicationContext, this)
        private val scaleDetector = ScaleGestureDetector(applicationContext, this)

        private val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        
        private val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            refreshSettings()
            lastFrameTime = System.nanoTime()
            drawFrame(0f)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width.toFloat()
            surfaceHeight = height.toFloat()
            val density = resources.displayMetrics.density
            fluidRenderer.setSurfaceSize(surfaceWidth, surfaceHeight, density)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopPrefsObserver()
            stopAudioCapture()
            stopRendering()
            engineScope.cancel()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                startPrefsObserver()
                refreshSettings()
                if (isMicEnabled) startAudioCapture()
                startRendering()
            } else {
                stopPrefsObserver()
                stopAudioCapture()
                stopRendering()
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector.onTouchEvent(event)
            scaleDetector.onTouchEvent(event)
            
            val currentTime = System.currentTimeMillis()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    currentTouchState = currentTouchState.copy(isPressed = true, lastInteractionTime = currentTime)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    currentTouchState = currentTouchState.copy(isPressed = false, isLongPress = false, lastInteractionTime = currentTime)
                }
            }
            super.onTouchEvent(event)
        }

        // --- Gesture Listeners ---
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onShowPress(e: MotionEvent) {}
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            synchronized(ripples) {
                ripples.add(Ripple(e.x, e.y, 0f, 180, activeWorld.thumbnailColor.toArgb()))
            }
            return true
        }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            currentTouchState = currentTouchState.copy(
                velocityX = -distanceX * 10f,
                velocityY = -distanceY * 10f,
                lastX = e2.x,
                lastY = e2.y
            )
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            currentTouchState = currentTouchState.copy(isLongPress = true)
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            currentTouchState = currentTouchState.copy(velocityX = velocityX, velocityY = velocityY)
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentTouchState = currentTouchState.copy(pinchScale = currentTouchState.pinchScale * detector.scaleFactor)
            return true
        }
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true
        override fun onScaleEnd(detector: ScaleGestureDetector) {}

        private fun refreshSettings() {
            sensitivity = userPreferences.sensitivity
            isMicEnabled = userPreferences.isMicEnabled
            selectedWorldId = userPreferences.selectedWorldId
            activeWorld = SampleWorlds.find { it.id == selectedWorldId } ?: SampleWorlds.first()
        }

        private fun startPrefsObserver() {
            prefsJob?.cancel()
            prefsJob = engineScope.launch {
                userPreferences.observePreferences().collect { key ->
                    refreshSettings()
                    if (key == com.example.echoverse.data.preferences.UserPreferences.KEY_MIC_ENABLED) {
                        if (isMicEnabled) startAudioCapture() else stopAudioCapture()
                    }
                }
            }
        }

        private fun stopPrefsObserver() {
            prefsJob?.cancel()
        }

        private fun startAudioCapture() {
            audioJob?.cancel()
            if (!isMicEnabled) return
            audioJob = engineScope.launch {
                audioSourceManager.getAudioStream().collect { bytes ->
                    currentAudioState = audioAnalyzer.process(bytes)
                }
            }
        }

        private fun stopAudioCapture() { audioJob?.cancel() }

        private fun startRendering() {
            renderJob?.cancel()
            renderJob = engineScope.launch {
                var phase = 0f
                lastFrameTime = System.nanoTime()
                
                while (isActive) {
                    val now = System.nanoTime()
                    val dt = ((now - lastFrameTime) / 1_000_000_000f)
                    lastFrameTime = now
                    val safeDt = dt.coerceIn(0.001f, 0.1f)

                    val (mode, params) = worldEngine.update(
                        currentAudioState, currentTouchState, activeWorld.category, activeWorld.behaviorProfile
                    )
                    currentMode = mode
                    currentParams = params
                    
                    phase += (0.02f + currentParams.particleSpeed * 0.03f)
                    if (phase > PI * 2) phase = 0f
                    
                    if (activeWorld.category == WorldCategory.FLUID) {
                        fluidRenderer.setWorld(activeWorld.id)
                        fluidRenderer.update(safeDt, currentAudioState, currentTouchState)
                    }
                    
                    drawFrame(phase)
                    
                    currentTouchState = currentTouchState.copy(
                        velocityX = currentTouchState.velocityX * 0.9f,
                        velocityY = currentTouchState.velocityY * 0.9f,
                        pinchScale = currentTouchState.pinchScale + (1f - currentTouchState.pinchScale) * 0.05f
                    )
                    delay(16)
                }
            }
        }

        private fun stopRendering() { renderJob?.cancel() }

        private fun drawFrame(phase: Float) {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas() ?: return
            try {
                drawFrameToCanvas(canvas, phase)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
        
        private fun drawFrameToCanvas(canvas: Canvas, phase: Float) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val cx = width / 2f
            val cy = height / 2f
            val worldColor = activeWorld.thumbnailColor.toArgb()

            // Update Ripples
            synchronized(ripples) {
                val it = ripples.iterator()
                while (it.hasNext()) {
                    val r = it.next()
                    r.radius += 20f
                    r.alpha -= 8
                    if (r.alpha <= 0) it.remove()
                }
            }

            // 1. Dynamic Background (reacts to category profiles)
            drawBackground(canvas, width, height, worldColor)

            // 2. Category System Renderers
            when (activeWorld.category) {
                WorldCategory.CALM -> renderCalm(canvas, cx, cy, width, height, worldColor, phase)
                WorldCategory.ENERGETIC -> renderEnergetic(canvas, cx, cy, width, height, worldColor, phase)
                WorldCategory.ABSTRACT -> renderAbstract(canvas, cx, cy, width, height, worldColor, phase)
                WorldCategory.NATURE -> renderNature(canvas, cx, cy, width, height, worldColor, phase)
                WorldCategory.FLUID -> renderFluid(canvas, cx, cy, width, height, worldColor, phase)
                WorldCategory.STORY -> renderStory(canvas, cx, cy, width, height, worldColor, phase)
            }
            
            // 3. Global Interaction Layer
            synchronized(ripples) {
                for (r in ripples) {
                    fillPaint.shader = null
                    fillPaint.color = r.color
                    fillPaint.alpha = r.alpha
                    canvas.drawCircle(r.x, r.y, r.radius, fillPaint)
                }
            }
        }

        private fun drawBackground(canvas: Canvas, w: Float, h: Float, color: Int) {
            val pulse = (sin(System.currentTimeMillis() / 1000.0).toFloat() * 0.1f + 0.1f)
            val bgGradient = LinearGradient(0f, 0f, 0f, h,
                intArrayOf(Color.BLACK, color, Color.BLACK),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            fillPaint.shader = bgGradient
            // Increased base alpha from 20 to 40 for better visibility, matches preview better
            fillPaint.alpha = (40 + currentParams.colorIntensity * 60 + pulse * 50).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, fillPaint)
        }

        private fun renderCalm(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val rBase = w * 0.3f * currentParams.masterScale * sensitivity
            linePaint.color = color
            linePaint.strokeWidth = 2f
            for (i in 1..5) {
                val r = rBase * (1f + i * 0.15f + sin(phase + i).toFloat() * 0.05f)
                linePaint.alpha = (100 / i).coerceIn(0, 255)
                canvas.drawCircle(cx, cy, r, linePaint)
            }
            // Soft center
            val g = RadialGradient(cx, cy, rBase, intArrayOf(color, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            fillPaint.shader = g; fillPaint.alpha = 100; canvas.drawCircle(cx, cy, rBase * 0.5f, fillPaint)
        }

        private fun renderEnergetic(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val path = Path()
            linePaint.color = color; linePaint.strokeWidth = 12f; linePaint.alpha = 255
            val points = 25
            for (i in 0..points) {
                val x = (i.toFloat() / points) * w
                val y = cy + sin(i * 1.2f + phase * 8f).toFloat() * (currentAudioState.bass * 400f * sensitivity + abs(currentTouchState.velocityY) * scaleInverse(w))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, linePaint)
            // Core
            val s = 200f * currentParams.masterScale * sensitivity
            canvas.save(); canvas.rotate(phase * 200f, cx, cy)
            fillPaint.color = color; fillPaint.alpha = 180; canvas.drawRect(cx-s/2, cy-s/2, cx+s/2, cy+s/2, fillPaint); canvas.restore()
        }

        private fun renderAbstract(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val path = Path(); fillPaint.color = color; fillPaint.alpha = 140
            val segs = 16
            val r0 = w * 0.35f * currentParams.masterScale * sensitivity
            for (i in 0 until segs) {
                val a = (i.toFloat() / segs) * 2 * PI
                val r = r0 * (1f + sin(a * 5 + phase * 3).toFloat() * (currentParams.distortion + currentTouchState.velocityX*0.001f))
                val x = cx + cos(a).toFloat() * r
                val y = cy + sin(a).toFloat() * r
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, fillPaint)
        }

        private fun renderNature(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            if (natureInitDone != activeWorld.id) initNatureSubState(w, h)
            
            when (activeWorld.id) {
                "n1" -> renderForest(canvas, cx, cy, w, h, color, phase)
                "n2" -> renderFireflies(canvas, cx, cy, w, h, color, phase)
                "n3" -> renderRain(canvas, cx, cy, w, h, color, phase)
                "n4" -> renderWind(canvas, cx, cy, w, h, color, phase)
                "n5" -> renderAurora(canvas, cx, cy, w, h, color, phase)
                else -> { /* Fallback */ }
            }
        }

        private fun initNatureSubState(w: Float, h: Float) {
            natureEntities.clear()
            when (activeWorld.id) {
                "n1" -> { // Forest: Light rays
                    repeat(4) { natureEntities.add(NatureEntity.LightRay(Random.nextFloat() * 40f - 20f, 100f + Random.nextFloat() * 200f, 0.3f)) }
                }
                "n2" -> { // Fireflies
                    repeat(35) { natureEntities.add(NatureEntity.Firefly(Random.nextFloat() * w, Random.nextFloat() * h, (Random.nextFloat() - 0.5f) * 2f, (Random.nextFloat() - 0.5f) * 2f, 0.5f, Random.nextFloat())) }
                }
                "n3" -> { // Rain
                    repeat(60) { natureEntities.add(NatureEntity.RainDrop(Random.nextFloat() * w, Random.nextFloat() * h, 15f + Random.nextFloat() * 10f, 20f + Random.nextFloat() * 30f)) }
                }
                "n4" -> { // Wind: Leaves
                    repeat(20) { natureEntities.add(NatureEntity.Leaf(Random.nextFloat() * w, Random.nextFloat() * h, 2f + Random.nextFloat() * 3f, (Random.nextFloat() - 0.5f) * 2f, Random.nextFloat() * 360f, Random.nextFloat() * 5f)) }
                }
            }
            natureInitDone = activeWorld.id
        }

        private fun renderForest(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            // 1. Light Rays
            fillPaint.shader = null
            natureEntities.filterIsInstance<NatureEntity.LightRay>().forEach { ray ->
                val rayPath = Path()
                val topX = cx + tan(Math.toRadians(ray.angle.toDouble())).toFloat() * (-h/2)
                val bottomX = cx + tan(Math.toRadians(ray.angle.toDouble())).toFloat() * (h/2)
                rayPath.moveTo(topX - ray.width/2, 0f)
                rayPath.lineTo(topX + ray.width/2, 0f)
                rayPath.lineTo(bottomX + ray.width/2, h)
                rayPath.lineTo(bottomX - ray.width/2, h)
                rayPath.close()
                
                val alpha = (30 + 10 * sin(phase + ray.angle)).toInt().coerceIn(0, 255)
                fillPaint.color = Color.WHITE
                fillPaint.alpha = alpha
                canvas.drawPath(rayPath, fillPaint)
            }
            
            // 2. Tree Silhouettes (simplified layers)
            linePaint.color = color
            linePaint.alpha = 50
            linePaint.strokeWidth = 40f
            val sway = sin(phase).toFloat() * 20f * (1f + currentAudioState.amplitude)
            for (i in 0..5) {
                val x = (i / 5f) * w
                canvas.drawLine(x + sway, h, x + sway * 1.5f, h * 0.3f, linePaint)
            }
            
            // 3. Pollen
            fillPaint.color = Color.WHITE
            repeat(15) { i ->
                val px = (sin(i * 1.1f + phase * 0.2f).toFloat() * 0.5f + 0.5f) * w
                val py = (cos(i * 1.4f + phase * 0.15f).toFloat() * 0.5f + 0.5f) * h
                fillPaint.alpha = 60
                canvas.drawCircle(px, py, 2f, fillPaint)
            }
        }

        private fun renderFireflies(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val audioMod = currentAudioState.amplitude * 200
            val sensitivity = currentParams.masterScale
            
            natureEntities.filterIsInstance<NatureEntity.Firefly>().forEach { f ->
                // Motion
                f.x += f.vx * sensitivity
                f.y += f.vy * sensitivity
                
                // Audio Reactivity
                f.brightness = (0.3f + audioMod / 255f + 0.2f * sin(phase * 2 + f.seed * 10).toFloat()).coerceIn(0f, 1f)
                
                // Interaction
                if (currentTouchState.isPressed) {
                    val dx = currentTouchState.lastX - f.x
                    val dy = currentTouchState.lastY - f.y
                    val dist = sqrt(dx*dx + dy*dy)
                    if (dist < 400) {
                        f.vx += dx / dist * 0.2f
                        f.vy += dy / dist * 0.2f
                    }
                }
                
                // Damping
                f.vx *= 0.98f; f.vy *= 0.98f
                // Wrap
                if (f.x < 0) f.x = w; if (f.x > w) f.x = 0f
                if (f.y < 0) f.y = h; if (f.y > h) f.y = 0f
                
                fillPaint.color = color
                fillPaint.alpha = (f.brightness * 255).toInt()
                canvas.drawCircle(f.x, f.y, 4f + f.brightness * 4f, fillPaint)
            }
        }

        private fun renderRain(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val rainAlpha = (100 + currentAudioState.amplitude * 155).toInt().coerceIn(0, 255)
            val gravity = 10f + currentAudioState.amplitude * 20f
            
            linePaint.color = color
            linePaint.strokeWidth = 2f
            
            natureEntities.filterIsInstance<NatureEntity.RainDrop>().forEach { r ->
                r.y += (r.speed + gravity)
                if (r.y > h) {
                    r.y = -50f
                    r.x = Random.nextFloat() * w
                    if (Random.nextFloat() > 0.8f) { // Spawn ripple
                        natureEntities.add(NatureEntity.GroundRipple(r.x, h - 20f, 0f, 100))
                    }
                }
                linePaint.alpha = rainAlpha / 2
                canvas.drawLine(r.x, r.y, r.x, r.y + r.length, linePaint)
            }
            
            // Ripples
            val ripples = natureEntities.filterIsInstance<NatureEntity.GroundRipple>()
            val it = natureEntities.iterator()
            while(it.hasNext()){
                val entity = it.next()
                if(entity is NatureEntity.GroundRipple){
                    entity.radius += 2f
                    entity.alpha -= 4
                    if(entity.alpha <= 0){
                        it.remove()
                    } else {
                        linePaint.alpha = entity.alpha
                        canvas.drawCircle(entity.x, entity.y, entity.radius, linePaint)
                    }
                }
            }
        }

        private fun renderWind(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            // Wind Flow Lines
            linePaint.color = color
            linePaint.strokeWidth = 1f
            val windStrength = 1f + currentAudioState.bass * 2f
            
            for (i in 0..8) {
                val y = (i / 8f) * h
                val path = Path()
                for (x in 0..w.toInt() step 20) {
                    val py = y + sin(x * 0.01f + phase + i).toFloat() * 50f * windStrength
                    if (x == 0) path.moveTo(x.toFloat(), py) else path.lineTo(x.toFloat(), py)
                }
                linePaint.alpha = 30
                canvas.drawPath(path, linePaint)
            }
            
            // Leaves
            natureEntities.filterIsInstance<NatureEntity.Leaf>().forEach { l ->
                l.x += l.vx * windStrength + currentTouchState.velocityX * 0.01f
                l.y += l.vy + currentTouchState.velocityY * 0.01f
                l.rot += l.vRot
                
                if (l.x > w) l.x = -20f
                if (l.y < 0) l.y = h; if (l.y > h) l.y = 0f
                
                canvas.save()
                canvas.translate(l.x, l.y)
                canvas.rotate(l.rot)
                fillPaint.color = color
                fillPaint.alpha = 150
                canvas.drawOval(-10f, -5f, 10f, 5f, fillPaint)
                canvas.restore()
            }
        }

        private fun renderAurora(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val audioIntensity = currentAudioState.amplitude
            
            for (layer in 0..2) {
                val path = Path()
                val layerColor = if (layer == 0) color else Color.WHITE
                val layerPhase = phase + layer * 1.5f
                
                for (x in 0..w.toInt() step 15) {
                    val normX = x / w
                    val yTop = h * 0.2f + sin(normX * 5 + layerPhase).toFloat() * 100f
                    val heightMod = (200f + 300f * audioIntensity) * (1f + sin(normX * 3 + layerPhase * 0.5f).toFloat() * 0.5f)
                    
                    if (x == 0) path.moveTo(x.toFloat(), yTop) else path.lineTo(x.toFloat(), yTop)
                    // We'll draw ribbons via fill with vertical gradient later or simple lines
                }
                
                linePaint.color = layerColor
                linePaint.alpha = (50 + 100 * audioIntensity).toInt().coerceIn(0, 255)
                linePaint.strokeWidth = 150f
                val blur = BlurMaskFilter(100f, BlurMaskFilter.Blur.NORMAL)
                linePaint.maskFilter = blur
                canvas.drawPath(path, linePaint)
                linePaint.maskFilter = null
            }
            
            // Stars
            fillPaint.color = Color.WHITE
            repeat(30) { i ->
                val px = (sin(i * 123.4f).toFloat() * 0.5f + 0.5f) * w
                val py = (cos(i * 567.8f).toFloat() * 0.5f + 0.5f) * h * 0.5f
                fillPaint.alpha = (50 + 150 * abs(sin(phase + i))).toInt().coerceIn(0, 255)
                canvas.drawCircle(px, py, 1.5f, fillPaint)
            }
        }

        private fun renderStory(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            fillPaint.color = Color.WHITE; fillPaint.alpha = 255
            canvas.drawCircle(cx, cy, 12f * currentParams.masterScale, fillPaint)
            linePaint.color = Color.WHITE; linePaint.strokeWidth = 3f
            for (i in 0..4) {
                val p = (phase + i * 1.2f) % (PI.toFloat() * 2) / (PI.toFloat() * 2)
                val d = p * 1000f * currentParams.masterScale
                linePaint.alpha = (255 * (1f - p)).toInt().coerceIn(0, 255)
                canvas.drawCircle(cx, cy, d, linePaint)
            }
            // Narrative Text (Mock)
            if (currentTouchState.isLongPress) {
                textPaint.alpha = 255; canvas.drawText("SIGN_FOUND", cx - 100f, cy + 300f, textPaint)
            }
        }

        private fun renderFluid(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
             fluidRenderer.render(canvas)
        }

        private fun initFluidSubState(w: Float, h: Float) {
            fluidEntities.clear()
            when (activeWorld.id) {
                "f1" -> { // Liquid Dream: Metaballs
                    repeat(8) { fluidEntities.add(FluidEntity.FluidBlob(Random.nextFloat() * w, Random.nextFloat() * h, (Random.nextFloat() - 0.5f) * 3f, (Random.nextFloat() - 0.5f) * 3f, 80f + Random.nextFloat() * 40f, Random.nextFloat())) }
                }
                "f2" -> { // Vapor Flow: Smoke particles
                    repeat(30) { fluidEntities.add(FluidEntity.FluidParticle(Random.nextFloat() * w, Random.nextFloat() * h, (Random.nextFloat() - 0.5f) * 1f, (Random.nextFloat() - 0.5f) * 1f, Random.nextFloat(), Random.nextFloat())) }
                }
                "f3" -> { // Plasma Wave: No entities needed
                }
                "f4" -> { // Mercury Dance: Liquid droplets
                    repeat(12) { fluidEntities.add(FluidEntity.FluidBlob(Random.nextFloat() * w, Random.nextFloat() * h, (Random.nextFloat() - 0.5f) * 4f, (Random.nextFloat() - 0.5f) * 4f, 50f + Random.nextFloat() * 30f, Random.nextFloat())) }
                }
                "f5" -> { // Oil Canvas: No entities needed
                }
            }
            fluidInitDone = activeWorld.id
        }

        private fun renderLiquidDream(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val audioMod = currentAudioState.amplitude * 100
            
            fluidEntities.filterIsInstance<FluidEntity.FluidBlob>().forEach { blob ->
                // Circular motion with audio influence
                blob.x += blob.vx * currentParams.masterScale + cos(phase + blob.seed * 10).toFloat() * 2f
                blob.y += blob.vy * currentParams.masterScale + sin(phase + blob.seed * 10).toFloat() * 2f
                
                // Wrap around
                if (blob.x < -100) blob.x = w + 100; if (blob.x > w + 100) blob.x = -100f
                if (blob.y < -100) blob.y = h + 100; if (blob.y > h + 100) blob.y = -100f
                
                // Size pulsing
                val size = blob.size * (1f + 0.3f * sin(phase + blob.seed).toFloat()) * currentParams.masterScale
                
                // Draw with radial gradient
                val gradient = RadialGradient(blob.x, blob.y, size, intArrayOf(color, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
                fillPaint.shader = gradient
                // Ensure visibility even without audio (base 180 instead of 150)
                fillPaint.alpha = (180 + audioMod).toInt().coerceIn(0, 255)
                canvas.drawCircle(blob.x, blob.y, size, fillPaint)
            }
        }

        private fun renderVaporFlow(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val audioMod = currentAudioState.amplitude
            
            fluidEntities.filterIsInstance<FluidEntity.FluidParticle>().forEach { p ->
                // Organic floating motion
                p.x += p.vx + sin(phase * 0.5f + p.seed * 5).toFloat() * 2f
                p.y += p.vy + cos(phase * 0.3f + p.seed * 7).toFloat() * 2f
                
                // Wrap around
                if (p.x < 0) p.x = w; if (p.x > w) p.x = 0f
                if (p.y < 0) p.y = h; if (p.y > h) p.y = 0f
                
                // Alpha pulsing
                p.alpha = (0.15f + 0.15f * abs(sin(phase + p.seed).toFloat())).coerceIn(0f, 1f)
                
                val size = (40f + 30f * abs(sin(phase + p.seed * 0.5f).toFloat())) * currentParams.masterScale
                
                val gradient = RadialGradient(p.x, p.y, size, intArrayOf(color, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
                fillPaint.shader = gradient
                // Better base visibility for vapor (min 100 alpha)
                fillPaint.alpha = ((p.alpha * 255).toInt() + 100 + (audioMod * 50).toInt()).coerceIn(0, 255)
                canvas.drawCircle(p.x, p.y, size, fillPaint)
            }
        }

        private fun renderPlasmaWave(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val audioIntensity = currentAudioState.amplitude
            
            // Primary wave
            val path1 = Path()
            for (x in 0..w.toInt() step 10) {
                val nx = x / w
                val y1 = cy + sin(nx * 8 + phase).toFloat() * 150f * currentParams.masterScale * (1f + audioIntensity)
                if (x == 0) path1.moveTo(x.toFloat(), y1) else path1.lineTo(x.toFloat(), y1)
            }
            linePaint.color = color
            linePaint.strokeWidth = 60f
            // Better base visibility (min 150 instead of 127)
            linePaint.alpha = (150 + audioIntensity * 105).toInt().coerceIn(0, 255)
            canvas.drawPath(path1, linePaint)
            
            // Secondary wave
            val path2 = Path()
            for (x in 0..w.toInt() step 10) {
                val nx = x / w
                val y2 = cy + sin(nx * 6 + phase * 1.5f).toFloat() * 120f * currentParams.masterScale * (1f + audioIntensity * 0.5f)
                if (x == 0) path2.moveTo(x.toFloat(), y2) else path2.lineTo(x.toFloat(), y2)
            }
            linePaint.color = Color.WHITE
            linePaint.strokeWidth = 40f
            // Better base visibility for secondary wave (min 100)
            linePaint.alpha = (100 + audioIntensity * 80).toInt().coerceIn(0, 255)
            canvas.drawPath(path2, linePaint)
        }

        private fun renderMercuryDance(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val audioMod = currentAudioState.amplitude * 100
            
            fluidEntities.filterIsInstance<FluidEntity.FluidBlob>().forEach { blob ->
                // Smooth flowing motion
                blob.x += blob.vx * currentParams.masterScale + cos(phase * 0.6f + blob.seed * 8).toFloat() * 3f
                blob.y += blob.vy * currentParams.masterScale + sin(phase * 0.4f + blob.seed * 6).toFloat() * 3f
                
                // Wrap around
                if (blob.x < -100) blob.x = w + 100; if (blob.x > w + 100) blob.x = -100f
                if (blob.y < -100) blob.y = h + 100; if (blob.y > h + 100) blob.y = -100f
                
                // Size variation
                val size = blob.size * (1f + 0.3f * sin(phase * 0.8f + blob.seed).toFloat()) * currentParams.masterScale
                
                // Metallic gradient effect
                val gradient = RadialGradient(blob.x, blob.y, size, 
                    intArrayOf(Color.WHITE, color, Color.GRAY), 
                    floatArrayOf(0f, 0.5f, 1f), 
                    Shader.TileMode.CLAMP)
                fillPaint.shader = gradient
                fillPaint.alpha = (200 + audioMod * 0.5f).toInt().coerceIn(0, 255)
                canvas.drawCircle(blob.x, blob.y, size, fillPaint)
            }
        }

        private fun renderOilCanvas(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, phase: Float) {
            val audioIntensity = currentAudioState.amplitude
            
            // Swirling paint patterns
            repeat(6) { i ->
                val path = Path()
                val centerAngle = i * 60f
                val segments = 36
                for (a in 0..360 step 10) {
                    val adjustedAngle = a + phase * 30f + centerAngle
                    val radius = 150f + 50f * sin(a * 0.05f + phase + i).toFloat() * currentParams.masterScale * (1f + audioIntensity)
                    val px = cx + cos(Math.toRadians(adjustedAngle.toDouble())).toFloat() * radius
                    val py = cy + sin(Math.toRadians(adjustedAngle.toDouble())).toFloat() * radius
                    if (a == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                
                fillPaint.shader = null
                fillPaint.color = if (i % 2 == 0) color else Color.argb(127, Color.red(color), Color.green(color), Color.blue(color))
                // Better base visibility for oil canvas (min 120)
                fillPaint.alpha = (120 + audioIntensity * 100).toInt().coerceIn(0, 255)
                canvas.drawPath(path, fillPaint)
            }
        }

        private fun scaleInverse(val_in: Float): Float = if (val_in != 0f) 1f/val_in else 1f
        
        private val textPaint = Paint().apply {
            color = Color.WHITE; textSize = 40f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
    }
}
