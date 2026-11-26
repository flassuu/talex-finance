package io.github.flassuu.talex.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.flassuu.talex.R
import io.github.flassuu.talex.domain.money.AmountCalculator
import io.github.flassuu.talex.domain.money.Calculation
import java.math.BigDecimal
import kotlinx.coroutines.launch

/** A plain number: optionally signed, with a decimal separator. */
private val PLAIN_NUMBER = Regex("-?\\d+([.,]\\d*)?")

private val SHEET_COMPACT = 360.dp
private val SHEET_EXPANDED = 540.dp

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

@Composable
fun CalculatorSheet(
    initial: String,
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
) {
    val initialExpression = initial.filter { it.isDigit() || it in ".,+-*/()%" }
    var expression by remember { mutableStateOf(initialExpression) }
    var caret by remember { mutableStateOf(initialExpression.length) }
    var failed by remember { mutableStateOf(false) }

    val calculation = AmountCalculator.analyze(expression)
    val result = (calculation as? Calculation.Value)?.value
    val isComplete = expression.isBlank() || (result != null && PLAIN_NUMBER.matches(expression))

    val scope = rememberCoroutineScope()
    val sheetHeight = remember { Animatable(0.dp, Dp.VectorConverter) }

    LaunchedEffect(Unit) {
        sheetHeight.animateTo(
            SHEET_COMPACT,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        )
    }

    fun snapTo(target: Dp) {
        scope.launch {
            sheetHeight.animateTo(
                target,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Transparent scrim: tap outside dismisses without dimming the screen.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() },
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeight.value)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val target = (sheetHeight.value - dragAmount.dp)
                                    .coerceIn(SHEET_COMPACT, SHEET_EXPANDED)
                                scope.launch { sheetHeight.snapTo(target) }
                            },
                            onDragEnd = {
                                snapTo(nearestSnap(sheetHeight.value))
                            },
                            onDragCancel = {
                                snapTo(nearestSnap(sheetHeight.value))
                            },
                        )
                    },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(2.dp),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        AnimatedContent(
                            targetState = expression,
                            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(90)) },
                            label = "expression",
                        ) { text ->
                            ExpressionField(
                                expression = text,
                                caret = caret.coerceIn(0, text.length),
                                onTap = { caret = it },
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        AnimatedContent(
                            targetState = when {
                                failed -> stringResource(R.string.calculator_error)
                                !isComplete && result != null -> "= " + formatResult(result)
                                else -> " "
                            },
                            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(80)) },
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
                    Spacer(Modifier.height(8.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val gap = 8.dp
                        val horizontalPadding = 20.dp
                        val reserved = 156.dp
                        val horizontalKey = (maxWidth - horizontalPadding * 2 - gap * 3) / 4
                        val verticalKey = (sheetHeight.value - reserved) / 5
                        val keySize = minOf(horizontalKey, verticalKey).coerceIn(42.dp, 92.dp)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CalcRow(keySize, gap) {
                                CalcKey("AC", keySize, accent = true) { press { expression = ""; caret = 0 } }
                                CalcKey("( )", keySize) { toggleParentheses() }
                                CalcKey("%", keySize) { insert("%") }
                                CalcKey("÷", keySize) { insert("/") }
                            }
                            CalcRow(keySize, gap) {
                                CalcKey("7", keySize) { insert("7") }
                                CalcKey("8", keySize) { insert("8") }
                                CalcKey("9", keySize) { insert("9") }
                                CalcKey("×", keySize) { insert("*") }
                            }
                            CalcRow(keySize, gap) {
                                CalcKey("4", keySize) { insert("4") }
                                CalcKey("5", keySize) { insert("5") }
                                CalcKey("6", keySize) { insert("6") }
                                CalcKey("−", keySize) { insert("-") }
                            }
                            CalcRow(keySize, gap) {
                                CalcKey("1", keySize) { insert("1") }
                                CalcKey("2", keySize) { insert("2") }
                                CalcKey("3", keySize) { insert("3") }
                                CalcKey("+", keySize) { insert("+") }
                            }
                            CalcRow(keySize, gap) {
                                CalcKey(",", keySize) { insert(",") }
                                CalcKey("0", keySize) { insert("0") }
                                CalcKey("⌫", keySize) { backspace() }
                                ConfirmKey(isComplete, keySize) { confirmOrEvaluate() }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun nearestSnap(value: Dp): Dp {
    val mid = (SHEET_COMPACT + SHEET_EXPANDED) / 2
    return if (value >= mid) SHEET_EXPANDED else SHEET_COMPACT
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
    val cursorColor = MaterialTheme.colorScheme.onSurface

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
            }
            .drawBehind {
                val result = layoutResult ?: return@drawBehind
                val rect = result.getCursorRect(caret)
                val textLeft = (size.width - result.size.width).coerceAtLeast(0f)
                val textTop = (size.height - result.size.height) / 2f
                drawLine(
                    color = cursorColor,
                    start = Offset(textLeft + rect.left, textTop + rect.top),
                    end = Offset(textLeft + rect.left, textTop + rect.bottom),
                    strokeWidth = 2.dp.toPx(),
                    alpha = cursorAlpha,
                )
            },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = displayExpression(expression).ifEmpty { "0" },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.End,
            onTextLayout = { layoutResult = it },
        )
    }
}

@Composable
private fun CalcRow(keySize: Dp, gap: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = gap / 2),
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
        content = content,
    )
}

@Composable
private fun RowScope.ConfirmKey(isComplete: Boolean, size: Dp, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        AnimatedContent(
            targetState = isComplete,
            transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(80)) },
            label = "confirm",
        ) { complete ->
            if (complete) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
            } else {
                Text(text = "=", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun RowScope.CalcKey(label: String, size: Dp, accent: Boolean = false, onClick: () -> Unit) {
    if (accent) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(size),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.size(size),
            shape = CircleShape,
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
