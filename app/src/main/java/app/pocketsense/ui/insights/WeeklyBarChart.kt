package app.pocketsense.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeeklyBarChart(
    amounts: List<Long>,
    modifier: Modifier = Modifier,
) {
    if (amounts.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val textMeasurer = rememberTextMeasurer()

    val today = LocalDate.now()
    val labels = remember(amounts.size) {
        val fmt = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        (0 until amounts.size).map { i ->
            today.minusDays((amounts.size - 1 - i).toLong()).format(fmt).uppercase()
        }
    }
    val maxAmount = amounts.max().coerceAtLeast(1L)

    Canvas(modifier) {
        val labelHeight = 18.dp.toPx()
        val gap = 6.dp.toPx()
        val barAreaHeight = size.height - labelHeight - gap
        val n = amounts.size
        val totalGap = gap * (n - 1)
        val barWidth = ((size.width - totalGap) / n).coerceAtLeast(1f)
        val cornerR = CornerRadius(4.dp.toPx())

        amounts.forEachIndexed { i, value ->
            val left = i * (barWidth + gap)
            val pct = value.toFloat() / maxAmount
            val barHeight = (barAreaHeight * pct).coerceAtLeast(2f)
            val color = if (value > 0) barColor else emptyBarColor
            drawRoundRect(
                color = color,
                topLeft = Offset(left, barAreaHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerR,
            )
            val label = labels[i]
            val textLayout = textMeasurer.measure(label, style = labelStyle)
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    left + (barWidth - textLayout.size.width) / 2f,
                    barAreaHeight + gap,
                ),
            )
        }
    }
}
