package io.github.flassuu.talex.feature.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.preferences.NavigationPreferences
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.data.session.LockState
import io.github.flassuu.talex.domain.usecase.GenerateRecurringUseCase
import io.github.flassuu.talex.domain.usecase.RecurringGeneration
import io.github.flassuu.talex.domain.usecase.SeedCategoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    session: AppSessionManager,
    private val seedCategories: SeedCategoriesUseCase,
    private val generateRecurring: GenerateRecurringUseCase,
    navigationPreferences: NavigationPreferences,
) : ViewModel() {

    val lockState: StateFlow<LockState> = session.state

    /** Three bottom-bar slots, available before the database is opened (ADR-0034). */
    val navigationSlots: StateFlow<List<String>> = navigationPreferences.slots

    private val _isDemo = MutableStateFlow(session.isDemoProfile())
    val isDemo: StateFlow<Boolean> = _isDemo.asStateFlow()

    private val _recurringSummary = MutableStateFlow<RecurringGeneration?>(null)
    val recurringSummary: StateFlow<RecurringGeneration?> = _recurringSummary.asStateFlow()

    init {
        // The demo profile opens without a passphrase (ADR-0031); if its key is
        // gone, drop it and fall back to first-run setup.
        viewModelScope.launch {
            if (session.state.value == LockState.LOCKED && session.isDemoProfile()) {
                if (session.unlockDemo() is Outcome.Failure) {
                    session.deleteDemo()
                }
            }
        }
        // Runs once per unlock: seeds defaults (idempotent) and creates due
        // recurring entries before the user works with the data.
        viewModelScope.launch {
            session.state.collect { state ->
                _isDemo.value = session.isDemoProfile()
                if (state == LockState.UNLOCKED) {
                    seedCategories()
                    val result = generateRecurring(LocalDate.now())
                    if (result.created.isNotEmpty() || result.skipped.isNotEmpty()) {
                        _recurringSummary.value = result
                    }
                }
            }
        }
    }

    fun clearRecurringSummary() {
        _recurringSummary.value = null
    }
}
