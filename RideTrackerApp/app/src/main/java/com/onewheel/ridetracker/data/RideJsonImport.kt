package com.onewheel.ridetracker.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Parses a JSON file of ride records into [Ride] entities.
 *
 * Accepts either a bare array of ride objects, or an object with a
 * top-level "rides" array (so an export that wraps things in metadata
 * still works). Each ride object looks like:
 *
 * {
 *   "board": "XRV",           // required — "XRV" or "X7"
 *   "date": "2026-09-20",     // required — yyyy-MM-dd
 *   "whUsed": 250.0,          // required
 *   "miles": 12.0,            // required
 *   "avgSpeed": 13.5,         // required
 *   "maxSpeed": 22.0,         // required
 *   "session": "AM",          // optional, defaults to ""
 *   "temp": 75,               // optional
 *   "psi": 15.0,              // optional
 *   "notes": "...",           // optional
 *   "id": "..."               // optional — auto-generated if omitted
 * }
 */
object RideJsonImport {

    /** Returns the successfully parsed rides plus a human-readable note per skipped entry. */
    fun parse(jsonText: String): Pair<List<Ride>, List<String>> {
        val root = JSONObject(jsonText.trim().let { if (it.startsWith("[")) "{\"rides\":$it}" else it })
        val array: JSONArray = root.optJSONArray("rides")
            ?: throw IllegalArgumentException("Expected a JSON array of rides.")

        val rides = mutableListOf<Ride>()
        val errors = mutableListOf<String>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            if (obj == null) {
                errors.add("Entry ${i + 1}: not a valid ride object.")
                continue
            }
            try {
                rides.add(parseOne(obj))
            } catch (e: Exception) {
                errors.add("Entry ${i + 1}: ${e.message}")
            }
        }

        return rides to errors
    }

    private fun parseOne(obj: JSONObject): Ride {
        val board = obj.optString("board", "").uppercase()
        require(board == "XRV" || board == "X7") { "board must be \"XRV\" or \"X7\"" }

        val date = obj.optString("date", "")
        require(Regex("""\d{4}-\d{2}-\d{2}""").matches(date)) { "date must be yyyy-MM-dd" }

        require(obj.has("whUsed")) { "missing whUsed" }
        require(obj.has("miles")) { "missing miles" }
        require(obj.has("avgSpeed")) { "missing avgSpeed" }
        require(obj.has("maxSpeed")) { "missing maxSpeed" }

        val whUsed = obj.getDouble("whUsed")
        val miles = obj.getDouble("miles")
        val capacity = Board.valueOf(board).capacityWh
        val pctUsed = whUsed / capacity * 100

        val id = obj.optString("id", "").ifBlank {
            "${board.lowercase()}-$date-${UUID.randomUUID().toString().take(8)}"
        }

        return Ride(
            id = id,
            board = board,
            date = date,
            session = obj.optString("session", ""),
            whUsed = whUsed,
            pctUsed = pctUsed,
            miles = miles,
            avgSpeed = obj.getDouble("avgSpeed"),
            maxSpeed = obj.getDouble("maxSpeed"),
            temp = if (obj.has("temp") && !obj.isNull("temp")) obj.getInt("temp") else null,
            psi = if (obj.has("psi") && !obj.isNull("psi")) obj.getDouble("psi") else null,
            notes = obj.optString("notes", "")
        )
    }
}
