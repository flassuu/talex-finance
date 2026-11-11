package io.github.flassuu.talex.feature.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    val lockState: StateFlow<LockState> = session.state

    private val _recurringSummary = MutableStateFlow<RecurringGeneration?>(null)
    val recurringSummary: StateFlow<RecurringGeneration?> = _recurringSummary.asStateFlow()

    init {
        // Runs once per unlock: seeds defaults (idempotent) and creates due
        // recurring entries before the user works with the data.
        viewModelScope.launch {
            session.state.collect { state ->
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
