package com.dreamteam.kpilabs

import com.dreamteam.kpilabs.entity.*
import com.dreamteam.kpilabs.utils.BoxSearch
import com.dreamteam.kpilabs.utils.Vec

class World(
        val worldInfo: WorldInfo,
        private val worldSpeed: Float,
        rabbitNumber: Int,
        wolfNumber: Int,
        deerNumber: Int,
) {

    private var entities = mutableMapOf<Int, Entity>()
    private val boxSearch = kotlin.run {
        val density = 50
        val count = (worldInfo.width / density).toInt() + 1
        BoxSearch(count, density, density)
    }

    var restartCallback: () -> Unit = {}

    init {
        addEntity(Hunter.generate(worldInfo))
        (1..rabbitNumber).forEach {
            addEntity(Rabbit.generate(worldInfo))
        }
        (1..wolfNumber).forEach {
            addEntity(Wolf.generate(worldInfo))
        }
        (1..deerNumber).forEach {
            addEntity(Deer.generate(worldInfo))
        }
        updateBoxSearch()
    }

    fun move(deltaTime: Float): List<Entity> {
        if (entities.filter { it.value is Hunter }.isEmpty()) return kotlin.run {
            restartCallback()
            emptyList()
        }
        entities.forEach { (hash, ent) ->
            ent.askToMove(boxSearch.getNeighbours(hash, 12).mapNotNull {
                entities[it.id]
            }, deltaTime * worldSpeed, worldInfo)
        }
        entities = entities.filterValues {
            it.isAlive()
        }.toMutableMap()
        updateBoxSearch()
        return entities.values.toList()
    }

    fun moveHunter(force: Vec, deltaTime: Float, pointer: Vec) =
            findHunter()?.let {
                it.updatePointer(pointer)
                it.applyWorldForce(force, deltaTime, worldInfo)
                it.position
            }

    fun hunterShoot() {
        findHunter()?.let {
            if (!it.shoot()) restartCallback()
        }
    }

    fun translateMousePosition(x: Float, y: Float, cameraWidth: Float, cameraHeight: Float) =
            (findHunter()?.position ?: Vec(0f, 0f)).let {
                Vec(x + it.x - cameraWidth / 2, cameraHeight - y + it.y - cameraHeight / 2)
            }

    private fun updateBoxSearch() {
        entities.forEach { hash, ent ->
            boxSearch.updatePoint(hash, ent.position.x, ent.position.y)
        }
    }

    private fun findHunter(): Hunter? = entities.values.filter { it is Hunter }.firstOrNull()?.let {
        if (it is Hunter) it else null
    }

    private fun addEntity(entity: Entity) {
        entities[entity.hashCode()] = entity
    }

    data class WorldInfo(val width: Float, val height: Float, val screenWidth: Float, val screenHeight: Float) {

        fun adjustBounds(to: Vec) = Vec(
                to.x.coerceIn(0f, width),
                to.y.coerceIn(0f, height)
        )

        fun checkPosition(to: Vec, radius: Float = 0f) =
                to.x >= radius && to.x <= width - radius &&
                        to.y >= radius && to.y <= height - radius
    }
}