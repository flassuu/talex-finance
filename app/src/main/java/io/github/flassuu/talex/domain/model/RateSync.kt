package io.github.flassuu.talex.domain.model

/** When Talex may fetch rates on its own (ADR-0037). */
enum class RateRefreshSchedule {
    MANUAL,
    ON_LAUNCH,
    WEEKLY,
    MONTHLY,
    CUSTOM,
}

/**
 * User preferences for rate synchronisation. They live in the encrypted
 * `settings` table with the profile, so a backup carries them too.
 */
data class RateSyncSettings(
    val schedule: RateRefreshSchedule = RateRefreshSchedule.MANUAL,
    val customDays: Int = DEFAULT_CUSTOM_DAYS,
    val precision: Int = DEFAULT_PRECISION,
    /** Currency codes that are manual-only; everything else may be fetched. */
    val disabledCurrencies: Set<String> = emptySet(),
    val introSeen: Boolean = false,
    val lastFetchAt: Long = 0L,
) {
    fun isCurrencyEnabled(currency: String): Boolean = currency !in disabledCurrencies

    fun isDue(now: Long): Boolean = when (schedule) {
        RateRefreshSchedule.MANUAL -> false
        RateRefreshSchedule.ON_LAUNCH -> true
        RateRefreshSchedule.WEEKLY -> dueAfter(now, WEEK_MS)
        RateRefreshSchedule.MONTHLY -> dueAfter(now, MONTH_MS)
        RateRefreshSchedule.CUSTOM -> dueAfter(now, customDays.coerceAtLeast(1) * DAY_MS)
    }

    private fun dueAfter(now: Long, intervalMs: Long): Boolean =
        intervalMs > 0 && now - lastFetchAt >= intervalMs

    companion object {
        const val MIN_PRECISION = 2
        const val MAX_PRECISION = 8
        const val DEFAULT_PRECISION = 6
        const val MIN_CUSTOM_DAYS = 1
        const val MAX_CUSTOM_DAYS = 365
        const val DEFAULT_CUSTOM_DAYS = 7

        const val DAY_MS = 24L * 60 * 60 * 1000
        const val WEEK_MS = 7 * DAY_MS
        const val MONTH_MS = 30 * DAY_MS

        fun scheduleOf(raw: String?): RateRefreshSchedule =
            RateRefreshSchedule.entries
                .firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: RateRefreshSchedule.MANUAL

        fun normalizePrecision(value: Int): Int = value.coerceIn(MIN_PRECISION, MAX_PRECISION)

        fun normalizeCustomDays(value: Int): Int = value.coerceIn(MIN_CUSTOM_DAYS, MAX_CUSTOM_DAYS)
    }
}
