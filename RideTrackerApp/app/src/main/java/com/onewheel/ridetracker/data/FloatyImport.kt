package com.onewheel.ridetracker.data

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parses a single-session export from the Floaty app (in Floaty: Sessions -> tap a ride ->
 * the ⋮ menu -> Export session). Floaty's export is the *entire* raw VESC telemetry log for
 * that ride — hundreds to thousands of samples (speed, wattHours, battery, etc.), not just a
 * summary — so everything pulled from it is an exact recorded value, not an OCR guess off a
 * screenshot.
 */
object FloatyImport {

    // Floaty's raw export records speed and distance in metric (km/h, km) even though the
    // app's own UI displays imperial units — confirmed by cross-checking a real export against
    // what Floaty itself showed on screen for the same ride (its "Max controller speed" and
    // "Distance" only line up with this file's raw numbers once converted from metric).
    private const val KM_TO_MI = 0.6213711922

    data class ParsedSession(
        val boardId: String,
        val date: String,          // yyyy-MM-dd, device's local timezone
        val whUsed: Double,
        val miles: Double,
        val avgSpeed: Double,
        val maxSpeed: Double,
        val durationMinutes: Double
    )

    /** True if this parsed JSON object looks like a Floaty session export rather than our own
     *  ride-import format (which has a top-level "rides" array/object instead). */
    fun looksLikeFloatySession(json: JSONObject): Boolean =
        json.has("logs") && json.has("startTime") && json.has("endTime")

    fun parse(jsonText: String): ParsedSession {
        val json = JSONObject(jsonText)
        require(looksLikeFloatySession(json)) { "This doesn't look like a Floaty session export." }

        val boardId = json.optString("boardId", "").ifBlank { "unknown board" }
        val startTime = json.optLong("startTime", -1)
        val endTime = json.optLong("endTime", -1)
        require(startTime > 0 && endTime > startTime) { "Missing or invalid ride start/end time." }
        val durationHours = (endTime - startTime) / 1000.0 / 3600.0

        val logs = json.optJSONArray("logs")
        require(logs != null && logs.length() > 0) { "This session export has no telemetry samples." }

        // wattHours in each sample is cumulative for the session (already in real Wh, no unit
        // conversion needed), so the highest value seen (normally the last sample) is the
        // total energy used. Speed samples are summed (in raw km/h) to compute a plain mean —
        // verified against a real export to land within 0.1 mph of what Floaty itself displays.
        var whUsed = 0.0
        var maxSpeedKmh = 0.0
        var speedSumKmh = 0.0
        var speedCount = 0
        var lastTripDistanceKm = 0.0
        for (i in 0 until logs.length()) {
            val entry = logs.optJSONObject(i) ?: continue
            val wh = entry.optDouble("wattHours", Double.NaN)
            if (!wh.isNaN() && wh > whUsed) whUsed = wh
            val speedKmh = entry.optDouble("speed", Double.NaN)
            if (!speedKmh.isNaN()) {
                if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh
                speedSumKmh += speedKmh
                speedCount++
            }
            val trip = entry.optDouble("tripDistance", Double.NaN)
            if (!trip.isNaN()) lastTripDistanceKm = trip
        }

        // Prefer the session's own top-level "distance" summary; fall back to the last
        // telemetry sample's cumulative tripDistance if that field is missing. Both are in km.
        val topDistanceKm = json.optDouble("distance", Double.NaN)
        val distanceKm = if (!topDistanceKm.isNaN() && topDistanceKm > 0) topDistanceKm else lastTripDistanceKm
        val miles = distanceKm * KM_TO_MI

        val maxSpeed = maxSpeedKmh * KM_TO_MI
        val avgSpeed = if (speedCount > 0) (speedSumKmh / speedCount) * KM_TO_MI else 0.0

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startTime))

        return ParsedSession(
            boardId = boardId,
            date = date,
            whUsed = whUsed,
            miles = miles,
            avgSpeed = avgSpeed,
            maxSpeed = maxSpeed,
            durationMinutes = durationHours * 60
        )
    }
}
