package com.dreamteam.kpilabs.entity

import com.badlogic.gdx.graphics.Color
import com.dreamteam.kpilabs.GameSettings.WOLF_PHYSICS_INFO
import com.dreamteam.kpilabs.GameSettings.WOLF_VISION_RADIUS
import com.dreamteam.kpilabs.World
import com.dreamteam.kpilabs.utils.Vec
import com.dreamteam.kpilabs.utils.computeSteeringForce

class Wolf(position: Vec) : Entity(
        position,
        Color.BROWN,
        radius = WOLF_RADIUS,
        WOLF_PHYSICS_INFO
) {

    companion object {

        private const val WOLF_RADIUS = 8f
        private const val WOLF_EAT_DURATION = 5f
        private const val LIFE_TIME_WITHOUT_MEAL = 100f

        fun generate(worldInfo: World.WorldInfo) = Wolf(generatePos(worldInfo, WOLF_RADIUS))
    }

    private var lifeTimeWithoutMeal = LIFE_TIME_WITHOUT_MEAL

    override val visionRadius: Float = WOLF_VISION_RADIUS

    override val runDuration: Float = 5f

    override fun move(entities: List<Entity>, deltaTime: Float, worldInfo: World.WorldInfo) {
        if (state is State.EAT) return run {
            eat(deltaTime)
        } else {
            lifeTimeWithoutMeal -= deltaTime
            if (lifeTimeWithoutMeal < 0) return run {
                die()
            }
        }
        entitiesNearby(entities).filter {
            it is Rabbit || it is Deer || it is Hunter
        }.takeIf {
            it.isNotEmpty()
        }?.map {
            position.dist(it.position) to it
        }?.let {
            val victim = it.sortedBy { it.first }.first().second
            if (victim.position.dist(position) < victim.radius + radius) {
                victim.die()
                state = State.EAT(WOLF_EAT_DURATION)
                velocity = Vec(0f, 0f)
            } else {
                runForVictim(victim, deltaTime, worldInfo)
            }
        } ?: kotlin.run {
            moveIdle(deltaTime, worldInfo)
        }
    }

    private fun eat(deltaTime: Float) {
        (state as? State.EAT)?.let {
            lifeTimeWithoutMeal = LIFE_TIME_WITHOUT_MEAL
            val tm = it.countDown - deltaTime
            state = if (tm <= 0f) State.IDLE() else State.EAT(tm)
        }
    }

    private fun runForVictim(ent: Entity, deltaTime: Float, worldInfo: World.WorldInfo) {
        val delta = (ent.position - position).norm()
        state = State.RUN()
        applyForce(
                computeSteeringForce(position, position + delta + calcWallsForceVector(worldInfo), velocity, physicsInfo.maxSpeed, physicsInfo.maxForce),
                deltaTime,
                worldInfo
        )
    }
}