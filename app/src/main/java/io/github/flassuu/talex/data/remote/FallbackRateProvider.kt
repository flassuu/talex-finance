package io.github.flassuu.talex.data.remote

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.domain.repository.RateProvider
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tries the ECB-backed provider first and asks the open.er-api fallback only for
 * the currencies still missing (ADR-0037). This keeps the common case to one
 * request while making UAH and other non-ECB currencies work.
 */
@Singleton
class FallbackRateProvider @Inject constructor(
    private val frankfurter: FrankfurterRateProvider,
    private val openErApi: OpenErApiRateProvider,
) : RateProvider {

    override suspend fun fetch(
        base: CurrencyCode,
        targets: Set<CurrencyCode>,
    ): Result<Map<CurrencyCode, BigDecimal>> {
        val wanted = targets.filter { it != base }.toSet()
        if (wanted.isEmpty()) return Result.success(emptyMap())

        val primary = frankfurter.fetch(base, wanted).getOrNull().orEmpty()
        val missing = wanted - primary.keys
        if (missing.isEmpty()) return Result.success(primary)

        val secondary = openErApi.fetch(base, missing).getOrNull().orEmpty()
        return mergeRates(primary, secondary, wanted)
    }

    companion object {
        internal fun mergeRates(
            primary: Map<CurrencyCode, BigDecimal>,
            secondary: Map<CurrencyCode, BigDecimal>,
            targets: Set<CurrencyCode>,
        ): Result<Map<CurrencyCode, BigDecimal>> {
            val merged = (primary + secondary).filterKeys { it in targets }
            return if (merged.isEmpty()) {
                Result.failure(IOException("No rate provider returned rates"))
            } else {
                Result.success(merged)
            }
        }
    }
}
