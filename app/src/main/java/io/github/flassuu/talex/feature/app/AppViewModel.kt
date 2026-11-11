package io.github.flassuu.talex.feature.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.data.session.LockState
import io.github.flassuu.talex.domain.usecase.SeedCategoriesUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    session: AppSessionManager,
    private val seedCategories: SeedCategoriesUseCase,
) : ViewModel() {

    val lockState: StateFlow<LockState> = session.state

    init {
        // Idempotent: seeds default categories on the first unlock and does nothing after.
        viewModelScope.launch {
            session.state.collect { state ->
                if (state == LockState.UNLOCKED) seedCategories()
            }
        }
    }
}
