package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object GestationalAgeCalculator {

    private fun parseDateCandidates(dateStr: String): List<Date> {
        val cleaned = dateStr.trim()
        if (cleaned.isEmpty()) return emptyList()

        val candidates = mutableListOf<Date>()

        // Try raw format parsing with common patterns (DD/MM/YY prioritized)
        val patterns = listOf(
            "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
            "dd.MM.yyyy", "d.M.yyyy", "dd-MM-yyyy", "d-M-yyyy",
            "MM/dd/yyyy", "M/d/yyyy", "MM/dd/yy", "M/d/yy",
            "yyyy-MM-dd", "yyyy.MM.dd", "yyyy/MM/dd"
        )
        
        for (pattern in patterns) {
            try {
                // Correct m -> M where appropriate (since m is minutes, M is month)
                val safePattern = pattern.replace('m', 'M')
                val df = SimpleDateFormat(safePattern, Locale.US).apply { isLenient = false }
                df.parse(cleaned)?.let { candidates.add(it) }
            } catch (e: Exception) {
                // Ignore and try next
            }
        }

        // Try after normalizing all punctuation/separators to '/'
        val normalized = cleaned.replace(Regex("[-.\\s]+"), "/")
        val normalizedPatterns = listOf(
            "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
            "dd/MMM/yyyy", "d/MMM/yyyy", "dd/MMMM/yyyy", "d/MMMM/yyyy",
            "MM/dd/yyyy", "M/d/yyyy", "MM/dd/yy", "M/d/yy",
            "yyyy/MM/dd", "yyyy/M/d",
            "MMM/dd/yyyy", "MMMM/dd/yyyy", "yyyy/MMM/dd"
        )

        for (pattern in normalizedPatterns) {
            // US Locale
            try {
                val df = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                df.parse(normalized)?.let { candidates.add(it) }
            } catch (e: Exception) {}

            // System Default Locale (handles localized month words)
            try {
                val df = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                df.parse(normalized)?.let { candidates.add(it) }
            } catch (e: Exception) {}
        }

        return candidates.distinct()
    }

    /**
     * Calculates Gestational Age in days relative to today.
     * Returns a pair of (weeks, days) or null if unable to calculate.
     */
    fun calculateFromLmp(lmpStr: String, referenceDate: Date = Date()): Pair<Int, Int>? {
        val candidates = parseDateCandidates(lmpStr)
        if (candidates.isEmpty()) return null

        // Look for a candidate date that yields a clinically logical active pregnancy (0 to 45 weeks)
        var bestCandidate: Date? = null
        var bestWeeks = -1
        var bestDays = -1

        for (candidate in candidates) {
            val diffMs = referenceDate.time - candidate.time
            if (diffMs >= 0) {
                val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
                val weeks = (diffDays / 7).toInt()
                val days = (diffDays % 7).toInt()
                if (weeks in 0..45) {
                    bestCandidate = candidate
                    bestWeeks = weeks
                    bestDays = days
                    break // Found a perfect gestation candidate!
                }
            }
        }

        // Fallback to first parsed candidate if no candidate falls into the clinical range
        if (bestCandidate == null) {
            val firstCandidate = candidates.first()
            val diffMs = referenceDate.time - firstCandidate.time
            if (diffMs < 0) return null
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            val weeks = (diffDays / 7).toInt()
            val days = (diffDays % 7).toInt()
            if (weeks > 50) return null // Ignore extremely old dates as they aren't pregnancies
            return Pair(weeks, days)
        }

        return Pair(bestWeeks, bestDays)
    }

    /**
     * Calculates Gestational Age based on EDD.
     * EDD is exactly 40 weeks (280 days) from LMP.
     * So LMP = EDD - 280 days.
     */
    fun calculateFromEdd(eddStr: String, referenceDate: Date = Date()): Pair<Int, Int>? {
        val candidates = parseDateCandidates(eddStr)
        if (candidates.isEmpty()) return null

        var bestWeeks = -1
        var bestDays = -1
        var candidateFound = false

        for (eddDate in candidates) {
            // Calculate estimated LMP
            val calendar = Calendar.getInstance().apply {
                time = eddDate
                add(Calendar.DAY_OF_YEAR, -280)
            }
            val estLmpDate = calendar.time
            val diffMs = referenceDate.time - estLmpDate.time
            if (diffMs >= 0) {
                val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
                val weeks = (diffDays / 7).toInt()
                val days = (diffDays % 7).toInt()
                if (weeks in 0..45) {
                    bestWeeks = weeks
                    bestDays = days
                    candidateFound = true
                    break
                }
            }
        }

        if (!candidateFound) {
            val firstEdd = candidates.first()
            val calendar = Calendar.getInstance().apply {
                time = firstEdd
                add(Calendar.DAY_OF_YEAR, -280)
            }
            val estLmp = calendar.time
            val diffMs = referenceDate.time - estLmp.time
            if (diffMs < 0) return null
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            val weeks = (diffDays / 7).toInt()
            val days = (diffDays % 7).toInt()
            if (weeks < 0 || weeks > 50) return null
            return Pair(weeks, days)
        }

        return Pair(bestWeeks, bestDays)
    }

    fun calculateGestationalAge(lmpStr: String, eddStr: String, referenceDate: Date = Date()): String? {
        val lmpResult = calculateFromLmp(lmpStr, referenceDate)
        if (lmpResult != null) {
            val (weeks, days) = lmpResult
            return if (days == 0) "${weeks}wks" else "${weeks}wks+${days}D"
        }
        val eddResult = calculateFromEdd(eddStr, referenceDate)
        if (eddResult != null) {
            val (weeks, days) = eddResult
            return if (days == 0) "${weeks}wks" else "${weeks}wks+${days}D"
        }
        return null
    }

    fun calculateGestationalAgeLong(lmpStr: String, eddStr: String, referenceDate: Date = Date()): String? {
        val lmpResult = calculateFromLmp(lmpStr, referenceDate)
        if (lmpResult != null) {
            val (weeks, days) = lmpResult
            return if (days == 0) {
                "${weeks} Weeks (by LMP)"
            } else {
                "${weeks} Weeks + ${days} Days (by LMP)"
            }
        }
        val eddResult = calculateFromEdd(eddStr, referenceDate)
        if (eddResult != null) {
            val (weeks, days) = eddResult
            return if (days == 0) {
                "${weeks} Weeks (by EDD)"
            } else {
                "${weeks} Weeks + ${days} Days (by EDD)"
            }
        }
        return null
    }

    /**
     * Regex to match gestational age formats in a diagnosis string.
     * Matches patterns like "30wks+5D", "30wk+5d", "30w+5d", or just solid weeks like "30wks", "30wk", "30 weeks" (with optional spaces around '+' and boundaries).
     */
    val GA_PATTERN_REGEX = Regex("""\b\d+\s*(?:wks?|w|weeks?)\s*\+\s*\d+\s*(?:[Dd]|days?)\b|\b\d+\s*(?:wks?|weeks?)\b""", RegexOption.IGNORE_CASE)

    /**
     * Updates or replaces any existing gestational age pattern in the diagnosis with the calculated GA.
     * If no pattern exists in the diagnosis and [forceAppend] is true, it appends the GA.
     */
    fun updateGaInDiagnosis(diagnosis: String, currentGa: String, forceAppend: Boolean = false): String {
        if (currentGa.isEmpty()) return diagnosis
        
        val hasExisting = GA_PATTERN_REGEX.containsMatchIn(diagnosis)
        return if (hasExisting) {
            diagnosis.replace(GA_PATTERN_REGEX, currentGa)
        } else if (forceAppend) {
            if (diagnosis.trim().isEmpty()) {
                currentGa
            } else {
                "${diagnosis.trim()}, $currentGa"
            }
        } else {
            diagnosis
        }
    }
}
