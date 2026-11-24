package io.github.flassuu.talex.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The amount-privacy switch, shared by the overview quick toggle and Settings. */
@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val hidden: StateFlow<Boolean> = settings.observeHideAmounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setHidden(hidden: Boolean) {
        viewModelScope.launch { settings.setHideAmounts(hidden) }
    }

    fun toggle() = setHidden(!hidden.value)
}
