package com.onewheel.ridetracker.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes rides (and optionally boards) into the same JSON shape [RideJsonImport] accepts,
 * so an export from this app is always re-importable — including into a fresh install after
 * an uninstall/reinstall, which is the whole point: this is a manual backup, since app-private
 * storage (where the Room database actually lives) is wiped whenever the app is uninstalled.
 */
object RideJsonExport {

    fun toJson(rides: List<Ride>, boards: List<BoardEntity> = emptyList()): String {
        val ridesJson = JSONArray().apply {
            rides.sortedBy { it.date }.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("board", r.board)
                    put("date", r.date)
                    put("session", r.session)
                    put("whUsed", r.whUsed)
                    put("miles", r.miles)
                    put("avgSpeed", r.avgSpeed)
                    put("maxSpeed", r.maxSpeed)
                    if (r.temp != null) put("temp", r.temp)
                    if (r.psi != null) put("psi", r.psi)
                    if (r.notes.isNotBlank()) put("notes", r.notes)
                })
            }
        }

        val root = JSONObject().apply {
            put("rides", ridesJson)
            if (boards.isNotEmpty()) {
                put("boards", JSONArray().apply {
                    boards.forEach { b ->
                        put(JSONObject().apply {
                            put("name", b.name)
                            put("capacityWh", b.capacityWh)
                            if (b.subtitle.isNotBlank()) put("subtitle", b.subtitle)
                            put("colorHex", b.colorHex)
                        })
                    }
                })
            }
        }
        return root.toString(2)
    }
}
