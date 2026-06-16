package app.pocketsense.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class Cycle(val start: LocalDate, val endExclusive: LocalDate) {
    fun startInstant(zone: ZoneId = ZoneId.systemDefault()) =
        start.atStartOfDay(zone).toInstant()
    fun endInstant(zone: ZoneId = ZoneId.systemDefault()) =
        endExclusive.atStartOfDay(zone).toInstant()
    fun lengthDays(): Int = ChronoUnit.DAYS.between(start, endExclusive).toInt()
    fun daysRemaining(today: LocalDate = LocalDate.now()): Int =
        ChronoUnit.DAYS.between(today, endExclusive).coerceAtLeast(0).toInt()
}

fun currentCycle(today: LocalDate = LocalDate.now(), cycleStartDay: Int = 1): Cycle {
    val day = cycleStartDay.coerceIn(1, 28)
    val candidate = today.withDayOfMonth(day)
    val start = if (today.dayOfMonth >= day) candidate else candidate.minusMonths(1)
    val end = start.plusMonths(1)
    return Cycle(start, end)
}

fun previousCycle(today: LocalDate = LocalDate.now(), cycleStartDay: Int = 1): Cycle {
    val cur = currentCycle(today, cycleStartDay)
    return Cycle(cur.start.minusMonths(1), cur.start)
}

/**
 * The cycle [offset] cycles back from the current one. offset 0 = current cycle,
 * 1 = previous cycle, 2 = the one before that, and so on. Negative offsets are
 * clamped to the current cycle.
 */
fun cycleForOffset(offset: Int, today: LocalDate = LocalDate.now(), cycleStartDay: Int = 1): Cycle {
    val back = offset.toLong().coerceAtLeast(0L)
    val cur = currentCycle(today, cycleStartDay)
    return Cycle(cur.start.minusMonths(back), cur.endExclusive.minusMonths(back))
}

fun safeToSpendTodayPaise(remainingPaise: Long, daysRemaining: Int): Long {
    val nonNeg = remainingPaise.coerceAtLeast(0)
    if (daysRemaining <= 0) return nonNeg
    return nonNeg / daysRemaining
}
