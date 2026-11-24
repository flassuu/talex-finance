package io.github.flassuu.talex.feature.tags

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tags: TagRepository,
) : ViewModel() {

    data class RenameState(
        val tag: Tag,
        val name: String,
        val error: AppError? = null,
        val busy: Boolean = false,
    )

    data class UiMessage(
        val id: Long = System.nanoTime(),
        @StringRes val res: Int? = null,
        val error: AppError? = null,
    )

    data class UiState(
        val tags: List<Tag> = emptyList(),
        val rename: RenameState? = null,
        val delete: Tag? = null,
        val message: UiMessage? = null,
    )

    private data class LocalState(
        val rename: RenameState? = null,
        val delete: Tag? = null,
        val message: UiMessage? = null,
    )

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<UiState> = combine(tags.observeTags(), local) { list, state ->
        UiState(list, state.rename, state.delete, state.message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun startRename(tag: Tag) = local.update {
        it.copy(rename = RenameState(tag = tag, name = tag.name))
    }

    fun onNameChange(value: String) = local.update { state ->
        state.copy(rename = state.rename?.copy(name = value, error = null))
    }

    fun cancelRename() = local.update { it.copy(rename = null) }

    fun confirmRename() {
        val rename = local.value.rename ?: return
        if (rename.busy) return
        if (rename.name.isBlank()) {
            local.update { it.copy(rename = rename.copy(error = AppError.NAME_REQUIRED)) }
            return
        }
        viewModelScope.launch {
            local.update { it.copy(rename = rename.copy(busy = true, error = null)) }
            when (val result = tags.rename(rename.tag.id, rename.name)) {
                is Outcome.Success -> local.update {
                    it.copy(rename = null, message = UiMessage(res = R.string.tag_renamed))
                }
                is Outcome.Failure -> local.update {
                    it.copy(rename = rename.copy(busy = false, error = result.error))
                }
            }
        }
    }

    fun requestDelete(tag: Tag) = local.update { it.copy(delete = tag) }

    fun cancelDelete() = local.update { it.copy(delete = null) }

    fun confirmDelete() {
        val tag = local.value.delete ?: return
        viewModelScope.launch {
            local.update { it.copy(delete = null) }
            when (val result = tags.delete(tag.id)) {
                is Outcome.Success -> local.update {
                    it.copy(message = UiMessage(res = R.string.tag_deleted))
                }
                is Outcome.Failure -> local.update {
                    it.copy(message = UiMessage(error = result.error))
                }
            }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }
}
