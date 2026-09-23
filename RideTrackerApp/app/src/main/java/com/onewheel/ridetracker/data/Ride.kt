package com.onewheel.ridetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Board(val label: String, val capacityWh: Double) {
    XRV("XRV", 648.0),
    X7("X7", 518.0)
}

@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey val id: String,
    val board: String,       // Board.name
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
    val estRangeMi: Double
        get() {
            val cap = Board.valueOf(board).capacityWh
            return if (whPerMi > 0) cap / whPerMi else 0.0
        }
}
