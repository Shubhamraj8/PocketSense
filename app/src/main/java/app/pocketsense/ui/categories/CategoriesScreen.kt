package app.pocketsense.ui.categories

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pocketsense.data.Allocation
import app.pocketsense.data.Category
import app.pocketsense.data.Cycle
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.currentCycle
import app.pocketsense.ui.emojiForIconKey
import app.pocketsense.data.formatRupees
import app.pocketsense.ui.parseColorOrFallback
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CategoriesScreen(
    repo: PocketRepository,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val cycle = remember(today) { currentCycle(today) }
    val categories by repo.observeCategories().collectAsState(initial = emptyList())
    val allocations by repo.observeAllocationsForCycle(cycle.start).collectAsState(initial = emptyList())
    val allocByCat = remember(allocations) { allocations.associateBy { it.categoryId } }
    val scope = rememberCoroutineScope()

    var setBudgetFor by remember { mutableStateOf<Category?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var creatingCategory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(categories, key = { it.id }) { cat ->
            CategoryCard(
                category = cat,
                allocation = allocByCat[cat.id],
                cycle = cycle,
                repo = repo,
                onTap = { onCategoryClick(cat.id) },
                onSetBudget = { setBudgetFor = cat },
                onEdit = { editingCategory = cat },
            )
        }
        item {
            OutlinedButton(
                onClick = { creatingCategory = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("New category")
            }
        }
    }

    setBudgetFor?.let { cat ->
        SetBudgetSheet(
            category = cat,
            existing = allocByCat[cat.id],
            onSave = { paise, rollover ->
                scope.launch {
                    repo.upsertAllocation(cat.id, cycle.start, paise, rollover)
                    setBudgetFor = null
                }
            },
            onClear = {
                scope.launch {
                    repo.deleteAllocation(cat.id, cycle.start)
                    setBudgetFor = null
                }
            },
            onDismiss = { setBudgetFor = null },
        )
    }

    editingCategory?.let { cat ->
        EditCategorySheet(
            initial = cat,
            onSave = { updated ->
                scope.launch {
                    repo.upsertCategory(updated)
                    editingCategory = null
                }
            },
            onArchive = {
                scope.launch {
                    repo.archiveCategory(cat.id)
                    editingCategory = null
                }
            },
            onDismiss = { editingCategory = null },
        )
    }

    if (creatingCategory) {
        EditCategorySheet(
            initial = null,
            onSave = { newCat ->
                scope.launch {
                    repo.upsertCategory(newCat)
                    creatingCategory = false
                }
            },
            onArchive = null,
            onDismiss = { creatingCategory = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
    category: Category,
    allocation: Allocation?,
    cycle: Cycle,
    repo: PocketRepository,
    onTap: () -> Unit,
    onSetBudget: () -> Unit,
    onEdit: () -> Unit,
) {
    val spent by repo.observeSpentInWindow(category.id, cycle.startInstant(), cycle.endInstant())
        .collectAsState(initial = 0L)
    val pct = allocation?.amountPaise?.takeIf { it > 0 }?.let { spent.toFloat() / it } ?: 0f
    val pctClamped = pct.coerceIn(0f, 1f)

    val barColor = when {
        allocation == null -> MaterialTheme.colorScheme.surfaceVariant
        pct >= 1f -> MaterialTheme.colorScheme.error
        pct >= 0.8f -> Color(0xFFFFA726)
        else -> parseColorOrFallback(category.colorHex, MaterialTheme.colorScheme.primary)
    }

    ElevatedCard(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseColorOrFallback(category.colorHex, MaterialTheme.colorScheme.surfaceVariant)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emojiForIconKey(category.iconKey))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (allocation != null)
                            "${formatRupees(spent)} of ${formatRupees(allocation.amountPaise)}"
                        else
                            "${formatRupees(spent)} spent · No budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Edit category")
                }
            }
            if (allocation != null) {
                LinearProgressIndicator(
                    progress = { pctClamped },
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
            TextButton(onClick = onSetBudget) {
                Text(if (allocation != null) "Change budget" else "Set budget")
            }
        }
    }
}
