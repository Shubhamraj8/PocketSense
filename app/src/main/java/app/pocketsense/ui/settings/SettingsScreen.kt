package app.pocketsense.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.pocketsense.data.DarkModePref
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.Txn
import app.pocketsense.data.formatRupees
import app.pocketsense.data.paiseToRupeeText
import app.pocketsense.data.parseRupeesToPaise
import app.pocketsense.service.PaymentApps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    repo: PocketRepository,
    darkMode: DarkModePref,
    onDarkModeChange: (DarkModePref) -> Unit,
    watchedApps: Set<String>,
    onWatchedAppsChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallet by repo.observeWallet().collectAsState(initial = null)
    val snackbarHost = remember { SnackbarHostState() }

    var cycleDay by remember(wallet?.cycleStartDay) {
        mutableStateOf(wallet?.cycleStartDay ?: 1)
    }
    var incomeText by remember(wallet?.monthlyIncomePaise) {
        mutableStateOf(paiseToRupeeText(wallet?.monthlyIncomePaise ?: 0L))
    }
    val incomeParsed = parseRupeesToPaise(incomeText)
    val incomeChanged = incomeParsed != null && incomeParsed != (wallet?.monthlyIncomePaise ?: 0L)
    var customPkgText by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Section("Budget cycle") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Starts on day $cycleDay", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Changing this affects future cycles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = {
                                if (cycleDay > 1) {
                                    cycleDay -= 1
                                    scope.launch {
                                        wallet?.let { repo.updateWallet(it.copy(cycleStartDay = cycleDay)) }
                                    }
                                }
                            },
                            enabled = cycleDay > 1,
                        ) { Icon(Icons.Default.Remove, "Earlier") }
                        IconButton(
                            onClick = {
                                if (cycleDay < 28) {
                                    cycleDay += 1
                                    scope.launch {
                                        wallet?.let { repo.updateWallet(it.copy(cycleStartDay = cycleDay)) }
                                    }
                                }
                            },
                            enabled = cycleDay < 28,
                        ) { Icon(Icons.Default.Add, "Later") }
                    }
                }
            }

            item {
                Section("Monthly income") {
                    OutlinedTextField(
                        value = incomeText,
                        onValueChange = { incomeText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Stored for reference only. To add money to the wallet, use \"Add money\" on Home.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (incomeChanged) {
                        TextButton(
                            onClick = {
                                val paise = incomeParsed ?: return@TextButton
                                scope.launch {
                                    wallet?.let { repo.updateWallet(it.copy(monthlyIncomePaise = paise)) }
                                    snackbarHost.showSnackbar("Saved")
                                }
                            },
                        ) { Text("Save") }
                    }
                }
            }

            item {
                Section("Theme") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkModePref.values().forEach { pref ->
                            FilterChip(
                                selected = darkMode == pref,
                                onClick = { onDarkModeChange(pref) },
                                label = { Text(pref.displayName()) },
                            )
                        }
                    }
                }
            }

            item {
                Section("Watched payment apps") {
                    Text(
                        "PocketSense will prompt only after these apps are used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PaymentApps.knownPackages.sortedBy { PaymentApps.label(it) }.forEach { pkg ->
                            FilterChip(
                                selected = pkg in watchedApps,
                                onClick = {
                                    val updated = watchedApps.toMutableSet()
                                    if (pkg in updated) updated.remove(pkg) else updated.add(pkg)
                                    onWatchedAppsChange(updated)
                                },
                                label = { Text(PaymentApps.label(pkg)) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customPkgText,
                        onValueChange = { customPkgText = it.trim() },
                        label = { Text("Add custom app package") },
                        placeholder = { Text("e.g. com.somebank.upi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val pkg = customPkgText
                                if (pkg.isBlank()) return@OutlinedButton
                                onWatchedAppsChange(watchedApps + pkg)
                                customPkgText = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Add app") }
                        OutlinedButton(
                            onClick = { onWatchedAppsChange(PaymentApps.defaultPackages) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Reset defaults") }
                    }
                    if (watchedApps.isNotEmpty()) {
                        Text(
                            "Currently watching: ${watchedApps.sorted().joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Section("Data") {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val txns = repo.getAllTxns()
                                val cats = repo.observeCategories().first()
                                val csv = buildCsv(txns, cats.associateBy { it.id })
                                shareCsv(context, csv)
                                snackbarHost.showSnackbar("Exported ${txns.size} transactions")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.IosShare, null)
                        Text("  Export to CSV")
                    }
                    Text(
                        "Saves a CSV of all transactions and opens the share sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Section("About") {
                    Text("PocketSense", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Local-only Android budgeting app. All data stays on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SnackbarHost(snackbarHost) { data -> Snackbar(data) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content() }
        }
    }
}

private fun DarkModePref.displayName(): String = when (this) {
    DarkModePref.SYSTEM -> "System"
    DarkModePref.LIGHT -> "Light"
    DarkModePref.DARK -> "Dark"
}

private val csvDateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())

private fun buildCsv(txns: List<Txn>, categoryById: Map<Long, app.pocketsense.data.Category>): String {
    val sb = StringBuilder()
    sb.append("Datetime,Amount (₹),Category,Source,Trigger app,Split,Note\n")
    for (txn in txns) {
        val dt = csvDateFmt.format(txn.occurredAt)
        val amount = paiseToRupeeText(txn.amountPaise)
        val cat = categoryById[txn.categoryId]?.name ?: ""
        val src = txn.source.name
        val trigger = txn.triggerAppPackage?.let { PaymentApps.label(it) } ?: ""
        val split = if (txn.splitCount > 1) txn.splitCount.toString() else ""
        val note = (txn.note ?: "").replace("\"", "\"\"")
        sb.append("\"").append(dt).append("\",")
        sb.append("\"").append(amount).append("\",")
        sb.append("\"").append(cat).append("\",")
        sb.append("\"").append(src).append("\",")
        sb.append("\"").append(trigger).append("\",")
        sb.append("\"").append(split).append("\",")
        sb.append("\"").append(note).append("\"\n")
    }
    return sb.toString()
}

private fun shareCsv(context: Context, csv: String) {
    val file = File(context.cacheDir, "pocketsense-export-${LocalDate.now()}.csv")
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export PocketSense data"))
}
