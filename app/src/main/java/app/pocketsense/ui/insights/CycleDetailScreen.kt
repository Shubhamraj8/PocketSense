package app.pocketsense.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketsense.data.Category
import app.pocketsense.data.Cycle
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.cycleForOffset
import app.pocketsense.data.formatRupees
import app.pocketsense.ui.emojiForIconKey
import app.pocketsense.ui.parseColorOrFallback
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Browsable, category-wise breakdown of a single budget cycle. Opens on the
 * previous cycle and lets the user step back through older cycles (or forward to
 * the current one). Answers "how much did I spend on each category last cycle?".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDetailScreen(
    repo: PocketRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val wallet by repo.observeWallet().collectAsState(initial = null)
    val cycleStartDay = wallet?.cycleStartDay ?: 1

    // offset 1 = previous cycle (the default the user asked for); 0 = current.
    var offset by remember { mutableStateOf(1) }

    val cycle = remember(offset, cycleStartDay, today) {
        cycleForOffset(offset, today, cycleStartDay)
    }
    val cycleBefore = remember(offset, cycleStartDay, today) {
        cycleForOffset(offset + 1, today, cycleStartDay)
    }

    val categories by repo.observeAllCategories().collectAsState(initial = emptyList())
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    val expenses by repo
        .observeExpensesInWindow(cycle.startInstant(zone), cycle.endInstant(zone))
        .collectAsState(initial = emptyList())
    val expensesBefore by repo
        .observeExpensesInWindow(cycleBefore.startInstant(zone), cycleBefore.endInstant(zone))
        .collectAsState(initial = emptyList())

    val byCategory = remember(expenses) {
        expenses
            .groupBy { it.categoryId }
            .map { (categoryId, txns) ->
                CycleCategorySpend(categoryId, txns.sumOf { -it.amountPaise }, txns.size)
            }
            .sortedByDescending { it.spentPaise }
    }
    val total = remember(byCategory) { byCategory.sumOf { it.spentPaise } }
    val totalBefore = remember(expensesBefore) { expensesBefore.sumOf { -it.amountPaise } }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Cycle details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CycleNavigator(
                    rangeLabel = cycleRangeLabel(cycle),
                    relativeLabel = relativeCycleLabel(offset),
                    canGoNewer = offset > 0,
                    onOlder = { offset += 1 },
                    onNewer = { if (offset > 0) offset -= 1 },
                )
            }
            item { CycleSummaryCard(total = total, totalBefore = totalBefore) }
            item { Text("By category", style = MaterialTheme.typography.titleMedium) }
            if (byCategory.isEmpty()) {
                item {
                    Text(
                        "No spending recorded in this cycle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(byCategory, key = { it.categoryId ?: Long.MIN_VALUE }) { row ->
                CycleCategoryRow(
                    category = row.categoryId?.let { categoriesById[it] },
                    spent = row.spentPaise,
                    txnCount = row.txnCount,
                    total = total.coerceAtLeast(1L),
                )
            }
        }
    }
}

private data class CycleCategorySpend(
    val categoryId: Long?,
    val spentPaise: Long,
    val txnCount: Int,
)

@Composable
private fun CycleNavigator(
    rangeLabel: String,
    relativeLabel: String,
    canGoNewer: Boolean,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOlder) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Older cycle")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                rangeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                relativeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNewer, enabled = canGoNewer) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Newer cycle")
        }
    }
}

@Composable
private fun CycleSummaryCard(total: Long, totalBefore: Long) {
    val diff = total - totalBefore
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Total spent this cycle", style = MaterialTheme.typography.labelMedium)
            Text(
                formatRupees(total),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            if (totalBefore > 0) {
                val pctChange = abs(diff).toFloat() / totalBefore * 100
                val verb = if (diff >= 0) "more" else "less"
                Text(
                    "${formatRupees(abs(diff))} $verb than the cycle before (${"%.0f".format(pctChange)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (diff > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary,
                )
            } else {
                Text(
                    "No spending in the cycle before to compare.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun CycleCategoryRow(
    category: Category?,
    spent: Long,
    txnCount: Int,
    total: Long,
) {
    val pct = if (total > 0) spent.toFloat() / total else 0f
    val color = parseColorOrFallback(category?.colorHex, MaterialTheme.colorScheme.surfaceVariant)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(emojiForIconKey(category?.iconKey))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        category?.name ?: "Uncategorized",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatRupees(spent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pct.coerceIn(0f, 1f) },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(pct * 100).roundToInt()}% of cycle · $txnCount ${if (txnCount == 1) "transaction" else "transactions"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val cycleStartFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val cycleEndFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun cycleRangeLabel(cycle: Cycle): String {
    val lastDay = cycle.endExclusive.minusDays(1)
    return "${cycle.start.format(cycleStartFmt)} – ${lastDay.format(cycleEndFmt)}"
}

private fun relativeCycleLabel(offset: Int): String = when (offset) {
    0 -> "Current cycle"
    1 -> "Previous cycle"
    else -> "$offset cycles ago"
}
