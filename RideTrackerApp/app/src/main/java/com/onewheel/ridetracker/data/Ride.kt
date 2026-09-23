package com.onewheel.ridetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey val id: String,
    val board: String,       // matches BoardEntity.name
    val date: String,        // ISO yyyy-MM-dd
    val session: String,     // "AM" / "PM" / "" / "PM ·1" etc.
    val whUsed: Double,
    val pctUsed: Double,
    val miles: Double,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val temp: Int?,
    val psi: Double?,
    val notes: String
) {
    val whPerMi: Double get() = if (miles > 0) whUsed / miles else 0.0

    /** Estimated range given a board's pack capacity (looked up from BoardEntity by the caller). */
    fun estRangeMi(capacityWh: Double): Double = if (whPerMi > 0) capacityWh / whPerMi else 0.0
}
