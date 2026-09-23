package com.onewheel.ridetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A board configuration — name doubles as its id, since [Ride.board] stores
 * this same string. Boards are fully user-managed now (add/edit/delete via
 * "Manage boards"); nothing about a board is hardcoded into the app anymore.
 */
@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val name: String,
    val capacityWh: Double,
    val subtitle: String = "",
    val colorHex: String
)

/** Preset swatches offered in the board color picker. */
val BOARD_COLOR_PALETTE = listOf(
    "#0D7D78", // teal
    "#B5591A", // burnt orange
    "#4C51BF", // indigo
    "#B4267A", // pink/magenta
    "#1E7A4C", // green
    "#A6790A", // amber
    "#7C3AED", // violet
    "#B23A3A", // red
)
