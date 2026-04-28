package app.pocketsense.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

// All money is Long paise (₹1 = 100). Never use Float/Double.
// Wallet balance is *derived* — SUM(transactions.amountPaise). Sign convention:
// positive = inflow (income/refund), negative = outflow (expense).

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey val id: Long = 1L,
    val monthlyIncomePaise: Long,
    val cycleStartDay: Int = 1,
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isDefault: Boolean,
    val sortOrder: Int,
    val archivedAt: Instant? = null,
)

@Entity(
    tableName = "allocations",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["categoryId", "cycleStart"], unique = true)],
)
data class Allocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long,
    val cycleStart: LocalDate,
    val amountPaise: Long,
    val rolloverEnabled: Boolean,
    val rolledFromPreviousPaise: Long = 0L,
)

@Entity(
    tableName = "transactions",
    indices = [Index("categoryId"), Index("occurredAt")],
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountPaise: Long,
    val categoryId: Long?,
    val occurredAt: Instant,
    val source: Source,
    val note: String? = null,
    val triggerAppPackage: String? = null,
    @ColumnInfo(defaultValue = "1") val splitCount: Int = 1,
)

enum class Source { MANUAL, PROMPT, INCOME, ADJUSTMENT }

fun Allocation.effectivePaise(): Long = amountPaise + rolledFromPreviousPaise
