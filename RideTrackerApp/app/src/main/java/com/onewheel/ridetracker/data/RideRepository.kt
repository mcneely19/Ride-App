package com.onewheel.ridetracker.data

import kotlinx.coroutines.flow.Flow

class RideRepository(private val dao: RideDao) {

    val rides: Flow<List<Ride>> = dao.observeAll()

    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(SeedData.rides)
        }
    }

    suspend fun addRide(ride: Ride) = dao.insert(ride)

    suspend fun deleteRide(id: String) = dao.deleteById(id)
}
