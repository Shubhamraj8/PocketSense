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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketsense.data.Category
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.Txn
import app.pocketsense.data.currentCycle
import app.pocketsense.ui.emojiForIconKey
import app.pocketsense.data.formatRupees
import app.pocketsense.data.previousCycle
import app.pocketsense.ui.parseColorOrFallback
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@Composable
fun InsightsScreen(
    repo: PocketRepository,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val cycle = remember(today) { currentCycle(today) }
    val prev = remember(today) { previousCycle(today) }

    val sevenDayWindow = remember(today) {
        today.minusDays(6).atStartOfDay(zone).toInstant()
    }
    val txnsLast7d by repo.observeExpensesSince(sevenDayWindow).collectAsState(initial = emptyList())
    val daily = remember(txnsLast7d) { bucketByDay(txnsLast7d, 7, zone) }

    val categories by repo.observeCategories().collectAsState(initial = emptyList())
    val txnsThisCycle by repo.observeExpensesSince(cycle.startInstant(zone))
        .collectAsState(initial = emptyList())
    val txnsPrevCycle by repo.observeExpensesSince(prev.startInstant(zone))
        .collectAsState(initial = emptyList())

    val cycleEnd = cycle.endInstant(zone)
    val prevEnd = prev.endInstant(zone)
    val thisCycleByCat = remember(txnsThisCycle) {
        txnsThisCycle
            .filter { it.occurredAt < cycleEnd }
            .groupBy { it.categoryId }
            .mapValues { (_, ts) -> ts.sumOf { -it.amountPaise } }
    }
    val thisCycleTotal = thisCycleByCat.values.sum()
    val prevCycleTotal = txnsPrevCycle
        .filter { it.occurredAt < prevEnd }
        .sumOf { -it.amountPaise }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Total ${formatRupees(daily.sum())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WeeklyBarChart(
                    amounts = daily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        }
        item { MomCard(thisCycle = thisCycleTotal, lastCycle = prevCycleTotal) }
        item {
            Text("This cycle by category", style = MaterialTheme.typography.titleMedium)
        }
        if (thisCycleTotal == 0L) {
            item {
                Text(
                    "No spending this cycle yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(categories.filter { (thisCycleByCat[it.id] ?: 0L) > 0L }, key = { it.id }) { cat ->
            CategoryBreakdownRow(
                cat = cat,
                spent = thisCycleByCat[cat.id] ?: 0L,
                total = thisCycleTotal.coerceAtLeast(1L),
            )
        }
    }
}

private fun bucketByDay(txns: List<Txn>, days: Int, zone: ZoneId): List<Long> {
    val today = LocalDate.now(zone)
    val buckets = LongArray(days)
    for (txn in txns) {
        val date = txn.occurredAt.atZone(zone).toLocalDate()
        val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
        if (daysAgo in 0 until days) {
            buckets[days - 1 - daysAgo] += -txn.amountPaise
        }
    }
    return buckets.toList()
}

@Composable
private fun MomCard(thisCycle: Long, lastCycle: Long) {
    val diff = thisCycle - lastCycle
    val pctChange = if (lastCycle > 0) abs(diff).toFloat() / lastCycle * 100 else 0f
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("This cycle vs last", style = MaterialTheme.typography.titleMedium)
            Text(
                "This cycle: ${formatRupees(thisCycle)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Last cycle: ${formatRupees(lastCycle)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lastCycle > 0) {
                val verb = if (diff >= 0) "more" else "less"
                Text(
                    "${formatRupees(abs(diff))} $verb than last cycle (${"%.0f".format(pctChange)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (diff > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(cat: Category, spent: Long, total: Long) {
    val pct = if (total > 0) spent.toFloat() / total else 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val color = parseColorOrFallback(cat.colorHex, MaterialTheme.colorScheme.surfaceVariant)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(emojiForIconKey(cat.iconKey))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            formatRupees(spent),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
