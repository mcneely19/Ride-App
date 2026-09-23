package com.onewheel.ridetracker.data

import kotlinx.coroutines.flow.Flow

class BoardRepository(private val dao: BoardDao) {

    val boards: Flow<List<BoardEntity>> = dao.observeAll()

    suspend fun save(board: BoardEntity) = dao.upsert(board)

    /** Returns false (and does nothing) if this board still has rides logged against it. */
    suspend fun delete(name: String): Boolean {
        if (dao.rideCountForBoard(name) > 0) return false
        dao.deleteByName(name)
        return true
    }

    suspend fun rideCountForBoard(name: String): Int = dao.rideCountForBoard(name)
}
