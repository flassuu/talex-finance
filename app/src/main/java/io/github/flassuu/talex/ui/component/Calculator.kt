package io.github.flassuu.talex.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
    var failed by remember { mutableStateOf(false) }
    val result = AmountCalculator.evaluate(expression)
    val hasOperator = expression.any { it in "+-*/()%" }

    fun press(block: () -> Unit) {
        failed = false
        block()
    }

    fun key(token: String) = press {
        expression = appendToken(expression, token)
    }

    fun toggleParentheses() = press {
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        expression = appendToken(expression, if (open > close) ")" else "(")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Keep the screen behind the calculator fully visible.
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                // Running result, like the system calculator; hidden until needed.
                Text(
                    text = when {
                        failed -> stringResource(R.string.calculator_error)
                        result != null && hasOperator ->
                            "= " + formatResult(result)
                        else -> " "
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
                // The expression being typed; empty reads as 0, never an error.
                Text(
                    text = displayExpression(expression).ifEmpty { "0" },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                )
            }
            Spacer(Modifier.height(12.dp))

            CalcRow {
                CalcKey("AC", accent = true) { press { expression = "" } }
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
                CalcKey("⌫") { press { expression = expression.dropLast(1) } }
                CalcKey("=", accent = true) {
                    if (result != null) {
                        onResult(result.stripTrailingZeros().toPlainString())
                    } else {
                        failed = true
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun RowScope.CalcKey(label: String, accent: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    if (accent) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = shape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = shape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun formatResult(value: java.math.BigDecimal): String =
    value.stripTrailingZeros().toPlainString().replace('.', ',')

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
