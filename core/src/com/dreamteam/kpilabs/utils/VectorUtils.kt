package com.dreamteam.kpilabs.utils

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.sqrt

data class Vec(val x: Float, val y: Float) {

    operator fun plus(b: Vec) = Vec(x + b.x, y + b.y)

    operator fun minus(b: Vec) = Vec(x - b.x, y - b.y)

    operator fun times(sc: Float) = Vec(x * sc, y * sc)

    operator fun div(sc: Float) = Vec(x / sc, y / sc)

    operator fun unaryMinus() = Vec(-x, -y)

    fun len() = sqrt(x * x + y * y)

    fun dist(b: Vec) = (this - b).len()

    fun limit(max: Float) = if (len() > max) norm() * max else this

    fun norm(): Vec {
        val len = len()
        return if (len == 0f) this else Vec(x / len, y / len)
    }

    fun toVector2() = Vector2(x, y)

    fun toVector3() = Vector3(x, y, 0f)
}

fun dist(a: Vec, b: Vec, pt: Vec): Float {
    val A = a.x - b.x
    val B = a.y - b.y
    val C = pt.x - b.x
    val D = pt.y - b.y
    val E = -D
    val dot = A * E + B * C
    val len_sq = E * E + C * C
    return (abs(dot) / sqrt(len_sq.toDouble())).toFloat()
}