package io.github.flassuu.talex.feature.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.data.session.LockState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    session: AppSessionManager,
) : ViewModel() {
    val lockState: StateFlow<LockState> = session.state
}
