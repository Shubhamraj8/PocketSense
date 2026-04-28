package app.pocketsense.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.Wallet
import app.pocketsense.data.currentCycle
import app.pocketsense.data.parseRupeesToPaise
import app.pocketsense.ui.emojiForIconKey
import app.pocketsense.ui.parseColorOrFallback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun OnboardingScreen(
    repo: PocketRepository,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val categories by repo.observeCategories().collectAsState(initial = emptyList())

    var incomeText by remember { mutableStateOf("") }
    var cycleDay by remember { mutableStateOf(1) }
    val budgets = remember { mutableStateMapOf<Long, String>() }
    var saving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                "Welcome to PocketSense",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Local-only budgeting. No accounts, no servers, no data leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text("Monthly income", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(4.dp))
            OutlinedTextField(
                value = incomeText,
                onValueChange = { incomeText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Adds an opening balance entry. You can always add more later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text("Budget cycle", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(4.dp))
            CycleDayPicker(cycleDay = cycleDay, onChange = { cycleDay = it })
        }

        item {
            Text(
                "Set initial budgets",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Allocate amounts to each category. You can change these anytime, or skip and add later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(categories, key = { it.id }) { cat ->
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseColorOrFallback(cat.colorHex, MaterialTheme.colorScheme.surfaceVariant)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emojiForIconKey(cat.iconKey))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        cat.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedTextField(
                        value = budgets[cat.id] ?: "",
                        onValueChange = { input ->
                            budgets[cat.id] = input.filter { c -> c.isDigit() || c == '.' }
                        },
                        placeholder = { Text("₹0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        }

        item {
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    errorText = null
                    saving = true
                    val income = parseRupeesToPaise(incomeText) ?: 0L
                    scope.launch {
                        try {
                            val current = repo.observeWallet().first()
                                ?: Wallet(monthlyIncomePaise = 0L)
                            repo.updateWallet(
                                current.copy(
                                    monthlyIncomePaise = income,
                                    cycleStartDay = cycleDay,
                                )
                            )
                            if (income > 0) repo.addIncome(income, "Initial balance")
                            val cycleStart = currentCycle(LocalDate.now(), cycleDay).start
                            for ((catId, text) in budgets) {
                                val paise = parseRupeesToPaise(text) ?: continue
                                if (paise > 0) {
                                    repo.upsertAllocation(catId, cycleStart, paise, false)
                                }
                            }
                            onDone()
                        } catch (_: Throwable) {
                            errorText = "Could not save setup. Please try again."
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Get started") }
        }
        if (errorText != null) {
            item {
                Text(
                    text = errorText ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Skip for now") }
        }
    }
}

@Composable
private fun CycleDayPicker(cycleDay: Int, onChange: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Cycle starts on day $cycleDay",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Most people pick the day they get paid.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { if (cycleDay > 1) onChange(cycleDay - 1) },
                enabled = cycleDay > 1,
            ) { Icon(Icons.Default.Remove, "Earlier") }
            IconButton(
                onClick = { if (cycleDay < 28) onChange(cycleDay + 1) },
                enabled = cycleDay < 28,
            ) { Icon(Icons.Default.Add, "Later") }
        }
    }
}
