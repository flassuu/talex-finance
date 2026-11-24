package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.repository.SettingsRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Changes the base currency through the atomic repository operation (ADR-0033):
 * open budget limits are converted before the setting is written.
 */
class ChangeBaseCurrencyUseCase @Inject constructor(
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(newBase: CurrencyCode, today: LocalDate = LocalDate.now()): Outcome<Unit> {
        if (settings.getBaseCurrency() == newBase) return Outcome.Success(Unit)
        return settings.changeBaseCurrency(newBase, today)
    }
}
