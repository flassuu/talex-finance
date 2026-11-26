package io.github.flassuu.talex.ui.component

import android.view.WindowManager
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.flassuu.talex.R
import io.github.flassuu.talex.domain.money.AmountCalculator
import io.github.flassuu.talex.domain.money.Calculation
import java.math.BigDecimal
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
        val view = LocalView.current
        SideEffect {
            // Full-width, undimmed window so the sheet is centred and the form
            // behind it stays visible.
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(0f)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalPadding = 16.dp
            val gap = 8.dp
            val buttonWidth = (maxWidth - horizontalPadding * 2 - gap * 3) / 4
            val compactKey = 52.dp
            val expandedKey = buttonWidth
            val density = LocalDensity.current

            val keyHeight = remember { Animatable(0.dp, Dp.VectorConverter) }
            val dismissOffset = remember { Animatable(0f) }
            var panelHeightPx by remember { mutableStateOf(0f) }
            val scope = rememberCoroutineScope()

            val snapSpec = spring<Dp>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )

            LaunchedEffect(Unit) {
                keyHeight.animateTo(compactKey, snapSpec)
            }

            fun close() {
                scope.launch {
                    dismissOffset.animateTo(panelHeightPx + 80f, tween(220))
                    onDismiss()
                }
            }

            fun settle() {
                scope.launch {
                    val closeThreshold = with(density) { 90.dp.toPx() }
                    if (dismissOffset.value > closeThreshold) {
                        close()
                        return@launch
                    }
                    dismissOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    val mid = (compactKey + expandedKey) / 2
                    keyHeight.animateTo(
                        if (keyHeight.value >= mid) expandedKey else compactKey,
                        snapSpec,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { close() },
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset { IntOffset(0, dismissOffset.value.roundToInt()) }
                    .onSizeChanged { panelHeightPx = it.height.toFloat() },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        // Positive dragAmount is downwards.
                                        if (dragAmount < 0f || keyHeight.value > compactKey + 0.5.dp) {
                                            val delta = (-dragAmount).toDp()
                                            val target = (keyHeight.value + delta)
                                                .coerceIn(compactKey, expandedKey)
                                            scope.launch { keyHeight.snapTo(target) }
                                        } else {
                                            scope.launch {
                                                dismissOffset.snapTo(
                                                    (dismissOffset.value + dragAmount)
                                                        .coerceAtLeast(0f),
                                                )
                                            }
                                        }
                                    },
                                    onDragEnd = { settle() },
                                    onDragCancel = { settle() },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = RoundedCornerShape(2.dp),
                                ),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding),
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
                    Spacer(Modifier.height(6.dp))

                    CalcRow(gap) {
                        CalcKey("AC", buttonWidth, keyHeight.value, accent = true) {
                            press { expression = ""; caret = 0 }
                        }
                        CalcKey("( )", buttonWidth, keyHeight.value) { toggleParentheses() }
                        CalcKey("%", buttonWidth, keyHeight.value) { insert("%") }
                        CalcKey("÷", buttonWidth, keyHeight.value) { insert("/") }
                    }
                    CalcRow(gap) {
                        CalcKey("7", buttonWidth, keyHeight.value) { insert("7") }
                        CalcKey("8", buttonWidth, keyHeight.value) { insert("8") }
                        CalcKey("9", buttonWidth, keyHeight.value) { insert("9") }
                        CalcKey("×", buttonWidth, keyHeight.value) { insert("*") }
                    }
                    CalcRow(gap) {
                        CalcKey("4", buttonWidth, keyHeight.value) { insert("4") }
                        CalcKey("5", buttonWidth, keyHeight.value) { insert("5") }
                        CalcKey("6", buttonWidth, keyHeight.value) { insert("6") }
                        CalcKey("−", buttonWidth, keyHeight.value) { insert("-") }
                    }
                    CalcRow(gap) {
                        CalcKey("1", buttonWidth, keyHeight.value) { insert("1") }
                        CalcKey("2", buttonWidth, keyHeight.value) { insert("2") }
                        CalcKey("3", buttonWidth, keyHeight.value) { insert("3") }
                        CalcKey("+", buttonWidth, keyHeight.value) { insert("+") }
                    }
                    CalcRow(gap) {
                        CalcKey(",", buttonWidth, keyHeight.value) { insert(",") }
                        CalcKey("0", buttonWidth, keyHeight.value) { insert("0") }
                        CalcKey("⌫", buttonWidth, keyHeight.value) { backspace() }
                        ConfirmKey(isComplete, buttonWidth, keyHeight.value) { confirmOrEvaluate() }
                    }
                }
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
            text = displayExpression(expression),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            minLines = 1,
            textAlign = TextAlign.End,
            onTextLayout = { layoutResult = it },
        )
    }
}

@Composable
private fun CalcRow(gap: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = gap / 2),
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
        content = content,
    )
}

@Composable
private fun RowScope.ConfirmKey(
    isComplete: Boolean,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(width).height(height),
        shape = RoundedCornerShape(percent = 50),
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
private fun RowScope.CalcKey(
    label: String,
    width: Dp,
    height: Dp,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    if (accent) {
        Button(
            onClick = onClick,
            modifier = Modifier.width(width).height(height),
            shape = shape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.width(width).height(height),
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
