package com.algorithmx.planner.logic

import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

// Data class to hold the parsed result
data class ParsedTask(
    val title: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val durationMinutes: Int,
    val isZone: Boolean,
    val recurrenceRule: String?,
    val priority: Int
)

class GeminiParser {

    // Initialize Vertex AI
    // Ensure you enabled "Vertex AI in Firebase" in the console
    private val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")

    suspend fun parseTask(input: String): ParsedTask? = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val prompt = """
            You are a smart scheduler assistant. Extract structured data from this user input: "$input".
            Current Date: $today.
            
            Return ONLY a raw JSON object (no markdown) with these fields:
            - "title": Clean task name.
            - "date": YYYY-MM-DD. Calculate based on "tomorrow", "next friday". Null if missing.
            - "time": HH:mm (24-hour). Null if missing.
            - "duration": Integer minutes. Default 30.
            - "isZone": Boolean. True for events/classes/shifts. False for tasks.
            - "recurrence": RRULE string (e.g. "FREQ=WEEKLY;BYDAY=MO"). Null if one-off.
            - "priority": 1-4.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            // Cleanup response
            val text = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: return@withContext null

            val json = JSONObject(text)

            return@withContext ParsedTask(
                title = json.optString("title"),
                date = json.optString("date").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                time = json.optString("time").takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it) },
                durationMinutes = json.optInt("duration", 30),
                isZone = json.optBoolean("isZone", false),
                recurrenceRule = json.optString("recurrence").takeIf { it.isNotEmpty() },
                priority = json.optInt("priority", 1)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}