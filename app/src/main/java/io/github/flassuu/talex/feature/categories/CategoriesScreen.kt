package io.github.flassuu.talex.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.usecase.CreateCategoryInput
import io.github.flassuu.talex.domain.usecase.CreateCategoryUseCase
import io.github.flassuu.talex.domain.usecase.RenameCategoryUseCase
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val createCategory: CreateCategoryUseCase,
    private val renameCategory: RenameCategoryUseCase,
) : ViewModel() {

    data class FormState(
        val id: String? = null,
        val kind: CategoryKind = CategoryKind.EXPENSE,
        val name: String = "",
        val parentId: String? = null,
        val parentName: String? = null,
        val error: AppError? = null,
    )

    data class Message(val error: AppError, val categoryId: String?)

    private data class Local(
        val form: FormState? = null,
        val message: Message? = null,
    )

    data class UiState(
        val categories: List<Category> = emptyList(),
        val form: FormState? = null,
        val message: Message? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<UiState> = combine(
        categoryRepository.observeCategories(),
        local,
    ) { categories, l ->
        UiState(categories, l.form, l.message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun openCreate(kind: CategoryKind, parent: Category? = null) {
        local.value = Local(
            form = FormState(
                kind = kind,
                parentId = parent?.id,
                parentName = parent?.name,
            ),
        )
    }

    fun openEdit(category: Category) {
        local.value = Local(
            form = FormState(
                id = category.id,
                kind = category.kind,
                name = category.name,
                parentId = category.parentId,
            ),
        )
    }

    fun closeForm() = local.update { it.copy(form = null) }

    fun onNameChange(value: String) = local.update { l ->
        l.form?.let { l.copy(form = it.copy(name = value, error = null)) } ?: l
    }

    fun save() {
        val form = local.value.form ?: return
        viewModelScope.launch {
            val result: Outcome<*> = if (form.id == null) {
                createCategory(CreateCategoryInput(form.kind, form.name, form.parentId))
            } else {
                renameCategory(form.id, form.name)
            }
            when (result) {
                is Outcome.Success -> local.update { it.copy(form = null, message = null) }
                is Outcome.Failure -> local.update { l ->
                    l.form?.let { l.copy(form = it.copy(error = result.error)) } ?: l
                }
            }
        }
    }

    fun setArchived(category: Category, archived: Boolean) {
        viewModelScope.launch {
            when (val result = categoryRepository.setArchived(category.id, archived)) {
                is Outcome.Success -> local.update { it.copy(message = null) }
                is Outcome.Failure -> local.update { it.copy(message = Message(result.error, null)) }
            }
        }
    }

    fun setUnnecessary(category: Category, unnecessary: Boolean) {
        viewModelScope.launch {
            when (val result = categoryRepository.setUnnecessary(category.id, unnecessary)) {
                is Outcome.Success -> local.update { it.copy(message = null) }
                is Outcome.Failure -> local.update { it.copy(message = Message(result.error, null)) }
            }
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            when (val result = categoryRepository.delete(category.id)) {
                is Outcome.Success -> local.update { it.copy(message = null) }
                is Outcome.Failure -> local.update {
                    it.copy(message = Message(result.error, category.id))
                }
            }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }
}

@Composable
fun CategoriesScreen(
    onViewTransactions: (String) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        when (message.error) {
            AppError.CATEGORY_IN_USE -> stringResource(R.string.error_category_in_use)
            AppError.CATEGORY_HAS_CHILDREN -> stringResource(R.string.error_category_has_children)
            else -> errorMessage(message.error)
        }
    }
    val viewTransactionsLabel = stringResource(R.string.view_transactions)

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = messageText ?: "",
            actionLabel = if (message.error == AppError.CATEGORY_IN_USE) viewTransactionsLabel else null,
        )
        if (result == SnackbarResult.ActionPerformed) {
            message.categoryId?.let(onViewTransactions)
        }
        viewModel.clearMessage()
    }

    TalexScreen(
        title = stringResource(R.string.categories_title),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionHeader(
                    title = stringResource(R.string.expense_section),
                    onAdd = { viewModel.openCreate(CategoryKind.EXPENSE) },
                )
            }
            items(flatten(state.categories, CategoryKind.EXPENSE), key = { it.first.id }) { (category, depth) ->
                CategoryRow(
                    category = category,
                    depth = depth,
                    onEdit = { viewModel.openEdit(category) },
                    onAddChild = { viewModel.openCreate(category.kind, category) },
                    onArchive = { viewModel.setArchived(category, !category.archived) },
                    onToggleUnnecessary = { viewModel.setUnnecessary(category, !category.unnecessary) },
                    onDelete = { viewModel.delete(category) },
                )
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.income_section),
                    onAdd = { viewModel.openCreate(CategoryKind.INCOME) },
                )
            }
            items(flatten(state.categories, CategoryKind.INCOME), key = { it.first.id }) { (category, depth) ->
                CategoryRow(
                    category = category,
                    depth = depth,
                    onEdit = { viewModel.openEdit(category) },
                    onAddChild = { viewModel.openCreate(category.kind, category) },
                    onArchive = { viewModel.setArchived(category, !category.archived) },
                    onToggleUnnecessary = { viewModel.setUnnecessary(category, !category.unnecessary) },
                    onDelete = { viewModel.delete(category) },
                )
            }
        }
    }

    state.form?.let { form ->
        CategoryDialog(
            form = form,
            onDismiss = viewModel::closeForm,
            onNameChange = viewModel::onNameChange,
            onConfirm = viewModel::save,
        )
    }
}

private fun flatten(categories: List<Category>, kind: CategoryKind): List<Pair<Category, Int>> {
    val ofKind = categories.filter { it.kind == kind }
    val childrenByParent = ofKind.filter { it.parentId != null }.groupBy { it.parentId }
    val result = mutableListOf<Pair<Category, Int>>()
    ofKind.filter { it.parentId == null }.sortedBy { it.name.lowercase(Locale.ROOT) }.forEach { parent ->
        result.add(parent to 0)
        childrenByParent[parent.id]?.sortedBy { it.name.lowercase(Locale.ROOT) }?.forEach { child ->
            result.add(child to 1)
        }
    }
    return result
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAdd) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = stringResource(R.string.add_category))
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    depth: Int,
    onEdit: () -> Unit,
    onAddChild: () -> Unit,
    onArchive: () -> Unit,
    onToggleUnnecessary: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (if (depth > 0) "– " else "") + categoryDisplayName(category),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (category.archived) {
                    Text(
                        text = stringResource(R.string.category_archived_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (category.unnecessary) {
                    Text(
                        text = stringResource(R.string.category_unnecessary_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.more_actions),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (depth == 0 && !category.archived) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_subcategory)) },
                        onClick = {
                            menuOpen = false
                            onAddChild()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rename_action)) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (category.archived) R.string.unarchive_action else R.string.archive_action,
                            ),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onArchive()
                    },
                )
                if (category.kind == CategoryKind.EXPENSE) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (category.unnecessary) {
                                        R.string.mark_not_unnecessary
                                    } else {
                                        R.string.mark_unnecessary
                                    },
                                ),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleUnnecessary()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_action)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    form: CategoriesViewModel.FormState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (form.id == null) R.string.add_category else R.string.edit_category,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                form.parentName?.let { parent ->
                    Text(
                        text = stringResource(R.string.category_parent_label, parent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    isError = form.error == AppError.NAME_REQUIRED || form.error == AppError.NAME_TOO_LONG,
                    modifier = Modifier.fillMaxWidth(),
                )
                form.error?.let { error ->
                    Text(
                        text = errorMessage(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}
