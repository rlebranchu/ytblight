package fr.rlebranchu.ytblight.presentation.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FeedFormattingTest {
    private val now = Instant.fromEpochSeconds(1_800_000_000)

    @Test
    fun formatsShortDurations() {
        assertEquals("0:45", 45.seconds.toClockLabel())
        assertEquals("4:13", (4.minutes + 13.seconds).toClockLabel())
        assertEquals("24:05", (24.minutes + 5.seconds).toClockLabel())
    }

    @Test
    fun formatsDurationsOfAnHourOrMore() {
        assertEquals("1:00:00", 1.hours.toClockLabel())
        assertEquals("1:02:03", (1.hours + 2.minutes + 3.seconds).toClockLabel())
        assertEquals("12:34:56", (12.hours + 34.minutes + 56.seconds).toClockLabel())
    }

    @Test
    fun formatsRelativeTimesInFrench() {
        fun label(
            ago: kotlin.time.Duration,
        ) = relativeTimeLabel(
            publishedAt = now - ago,
            now = now,
        )

        assertEquals(
            "à l'instant",
            label(
                ago = 30.seconds,
            ),
        )
        assertEquals(
            "il y a 5 min",
            label(
                ago = 5.minutes,
            ),
        )
        assertEquals(
            "il y a 59 min",
            label(
                ago = 59.minutes,
            ),
        )
        assertEquals(
            "il y a 1 h",
            label(
                ago = 1.hours,
            ),
        )
        assertEquals(
            "il y a 23 h",
            label(
                ago = 23.hours,
            ),
        )
        assertEquals(
            "il y a 1 jour",
            label(
                ago = 1.days,
            ),
        )
        assertEquals(
            "il y a 6 jours",
            label(
                ago = 6.days,
            ),
        )
        assertEquals(
            "il y a 1 semaine",
            label(
                ago = 7.days,
            ),
        )
        assertEquals(
            "il y a 3 semaines",
            label(
                ago = 21.days,
            ),
        )
        assertEquals(
            "il y a 1 mois",
            label(
                ago = 30.days,
            ),
        )
        assertEquals(
            "il y a 11 mois",
            label(
                ago = 340.days,
            ),
        )
        assertEquals(
            "il y a 1 an",
            label(
                ago = 365.days,
            ),
        )
        assertEquals(
            "il y a 2 ans",
            label(
                ago = 800.days,
            ),
        )
    }

    @Test
    fun treatsFuturePublicationDatesAsJustNow() {
        assertEquals(
            "à l'instant",
            relativeTimeLabel(
                publishedAt = now + 5.minutes,
                now = now,
            ),
        )
    }
}
