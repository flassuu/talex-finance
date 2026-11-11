package io.github.flassuu.talex.feature.entries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.money.MoneyFormat
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.EntryLineInput
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.usecase.SaveEntryInput
import io.github.flassuu.talex.domain.usecase.SaveEntryUseCase
import io.github.flassuu.talex.ui.component.TalexScreen
import io.github.flassuu.talex.ui.component.categoryDisplayName
import io.github.flassuu.talex.ui.component.entryTypeLabel
import io.github.flassuu.talex.ui.component.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EntryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entryRepository: EntryRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val saveEntry: SaveEntryUseCase,
) : ViewModel() {

    private val entryId: String? = savedStateHandle.get<String>("entryId")

    private val initialType: EntryType = savedStateHandle.get<String>("type")
        ?.let { runCatching { EntryType.valueOf(it) }.getOrNull() }
        ?.takeIf { it == EntryType.INCOME || it == EntryType.EXPENSE }
        ?: EntryType.EXPENSE

    data class LineState(
        val categoryId: String? = null,
        val amount: String = "",
        val note: String = "",
    )

    data class FormState(
        val type: EntryType = EntryType.EXPENSE,
        val amount: String = "",
        val accountId: String? = null,
        val categoryId: String? = null,
        val date: LocalDate = LocalDate.now(),
        val description: String = "",
        val payee: String = "",
        val notes: String = "",
        val tags: List<String> = emptyList(),
        val tagInput: String = "",
        val split: Boolean = false,
        val lines: List<LineState> = emptyList(),
        val error: AppError? = null,
    )

    data class UiState(
        val accounts: List<Account> = emptyList(),
        val categories: List<Category> = emptyList(),
        val form: FormState = FormState(),
        val editing: Boolean = false,
        val loading: Boolean = true,
        val saving: Boolean = false,
        val done: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = accountRepository.observeAccounts().first()
            val categories = categoryRepository.observeCategories().first()
            val entry = entryId?.let { entryRepository.get(it) }
            val tags = entryId?.let { entryRepository.tagsForEntry(it) } ?: emptyList()
            val form = if (entry != null) {
                val entryLines = entry.lines.map { line ->
                    LineState(
                        categoryId = line.categoryId,
                        amount = amountToText(line.amount.amountMinor, line.amount.currency.fractionDigits),
                        note = line.note.orEmpty(),
                    )
                }
                FormState(
                    type = entry.type,
                    amount = amountToText(entry.amount.amountMinor, entry.amount.currency.fractionDigits),
                    accountId = entry.accountId,
                    categoryId = entry.categoryId,
                    date = entry.date,
                    description = entry.description,
                    payee = entry.payee.orEmpty(),
                    notes = entry.notes.orEmpty(),
                    tags = tags.map { it.name },
                    split = entryLines.isNotEmpty(),
                    lines = entryLines,
                )
            } else {
                FormState(
                    type = initialType,
                    accountId = accounts.firstOrNull { !it.archived }?.id,
                )
            }
            _uiState.value = UiState(accounts, categories, form, editing = entry != null, loading = false)
        }
    }

    fun onTypeChange(type: EntryType) = updateForm { it.copy(type = type, error = null) }

    fun onAmountChange(value: String) = updateForm { it.copy(amount = value, error = null) }

    fun onAccountChange(id: String) = updateForm { it.copy(accountId = id, error = null) }

    fun onCategoryChange(id: String?) = updateForm { it.copy(categoryId = id) }

    fun onDateChange(date: LocalDate) = updateForm { it.copy(date = date) }

    fun onDescriptionChange(value: String) = updateForm { it.copy(description = value, error = null) }

    fun onPayeeChange(value: String) = updateForm { it.copy(payee = value) }

    fun onNotesChange(value: String) = updateForm { it.copy(notes = value) }

    fun onTagInputChange(value: String) = updateForm { it.copy(tagInput = value) }

    fun addTag() {
        val tag = _uiState.value.form.tagInput.trim()
        if (tag.isEmpty()) return
        updateForm { form ->
            if (form.tags.any { it.equals(tag, ignoreCase = true) }) {
                form.copy(tagInput = "")
            } else {
                form.copy(tags = form.tags + tag, tagInput = "")
            }
        }
    }

    fun removeTag(tag: String) = updateForm { form ->
        form.copy(tags = form.tags.filterNot { it == tag })
    }

    fun onSplitChange(enabled: Boolean) = updateForm { form ->
        if (enabled) {
            form.copy(
                split = true,
                categoryId = null,
                lines = if (form.lines.size >= 2) form.lines else listOf(LineState(), LineState()),
            )
        } else {
            form.copy(split = false, lines = emptyList())
        }
    }

    fun onLineCategoryChange(index: Int, categoryId: String) = updateForm { form ->
        form.copy(
            lines = form.lines.mapIndexed { i, line ->
                if (i == index) line.copy(categoryId = categoryId) else line
            },
        )
    }

    fun onLineAmountChange(index: Int, value: String) = updateForm { form ->
        form.copy(
            lines = form.lines.mapIndexed { i, line ->
                if (i == index) line.copy(amount = value) else line
            },
            error = null,
        )
    }

    fun onLineNoteChange(index: Int, value: String) = updateForm { form ->
        form.copy(
            lines = form.lines.mapIndexed { i, line ->
                if (i == index) line.copy(note = value) else line
            },
        )
    }

    fun addLine() = updateForm { it.copy(lines = it.lines + LineState()) }

    fun removeLine(index: Int) = updateForm { form ->
        val next = form.lines.filterIndexed { i, _ -> i != index }
        form.copy(lines = next, split = next.isNotEmpty())
    }

    fun save() {
        val state = _uiState.value
        if (state.saving) return
        val form = state.form
        val account = state.accounts.firstOrNull { it.id == form.accountId }
        if (account == null) {
            updateForm { it.copy(error = AppError.ACCOUNT_NOT_FOUND) }
            return
        }
        val lines = if (form.split) {
            val built = mutableListOf<EntryLineInput>()
            for (line in form.lines) {
                val categoryId = line.categoryId
                if (categoryId == null) {
                    updateForm { it.copy(error = AppError.SPLIT_INVALID) }
                    return
                }
                val lineAmount = MoneyFormat.parse(line.amount, account.currency)
                if (lineAmount == null || lineAmount.isZero) {
                    updateForm { it.copy(error = AppError.AMOUNT_INVALID) }
                    return
                }
                val lineSigned = if (form.type == EntryType.INCOME) lineAmount.abs() else -lineAmount.abs()
                built.add(EntryLineInput(categoryId, lineSigned, line.note.ifBlank { null }))
            }
            if (built.size < 2) {
                updateForm { it.copy(error = AppError.SPLIT_INVALID) }
                return
            }
            built
        } else {
            emptyList()
        }
        // The split total is the sum of its lines; a simple entry uses its amount field.
        val signed = if (form.split) {
            Money(lines.sumOf { it.amount.amountMinor }, account.currency)
        } else {
            val parsed = MoneyFormat.parse(form.amount, account.currency)
            if (parsed == null || parsed.isZero) {
                updateForm { it.copy(error = AppError.AMOUNT_INVALID) }
                return
            }
            when (form.type) {
                EntryType.INCOME -> parsed.abs()
                EntryType.EXPENSE -> -parsed.abs()
                else -> parsed
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val result = saveEntry(
                SaveEntryInput(
                    id = entryId,
                    accountId = account.id,
                    type = form.type,
                    amount = signed,
                    date = form.date,
                    description = form.description,
                    payee = form.payee,
                    notes = form.notes,
                    categoryId = if (form.split) null else form.categoryId,
                    tagNames = form.tags,
                    lines = lines,
                ),
            )
            when (result) {
                is Outcome.Success -> _uiState.update { it.copy(saving = false, done = true) }
                is Outcome.Failure -> _uiState.update {
                    it.copy(saving = false, form = it.form.copy(error = result.error))
                }
            }
        }
    }

    fun delete() {
        val id = entryId ?: return
        viewModelScope.launch {
            when (val result = entryRepository.delete(id)) {
                is Outcome.Success -> _uiState.update { it.copy(done = true) }
                is Outcome.Failure -> updateForm { it.copy(error = result.error) }
            }
        }
    }

    private fun updateForm(block: (FormState) -> FormState) {
        _uiState.update { it.copy(form = block(it.form)) }
    }

    private fun amountToText(amountMinor: Long, fractionDigits: Int): String =
        BigDecimal(amountMinor)
            .abs()
            .movePointLeft(fractionDigits)
            .setScale(fractionDigits, RoundingMode.UNNECESSARY)
            .toPlainString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(
    onDone: () -> Unit,
    viewModel: EntryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var linePickerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    val title = when {
        state.editing -> stringResource(R.string.edit_transaction_title)
        state.form.type == EntryType.INCOME -> stringResource(R.string.new_income_title)
        else -> stringResource(R.string.new_expense_title)
    }

    val form = state.form
    val selectedAccount = state.accounts.firstOrNull { it.id == form.accountId }
    val selectedCategory = state.categories.firstOrNull { it.id == form.categoryId }
    val accountText = selectedAccount?.let { it.name + " (" + it.currency.value + ")" }
        ?: stringResource(R.string.entry_account_label)
    val categoryText = selectedCategory?.let { categoryDisplayName(it) }
        ?: stringResource(R.string.category_none)

    TalexScreen(
        title = title,
        navigationIcon = {
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_action),
                )
            }
        },
        actions = {
            if (state.editing) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete_entry),
                    )
                }
            }
        },
    ) { padding ->
        if (state.loading) return@TalexScreen

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = form.type == EntryType.EXPENSE,
                    onClick = { viewModel.onTypeChange(EntryType.EXPENSE) },
                    label = { Text(stringResource(R.string.entry_type_expense)) },
                )
                FilterChip(
                    selected = form.type == EntryType.INCOME,
                    onClick = { viewModel.onTypeChange(EntryType.INCOME) },
                    label = { Text(stringResource(R.string.entry_type_income)) },
                )
            }

            Row {
                FilterChip(
                    selected = form.split,
                    onClick = { viewModel.onSplitChange(!form.split) },
                    label = { Text(stringResource(R.string.split_label)) },
                )
            }

            if (form.split) {
                val currency = selectedAccount?.currency ?: CurrencyCode.EUR
                val totalMinor = form.lines.sumOf { line ->
                    MoneyFormat.parse(line.amount, currency)?.amountMinor ?: 0L
                }
                val totalSigned = if (form.type == EntryType.EXPENSE) -totalMinor else totalMinor
                Text(
                    text = stringResource(R.string.split_total) + ": " +
                        MoneyFormat.format(Money(totalSigned, currency)),
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text(stringResource(R.string.entry_amount_label)) },
                    singleLine = true,
                    isError = form.error == AppError.AMOUNT_INVALID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SelectorField(
                label = stringResource(R.string.entry_account_label),
                value = accountText,
                onClick = { showAccountPicker = true },
            )

            if (form.split) {
                Text(
                    text = stringResource(R.string.split_lines_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                form.lines.forEachIndexed { index, line ->
                    val lineCategory = state.categories.firstOrNull { it.id == line.categoryId }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        linePickerIndex = index
                                        showCategoryPicker = true
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = lineCategory?.let { categoryDisplayName(it) }
                                            ?: stringResource(R.string.category_none),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { viewModel.removeLine(index) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.remove_split_line),
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = line.amount,
                                onValueChange = { viewModel.onLineAmountChange(index, it) },
                                label = { Text(stringResource(R.string.entry_amount_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = line.note,
                                onValueChange = { viewModel.onLineNoteChange(index, it) },
                                label = { Text(stringResource(R.string.split_note_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                TextButton(onClick = viewModel::addLine) {
                    Text(stringResource(R.string.add_split_line))
                }
            } else {
                SelectorField(
                    label = stringResource(R.string.entry_category_label),
                    value = categoryText,
                    onClick = { showCategoryPicker = true },
                )
            }

            SelectorField(
                label = stringResource(R.string.entry_date_label),
                value = form.date.toString(),
                onClick = { showDatePicker = true },
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.entry_description_optional)) },
                singleLine = true,
                isError = form.error == AppError.DESCRIPTION_TOO_LONG,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.payee,
                onValueChange = viewModel::onPayeeChange,
                label = { Text(stringResource(R.string.entry_payee_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.entry_notes_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.entry_tags_label),
                style = MaterialTheme.typography.labelLarge,
            )
            if (form.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    form.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeTag(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.remove_tag),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = form.tagInput,
                    onValueChange = viewModel::onTagInputChange,
                    label = { Text(stringResource(R.string.add_tag_action)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::addTag) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_tag_action),
                    )
                }
            }

            if (state.accounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_accounts_hint),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            form.error?.let { error ->
                Text(
                    text = errorMessage(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving && state.accounts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_action))
            }
        }
    }

    if (showAccountPicker) {
        AlertDialog(
            onDismissRequest = { showAccountPicker = false },
            title = { Text(stringResource(R.string.entry_account_label)) },
            text = {
                Column {
                    state.accounts.forEach { account ->
                        OptionRow(
                            text = account.name + " (" + account.currency.value + ")",
                            selected = account.id == form.accountId,
                            enabled = !account.archived,
                            onClick = {
                                viewModel.onAccountChange(account.id)
                                showAccountPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAccountPicker = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }

    if (showCategoryPicker) {
        val kind = if (form.type == EntryType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        val pickerIndex = linePickerIndex
        CategoryPickerDialog(
            kind = kind,
            categories = state.categories,
            selectedId = if (pickerIndex != null) {
                form.lines.getOrNull(pickerIndex)?.categoryId
            } else {
                form.categoryId
            },
            onSelect = { id ->
                if (pickerIndex != null) {
                    if (id != null) viewModel.onLineCategoryChange(pickerIndex, id)
                    linePickerIndex = null
                } else {
                    viewModel.onCategoryChange(id)
                }
                showCategoryPicker = false
            },
            onDismiss = {
                linePickerIndex = null
                showCategoryPicker = false
            },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDateChange(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                            )
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_entry)) },
            text = { Text(stringResource(R.string.delete_entry_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

@Composable
private fun SelectorField(label: String, value: String, onClick: () -> Unit) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selected) {
            Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    kind: CategoryKind,
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val flattened = flattenCategories(categories.filter { it.kind == kind })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.entry_category_label)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                OptionRow(
                    text = stringResource(R.string.category_none),
                    selected = selectedId == null,
                    onClick = { onSelect(null) },
                )
                flattened.forEach { (category, depth) ->
                    OptionRow(
                        text = (if (depth > 0) "– " else "") + categoryDisplayName(category),
                        selected = category.id == selectedId,
                        enabled = !category.archived,
                        onClick = { onSelect(category.id) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}

private fun flattenCategories(categories: List<Category>): List<Pair<Category, Int>> {
    val childrenByParent = categories.filter { it.parentId != null }.groupBy { it.parentId }
    val result = mutableListOf<Pair<Category, Int>>()
    categories.filter { it.parentId == null }
        .sortedBy { it.name.lowercase(Locale.ROOT) }
        .forEach { parent ->
            result.add(parent to 0)
            childrenByParent[parent.id]
                ?.sortedBy { it.name.lowercase(Locale.ROOT) }
                ?.forEach { child -> result.add(child to 1) }
        }
    return result
}
