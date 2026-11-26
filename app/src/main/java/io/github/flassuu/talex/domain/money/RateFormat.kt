package io.github.flassuu.talex.domain.money

import io.github.flassuu.talex.domain.model.RateSyncSettings
import java.math.BigDecimal
import java.math.RoundingMode

/** Rounds a stored rate to the user's precision, dropping trailing zeros. */
fun roundRate(rate: BigDecimal, precision: Int): BigDecimal =
    rate.setScale(RateSyncSettings.normalizePrecision(precision), RoundingMode.HALF_UP)
        .stripTrailingZeros()

/** Formats a rate for display at the chosen precision. */
fun formatRate(rate: BigDecimal, precision: Int): String =
    roundRate(rate, precision).toPlainString()
