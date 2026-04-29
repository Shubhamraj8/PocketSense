package app.pocketsense.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pocketsense.data.Allocation
import app.pocketsense.data.Category
import app.pocketsense.data.paiseToRupeeText
import app.pocketsense.data.parseRupeesToPaise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetSheet(
    category: Category,
    existing: Allocation?,
    onSave: (amountPaise: Long, rolloverEnabled: Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember {
        mutableStateOf(existing?.let { paiseToRupeeText(it.amountPaise) } ?: "")
    }
    var rollover by remember { mutableStateOf(existing?.rolloverEnabled ?: false) }
    val paise = parseRupeesToPaise(amountText)
    val canSave = paise != null && paise > 0L

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
                "Budget for ${category.name}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Set how much you want to spend on this category each cycle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { c -> c.isDigit() || c == '.' }
                },
                label = { Text("Monthly budget (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Roll over", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Carry unspent amount to next cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rollover, onCheckedChange = { rollover = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (existing != null) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                    ) { Text("Clear") }
                }
                Button(
                    onClick = { paise?.let { onSave(it, rollover) } },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}
