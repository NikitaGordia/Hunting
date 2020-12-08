package com.dreamteam.kpilabs.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.dreamteam.kpilabs.GameSettings
import com.dreamteam.kpilabs.HuntingGame
import com.dreamteam.kpilabs.World
import com.dreamteam.kpilabs.utils.Vec
import com.dreamteam.kpilabs.utils.computeSteeringForce
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

abstract class Entity(
        var position: Vec,
        private val color: Color,
        val radius: Float,
        val physicsInfo: GameSettings.PhysicsInfo
) {

    companion object {

        private val VISION_COLOR = Color(1f, 1f, 1f, 0.1f)
        private val TARGET_COLOR = Color.RED
        private val TARGET_SIZE = 5f

        private const val WALLS_TRIGGER_DIST = 60f
        private const val WALLS_TRIGGER_ACC_MAX = 50f

        fun generatePos(worldInfo: World.WorldInfo, radius: Float) =
                Vec(Random.nextFloat() * (worldInfo.width - 2 * radius) + radius,
                        Random.nextFloat() * (worldInfo.height - 2 * radius) + radius)
    }

    protected var state: State = State.IDLE()

    protected var velocity: Vec = Vec(0f, 0f)

    abstract val visionRadius: Float
    open val runDuration: Float = 0f
    var isScared = false

    fun askToMove(entities: List<Entity>, deltaTime: Float, worldInfo: World.WorldInfo) {
        if (!worldInfo.checkPosition(position)) {
            die()
        }
        if (state is State.DEATH) return
        move(entities, deltaTime, worldInfo)
    }

    protected abstract fun move(entities: List<Entity>, deltaTime: Float, worldInfo: World.WorldInfo)

    fun die() {
        state = State.DEATH
    }

    fun isAlive() = state !is State.DEATH

    fun isRunning() = state is State.RUN

    fun applyWorldForce(force: Vec, deltaTime: Float, worldInfo: World.WorldInfo) = applyForce(force, deltaTime, worldInfo)

    open fun draw(render: ShapeRenderer) {
        if (state is State.DEATH) return

        if (HuntingGame.DISPLAY_VISIBLE_CIRCLE) {
            render.color = VISION_COLOR
            render.begin(ShapeRenderer.ShapeType.Line)
            render.circle(position.x, position.y, visionRadius)
            render.end()
        }

        if (HuntingGame.DISPLAY_TARGET) {
            (state as? State.IDLE)?.to?.let {
                render.color = TARGET_COLOR
                render.begin(ShapeRenderer.ShapeType.Filled)
                render.rect(it.x - TARGET_SIZE / 2f, it.y - TARGET_SIZE / 2f, TARGET_SIZE, TARGET_SIZE)
                render.end()
            }
        }

        if (HuntingGame.DISPLAY_VELOCITY_VECTOR) {
            render.color = Color.GOLD
            render.begin(ShapeRenderer.ShapeType.Filled)
            render.rectLine(position.toVector2(), (position + velocity).toVector2(), 5f)
            render.end()
        }

        render.color = color
        render.begin(ShapeRenderer.ShapeType.Filled)
        render.circle(position.x, position.y, radius)
        render.end()
    }

    fun runFromDanger(entities: List<Entity>, deltaTime: Float, worldInfo: World.WorldInfo) {
        val delta = -entities.fold(Vec(0f, 0f), { acc, ent ->
            acc + (ent.position - position)
        }).norm()
        state = State.RUN(runDuration, delta)
        val to = position + delta + calcWallsForceVector(worldInfo)
        applyDesirePosition(to, deltaTime, worldInfo)
    }

    protected fun continueRunning(deltaTime: Float, worldInfo: World.WorldInfo) {
        (state as? State.RUN)?.let {
            state = State.RUN(it.countDown - deltaTime, it.vec)
            val desirePos = position + it.vec * (it.countDown / runDuration) + calcWallsForceVector(worldInfo)
            applyForce(
                    computeSteeringForce(position, desirePos, velocity, physicsInfo.maxSpeed, physicsInfo.maxForce),
                    deltaTime,
                    worldInfo
            )
        }
    }

    protected fun moveIdle(deltaTime: Float, worldInfo: World.WorldInfo) {
        state.let {
            state = if (it is State.IDLE && !it.copy) state else State.IDLE()
        }

        ((state as? State.IDLE)?.to?.takeIf {
            position.dist(it) > radius
        } ?: chooseIdlePoint(worldInfo).also {
            state = State.IDLE(it)
        }).let { to ->
            applyForce(
                    computeSteeringForce(position, to, velocity, physicsInfo.maxIdleSpeed, physicsInfo.maxIdleForce),
                    deltaTime,
                    worldInfo
            )
        }
    }

    protected fun applyDesirePosition(
            to: Vec,
            deltaTime: Float,
            worldInfo: World.WorldInfo,
            mxSpeed: Float = physicsInfo.maxSpeed
    ) {
        applyForce(
                computeSteeringForce(position, to, velocity, mxSpeed, physicsInfo.maxForce),
                deltaTime,
                worldInfo
        )
    }

    protected fun applyForce(acc: Vec, deltaTime: Float, worldInfo: World.WorldInfo) {
        velocity += acc.limit(physicsInfo.maxForce) / physicsInfo.mass
        velocity -= velocity.norm() * min(velocity.len(), physicsInfo.mass * physicsInfo.frictionK)
        velocity = velocity.limit(physicsInfo.maxSpeed)
        position = worldInfo.adjustBounds(position + velocity * deltaTime)
    }

    protected fun entitiesNearby(entities: List<Entity>) = entities.filter {
        position.dist(it.position) < visionRadius
    }

    protected fun calcWallsForceVector(worldInfo: World.WorldInfo) = Vec(when {
        position.x < WALLS_TRIGGER_DIST ->
            (WALLS_TRIGGER_DIST - position.x) / WALLS_TRIGGER_DIST * WALLS_TRIGGER_ACC_MAX
        position.x > worldInfo.width - WALLS_TRIGGER_DIST ->
            (worldInfo.width - WALLS_TRIGGER_DIST - position.x) / WALLS_TRIGGER_DIST * WALLS_TRIGGER_ACC_MAX
        else -> 0f
    }, when {
        position.y < WALLS_TRIGGER_DIST ->
            (WALLS_TRIGGER_DIST - position.y) / WALLS_TRIGGER_DIST * WALLS_TRIGGER_ACC_MAX
        position.y > worldInfo.height - WALLS_TRIGGER_DIST ->
            (worldInfo.height - WALLS_TRIGGER_DIST - position.y) / WALLS_TRIGGER_DIST * WALLS_TRIGGER_ACC_MAX
        else -> 0f
    })

    protected fun chooseIdlePoint(worldInfo: World.WorldInfo, distFromBounds: Float = physicsInfo.mass * radius): Vec =
            listOf(
                    Random.nextFloat() * PI / 2,
                    PI / 2 + Random.nextFloat() * PI / 2,
                    PI + Random.nextFloat() * PI / 2,
                    PI * 3 / 2 + Random.nextFloat() * PI / 2,
            ).map { rad ->
                val r = Random.nextFloat() * visionRadius
                position + Vec((cos(rad) * r).toFloat(), (sin(rad) * r).toFloat())
            }.filter { to ->
                worldInfo.checkPosition(to, distFromBounds)
            }.let { list ->
                list.takeIf {
                    it.isNotEmpty()
                }?.let {
                    list[Random.nextInt(list.size)]
                } ?: Vec(0f, 0f).also {
                    state = State.DEATH
                }
            }

    sealed class State {
        class IDLE(val to: Vec? = null, val copy: Boolean = false) : State() {

            fun copyState(position: Vec) = IDLE(to!! - position, true)
        }

        object DEATH : State()
        class EAT(val countDown: Float = 0f) : State()
        class RUN(val countDown: Float = 0f, val vec: Vec = Vec(0f, 0f)) : State()
    }
}