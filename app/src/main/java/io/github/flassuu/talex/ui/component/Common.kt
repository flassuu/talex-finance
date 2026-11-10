package io.github.flassuu.talex.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.flassuu.talex.R
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalexScreen(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = actions,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
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
