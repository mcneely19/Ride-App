package com.onewheel.ridetracker.data

import kotlinx.coroutines.flow.Flow

class RideRepository(private val dao: RideDao) {

    val rides: Flow<List<Ride>> = dao.observeAll()

    // Not auto-invoked anymore — SeedData.rides is your personal ride history, not generic
    // sample data, so a fresh install (someone else setting up their own board) shouldn't
    // get it automatically. Existing installs already have these rows persisted from before.
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(SeedData.rides)
        }
    }

    suspend fun addRide(ride: Ride) = dao.insert(ride)

    /** Returns the number of rides actually inserted (duplicates by id are skipped, not overwritten). */
    suspend fun importRides(rides: List<Ride>): Int {
        if (rides.isEmpty()) return 0
        val rowIds = dao.insertAllIgnoreConflict(rides)
        return rowIds.count { it != -1L }
    }

    suspend fun deleteRide(id: String) = dao.deleteById(id)
}
