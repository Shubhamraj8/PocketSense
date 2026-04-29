package app.pocketsense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.Source
import app.pocketsense.data.Txn
import app.pocketsense.data.formatRupees
import app.pocketsense.data.paiseToRupeeText
import app.pocketsense.data.parseRupeesToPaise
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditTxnSheet(
    txn: Txn,
    repo: PocketRepository,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val categories by repo.observeCategories().collectAsState(initial = emptyList())
    val isIncome = txn.source == Source.INCOME

    // For expenses: stored amount is negative share. We display the original
    // total (share × splitCount) so editing is intuitive.
    val originalSharePaise = if (txn.amountPaise < 0) -txn.amountPaise else txn.amountPaise
    val originalTotal = if (txn.splitCount > 1) originalSharePaise * txn.splitCount else originalSharePaise
    val initialAmount = if (isIncome) txn.amountPaise else originalTotal

    var amountText by remember { mutableStateOf(paiseToRupeeText(initialAmount)) }
    var selectedCategoryId by remember { mutableStateOf(txn.categoryId) }
    var note by remember { mutableStateOf(txn.note ?: "") }
    var splitOn by remember { mutableStateOf(txn.splitCount > 1) }
    var splitCount by remember { mutableStateOf(txn.splitCount.coerceAtLeast(2)) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val paise = parseRupeesToPaise(amountText)
    val effectiveSplit = if (splitOn && !isIncome) splitCount else 1
    val sharePaise = paise?.let { if (effectiveSplit > 1) it / effectiveSplit else it } ?: 0L
    val canSave = paise != null &&
        paise > 0L &&
        (isIncome || selectedCategoryId != null) &&
        !saving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (isIncome) "Edit income" else "Edit expense",
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { c -> c.isDigit() || c == '.' }
                },
                label = { Text(if (splitOn) "Total bill (₹)" else "Amount (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!isIncome) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Split with others", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Divide the bill across people",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = splitOn,
                        onCheckedChange = {
                            splitOn = it
                            if (it && splitCount < 2) splitCount = 2
                        },
                    )
                }

                if (splitOn) {
                    SplitStepper(
                        count = splitCount,
                        onChange = { splitCount = it },
                    )
                    if (paise != null && paise > 0L) {
                        Text(
                            "Your share: ${formatRupees(sharePaise)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
                Button(
                    onClick = {
                        val p = paise ?: return@Button
                        saving = true
                        scope.launch {
                            if (isIncome) {
                                repo.updateIncome(txn.id, p, note.takeIf { it.isNotBlank() })
                            } else {
                                val cat = selectedCategoryId ?: return@launch
                                repo.updateExpense(
                                    id = txn.id,
                                    amountPaise = p,
                                    categoryId = cat,
                                    note = note.takeIf { it.isNotBlank() },
                                    splitCount = effectiveSplit,
                                )
                            }
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("It will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.deleteTxn(txn.id)
                        confirmDelete = false
                        onDismiss()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun SplitStepper(
    count: Int,
    onChange: (Int) -> Unit,
    min: Int = 2,
    max: Int = 20,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Total people: $count",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButton(
            onClick = { if (count > min) onChange(count - 1) },
            enabled = count > min,
        ) { Icon(Icons.Default.Remove, "Decrease") }
        IconButton(
            onClick = { if (count < max) onChange(count + 1) },
            enabled = count < max,
        ) { Icon(Icons.Default.Add, "Increase") }
    }
}
