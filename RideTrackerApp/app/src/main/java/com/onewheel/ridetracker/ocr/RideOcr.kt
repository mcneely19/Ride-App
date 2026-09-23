package com.onewheel.ridetracker.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Best-guess ride numbers pulled from a screenshot. Anything the parser
 * couldn't find with confidence is left null so the "Log a ride" form
 * shows it blank rather than a wrong number.
 */
data class OcrGuess(
    val board: String? = null,
    val whUsed: Double? = null,
    val miles: Double? = null,
    val avgSpeed: Double? = null,
    val maxSpeed: Double? = null,
    val rawText: String = ""
)

/**
 * Runs on-device text recognition (ML Kit's bundled Latin text model —
 * ships inside the app, no network call, works with the phone in
 * airplane mode) against a picked image and returns reconstructed text,
 * one visual row per line.
 *
 * ML Kit's own `visionText.text` is grouped by detected "block" in whatever
 * order its layout heuristics settle on — for a two-column list (a label
 * left, its value right, like "Average speed        11.4 mph") that can
 * come back as every label in a block, then every value in a *separate*
 * block, so the label and its value end up nowhere near each other in the
 * text. To avoid that, this pulls every recognized line's bounding box and
 * re-groups them into rows by vertical position instead, then joins each
 * row's pieces left-to-right — which reflects what's actually on screen
 * regardless of how ML Kit chose to group its blocks.
 */
suspend fun recognizeText(context: Context, uri: Uri): String =
    suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText -> cont.resume(reconstructRows(visionText)) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

private data class OcrLine(val text: String, val left: Int, val centerY: Int, val height: Int)

private fun reconstructRows(visionText: com.google.mlkit.vision.text.Text): String {
    val lines = mutableListOf<OcrLine>()
    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            val box = line.boundingBox ?: continue
            lines.add(OcrLine(line.text, box.left, (box.top + box.bottom) / 2, box.bottom - box.top))
        }
    }
    if (lines.isEmpty()) return visionText.text // fall back to whatever ML Kit gave us

    val avgHeight = lines.map { it.height }.average().takeIf { it > 0 } ?: 20.0
    val rowTolerance = (avgHeight * 0.6).toInt().coerceAtLeast(4)

    val sorted = lines.sortedBy { it.centerY }
    val rows = mutableListOf<MutableList<OcrLine>>()
    for (line in sorted) {
        val currentRow = rows.lastOrNull()
        val rowCenterY = currentRow?.let { row -> row.sumOf { it.centerY } / row.size }
        if (currentRow != null && rowCenterY != null && kotlin.math.abs(line.centerY - rowCenterY) <= rowTolerance) {
            currentRow.add(line)
        } else {
            rows.add(mutableListOf(line))
        }
    }

    return rows.joinToString("\n") { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
}

/**
 * Heuristic parser: looks for common phrasings ride-tracking apps use
 * ("268 Wh", "12.1 mi", "13.8 mph avg", "Max Speed 22.1", board names)
 * and pulls the first plausible match for each field. This is a best
 * effort, not a guarantee — the app always shows these as an editable,
 * pre-filled form rather than saving them directly, so a wrong OCR read
 * never quietly ends up in your ride log.
 */
object RideTextParser {

