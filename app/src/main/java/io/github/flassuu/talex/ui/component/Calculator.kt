package io.github.flassuu.talex.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.flassuu.talex.R
import io.github.flassuu.talex.domain.money.AmountCalculator
import io.github.flassuu.talex.domain.money.Calculation
import java.math.BigDecimal
import kotlin.math.roundToInt

/** A plain number: optionally signed, with a decimal separator. */
private val PLAIN_NUMBER = Regex("-?\\d+([.,]\\d*)?")

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
    val initialExpression = initial.filter { it.isDigit() || it in ".,+-*/()%" }
    var expression by remember { mutableStateOf(initialExpression) }
    var caret by remember { mutableStateOf(initialExpression.length) }
    var failed by remember { mutableStateOf(false) }

    val calculation = AmountCalculator.analyze(expression)
    val result = (calculation as? Calculation.Value)?.value
    val isComplete = expression.isBlank() || (result != null && PLAIN_NUMBER.matches(expression))

    fun press(block: () -> Unit) {
        failed = false
        block()
    }

    fun insert(token: String) = press {
        val left = expression.substring(0, caret)
        val right = expression.substring(caret)
        val newLeft = appendToken(left, token)
        expression = newLeft + right
        caret = newLeft.length
    }

    fun backspace() = press {
        if (caret > 0) {
            expression = expression.removeRange(caret - 1, caret)
            caret--
        }
    }

    fun toggleParentheses() = press {
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        insert(if (open > close) ")" else "(")
    }

    // "=" on an equation, "✓" on a completed single number.
    fun confirmOrEvaluate() = press {
        if (isComplete) {
            val value = if (expression.isBlank()) BigDecimal.ZERO else result
            value?.let { onResult(it.stripTrailingZeros().toPlainString()) }
        } else {
            val value = when (calculation) {
                is Calculation.Value -> calculation.value
                Calculation.Error -> null
                Calculation.Incomplete -> committableValue(expression)
            }
            if (value != null) {
                expression = value.stripTrailingZeros().toPlainString()
                caret = expression.length
            } else {
                failed = true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                // Main field: the expression being typed, with a movable caret.
                AnimatedContent(
                    targetState = expression,
                    transitionSpec = {
                        (fadeIn(tween(160)) + slideInVertically { it / 4 } + scaleIn(initialScale = 0.97f))
                            .togetherWith(fadeOut(tween(80)))
                    },
                    label = "expression",
                ) { text ->
                    ExpressionField(
                        expression = text,
                        caret = caret.coerceIn(0, text.length),
                        onTap = { caret = it },
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Lower field: the running result, or an error after "=".
                AnimatedContent(
                    targetState = when {
                        failed -> stringResource(R.string.calculator_error)
                        !isComplete && result != null -> "= " + formatResult(result)
                        else -> " "
                    },
                    transitionSpec = {
                        (fadeIn(tween(140)) + scaleIn(initialScale = 0.92f)).togetherWith(fadeOut(tween(80)))
                    },
                    label = "result",
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        textAlign = TextAlign.End,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            CalcRow {
                CalcKey("AC", accent = true) { press { expression = ""; caret = 0 } }
                CalcKey("( )") { toggleParentheses() }
                CalcKey("%") { insert("%") }
                CalcKey("÷") { insert("/") }
            }
            CalcRow {
                CalcKey("7") { insert("7") }
                CalcKey("8") { insert("8") }
                CalcKey("9") { insert("9") }
                CalcKey("×") { insert("*") }
            }
            CalcRow {
                CalcKey("4") { insert("4") }
                CalcKey("5") { insert("5") }
                CalcKey("6") { insert("6") }
                CalcKey("−") { insert("-") }
            }
            CalcRow {
                CalcKey("1") { insert("1") }
                CalcKey("2") { insert("2") }
                CalcKey("3") { insert("3") }
                CalcKey("+") { insert("+") }
            }
            CalcRow {
                CalcKey(",") { insert(",") }
                CalcKey("0") { insert("0") }
                CalcKey("⌫") { backspace() }
                ConfirmKey(isComplete = isComplete) { confirmOrEvaluate() }
            }
        }
    }
}

@Composable
private fun ExpressionField(
    expression: String,
    caret: Int,
    onTap: (Int) -> Unit,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(expression) {
                detectTapGestures { offset ->
                    val result = layoutResult ?: return@detectTapGestures
                    val textLeft = (size.width - result.size.width).coerceAtLeast(0)
                    val position = Offset(offset.x - textLeft, offset.y)
                    onTap(result.getOffsetForPosition(position).coerceIn(0, expression.length))
                }
            },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box {
            Text(
                text = displayExpression(expression).ifEmpty { "0" },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.End,
                onTextLayout = { layoutResult = it },
            )
            val caretRect = layoutResult?.getCursorRect(caret)
            if (caretRect != null) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(caretRect.left.roundToInt(), caretRect.top.roundToInt())
                        }
                        .width(2.dp)
                        .height(with(androidx.compose.ui.platform.LocalDensity.current) {
                            caretRect.height.toDp()
                        })
                        .alpha(cursorAlpha),
                )
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
private fun RowScope.ConfirmKey(isComplete: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(52.dp),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        AnimatedContent(
            targetState = if (isComplete) "✓" else "=",
            transitionSpec = {
                (fadeIn(tween(160)) + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut(tween(80)))
            },
            label = "confirm",
        ) { label ->
            Text(text = label, style = MaterialTheme.typography.titleLarge)
        }
    }
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

private fun formatResult(value: BigDecimal): String =
    value.stripTrailingZeros().toPlainString().replace('.', ',')

/** What "=" collapses to for an incomplete expression: 0 when empty, else the last valid prefix. */
private fun committableValue(expression: String): BigDecimal? {
    if (expression.isBlank()) return BigDecimal.ZERO
    var candidate = expression
    while (candidate.isNotEmpty()) {
        candidate = candidate.dropLast(1)
        val balanced = candidate + ")".repeat(
            (candidate.count { it == '(' } - candidate.count { it == ')' }).coerceAtLeast(0),
        )
        AmountCalculator.evaluate(balanced)?.let { return it }
    }
    return null
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
