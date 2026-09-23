package com.onewheel.ridetracker.claude

import com.onewheel.ridetracker.data.BoardEntity
import com.onewheel.ridetracker.data.Ride
import org.json.JSONArray
import org.json.JSONObject

/** Packages your boards + ride log into a compact prompt so Claude has real numbers to work with. */
object RidePromptBuilder {

    fun build(boards: List<BoardEntity>, rides: List<Ride>, question: String): String {
        val boardsJson = JSONArray().apply {
            boards.forEach { b ->
                put(JSONObject().apply {
                    put("name", b.name)
                    put("capacityWh", b.capacityWh)
                    if (b.subtitle.isNotBlank()) put("subtitle", b.subtitle)
                })
            }
        }
        val ridesJson = JSONArray().apply {
            rides.sortedByDescending { it.date }.forEach { r ->
                put(JSONObject().apply {
                    put("board", r.board)
                    put("date", r.date)
                    if (r.session.isNotBlank()) put("session", r.session)
                    put("whUsed", r.whUsed)
                    put("miles", r.miles)
                    put("whPerMi", (Math.round(r.whPerMi * 100) / 100.0))
                    put("avgSpeed", r.avgSpeed)
                    put("maxSpeed", r.maxSpeed)
                    r.temp?.let { put("temp", it) }
                    r.psi?.let { put("psi", it) }
                    if (r.notes.isNotBlank()) put("notes", r.notes)
                })
            }
        }

        return """
            You're helping analyze data from a personal electric-vehicle ride-tracking app
            (Onewheel-style boards). Boards (name, battery pack capacity in Wh):
            $boardsJson

            Ride log, newest first (whUsed = energy used in Wh, whPerMi = Wh per mile, the
            efficiency metric — lower is better; avgSpeed/maxSpeed in mph; temp in °F ambient;
            psi = tire pressure where tracked):
            $ridesJson

            Question: $question

            Answer using only the data above. Be specific and cite actual numbers/dates from
            the log where relevant. Keep it concise unless the question asks for depth.
        """.trimIndent()
    }
}
