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
 * airplane mode) against a picked image and returns the raw text.
 */
suspend fun recognizeText(context: Context, uri: Uri): String =
    suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText -> cont.resume(visionText.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
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

    fun parse(text: String): OcrGuess {
        val lower = text.lowercase()

        fun firstMatch(vararg patterns: Regex): Double? {
            for (p in patterns) {
                val m = p.find(text) ?: continue
                val value = m.groupValues[1].replace(",", "").toDoubleOrNull()
                if (value != null) return value
            }
            return null
        }

        val wh = firstMatch(
            Regex("""wh\s*used[:\s]*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""energy[:\s]*([\d,]+\.?\d*)\s*wh""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+\.?\d*)\s*wh\b""", RegexOption.IGNORE_CASE)
        )

        val miles = firstMatch(
            Regex("""distance[:\s]*([\d,]+\.?\d*)\s*mi""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+\.?\d*)\s*mi(?:les)?\b""", RegexOption.IGNORE_CASE)
        )

        val avg = firstMatch(
            Regex("""avg\.?\s*(?:speed)?[:\s]*([\d,]+\.?\d*)\s*mph""", RegexOption.IGNORE_CASE),
            Regex("""average\s*speed[:\s]*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+\.?\d*)\s*mph\s*avg""", RegexOption.IGNORE_CASE)
        )

        val max = firstMatch(
            Regex("""(?:max|top)\.?\s*(?:speed)?[:\s]*([\d,]+\.?\d*)\s*mph""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+\.?\d*)\s*mph\s*(?:max|top)""", RegexOption.IGNORE_CASE)
        )

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
