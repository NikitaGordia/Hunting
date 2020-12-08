package com.dreamteam.kpilabs

object GameSettings {

    const val WORLD_SCALE = 4f
    const val CAMERA_SCALE = 1.5f

    const val FRICTION_K = 0.2f
    const val HUNTER_MOVING_FORCE = 100f

    const val RABBIT_VISION_RADIUS = 200f
    const val WOLF_VISION_RADIUS = 190f
    const val DEER_VISION_RADIUS = 200f
    const val HUNTER_VISION_RADIUS = 260f

    const val DEER_NEARBY_RADIUS = 70f

    const val DEER_MAX_SEARCH_SPEED = 30f

    const val SHOOTING_RUN = 1000f

    const val SHOOTING_RANGES_DELTA = 60f
    val SHOOTING_RANGES = listOf(
            200f to 1f,
            210f to 0.8f,
            225f to 0.3f,
            270f to 0.1f,
            360f to 0.01f
    )

    data class PhysicsInfo(
            val mass: Float,
            val maxSpeed: Float,
            val maxForce: Float,
            val maxIdleSpeed: Float = 0f,
            val maxIdleForce: Float = 0f,
            val frictionK: Float = FRICTION_K
    )

    val RABBIT_PHYSICS_INFO = PhysicsInfo(mass = 1f, maxSpeed = 200f, maxForce = 20f, maxIdleSpeed = 50f, maxIdleForce = 1f)
    val WOLF_PHYSICS_INFO = PhysicsInfo(mass = 3f, maxSpeed = 150f, maxForce = 100f, maxIdleSpeed = 20f, maxIdleForce = 6f)
    val DEER_PHYSICS_INFO = PhysicsInfo(mass = 10f, maxSpeed = 130f, maxForce = 50f, maxIdleSpeed = 40f, maxIdleForce = 23f)
    val HUNTER_PHYSICS_INFO = PhysicsInfo(mass = 8f, maxSpeed = 80f, maxForce = 100f, frictionK = 0.65f)
}