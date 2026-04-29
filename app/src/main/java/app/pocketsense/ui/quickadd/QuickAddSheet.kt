package app.pocketsense.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import app.pocketsense.data.formatRupees
import app.pocketsense.data.parseRupeesToPaise
import app.pocketsense.service.PaymentApps
import app.pocketsense.ui.SplitStepper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    repo: PocketRepository,
    triggerAppPackage: String?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val categories by repo.observeCategories().collectAsState(initial = emptyList())

    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var splitOn by remember { mutableStateOf(false) }
    var splitCount by remember { mutableStateOf(2) }
    var saving by remember { mutableStateOf(false) }

    val paise = parseRupeesToPaise(amountText)
    val effectiveSplit = if (splitOn) splitCount else 1
    val sharePaise = paise?.let { if (effectiveSplit > 1) it / effectiveSplit else it } ?: 0L
    val canSave = paise != null && paise > 0L && selectedCategoryId != null && !saving

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Log expense", style = MaterialTheme.typography.headlineSmall)

            if (triggerAppPackage != null) {
                AssistChip(
                    onClick = {},
                    label = { Text("From ${PaymentApps.label(triggerAppPackage)}") },
                )
            }

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
                SplitStepper(count = splitCount, onChange = { splitCount = it })
                if (paise != null && paise > 0L) {
                    Text(
                        "Your share: ${formatRupees(sharePaise)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val p = paise ?: return@Button
                    val cat = selectedCategoryId ?: return@Button
                    saving = true
                    scope.launch {
                        repo.addExpense(
                            amountPaise = p,
                            categoryId = cat,
                            note = note.takeIf { it.isNotBlank() },
                            triggerAppPackage = triggerAppPackage,
                            splitCount = effectiveSplit,
                        )
                        sheetState.hide()
                        onDismiss()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}
