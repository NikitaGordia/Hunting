package com.dreamteam.kpilabs.entity

import com.badlogic.gdx.graphics.Color
import com.dreamteam.kpilabs.GameSettings.RABBIT_PHYSICS_INFO
import com.dreamteam.kpilabs.GameSettings.RABBIT_VISION_RADIUS
import com.dreamteam.kpilabs.World
import com.dreamteam.kpilabs.utils.Vec

class Rabbit(
        position: Vec
) : Entity(
        position,
        Color.WHITE,
        RABBIT_RADIUS,
        RABBIT_PHYSICS_INFO
) {

    companion object {

        private const val RABBIT_RADIUS = 4f

        fun generate(worldInfo: World.WorldInfo) = Rabbit(generatePos(worldInfo, RABBIT_RADIUS))
    }

    override val visionRadius: Float = RABBIT_VISION_RADIUS

    override val runDuration: Float = 35f

    override fun move(entities: List<Entity>, deltaTime: Float, worldInfo: World.WorldInfo) {
        entitiesNearby(entities).takeIf {
            it.isNotEmpty()
        }?.let {
            runFromDanger(it, deltaTime, worldInfo)
        } ?: state.takeIf {
            it is State.RUN && it.countDown > 0f
        }?.let {
            continueRunning(deltaTime, worldInfo)
        } ?: run {
            moveIdle(deltaTime, worldInfo)
        }
    }
}