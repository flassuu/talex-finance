package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.repository.RateProvider
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Fetches one pair for the add-rate form, so the user can review the value
 * before saving it. It uses the same provider chain as the full refresh.
 */
class FetchRateUseCase @Inject constructor(
    private val provider: RateProvider,
) {
    suspend operator fun invoke(base: CurrencyCode, quote: CurrencyCode): Outcome<BigDecimal> {
        if (base == quote) return Outcome.Failure(AppError.RATE_INVALID)
        val rates = provider.fetch(base, setOf(quote))
            .getOrElse { return Outcome.Failure(AppError.RATE_FETCH_FAILED) }
        val rate = rates[quote] ?: return Outcome.Failure(AppError.RATE_FETCH_FAILED)
        return Outcome.Success(rate)
    }
}
