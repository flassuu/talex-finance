package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Runs the scheduled rate refresh when its interval is due (ADR-0037). It is
 * called on unlock and never runs in the background, so the app stays offline
 * unless the user chose a schedule.
 */
class AutoRefreshRatesUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val updateRates: UpdateRatesUseCase,
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): Outcome<UpdateRatesResult>? {
        val sync = settings.getRateSync()
        if (!sync.isDue(now)) return null
        return updateRates()
    }
}
