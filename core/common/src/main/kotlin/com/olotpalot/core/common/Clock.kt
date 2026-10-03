package com.olotpalot.core.common

/** Thin clock abstraction so recency/trending math is testable. */
fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

object TimeFormat {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /** "12 min", "1 h 05 min", "45 s" — product copy, never raw numbers. */
    fun duration(ms: Long): String {
        if (ms <= 0) return "—"
        val totalMin = ms / MINUTE
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 -> "$h h ${m.toString().padStart(2, '0')} min"
            totalMin > 0 -> "$totalMin min"
            else -> "${ms / 1000} s"
        }
    }

    /** "3:05" / "1:02:09" for players. */
    fun clock(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun relative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp
        return when {
            diff < MINUTE -> "Just now"
            diff < HOUR -> "${diff / MINUTE} min ago"
            diff < DAY -> "${diff / HOUR} h ago"
            diff < 7 * DAY -> "${diff / DAY} d ago"
            diff < 30 * DAY -> "${diff / (7 * DAY)} w ago"
            else -> "${diff / (30 * DAY)} mo ago"
        }
    }

    fun compactCount(n: Long): String = when {
        n >= 1_000_000 -> trim1(n / 1_000_000.0) + "M"
        n >= 1_000 -> trim1(n / 1_000.0) + "k"
        else -> n.toString()
    }

    private fun trim1(v: Double): String =
        if (v >= 10) v.toLong().toString() else "%.1f".format(v).removeSuffix(".0")
}
