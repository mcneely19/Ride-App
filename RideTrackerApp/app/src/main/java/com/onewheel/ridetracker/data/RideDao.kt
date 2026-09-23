package com.onewheel.ridetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY date DESC, session DESC")
    fun observeAll(): Flow<List<Ride>>

    @Query("SELECT COUNT(*) FROM rides")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rides: List<Ride>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ride: Ride)

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteById(id: String)
}
