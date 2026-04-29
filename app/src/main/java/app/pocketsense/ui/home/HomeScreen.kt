package app.pocketsense.ui.home

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.pocketsense.data.Category
import app.pocketsense.data.DarkModePref
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.Source
import app.pocketsense.data.Txn
import app.pocketsense.data.currentCycle
import app.pocketsense.data.effectivePaise
import app.pocketsense.data.formatRupees
import app.pocketsense.data.parseRupeesToPaise
import app.pocketsense.data.safeToSpendTodayPaise
import app.pocketsense.service.ExpensePromptService
import app.pocketsense.service.PaymentApps
import app.pocketsense.ui.EditTxnSheet
import app.pocketsense.ui.emojiForIconKey
import app.pocketsense.ui.parseColorOrFallback
import app.pocketsense.ui.theme.PocketSenseTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    repo: PocketRepository,
    userDisplayName: String,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember(context) { app.pocketsense.data.Preferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val displayName = userDisplayName.trim().ifBlank { "there" }
    val wallet by repo.observeWallet().collectAsState(initial = null)
    val today = LocalDate.now()
    val cycle = remember(today, wallet?.cycleStartDay) {
        currentCycle(today, wallet?.cycleStartDay ?: 1)
    }

    val balance by repo.observeWalletBalance().collectAsState(initial = 0L)
    val txns by repo.observeRecentTxns(8).collectAsState(initial = emptyList())
    val categories by repo.observeCategories().collectAsState(initial = emptyList())
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val allocations by repo.observeAllocationsForCycle(cycle.start).collectAsState(initial = emptyList())
    val cycleExpenses by repo.observeExpensesSince(cycle.startInstant()).collectAsState(initial = emptyList())

    val cycleEnd = cycle.endInstant()
    val spentByCat = remember(cycleExpenses) {
        cycleExpenses
            .filter { it.occurredAt < cycleEnd }
            .groupBy { it.categoryId }
            .mapValues { (_, ts) -> ts.sumOf { -it.amountPaise } }
    }
    val totalAllocated = allocations.sumOf { it.effectivePaise() }
    val totalSpentInCycle = cycleExpenses
        .filter { it.occurredAt < cycleEnd }
        .sumOf { -it.amountPaise }
    val daysRemaining = cycle.daysRemaining(today)
    val safeToSpend = if (totalAllocated > 0) {
        safeToSpendTodayPaise(totalAllocated - totalSpentInCycle, daysRemaining)
    } else {
        safeToSpendTodayPaise(balance, daysRemaining)
    }

    val overBudgetCats = remember(allocations, spentByCat, categoriesById) {
        allocations.mapNotNull { alloc ->
            val cat = categoriesById[alloc.categoryId] ?: return@mapNotNull null
            val effective = alloc.effectivePaise()
            val spent = spentByCat[alloc.categoryId] ?: 0L
            val pct = if (effective > 0) spent.toFloat() / effective else 0f
            if (pct >= 0.8f) AlertItem(cat.id, cat.name, pct, spent, effective) else null
        }
    }

    var hasUsageAccess by remember { mutableStateOf(checkUsageAccess(context)) }
    var hasNotifPerm by remember { mutableStateOf(checkNotificationPerm(context)) }
    var serviceRunning by remember { mutableStateOf(prefs.isWatcherEnabled()) }
    var addMoneyOpen by remember { mutableStateOf(false) }
    var editingTxn by remember { mutableStateOf<Txn?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = checkUsageAccess(context)
                hasNotifPerm = checkNotificationPerm(context)
                serviceRunning = prefs.isWatcherEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsageAccess, hasNotifPerm, serviceRunning) {
        if (serviceRunning && hasUsageAccess && hasNotifPerm) {
            ExpensePromptService.start(context)
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifPerm = granted }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "${greetingForNow()}, $displayName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            WalletCard(
                balancePaise = balance,
                safeToSpendPaise = safeToSpend,
                hasBudgets = totalAllocated > 0,
                onAddMoney = { addMoneyOpen = true },
            )
        }
        if (overBudgetCats.isNotEmpty()) {
            item { AlertBanner(items = overBudgetCats, onItemClick = onCategoryClick) }
        }
        if (allocations.isNotEmpty()) {
            item { SectionTitle("Budgets this cycle") }
            items(allocations, key = { it.id }) { alloc ->
                val cat = categoriesById[alloc.categoryId] ?: return@items
                BudgetRow(
                    category = cat,
                    spent = spentByCat[alloc.categoryId] ?: 0L,
                    allocated = alloc.effectivePaise(),
                    onClick = { onCategoryClick(cat.id) },
                )
            }
        }
        item { SectionTitle("Recent activity") }
        if (txns.isEmpty()) {
            item {
                Text(
                    "Nothing yet — tap Add expense or use a payments app while watching is on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(txns, key = { it.id }) { txn ->
            TxnRow(
                txn = txn,
                category = categoriesById[txn.categoryId],
                onLongClick = { editingTxn = txn },
            )
        }
        item {
            WatcherCard(
                hasUsageAccess = hasUsageAccess,
                hasNotifPerm = hasNotifPerm,
                serviceRunning = serviceRunning,
                onOpenUsageSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onRequestNotifPerm = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onToggleService = {
                    if (serviceRunning) {
                        ExpensePromptService.stop(context)
                        prefs.setWatcherEnabled(false)
                        serviceRunning = false
                    } else {
                        ExpensePromptService.start(context)
                        prefs.setWatcherEnabled(true)
                        serviceRunning = true
                    }
                },
            )
        }
    }

    if (addMoneyOpen) {
        AddMoneyDialog(
            onDismiss = { addMoneyOpen = false },
            onConfirm = { paise, note ->
                scope.launch {
                    repo.addIncome(paise, note)
                    addMoneyOpen = false
                }
            },
        )
    }

    editingTxn?.let { txn ->
        EditTxnSheet(
            txn = txn,
            repo = repo,
            onDismiss = { editingTxn = null },
        )
    }
}

