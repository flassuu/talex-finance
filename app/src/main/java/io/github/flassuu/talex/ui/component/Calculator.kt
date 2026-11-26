package io.github.flassuu.talex.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.flassuu.talex.R
import io.github.flassuu.talex.domain.money.AmountCalculator

/**
 * A read-only amount field that opens [CalculatorSheet] on tap instead of the
 * system keyboard. The committed value is a plain decimal string.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            label = { Text(label) },
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay: tapping anywhere opens the calculator and keeps the
        // text field from requesting focus or the keyboard.
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { open = true },
            )
        }
    }
    if (open) {
        CalculatorSheet(
            initial = value,
            onDismiss = { open = false },
            onResult = {
                onValueChange(it)
                open = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    initial: String,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expression by remember {
        mutableStateOf(initial.filter { it.isDigit() || it in ".,+-*/()%" })
    }
    val result = AmountCalculator.evaluate(expression)

    fun key(token: String) {
        expression = appendToken(expression, token)
    }

    fun toggleParentheses() {
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        key(if (open > close) ")" else "(")
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = displayExpression(expression).ifEmpty { "0" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = result?.stripTrailingZeros()?.toPlainString()?.replace('.', ',')
                        ?: stringResource(R.string.calculator_error),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (result == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(16.dp))

            CalcRow {
                CalcKey("AC", accent = true) { expression = "" }
                CalcKey("( )") { toggleParentheses() }
                CalcKey("%") { key("%") }
                CalcKey("÷") { key("/") }
            }
            CalcRow {
                CalcKey("7") { key("7") }
                CalcKey("8") { key("8") }
                CalcKey("9") { key("9") }
                CalcKey("×") { key("*") }
            }
            CalcRow {
                CalcKey("4") { key("4") }
                CalcKey("5") { key("5") }
                CalcKey("6") { key("6") }
                CalcKey("−") { key("-") }
            }
            CalcRow {
                CalcKey("1") { key("1") }
                CalcKey("2") { key("2") }
                CalcKey("3") { key("3") }
                CalcKey("+") { key("+") }
            }
            CalcRow {
                CalcKey("0") { key("0") }
                CalcKey(",") { key(",") }
                CalcKey("⌫") { expression = expression.dropLast(1) }
                CalcKey("=", accent = true) {
                    result?.let { onResult(it.stripTrailingZeros().toPlainString()) }
                }
            }
        }
    }
}

@Composable
private fun CalcRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun RowScope.CalcKey(label: String, accent: Boolean = false, onClick: () -> Unit) {
    if (accent) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f).aspectRatio(1f),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.headlineSmall)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.weight(1f).aspectRatio(1f),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** Keeps the expression valid enough to stay readable while typing. */
private fun appendToken(expression: String, token: String): String {
    if (token.isEmpty()) return expression
    val last = expression.lastOrNull()
    return when {
        token == "." || token == "," -> {
            val currentNumber = expression.takeLastWhile { it.isDigit() || it == '.' || it == ',' }
            when {
                currentNumber.contains('.') || currentNumber.contains(',') -> expression
                last == null || last in "+-*/(" -> expression + "0" + token
                else -> expression + token
            }
        }

        token[0].isDigit() -> expression + token

        token in setOf("+", "-", "*", "/") -> when {
            expression.isEmpty() -> if (token == "-") "-" else expression
            last in setOf('+', '-', '*', '/', '(') ->
                if (token == "-" && (last == '*' || last == '/')) expression + token
                else expression.dropLast(1) + token
            else -> expression + token
        }

        token == "%" -> if (last != null && (last.isDigit() || last == ')')) {
            expression + token
        } else {
            expression
        }

        token == "(" -> expression + token

        token == ")" -> if (expression.count { it == '(' } > expression.count { it == ')' }) {
            expression + token
        } else {
            expression
        }

        else -> expression + token
    }
}

private fun displayExpression(expression: String): String =
    expression.replace('*', '×').replace('/', '÷')
