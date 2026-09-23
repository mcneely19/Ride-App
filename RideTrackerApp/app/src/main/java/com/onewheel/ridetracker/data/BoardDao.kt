package com.onewheel.ridetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM boards ORDER BY name ASC")
    fun observeAll(): Flow<List<BoardEntity>>

    @Query("SELECT COUNT(*) FROM boards")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(board: BoardEntity)

    @Query("DELETE FROM boards WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT COUNT(*) FROM rides WHERE board = :name")
    suspend fun rideCountForBoard(name: String): Int
}
