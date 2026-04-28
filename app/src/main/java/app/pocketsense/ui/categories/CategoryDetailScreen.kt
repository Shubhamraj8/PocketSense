package app.pocketsense.ui.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.Source
import app.pocketsense.data.Txn
import app.pocketsense.data.currentCycle
import app.pocketsense.data.effectivePaise
import app.pocketsense.data.formatRupees
import app.pocketsense.service.PaymentApps
import app.pocketsense.ui.EditTxnSheet
import app.pocketsense.ui.emojiForIconKey
import app.pocketsense.ui.parseColorOrFallback
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Long,
    repo: PocketRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category by repo.observeCategoryById(categoryId).collectAsState(initial = null)
    val txns by repo.observeRecentTxnsByCategory(categoryId, 200).collectAsState(initial = emptyList())
    val wallet by repo.observeWallet().collectAsState(initial = null)
    val cycle = remember(wallet?.cycleStartDay) {
        currentCycle(cycleStartDay = wallet?.cycleStartDay ?: 1)
    }
    val spent by repo.observeSpentInWindow(categoryId, cycle.startInstant(), cycle.endInstant())
        .collectAsState(initial = 0L)
    val allocations by repo.observeAllocationsForCycle(cycle.start).collectAsState(initial = emptyList())
    val allocation = allocations.find { it.categoryId == categoryId }

    var editingTxn by remember { mutableStateOf<Txn?>(null) }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(category?.name ?: "Category") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CategoryStatCard(
                    spent = spent,
                    allocated = allocation?.effectivePaise(),
                    daysRemaining = cycle.daysRemaining(),
                )
            }
            item {
                Text("Transactions", style = MaterialTheme.typography.titleMedium)
            }
            if (txns.isEmpty()) {
                item {
                    Text(
                        "No transactions in this category yet. Long-press a row in Recent activity to move it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(txns, key = { it.id }) { txn ->
                TxnDetailRow(
                    txn = txn,
                    categoryName = category?.name ?: "",
                    categoryColor = parseColorOrFallback(category?.colorHex, MaterialTheme.colorScheme.surfaceVariant),
                    iconKey = category?.iconKey,
                    onLongClick = { editingTxn = txn },
                )
            }
        }
    }

    editingTxn?.let { txn ->
        EditTxnSheet(
            txn = txn,
            repo = repo,
            onDismiss = { editingTxn = null },
        )
    }
}

@Composable
private fun CategoryStatCard(spent: Long, allocated: Long?, daysRemaining: Int) {
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
            Text("Spent this cycle", style = MaterialTheme.typography.labelMedium)
            Text(
                formatRupees(spent),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            if (allocated != null) {
                val remaining = (allocated - spent).coerceAtLeast(0)
                Text(
                    "${formatRupees(remaining)} left of ${formatRupees(allocated)} · $daysRemaining days remaining",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val pct = (spent.toFloat() / allocated.coerceAtLeast(1L)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { pct },
                    color = when {
                        pct >= 1f -> MaterialTheme.colorScheme.error
                        pct >= 0.8f -> Color(0xFFFFA726)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            } else {
                Text("No budget set for this cycle", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val txnTimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault())

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxnDetailRow(
    txn: Txn,
    categoryName: String,
    categoryColor: Color,
    iconKey: String?,
    onLongClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(emojiForIconKey(iconKey))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val title = when (txn.source) {
                Source.PROMPT -> txn.triggerAppPackage?.let { PaymentApps.label(it) } ?: categoryName
                Source.MANUAL -> categoryName
                Source.INCOME -> "Income"
                Source.ADJUSTMENT -> "Adjustment"
            }
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append(txnTimeFmt.format(txn.occurredAt))
                    if (txn.splitCount > 1) append(" · split ${txn.splitCount} ways")
                    if (!txn.note.isNullOrBlank()) append(" · ${txn.note}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = (if (txn.amountPaise > 0) "+" else "") + formatRupees(txn.amountPaise),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
