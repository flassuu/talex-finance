package io.github.flassuu.talex.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.EntryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalexScreen(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { navigationIcon?.invoke() },
                actions = actions,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        snackbarHost = snackbarHost,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content,
    )
}

@Composable
fun PlaceholderScreen(@StringRes titleRes: Int) {
    TalexScreen(title = stringResource(titleRes)) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.placeholder_coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun errorMessage(error: AppError): String = stringResource(
    when (error) {
        AppError.NAME_REQUIRED -> R.string.error_name_required
        AppError.NAME_TOO_LONG -> R.string.error_name_too_long
        AppError.AMOUNT_INVALID -> R.string.error_amount_invalid
        AppError.AMOUNT_REQUIRED -> R.string.error_amount_required
        AppError.DESCRIPTION_REQUIRED -> R.string.error_description_required
        AppError.DESCRIPTION_TOO_LONG -> R.string.error_description_too_long
        AppError.ACCOUNT_NOT_FOUND -> R.string.error_account_not_found
        AppError.ACCOUNT_ARCHIVED -> R.string.error_account_archived
        AppError.ACCOUNT_NOT_EMPTY -> R.string.error_account_not_empty
        AppError.CURRENCY_MISMATCH -> R.string.error_currency_mismatch
        AppError.CATEGORY_REQUIRED -> R.string.error_category_required
        AppError.CATEGORY_NOT_FOUND -> R.string.error_category_not_found
        AppError.CATEGORY_ARCHIVED -> R.string.error_category_archived
        AppError.CATEGORY_KIND_MISMATCH -> R.string.error_category_kind_mismatch
        AppError.CATEGORY_PARENT_INVALID -> R.string.error_category_parent_invalid
        AppError.CATEGORY_IN_USE -> R.string.error_category_in_use
        AppError.CATEGORY_HAS_CHILDREN -> R.string.error_category_has_children
        AppError.ENTRY_NOT_FOUND -> R.string.error_entry_not_found
        AppError.SAME_ACCOUNT_TRANSFER -> R.string.error_same_account_transfer
        AppError.TRANSFER_AMOUNT_MISMATCH -> R.string.error_transfer_amount_mismatch
        AppError.SPLIT_INVALID -> R.string.error_split_invalid
        AppError.SPLIT_SUM_MISMATCH -> R.string.error_split_sum_mismatch
        AppError.SPLIT_SAME_PARENT -> R.string.error_split_same_parent
        AppError.DATABASE_LOCKED -> R.string.error_database_locked
        AppError.WRONG_PASSPHRASE -> R.string.unlock_error
        AppError.PASSPHRASE_TOO_SHORT -> R.string.passphrase_too_short
        AppError.PASSPHRASE_MISMATCH -> R.string.passphrase_mismatch
        AppError.BIOMETRIC_FAILED -> R.string.unlock_biometric_error
        else -> R.string.error_unknown
    },
)

@Composable
fun accountTypeLabel(type: AccountType): String = stringResource(
    when (type) {
        AccountType.CHECKING -> R.string.account_type_checking
        AccountType.SAVINGS -> R.string.account_type_savings
        AccountType.CASH -> R.string.account_type_cash
        AccountType.CREDIT_CARD -> R.string.account_type_credit_card
        AccountType.INVESTMENT -> R.string.account_type_investment
        AccountType.LOAN -> R.string.account_type_loan
        AccountType.OTHER -> R.string.account_type_other
    },
)

@Composable
fun entryTypeLabel(type: EntryType): String = stringResource(
    when (type) {
        EntryType.INCOME -> R.string.entry_type_income
        EntryType.EXPENSE -> R.string.entry_type_expense
        EntryType.TRANSFER -> R.string.entry_type_transfer
        EntryType.OPENING -> R.string.entry_type_opening
        EntryType.ADJUSTMENT -> R.string.entry_type_adjustment
    },
)

/** Seeded defaults are stored with a stable key and shown translated; custom categories show their name. */
@Composable
fun categoryDisplayName(category: Category): String {
    val res = category.stableKey?.let(::defaultCategoryRes)
    return if (res != null) stringResource(res) else category.name
}

private fun defaultCategoryRes(key: String): Int? = when (key) {
    "food" -> R.string.category_food
    "food_groceries" -> R.string.category_food_groceries
    "food_restaurants" -> R.string.category_food_restaurants
    "transport" -> R.string.category_transport
    "housing" -> R.string.category_housing
    "health" -> R.string.category_health
    "shopping" -> R.string.category_shopping
    "entertainment" -> R.string.category_entertainment
    "other_expense" -> R.string.category_other_expense
    "salary" -> R.string.category_salary
    "bonus" -> R.string.category_bonus
    "gift" -> R.string.category_gift
    "other_income" -> R.string.category_other_income
    else -> null
}

@Composable
fun AccountTypeSelector(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.account_type_label) + ": " + accountTypeLabel(selected),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AccountType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(accountTypeLabel(type)) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                )
            }
        }
    }
}
