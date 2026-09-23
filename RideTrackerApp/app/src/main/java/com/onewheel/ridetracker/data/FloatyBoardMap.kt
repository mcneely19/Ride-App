package com.onewheel.ridetracker.data

import android.content.Context

/**
 * Remembers which of your boards a given Floaty "boardId" (its VESC's MAC address, e.g.
 * "D0:58:20:9D:76:84") corresponds to, so you're only asked to match them up once per board
 * rather than every time you import a session. Stored locally on-device only.
 */
object FloatyBoardMap {
    private const val PREFS = "floaty_board_map"

    fun get(context: Context, boardId: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(boardId, null)

    fun set(context: Context, boardId: String, boardName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(boardId, boardName)
            .apply()
    }
}
