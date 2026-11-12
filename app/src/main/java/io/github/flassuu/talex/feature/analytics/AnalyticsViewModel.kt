package io.github.flassuu.talex.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.repository.ValuationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    accountRepository: AccountRepository,
    entryRepository: EntryRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    rateRepository: RateRepository,
    valuationRepository: ValuationRepository,
) : ViewModel() {

    data class UiState(
        val period: AnalyticsPeriod = AnalyticsPeriod.current(),
        val data: AnalyticsData? = null,
    )

    private val period = MutableStateFlow<AnalyticsPeriod>(AnalyticsPeriod.current())

    private val accountData = combine(
        accountRepository.observeAccounts(),
        valuationRepository.observeAll(),
    ) { accounts, valuations -> accounts to valuations }

    private val ledgerData = combine(
        entryRepository.observeAllWithLines(),
        categoryRepository.observeCategories(),
    ) { entries, categories -> entries to categories }

    val uiState: StateFlow<UiState> = combine(
        period,
        accountData,
        ledgerData,
        settingsRepository.observeBaseCurrency(),
        rateRepository.observeAll(),
    ) { selectedPeriod, (accounts, valuations), (entries, categories), base, rates ->
        UiState(
            period = selectedPeriod,
            data = computeAnalytics(
                accounts = accounts,
                entries = entries,
                categories = categories,
                valuations = valuations,
                rates = rates,
                base = base ?: CurrencyCode.EUR,
                period = selectedPeriod,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setPeriod(newPeriod: AnalyticsPeriod) {
        period.update { newPeriod }
    }
}
