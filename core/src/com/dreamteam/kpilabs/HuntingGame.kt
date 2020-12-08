package com.dreamteam.kpilabs

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.dreamteam.kpilabs.GameSettings.CAMERA_SCALE
import com.dreamteam.kpilabs.GameSettings.HUNTER_MOVING_FORCE
import com.dreamteam.kpilabs.GameSettings.WORLD_SCALE
import com.dreamteam.kpilabs.World.WorldInfo
import com.dreamteam.kpilabs.utils.Vec


class HuntingGame : ApplicationAdapter() {

    companion object {

        const val IS_DEBUG = false
        const val DISPLAY_VISIBLE_CIRCLE = IS_DEBUG
        const val DISPLAY_TARGET = IS_DEBUG
        const val DISPLAY_VELOCITY_VECTOR = IS_DEBUG
    }

    private lateinit var shapeRender: ShapeRenderer
    private lateinit var world: World
    private lateinit var camera: Camera

    override fun create() {
        initializeParams()
    }

    private fun initializeParams() {
        shapeRender = ShapeRenderer()
        world = World(WorldInfo(
                Gdx.graphics.width.toFloat() * WORLD_SCALE,
                Gdx.graphics.height.toFloat() * WORLD_SCALE,
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height.toFloat()
        ), worldSpeed = 1f, rabbitNumber = 20, wolfNumber = 10, deerNumber = 100).apply {
            restartCallback = {
                initializeParams()
            }
        }
        camera = OrthographicCamera(
                Gdx.graphics.width.toFloat() * CAMERA_SCALE,
                Gdx.graphics.height.toFloat() * CAMERA_SCALE
        )
    }

    override fun render() {
        super.render()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRender.projectionMatrix = camera.combined

        drawBackground()

        val deltaTime = Gdx.graphics.deltaTime
        updateHunter(deltaTime)
        world.move(deltaTime).let {
            it.forEach { it.draw(shapeRender) }
        }
        camera.update()
    }

    private fun updateHunter(deltaTime: Float) {
        if (Gdx.input.isTouched) {
            world.hunterShoot()
        }
        world.moveHunter(Vec(
                when {
                    Gdx.input.isKeyPressed(Input.Keys.A) -> -1f
                    Gdx.input.isKeyPressed(Input.Keys.D) -> 1f
                    else -> 0f
                } * HUNTER_MOVING_FORCE,
                when {
                    Gdx.input.isKeyPressed(Input.Keys.W) -> 1f
                    Gdx.input.isKeyPressed(Input.Keys.S) -> -1f
                    else -> 0f
                } * HUNTER_MOVING_FORCE
        ), deltaTime, world.translateMousePosition(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat(),
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height.toFloat()
        ))?.also {
            camera.position.set(it.toVector3())
        }
    }

    private fun drawBackground() {
        shapeRender.apply {
            color = Color.WHITE
            begin(ShapeRenderer.ShapeType.Line)
            rect(0f, 0f, world.worldInfo.width, world.worldInfo.height)
            end()

            color = Color(52 / 255f, 89 / 255f, 56 / 255f, 1f)
            var i = 0f
            while (i < world.worldInfo.width) {
                begin(ShapeRenderer.ShapeType.Line)
                rectLine(i, 0f, i, world.worldInfo.height, 1f)
                end()
                i += 150f
            }
            i = 0f
            while (i < world.worldInfo.height) {
                begin(ShapeRenderer.ShapeType.Line)
                rectLine(0f, i, world.worldInfo.width, i, 1f)
                end()
                i += 150f
            }
        }
    }
}