private fun greetingForNow(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private data class AlertItem(
    val categoryId: Long,
    val label: String,
    val pct: Float,
    val spent: Long,
    val allocated: Long,
)

@Composable
private fun AlertBanner(items: List<AlertItem>, onItemClick: (Long) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (items.size == 1) "1 category needs attention" else "${items.size} categories need attention",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold,
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item.categoryId) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.label,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        "${(item.pct * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetRow(category: Category, spent: Long, allocated: Long, onClick: () -> Unit) {
    val pct = (spent.toFloat() / allocated.coerceAtLeast(1L)).coerceAtLeast(0f)
    val pctClamped = pct.coerceAtMost(1f)
    val barColor = when {
        pct >= 1f -> MaterialTheme.colorScheme.error
        pct >= 0.8f -> Color(0xFFFFA726)
        else -> parseColorOrFallback(category.colorHex, MaterialTheme.colorScheme.primary)
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(parseColorOrFallback(category.colorHex, MaterialTheme.colorScheme.surfaceVariant)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emojiForIconKey(category.iconKey))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pctClamped },
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${formatRupees(spent)} / ${formatRupees(allocated)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WalletCard(
    balancePaise: Long,
    safeToSpendPaise: Long,
    hasBudgets: Boolean,
    onAddMoney: () -> Unit,
) {
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
            Text("Wallet balance", style = MaterialTheme.typography.labelMedium)
            Text(
                formatRupees(balancePaise),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Safe to spend today: ${formatRupees(safeToSpendPaise)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "(Remaining amount ÷ days left in cycle)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!hasBudgets) {
                Text(
                    "Set per-category budgets in Categories for tighter control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            TextButton(onClick = onAddMoney) { Text("Add money") }
        }
    }
}

@Composable
private fun WatcherCard(
    hasUsageAccess: Boolean,
    hasNotifPerm: Boolean,
    serviceRunning: Boolean,
    onOpenUsageSettings: () -> Unit,
    onRequestNotifPerm: () -> Unit,
    onToggleService: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Auto-prompt watcher", style = MaterialTheme.typography.titleMedium)
            PermRow(
                label = "Usage access",
                granted = hasUsageAccess,
                onGrant = onOpenUsageSettings,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermRow(
                    label = "Notifications",
                    granted = hasNotifPerm,
                    onGrant = onRequestNotifPerm,
                )
            }
            Button(
                onClick = onToggleService,
                enabled = hasUsageAccess && hasNotifPerm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (serviceRunning) "Stop watching" else "Start watching")
            }
        }
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (granted) Text("Granted", style = MaterialTheme.typography.labelMedium)
        else TextButton(onClick = onGrant) { Text("Grant") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxnRow(
    txn: Txn,
    category: Category?,
    onLongClick: () -> Unit,
) {
    val isIncome = txn.amountPaise > 0
    val amountColor =
        if (isIncome) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.onSurface

    val tint = parseColorOrFallback(category?.colorHex, MaterialTheme.colorScheme.surfaceVariant)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick,
            ),
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
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Text(emojiForIconKey(category?.iconKey))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category?.name ?: when (txn.source) {
                        Source.INCOME -> "Income"
                        Source.ADJUSTMENT -> "Adjustment"
                        else -> "Uncategorized"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                val sub = buildString {
                    append(formatTime(txn.occurredAt))
                    txn.triggerAppPackage?.let { append(" · ${PaymentApps.label(it)}") }
                    if (txn.splitCount > 1) append(" · split ${txn.splitCount} ways")
                    if (!txn.note.isNullOrBlank()) append(" · ${txn.note}")
                }
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = (if (isIncome) "+" else "") + formatRupees(txn.amountPaise),
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun AddMoneyDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val paise = parseRupeesToPaise(amountText)
    val canConfirm = paise != null && paise > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add money") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { c -> c.isDigit() || c == '.' }
                    },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. April salary)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { paise?.let { onConfirm(it, note.takeIf { n -> n.isNotBlank() }) } },
                enabled = canConfirm,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault())

private fun formatTime(instant: Instant): String = timeFormatter.format(instant)

private fun checkUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkNotificationPerm(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

@Preview(name = "Home UI Components", showBackground = true)
@Composable
private fun HomeScreenComponentsPreview() {
    val sampleCategory = Category(
        id = 1L,
        name = "Food",
        iconKey = "restaurant",
        colorHex = "#FF7043",
        isDefault = true,
        sortOrder = 0,
    )
    PocketSenseTheme(darkMode = DarkModePref.LIGHT, dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletCard(
                balancePaise = 1543200L,
                safeToSpendPaise = 28500L,
                hasBudgets = true,
                onAddMoney = {},
            )
            SectionTitle("Budgets this cycle")
            BudgetRow(
                category = sampleCategory,
                spent = 64000L,
                allocated = 90000L,
                onClick = {},
            )
            SectionTitle("Recent activity")
            TxnRow(
                txn = Txn(
                    id = 1L,
                    amountPaise = -34900L,
                    categoryId = 1L,
                    occurredAt = Instant.now(),
                    source = Source.MANUAL,
                    note = "Dinner with friends",
                ),
                category = sampleCategory,
                onLongClick = {},
            )
        }
    }
}
