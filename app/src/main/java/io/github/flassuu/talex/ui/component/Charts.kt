package io.github.flassuu.talex.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs

/**
 * Lightweight charts drawn with Compose Canvas. They deliberately avoid a chart
 * dependency and cover the four analytics views: income vs expenses, cash flow,
 * category shares and the net-worth trend.
 */

/** Two bars per bucket, for example income and expenses. Both values are positive. */
@Composable
fun GroupedBarChart(
    values: List<Pair<Long, Long>>,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val firstColor = MaterialTheme.colorScheme.primary
    val secondColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val max = values.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(1L)
        val slot = size.width / values.size
        val barWidth = (slot * 0.34f).coerceAtLeast(1f)
        values.forEachIndexed { index, pair ->
            val slotStart = index * slot
            val firstHeight = (pair.first.toFloat() / max) * size.height
            val secondHeight = (pair.second.toFloat() / max) * size.height
            drawRect(
                color = firstColor,
                topLeft = Offset(slotStart + slot * 0.10f, size.height - firstHeight),
                size = Size(barWidth, firstHeight),
            )
            drawRect(
                color = secondColor,
                topLeft = Offset(slotStart + slot * 0.52f, size.height - secondHeight),
                size = Size(barWidth, secondHeight),
            )
        }
    }
}

/** One signed bar per bucket. Positive bars grow up from the zero line, negative down. */
@Composable
fun SignedBarChart(
    values: List<Long>,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val positiveColor = MaterialTheme.colorScheme.primary
    val negativeColor = MaterialTheme.colorScheme.error
    val zeroLineColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val maxPositive = values.maxOfOrNull { it.coerceAtLeast(0L) } ?: 0L
        val maxNegative = values.maxOfOrNull { (-it).coerceAtLeast(0L) } ?: 0L
        val total = (maxPositive + maxNegative).coerceAtLeast(1L)
        val zeroY = size.height * (maxPositive.toFloat() / total)
        val slot = size.width / values.size
        val barWidth = (slot * 0.6f).coerceAtLeast(1f)
        drawLine(
            color = zeroLineColor,
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 2f,
        )
        values.forEachIndexed { index, value ->
            val left = index * slot + (slot - barWidth) / 2f
            val height = (abs(value).toFloat() / total) * size.height
            if (value >= 0) {
                drawRect(positiveColor, Offset(left, zeroY - height), Size(barWidth, height))
            } else {
                drawRect(negativeColor, Offset(left, zeroY), Size(barWidth, height))
            }
        }
    }
}

/** A smoothed line for a trend such as net worth over time. */
@Composable
fun LineChart(
    values: List<Long>,
    modifier: Modifier = Modifier,
) {
    if (values.size < 2) return
    val color = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(1L)
        val stepX = size.width / (values.size - 1)

        fun yFor(value: Long): Float =
            size.height - ((value - min).toFloat() / range) * size.height

        // A faint baseline marks the minimum so a flat series still reads as a line.
        drawLine(baselineColor, Offset(0f, size.height), Offset(size.width, size.height), 1f)

        val line = Path()
        val area = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = yFor(value)
            if (index == 0) {
                line.moveTo(x, y)
                area.moveTo(x, size.height)
                area.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        area.lineTo(size.width, size.height)
        area.close()
        drawPath(area, color = fill)
        drawPath(line, color = color, style = Stroke(width = 4f))
    }
}

/** A horizontal share bar used by the category breakdown. */
@Composable
fun CategoryShareBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        drawRect(track, size = size)
        drawRect(
            color = fill,
            size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
        )
    }
}
