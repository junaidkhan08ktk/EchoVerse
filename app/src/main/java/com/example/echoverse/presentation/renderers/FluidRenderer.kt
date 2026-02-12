package com.example.echoverse.presentation.renderers

import android.graphics.*
import com.example.echoverse.domain.model.AudioState
import com.example.echoverse.domain.model.TouchState
import com.example.echoverse.domain.utils.SimplexNoise
import kotlin.math.*
import kotlin.random.Random

class FluidRenderer {
    private var width: Float = 0f
    private var height: Float = 0f
    private var density: Float = 1f
    private var activeWorldId: String = ""
    
    // Normalized Time
    private var time: Float = 0f
    
    // Simulation State
    private val particles = mutableListOf<FluidParticle>()
    
    // Paints
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val linePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val blurPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL } // Initialized later with MaskFilter

    data class FluidParticle(
        var x: Float, var y: Float, // Normalized 0..1
        var vx: Float, var vy: Float,
        var life: Float, var maxLife: Float,
        var size: Float, var baseSize: Float,
        var color: Int,
        var phase: Float = 0f
    )

    fun setSurfaceSize(w: Float, h: Float, d: Float) {
        width = w
        height = h
        density = d
    }

    fun setWorld(worldId: String) {
        if (activeWorldId != worldId) {
            activeWorldId = worldId
            resetSimulation()
        }
    }

    fun resetSimulation() {
        particles.clear()
        time = 0f
        // Initial population
        when (activeWorldId) {
            "f1" -> initLiquidDream()
            "f2" -> initLiquidMarble()
            "f3" -> initNeonPlasma()
            "f4" -> initMercuryDance()
            "f5" -> initChromaticFlow()
        }
    }

    fun update(dt: Float, audio: AudioState, touch: TouchState) {
        // Clamp dt to avoid explosions
        val deltaTime = dt.coerceIn(0.001f, 0.05f)
        time += deltaTime

        when (activeWorldId) {
            "f1" -> updateLiquidDream(deltaTime, audio, touch)
            "f2" -> updateLiquidMarble(deltaTime, audio, touch)
            "f3" -> updateNeonPlasma(deltaTime, audio, touch)
            "f4" -> updateMercuryDance(deltaTime, audio, touch)
            "f5" -> updateChromaticFlow(deltaTime, audio, touch)
        }
    }

    fun render(canvas: Canvas) {
        if (width == 0f || height == 0f) return

        when (activeWorldId) {
            "f1" -> renderLiquidDream(canvas)
            "f2" -> renderLiquidMarble(canvas)
            "f3" -> renderNeonPlasma(canvas)
            "f4" -> renderMercuryDance(canvas)
            "f5" -> renderChromaticFlow(canvas)
        }
    }

    // =========================================================================
    // 1. LIQUID DREAM (f1)
    // Dreamy, colorful, floating nebulas.
    // =========================================================================
    private fun initLiquidDream() {
        particles.clear()
        // Spawn colorful dream particles
        repeat(20) {
            val color = if (it % 2 == 0) 0xFF00FFFF.toInt() else 0xFFFF00FF.toInt() // Cyan & Magenta
            particles.add(FluidParticle(
                x = Random.nextFloat(), y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.05f,
                vy = (Random.nextFloat() - 0.5f) * 0.05f,
                life = 1f, maxLife = 1f, // Infinite life cycle
                size = 0.3f + Random.nextFloat() * 0.3f, baseSize = 0.4f,
                color = color,
                phase = Random.nextFloat() * 100f
            ))
        }
    }

    private fun updateLiquidDream(dt: Float, audio: AudioState, touch: TouchState) {
        // Slow dreamy movement
        for (p in particles) {
            val noiseX = SimplexNoise.noise(p.x * 1.5, p.y * 1.5, time * 0.2 + p.phase)
            val noiseY = SimplexNoise.noise(p.x * 1.5 + 100, p.y * 1.5 + 100, time * 0.2 + p.phase)
            
            p.vx += (noiseX * 0.2f * dt).toFloat()
            p.vy += (noiseY * 0.2f * dt).toFloat()
            
            // Touch repel
            if (touch.isPressed) {
                val dx = p.x * width - touch.lastX
                val dy = p.y * height - touch.lastY
                val dist = sqrt(dx*dx + dy*dy)
                if (dist < width * 0.3f) {
                    p.vx += (dx/dist) * 2f * dt
                    p.vy += (dy/dist) * 2f * dt
                }
            }

            p.x += p.vx * dt
            p.y += p.vy * dt
            
            // Damping and boundary bounce
            p.vx *= 0.95f
            p.vy *= 0.95f
            if (p.x < -0.2f) p.x = 1.2f
            if (p.x > 1.2f) p.x = -0.2f
            if (p.y < -0.2f) p.y = 1.2f
            if (p.y > 1.2f) p.y = -0.2f
            
            // Audio pulse size
            p.size = p.baseSize * (1f + audio.bass * 0.3f)
        }
    }

    private fun renderLiquidDream(canvas: Canvas) {
        // Deep Purple Background
        paint.shader = LinearGradient(0f, 0f, 0f, height, 
            intArrayOf(0xFF1A0033.toInt(), 0xFF000033.toInt()), null, Shader.TileMode.CLAMP)
        paint.alpha = 255
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null

        // Additive blending for glowing nebula effect
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        
        for (p in particles) {
            val radius = p.size * width
            // Radial Gradient for soft orb
            paint.shader = RadialGradient(p.x * width, p.y * height, radius,
                intArrayOf(p.color, 0x00000000), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            paint.alpha = 150
            canvas.drawCircle(p.x * width, p.y * height, radius, paint)
        }
        paint.xfermode = null // Reset
        paint.shader = null
    }

    // =========================================================================
    // 2. LIQUID MARBLE
    // Swirling lines distorted by noise.
    // =========================================================================
    private fun initLiquidMarble() { /* Procedural */ }
    
    private fun updateLiquidMarble(dt: Float, audio: AudioState, touch: TouchState) { /* Procedural */ }

    private fun renderLiquidMarble(canvas: Canvas) {
        canvas.drawColor(0xFF101010.toInt())
        linePaint.strokeWidth = 3f * density
        linePaint.style = Paint.Style.STROKE

        val lines = 30
        val steps = 40
        
        for (i in 0 until lines) {
            val path = Path()
            var started = false
            
            // Interaction warp
            val warpTime = time * 0.2f
            
            for (j in 0..steps) {
                val u = j.toFloat() / steps
                val v = i.toFloat() / lines
                
                // Domain Warp
                val n1 = SimplexNoise.noise(u * 3.0, v * 3.0, warpTime.toDouble()).toFloat()
                val n2 = SimplexNoise.noise(u * 3.0 + 100, v * 3.0 + 100, warpTime.toDouble()).toFloat()
                
                val finalX = (u + n1 * 0.2f) * width
                val finalY = (v + n2 * 0.2f) * height
                
                if (!started) { path.moveTo(finalX, finalY); started = true }
                else path.lineTo(finalX, finalY)
            }
            
            val hue = (i * 10 + time * 10) % 360
            linePaint.color = Color.HSVToColor(180, floatArrayOf(hue, 0.7f, 0.9f))
            canvas.drawPath(path, linePaint)
        }
    }

    // =========================================================================
    // 3. NEON PLASMA
    // Electric, glowing, additive blending.
    // =========================================================================
    private fun initNeonPlasma() {
        particles.clear()
        repeat(15) { 
            particles.add(FluidParticle(
                x = Random.nextFloat(), y = Random.nextFloat(),
                vx = (Random.nextFloat()-0.5f)*0.1f, vy = (Random.nextFloat()-0.5f)*0.1f,
                life = 1f, maxLife = 1f,
                size = 0.2f + Random.nextFloat() * 0.3f, baseSize = 0.3f,
                color = Color.HSVToColor(floatArrayOf(Random.nextFloat()*360, 1f, 1f)),
                phase = Random.nextFloat() * 100f
            ))
        }
    }

    private fun updateNeonPlasma(dt: Float, audio: AudioState, touch: TouchState) {
        for (p in particles) {
            p.x += p.vx * dt + (SimplexNoise.noise(p.x * 2.0, time * 0.5).toFloat() * 0.1f * dt)
            p.y += p.vy * dt
            
            // Boundary bounce
            if(p.x < 0 || p.x > 1) p.vx *= -1
            if(p.y < 0 || p.y > 1) p.vy *= -1
            
            // Audio expands size
            p.size = p.baseSize * (1f + audio.high * 0.5f)
        }
    }

    private fun renderNeonPlasma(canvas: Canvas) {
        canvas.drawColor(0xFF050010.toInt())
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        
        for (p in particles) {
             val r = p.size * width
             val cx = p.x * width
             val cy = p.y * height
             
             // Radial glow
             paint.shader = RadialGradient(cx, cy, r,
                intArrayOf(p.color, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
             paint.alpha = 150
             canvas.drawCircle(cx, cy, r, paint)
        }
        paint.xfermode = null
        paint.shader = null
    }
    
    // =========================================================================
    // 4. MERCURY DANCE (f4)
    // Liquid metal blobs merging.
    // =========================================================================
    private fun initMercuryDance() {
        particles.clear()
        // Silver blobs
        repeat(15) { 
            particles.add(FluidParticle(
                x = Random.nextFloat(), y = Random.nextFloat(),
                vx = (Random.nextFloat()-0.5f)*0.1f, vy = (Random.nextFloat()-0.5f)*0.1f,
                life = 1f, maxLife = 1f,
                size = 0.15f + Random.nextFloat() * 0.1f, baseSize = 0.15f,
                color = 0xFFCCCCCC.toInt(), // White/Silver
                phase = Random.nextFloat() * 100f
            ))
        }
    }
    
    private fun updateMercuryDance(dt: Float, audio: AudioState, touch: TouchState) {
        // Cohesion: particles attract each other
        for (i in particles.indices) {
            val p1 = particles[i]
            
            // Touch attraction (Magnetic)
            if (touch.isPressed) {
                val tx = touch.lastX / width
                val ty = touch.lastY / height
                val dx = tx - p1.x
                val dy = ty - p1.y
                p1.vx += dx * 2.0f * dt
                p1.vy += dy * 2.0f * dt
            }
            
            p1.x += p1.vx * dt
            p1.y += p1.vy * dt
            p1.vx *= 0.92f // Heavy damping for mercury feel
            p1.vy *= 0.92f
            
            // Bounce
            if (p1.x < 0 || p1.x > 1) { p1.vx *= -1; p1.x = p1.x.coerceIn(0f, 1f) }
            if (p1.y < 0 || p1.y > 1) { p1.vy *= -1; p1.y = p1.y.coerceIn(0f, 1f) }
            
            p1.size = p1.baseSize * (1f + audio.mid * 0.5f)
        }
    }
    
    private fun renderMercuryDance(canvas: Canvas) {
        canvas.drawColor(0xFF000000.toInt())
        
        // Pseudo-metaball via soft additive gradients that blow out to white in center
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
        
        for (p in particles) {
            val r = p.size * width
            // Gradient: White center -> Gray edge -> Transparent
            paint.shader = RadialGradient(p.x * width, p.y * height, r,
                intArrayOf(0xFFFFFFFF.toInt(), 0xFF888888.toInt(), 0x00000000), 
                floatArrayOf(0f, 0.4f, 1f), 
                Shader.TileMode.CLAMP)
                
            paint.alpha = 255
            canvas.drawCircle(p.x * width, p.y * height, r, paint)
        }
        paint.xfermode = null
        paint.shader = null
    }

    // =========================================================================
    // 5. CHROMATIC FLOW
    // =========================================================================
    private fun initChromaticFlow() { /* Procedural */ }
    private fun updateChromaticFlow(dt: Float, audio: AudioState, touch: TouchState) { /* Procedural */ }
    
    private fun renderChromaticFlow(canvas: Canvas) {
        canvas.drawColor(0xFF000000.toInt())
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 5f * density
        
        val layers = 7
        for (i in 0 until layers) {
             val path = Path()
             val yCenter = height * (0.2f + 0.6f * (i.toFloat() / layers))
             
             var first = true
             for (x in 0..20) {
                 val u = x / 20f
                 val noise = SimplexNoise.noise(u * 2.0, i * 1.0, time * 0.4)
                 val amp = height * 0.15f
                 val py = yCenter + noise * amp
                 val px = u * width
                 if (first) { path.moveTo(px, py.toFloat()); first = false }
                 else path.lineTo(px, py.toFloat())
             }
             
             val col1 = Color.HSVToColor(floatArrayOf((time * 20 + i * 40)%360, 0.8f, 1f))
             
             linePaint.color = col1
             linePaint.alpha = 200
             canvas.drawPath(path, linePaint)
        }
    }
}
