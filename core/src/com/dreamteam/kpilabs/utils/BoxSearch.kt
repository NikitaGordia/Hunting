package com.dreamteam.kpilabs.utils

class BoxSearch(
        private val numberOfHorizontalChunks: Int,
        private val chunkSizeH: Int = CHUNK_NUMBER,
        private val chunkSizeW: Int = CHUNK_NUMBER
) {

    companion object {

        private const val CHUNK_NUMBER = 3
        private const val CHUNK_SEARCH_RADIUS = 1
    }

    private val chunks = mutableMapOf<Int, MutableSet<Int>>()
    private val tags = mutableMapOf<Int, Point>()

    fun updatePoint(id: Int, xFloat: Float, yFloat: Float) {
        val x = xFloat.toInt()
        val y = yFloat.toInt()
        val chunk = getChunk(x, y)
        tags[id]?.let { pt ->
            pt.x = x
            pt.y = y
            if (pt.chunk != chunk) {
                chunks[pt.chunk]?.remove(id)
                updateChunk(chunk, id)
            }
        } ?: run {
            tags[id] = Point(chunk, x, y)
            updateChunk(chunk, id)
        }
    }

    fun removePoint(id: Int) {
        tags[id]?.let {
            chunks[it.chunk]?.remove(id)
        }
        tags.remove(id)
    }

    fun getNeighbours(searchId: Int, searchRadius: Int = CHUNK_SEARCH_RADIUS): List<ResultPoint> {
        val res = mutableListOf<ResultPoint>()
        val data = tags[searchId] ?: return emptyList()
        val chunkH = data.x / chunkSizeH
        val chunkW = data.y / chunkSizeW

        for (i in chunkH - searchRadius until chunkH + searchRadius + 1)
            for (j in chunkW - searchRadius until chunkW + searchRadius + 1)
                chunks[numberOfHorizontalChunks * j + i]?.toList()?.let {
                    res.addAll(it.mapNotNull { id ->
                        tags[id]?.takeIf {
                            searchId != id
                        }?.let {
                            ResultPoint(id, it.x, it.y)
                        }
                    })
                }
        return res
    }

    private fun updateChunk(chunk: Int, id: Int) {
        chunks[chunk]?.add(id) ?: run {
            chunks[chunk] = mutableSetOf(id)
        }
    }

    private fun getChunk(x: Int, y: Int): Int {
        val chunkH = x / chunkSizeH
        val chunkW = y / chunkSizeW
        return numberOfHorizontalChunks * chunkW + chunkH
    }

    data class Point(val chunk: Int, var x: Int, var y: Int)

    data class ResultPoint(val id: Int, val x: Int, val y: Int)
}