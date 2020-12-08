package com.dreamteam.kpilabs.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import java.util.Timer
import kotlin.concurrent.schedule
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Polygon
import com.dreamteam.kpilabs.GameSettings.HUNTER_PHYSICS_INFO
import com.dreamteam.kpilabs.GameSettings.HUNTER_VISION_RADIUS
import com.dreamteam.kpilabs.GameSettings.SHOOTING_RANGES
import com.dreamteam.kpilabs.GameSettings.SHOOTING_RANGES_DELTA
import com.dreamteam.kpilabs.GameSettings.SHOOTING_RUN
import com.dreamteam.kpilabs.World
import com.dreamteam.kpilabs.utils.Vec
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.random.Random

class Hunter(position: Vec) : Entity(
        position,
        color = Color.SALMON,
        radius = HUNTER_RADIUS,
        HUNTER_PHYSICS_INFO
) {

    companion object {

        private const val HUNTER_RADIUS = 8f

        private const val GUN_WIDTH = 6f
        private const val GUN_HEIGHT = 25f

        private const val BULLET_SPEED = 10f
        private const val BULLET_RADIUS = 2f
        private val BULLET_RELOAD = TimeUnit.SECONDS.toMillis(2L)

        fun generate(worldInfo: World.WorldInfo) = Hunter(Vec(worldInfo.width / 2, worldInfo.height / 2))
    }

    private var pointer: Vec = Vec(0f, 0f)
    private val gun: Polygon
    private var bullets = mutableListOf<Bullet>()
    private var reloadTime = 0L
    private var bulletsCount = 20

    override val visionRadius: Float
        get() = HUNTER_VISION_RADIUS

    init {
        gun = Polygon(floatArrayOf(0f, 0f, GUN_WIDTH, 0f, GUN_WIDTH, GUN_HEIGHT, 0f, GUN_HEIGHT)).apply {
            setOrigin(GUN_WIDTH / 2f, 0f)
        }
    }

    override fun move(entities: List<Entity>, deltaTime: Float, worldInfo: World.WorldInfo) {
        bullets.forEach { bullet ->
            bullet.position = bullet.position + bullet.dir
            entities.forEach { ent ->
                val dist = bullet.position.dist(ent.position)
                when {
                    dist <= ent.radius && checkShootingRanges(bullet) -> {
                        ent.die()
                        bullet.position = Vec(Float.NEGATIVE_INFINITY, -Float.NEGATIVE_INFINITY)
                        return@forEach
                    }
                    dist <= SHOOTING_RUN && !ent.isScared -> {
                        ent.isScared = true
                        Timer("Running from Danger", false).schedule(1000) {
                            ent.isScared = false
                            ent.runFromDanger(listOf(this@Hunter), deltaTime, worldInfo)
                        }
                    }
                }
            }
        }
        bullets = bullets.filter {
            it.origin.dist(it.position) <= SHOOTING_RANGES[SHOOTING_RANGES.size - 1].first && worldInfo.checkPosition(it.position)
        }.toMutableList()
    }

    fun updatePointer(pointer: Vec) {
        this.pointer = (pointer - position).norm()
    }

    fun shoot(): Boolean {
        val now = System.currentTimeMillis()
        return if (now - reloadTime > BULLET_RELOAD && bulletsCount > 0) {
            bulletsCount -= 1
            reloadTime = now
            bullets.add(Bullet(position.copy(), pointer * BULLET_SPEED, position))
            true
        } else {
            bulletsCount > 0
        }
    }

    override fun draw(render: ShapeRenderer) {
        render.apply {
            drawShootingRanges()
            drawGun()
            super.draw(render)
            drawBullets()
            reloadTime()
        }
    }

    private fun ShapeRenderer.drawShootingRanges() {
        color = Color(105 / 255f, 16 / 255f, 13 / 255f, 0.5f)
        for ((dist, _) in calcShootingRanges()) {
            begin(ShapeRenderer.ShapeType.Line)
            circle(position.x, position.y, dist)
            end()
        }
    }

    private fun ShapeRenderer.drawBullets() {
        color = Color.GOLD
        bullets.forEach {
            begin(ShapeRenderer.ShapeType.Filled)
            circle(it.position.x, it.position.y, BULLET_RADIUS)
            end()
        }
    }

    private fun ShapeRenderer.reloadTime() {
        begin(ShapeRenderer.ShapeType.Filled)
        arc(
                position.x,
                position.y,
                radius + 2f,
                90f,
                360 * min(1f, (System.currentTimeMillis() - reloadTime).toFloat() / BULLET_RELOAD)
        )
        end()
    }

    private fun ShapeRenderer.drawGun() {
        color = Color.GOLD
        begin(ShapeRenderer.ShapeType.Line)
        gun.setPosition(position.x - GUN_WIDTH / 2, position.y)
        gun.rotation = 360 - atan2(pointer.x, pointer.y) * 360f / 2f / PI.toFloat()
        polygon(gun.transformedVertices)
        end()
    }

    private fun checkShootingRanges(bullet: Bullet): Boolean {
        val dist = bullet.dist()
        for ((distLimit, score) in calcShootingRanges()) {
            if (dist <= distLimit)
                return score > Random.nextFloat()
        }
        return false
    }

    private fun calcShootingRanges() = SHOOTING_RANGES.map {
        (it.first - SHOOTING_RANGES_DELTA * velocity.len() / physicsInfo.maxSpeed) to it.second
    }

    data class Bullet(var position: Vec, val dir: Vec, val origin: Vec) {

        fun dist() = origin.dist(position)
    }
}