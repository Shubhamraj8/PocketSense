package app.pocketsense.data

import app.pocketsense.service.BudgetAlertSink
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

class PocketRepository(
    private val walletDao: WalletDao,
    private val categoryDao: CategoryDao,
    private val txnDao: TxnDao,
    private val allocationDao: AllocationDao,
    private val alertSink: BudgetAlertSink? = null,
) {
    fun observeWallet() = walletDao.observe()
    fun observeWalletBalance() = txnDao.observeBalance()
    fun observeRecentTxns(limit: Int = 20) = txnDao.observeRecent(limit)
    fun observeCategories() = categoryDao.observeActive()
    fun observeCategoryById(id: Long) = categoryDao.observeById(id)
    fun observeRecentTxnsByCategory(categoryId: Long, limit: Int = 100) =
        txnDao.observeRecentByCategory(categoryId, limit)
    fun observeSpentInWindow(categoryId: Long, start: Instant, end: Instant) =
        txnDao.observeSpentByCategoryInWindow(categoryId, start, end)
    fun observeExpensesSince(since: Instant) = txnDao.observeExpensesSince(since)
    fun observeExpensesInWindow(start: Instant, end: Instant) =
        txnDao.observeExpensesInWindow(start, end)
    fun observeAllCategories() = categoryDao.observeAll()
    fun observeAllocationsForCycle(cycleStart: LocalDate) =
        allocationDao.observeByCycle(cycleStart)

    suspend fun getCategory(id: Long): Category? = categoryDao.getById(id)
    suspend fun getTxn(id: Long): Txn? = txnDao.getById(id)
    suspend fun hasAnyExpense(): Boolean = txnDao.anyExists()
    suspend fun getAllTxns(): List<Txn> = txnDao.getAll()

    suspend fun updateWallet(wallet: Wallet) {
        walletDao.update(wallet)
    }

    suspend fun upsertAllocation(
        categoryId: Long,
        cycleStart: LocalDate,
        amountPaise: Long,
        rolloverEnabled: Boolean,
    ): Long {
        val existing = allocationDao.getOne(categoryId, cycleStart)
        val toSave = existing?.copy(
            amountPaise = amountPaise,
            rolloverEnabled = rolloverEnabled,
        ) ?: Allocation(
            categoryId = categoryId,
            cycleStart = cycleStart,
            amountPaise = amountPaise,
            rolloverEnabled = rolloverEnabled,
        )
        return allocationDao.upsert(toSave)
    }

    suspend fun deleteAllocation(categoryId: Long, cycleStart: LocalDate) {
        allocationDao.delete(categoryId, cycleStart)
    }

    suspend fun upsertCategory(category: Category): Long {
        return if (category.id == 0L) {
            val nextOrder = categoryDao.maxSortOrder() + 1
            categoryDao.insert(category.copy(sortOrder = nextOrder))
        } else {
            categoryDao.update(category)
            category.id
        }
    }

    suspend fun archiveCategory(id: Long) {
        categoryDao.archive(id, Instant.now())
    }

    suspend fun addExpense(
        amountPaise: Long,
        categoryId: Long,
        note: String?,
        triggerAppPackage: String?,
        splitCount: Int = 1,
    ): Long {
        require(amountPaise > 0L) { "Expense amount must be positive paise" }
        require(splitCount >= 1) { "splitCount must be >= 1" }
        val sharePaise = if (splitCount > 1) amountPaise / splitCount else amountPaise
        val id = txnDao.insert(
            Txn(
                amountPaise = -sharePaise,
                categoryId = categoryId,
                occurredAt = Instant.now(),
                source = if (triggerAppPackage != null) Source.PROMPT else Source.MANUAL,
                note = note,
                triggerAppPackage = triggerAppPackage,
                splitCount = splitCount,
            )
        )
        alertSink?.let { sink ->
            val cycleStartDay = walletDao.observe().first()?.cycleStartDay ?: 1
            checkAndAlert(categoryId, sharePaise, sink, cycleStartDay)
        }
        return id
    }

    suspend fun addIncome(amountPaise: Long, note: String?): Long {
        require(amountPaise > 0L) { "Income amount must be positive paise" }
        return txnDao.insert(
            Txn(
                amountPaise = amountPaise,
                categoryId = null,
                occurredAt = Instant.now(),
                source = Source.INCOME,
                note = note,
            )
        )
    }

    suspend fun updateExpense(
        id: Long,
        amountPaise: Long,
        categoryId: Long,
        note: String?,
        splitCount: Int,
    ) {
        require(amountPaise > 0L) { "Expense amount must be positive paise" }
        require(splitCount >= 1) { "splitCount must be >= 1" }
        val sharePaise = if (splitCount > 1) amountPaise / splitCount else amountPaise
        val existing = txnDao.getById(id) ?: return
        txnDao.update(
            existing.copy(
                amountPaise = -sharePaise,
                categoryId = categoryId,
                note = note,
                splitCount = splitCount,
            )
        )
    }

    suspend fun updateIncome(id: Long, amountPaise: Long, note: String?) {
        require(amountPaise > 0L) { "Income amount must be positive paise" }
        val existing = txnDao.getById(id) ?: return
        txnDao.update(existing.copy(amountPaise = amountPaise, note = note))
    }

    suspend fun deleteTxn(id: Long) {
        txnDao.deleteById(id)
    }

    /**
     * Carry rollover-enabled allocations into the current cycle from the previous one.
     * Idempotent — only creates this-cycle entries that don't already exist.
     */
    suspend fun ensureCurrentCycleAllocations(today: LocalDate = LocalDate.now()) {
        val wallet = walletDao.observe().first()
        val cycleStartDay = wallet?.cycleStartDay ?: 1
        val cycle = currentCycle(today, cycleStartDay)
        val prev = previousCycle(today, cycleStartDay)
        val prevAllocs = allocationDao.observeByCycle(prev.start).first()
        for (prevAlloc in prevAllocs) {
            if (allocationDao.getOne(prevAlloc.categoryId, cycle.start) != null) continue
            val baseAmount = prevAlloc.amountPaise
            val rolledIn = if (prevAlloc.rolloverEnabled) {
                val prevSpent = txnDao.sumSpentInWindow(
                    prevAlloc.categoryId,
                    prev.startInstant(),
                    prev.endInstant(),
                )
                (prevAlloc.effectivePaise() - prevSpent).coerceAtLeast(0L)
            } else 0L
            allocationDao.upsert(
                Allocation(
                    categoryId = prevAlloc.categoryId,
                    cycleStart = cycle.start,
                    amountPaise = baseAmount,
                    rolloverEnabled = prevAlloc.rolloverEnabled,
                    rolledFromPreviousPaise = rolledIn,
                )
            )
        }
    }

    private suspend fun checkAndAlert(
        categoryId: Long,
        addedExpensePaise: Long,
        sink: BudgetAlertSink,
        cycleStartDay: Int,
    ) {
        val cycle = currentCycle(cycleStartDay = cycleStartDay)
        val alloc = allocationDao.getOne(categoryId, cycle.start) ?: return
        val effective = alloc.effectivePaise()
        if (effective <= 0) return
        val cat = categoryDao.getById(categoryId) ?: return
        val spent = txnDao.sumSpentInWindow(categoryId, cycle.startInstant(), cycle.endInstant())
        val prevSpent = (spent - addedExpensePaise).coerceAtLeast(0L)
        val cur = spent.toFloat() / effective
        val prev = prevSpent.toFloat() / effective
        when {
            prev < 1f && cur >= 1f -> sink.emit(cat, effective, spent, 1f)
            prev < 0.8f && cur >= 0.8f -> sink.emit(cat, effective, spent, 0.8f)
        }
    }
}
