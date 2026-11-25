package fr.rlebranchu.ytblight.presentation.feed

import kotlin.time.Duration
import kotlin.time.Instant

private const val MINUTES_PER_HOUR = 60
private const val DAYS_PER_WEEK = 7
private const val DAYS_PER_MONTH = 30
private const val DAYS_PER_YEAR = 365

/** `24:10` ou `1:02:03`. */
internal fun Duration.toClockLabel(): String =
    toComponents { hours, minutes, seconds, _ ->
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        if (hours > 0) "$hours:$mm:$ss" else "$minutes:$ss"
    }

/** `il y a 3 jours`, `à l'instant`… (français uniquement). */
internal fun relativeTimeLabel(
    publishedAt: Instant,
    now: Instant,
): String {
    val elapsed = now - publishedAt
    val minutes = elapsed.inWholeMinutes
    val hours = elapsed.inWholeHours
    val days = elapsed.inWholeDays
    return when {
        minutes < 1 -> "à l'instant"
        minutes < MINUTES_PER_HOUR -> "il y a $minutes min"
        hours < HOURS_PER_DAY -> "il y a $hours h"
        days < DAYS_PER_WEEK -> "il y a ${plural(days, "jour")}"
        days < DAYS_PER_MONTH -> "il y a ${plural(days / DAYS_PER_WEEK, "semaine")}"
        days < DAYS_PER_YEAR -> "il y a ${days / DAYS_PER_MONTH} mois"
        else -> "il y a ${plural(days / DAYS_PER_YEAR, "an")}"
    }
}

private const val HOURS_PER_DAY = 24

private fun plural(
    count: Long,
    unit: String,
): String = if (count > 1 && !unit.endsWith("s")) "$count ${unit}s" else "$count $unit"
