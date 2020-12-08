package com.dreamteam.kpilabs.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.dreamteam.kpilabs.GameSettings.DEER_MAX_SEARCH_SPEED
import com.dreamteam.kpilabs.GameSettings.DEER_NEARBY_RADIUS
import com.dreamteam.kpilabs.GameSettings.DEER_PHYSICS_INFO
import com.dreamteam.kpilabs.GameSettings.DEER_VISION_RADIUS
import com.dreamteam.kpilabs.World
import com.dreamteam.kpilabs.utils.Vec
import com.dreamteam.kpilabs.utils.computeSteeringForce

class Deer(position: Vec) : Entity(
        position,
        Color.YELLOW,
        radius = DEER_RADIUS,
        DEER_PHYSICS_INFO
) {

    companion object {

        private const val DEER_MIN_GROUP = 3

        private const val DEER_RADIUS = 10f

        fun generate(worldInfo: World.WorldInfo) = Deer(generatePos(worldInfo, DEER_RADIUS))
    }

    override val visionRadius: Float = DEER_VISION_RADIUS

    override val runDuration: Float = 25f

    override fun move(entities: List<Entity>, deltaTime: Float, worldInfo: World.WorldInfo) {
        entitiesNearby(entities).let { nearby ->
            nearby.filter {
                it is Wolf || it is Hunter
            }.takeIf {
                it.isNotEmpty()
            }?.let {
                runFromDanger(it, deltaTime, worldInfo)
            } ?: state.takeIf {
                it is State.RUN && it.countDown > 0f
            }?.let {
                continueRunning(deltaTime, worldInfo)
            } ?: nearby.filterIsInstance<Deer>().let { deer ->
                when {
                    deer.isEmpty() -> moveIdle(deltaTime, worldInfo)
                    deer.size < DEER_MIN_GROUP && !checkDeerAround(deer) -> goToOtherDeer(deer, deltaTime, worldInfo)
                    else -> searchGroupDirection(deer, deltaTime, worldInfo)
                }
            }
        }
    }

    private fun goToOtherDeer(deer: List<Deer>, deltaTime: Float, worldInfo: World.WorldInfo) {
        val desire = deer.fold(Vec(0f, 0f)) { acc, deer ->
            acc + deer.position
        } / deer.size.toFloat()
        applyDesirePosition(desire + calcWallsForceVector(worldInfo), deltaTime, worldInfo, DEER_MAX_SEARCH_SPEED)
    }

    private fun checkDeerAround(deer: List<Deer>) = deer.any {
        position.dist(it.position) < DEER_NEARBY_RADIUS
    }

    private fun searchGroupDirection(deer: List<Deer>, deltaTime: Float, worldInfo: World.WorldInfo) {
        val desirePoint = deer.find {
            (it.state as? State.IDLE)?.let {
                it.to != null && !it.copy
            } ?: false
        }?.let {
            (it.state as? State.IDLE)?.copyState(it.position)?.let { stateCopy ->
                stateCopy.to?.let {
                    state = stateCopy
                    it + position + calcWallsForceVector(worldInfo)
                }
            }
        } ?: return Unit.also {
            moveIdle(deltaTime, worldInfo)
        }

        computeSteeringForce(
                position,
                desirePoint,
                velocity,
                physicsInfo.maxIdleSpeed,
                physicsInfo.maxIdleForce
        ).also {
            applyForce(it, deltaTime, worldInfo)
        }
    }

    override fun draw(render: ShapeRenderer) {
        super.draw(render)
        state.let {
            if (it is State.IDLE && it.copy) render.apply {
                color = Color.GOLDENROD
                begin(ShapeRenderer.ShapeType.Filled)
                circle(position.x, position.y, radius)
                end()
            }
        }
    }
}