    fun parse(rawInput: String): OcrGuess {
        // OCR engines sometimes emit non-breaking spaces or other unicode whitespace
        // between words (invisible in a screenshot, but \s in a regex only matches plain
        // ASCII whitespace) — normalize everything to a regular space first so a label
        // like "Average speed" always matches regardless of which exact character ML Kit
        // used between the words.
        val text = rawInput.replace(Regex("""[  -​  　\t]"""), " ")
        val lower = text.lowercase()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val numberRegex = Regex("""([\d,]+\.?\d*)""")

        fun firstMatch(vararg patterns: Regex): Double? {
            for (p in patterns) {
                val m = p.find(text) ?: continue
                val value = m.groupValues[1].replace(",", "").toDoubleOrNull()
                if (value != null) return value
            }
            return null
        }

        // Label and value often land on separate OCR lines (a stat-tile layout: label on one
        // line, "13.5 mph" on the next, or vice versa) rather than "label: value" on one line,
        // so this checks the label's own line first, then the line after, then the line before.
        fun numberNearLabel(labelPattern: Regex): Double? {
            for (i in lines.indices) {
                if (!labelPattern.containsMatchIn(lines[i])) continue
                val sameLineRemainder = labelPattern.replace(lines[i], " ")
                numberRegex.find(sameLineRemainder)?.let { m ->
                    m.groupValues[1].replace(",", "").toDoubleOrNull()?.let { return it }
                }
                if (i + 1 < lines.size) {
                    numberRegex.find(lines[i + 1])?.let { m ->
                        m.groupValues[1].replace(",", "").toDoubleOrNull()?.let { return it }
                    }
                }
                if (i - 1 >= 0) {
                    numberRegex.find(lines[i - 1])?.let { m ->
                        m.groupValues[1].replace(",", "").toDoubleOrNull()?.let { return it }
                    }
                }
            }
            return null
        }

        val wh = firstMatch(
            Regex("""wh\s*used[:\s]*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""energy[:\s]*([\d,]+\.?\d*)\s*wh""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+\.?\d*)\s*wh\b""", RegexOption.IGNORE_CASE)
        ) ?: numberNearLabel(Regex("""wh\s*used|energy\s*used""", RegexOption.IGNORE_CASE))

        val miles = firstMatch(
            Regex("""distance[:\s]*([\d,]+\.?\d*)\s*mi""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+\.?\d*)\s*mi(?:les)?\b""", RegexOption.IGNORE_CASE)
        ) ?: numberNearLabel(Regex("""distance""", RegexOption.IGNORE_CASE))

        val avg = numberNearLabel(Regex("""avg\.?\s*speed|average\s*speed""", RegexOption.IGNORE_CASE))
            ?: firstMatch(
                Regex("""avg\.?\s*speed[:\s]*([\d,]+\.?\d*)\s*mph""", RegexOption.IGNORE_CASE),
                Regex("""average\s*speed[:\s]*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
                Regex("""([\d,]+\.?\d*)\s*mph\s*avg""", RegexOption.IGNORE_CASE)
            )

        // Some apps (Floaty, for one) show several different "speed" stats in one screenshot —
        // a plain "Max speed" summary tile plus separate "Max controller speed" / "Max gps
        // speed" detail rows. "Max gps speed" is tried first because it's an unambiguous
        // single-row match ("label ... value" on one line); the top summary tile is often a
        // compound line like "Distance Duration Max speed" sitting above a compound value line
        // like "6.1mi 60min 20.3 mph", where three different labels share one line — grabbing
        // "the number on the line above" for a compound row like that could land on the wrong
        // one of the three, so it's kept as a lower-priority fallback rather than tried first.
        val max = numberNearLabel(Regex("""gps\s*speed""", RegexOption.IGNORE_CASE))
            ?: numberNearLabel(Regex("""max\.?\s*speed|top\s*speed""", RegexOption.IGNORE_CASE))
            ?: firstMatch(
                Regex("""(?:max|top)\.?\s*speed[:\s]*([\d,]+\.?\d*)\s*mph""", RegexOption.IGNORE_CASE),
                Regex("""([\d,]+\.?\d*)\s*mph\s*(?:max|top)""", RegexOption.IGNORE_CASE)
            )
            ?: numberNearLabel(Regex("""controller\s*speed""", RegexOption.IGNORE_CASE))

        val board = when {
            lower.contains("xrv") -> "XRV"
            Regex("""\bx7\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower) -> "X7"
            else -> null
        }

        return OcrGuess(
            board = board,
            whUsed = wh,
            miles = miles,
            avgSpeed = avg,
            maxSpeed = max,
            rawText = text
        )
    }
}
