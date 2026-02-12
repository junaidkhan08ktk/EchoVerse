package com.example.echoverse.domain.utils

import kotlin.math.floor

object SimplexNoise {
    private val textureGrad3 = intArrayOf(
        1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1, 0,
        1, 0, 1, -1, 0, 1, 1, 0, -1, -1, 0, -1,
        0, 1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1
    )
    private val p = IntArray(256)
    private val perm = IntArray(512)
    private val permMod12 = IntArray(512)

    init {
        for (i in 0 until 256) {
            p[i] = floor(Math.random() * 256).toInt()
        }
        for (i in 0 until 512) {
            perm[i] = p[i and 255]
            permMod12[i] = perm[i] % 12
        }
    }

    private fun dot(g: IntArray, x: Double, y: Double): Double {
        return g[0] * x + g[1] * y
    }

    private fun dot(g: IntArray, x: Double, y: Double, z: Double): Double {
        return g[0] * x + g[1] * y + g[2] * z
    }

    // 2D Simplex Noise
    fun noise(xin: Double, yin: Double): Double {
        var n0: Double
        var n1: Double
        var n2: Double
        val F2 = 0.5 * (Math.sqrt(3.0) - 1.0)
        val s = (xin + yin) * F2
        val i = floor(xin + s).toInt()
        val j = floor(yin + s).toInt()
        val G2 = (3.0 - Math.sqrt(3.0)) / 6.0
        val t = (i + j) * G2
        val X0 = i - t; val Y0 = j - t
        val x0 = xin - X0; val y0 = yin - Y0
        var i1: Int; var j1: Int
        if (x0 > y0) { i1 = 1; j1 = 0 } else { i1 = 0; j1 = 1 }
        val x1 = x0 - i1 + G2; val y1 = y0 - j1 + G2
        val x2 = x0 - 1.0 + 2.0 * G2; val y2 = y0 - 1.0 + 2.0 * G2
        val ii = i and 255; val jj = j and 255
        val gi0 = permMod12[ii + perm[jj]]
        val gi1 = permMod12[ii + i1 + perm[jj + j1]]
        val gi2 = permMod12[ii + 1 + perm[jj + 1]]
        var t0 = 0.5 - x0 * x0 - y0 * y0
        if (t0 < 0) n0 = 0.0 else {
            t0 *= t0; n0 = t0 * t0 * dot(intArrayOf(textureGrad3[gi0 * 3], textureGrad3[gi0 * 3 + 1]), x0, y0)
        }
        var t1 = 0.5 - x1 * x1 - y1 * y1
        if (t1 < 0) n1 = 0.0 else {
            t1 *= t1; n1 = t1 * t1 * dot(intArrayOf(textureGrad3[gi1 * 3], textureGrad3[gi1 * 3 + 1]), x1, y1)
        }
        var t2 = 0.5 - x2 * x2 - y2 * y2
        if (t2 < 0) n2 = 0.0 else {
            t2 *= t2; n2 = t2 * t2 * dot(intArrayOf(textureGrad3[gi2 * 3], textureGrad3[gi2 * 3 + 1]), x2, y2)
        }
        return 70.0 * (n0 + n1 + n2)
    }

    // 3D Simplex Noise
    fun noise(xin: Double, yin: Double, zin: Double): Double {
        var n0: Double
        var n1: Double
        var n2: Double
        var n3: Double
        val F3 = 1.0 / 3.0
        val s = (xin + yin + zin) * F3
        val i = floor(xin + s).toInt()
        val j = floor(yin + s).toInt()
        val k = floor(zin + s).toInt()
        val G3 = 1.0 / 6.0
        val t = (i + j + k) * G3
        val X0 = i - t; val Y0 = j - t; val Z0 = k - t
        val x0 = xin - X0; val y0 = yin - Y0; val z0 = zin - Z0
        var i1: Int; var j1: Int; var k1: Int
        var i2: Int; var j2: Int; var k2: Int
        if (x0 >= y0) {
            if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0 }
            else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1 }
            else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1 }
        } else {
            if (y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1 }
            else if (x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1 }
            else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0 }
        }
        val x1 = x0 - i1 + G3; val y1 = y0 - j1 + G3; val z1 = z0 - k1 + G3
        val x2 = x0 - i2 + 2.0 * G3; val y2 = y0 - j2 + 2.0 * G3; val z2 = z0 - k2 + 2.0 * G3
        val x3 = x0 - 1.0 + 3.0 * G3; val y3 = y0 - 1.0 + 3.0 * G3; val z3 = z0 - 1.0 + 3.0 * G3
        val ii = i and 255; val jj = j and 255; val kk = k and 255
        val gi0 = permMod12[ii + perm[jj + perm[kk]]]
        val gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]]
        val gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]]
        val gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]]
        var t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0
        if (t0 < 0) n0 = 0.0 else {
            t0 *= t0; n0 = t0 * t0 * dot(intArrayOf(textureGrad3[gi0 * 3], textureGrad3[gi0 * 3 + 1], textureGrad3[gi0 * 3 + 2]), x0, y0, z0)
        }
        var t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1
        if (t1 < 0) n1 = 0.0 else {
            t1 *= t1; n1 = t1 * t1 * dot(intArrayOf(textureGrad3[gi1 * 3], textureGrad3[gi1 * 3 + 1], textureGrad3[gi1 * 3 + 2]), x1, y1, z1)
        }
        var t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2
        if (t2 < 0) n2 = 0.0 else {
            t2 *= t2; n2 = t2 * t2 * dot(intArrayOf(textureGrad3[gi2 * 3], textureGrad3[gi2 * 3 + 1], textureGrad3[gi2 * 3 + 2]), x2, y2, z2)
        }
        var t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3
        if (t3 < 0) n3 = 0.0 else {
            t3 *= t3; n3 = t3 * t3 * dot(intArrayOf(textureGrad3[gi3 * 3], textureGrad3[gi3 * 3 + 1], textureGrad3[gi3 * 3 + 2]), x3, y3, z3)
        }
        return 32.0 * (n0 + n1 + n2 + n3)
    }
}
