package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.repository.RateProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FetchRateUseCaseTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD

    @Test
    fun returnsTheRequestedRate() = runTest {
        val provider = FakeProvider { base, targets ->
            assertEquals(eur, base)
            assertEquals(setOf(usd), targets)
            Result.success(mapOf(usd to BigDecimal("1.09")))
        }

        val result = FetchRateUseCase(provider)(eur, usd)

        assertTrue(result is Outcome.Success)
        assertEquals(0, BigDecimal("1.09").compareTo((result as Outcome.Success).value))
    }

    @Test
    fun aMissingRateIsAFailure() = runTest {
        val provider = FakeProvider { _, _ -> Result.success(emptyMap()) }
        assertEquals(
            Outcome.Failure(AppError.RATE_FETCH_FAILED),
            FetchRateUseCase(provider)(eur, usd),
        )
    }

    @Test
    fun theSameCurrencyIsRejected() = runTest {
        val provider = FakeProvider { _, _ -> error("should not be called") }
        assertEquals(
            Outcome.Failure(AppError.RATE_INVALID),
            FetchRateUseCase(provider)(eur, eur),
        )
    }

    private class FakeProvider(
        private val block: (CurrencyCode, Set<CurrencyCode>) -> Result<Map<CurrencyCode, BigDecimal>>,
    ) : RateProvider {
        override suspend fun fetch(
            base: CurrencyCode,
            targets: Set<CurrencyCode>,
        ): Result<Map<CurrencyCode, BigDecimal>> = block(base, targets)
    }
}
