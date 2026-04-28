package app.pocketsense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet WHERE id = 1")
    fun observe(): Flow<Wallet?>

    @Update
    suspend fun update(wallet: Wallet)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE archivedAt IS NULL ORDER BY sortOrder")
    fun observeActive(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeById(id: Long): Flow<Category?>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("UPDATE categories SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Instant)
}

@Dao
interface AllocationDao {
    @Query("SELECT * FROM allocations WHERE cycleStart = :cycleStart")
    fun observeByCycle(cycleStart: LocalDate): Flow<List<Allocation>>

    @Query("SELECT * FROM allocations WHERE categoryId = :categoryId AND cycleStart = :cycleStart")
    suspend fun getOne(categoryId: Long, cycleStart: LocalDate): Allocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(allocation: Allocation): Long

    @Query("DELETE FROM allocations WHERE categoryId = :categoryId AND cycleStart = :cycleStart")
    suspend fun delete(categoryId: Long, cycleStart: LocalDate)
}

@Dao
interface TxnDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecentByCategory(categoryId: Long, limit: Int): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Txn?

    @Query("SELECT * FROM transactions ORDER BY occurredAt")
    suspend fun getAll(): List<Txn>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions LIMIT 1)")
    suspend fun anyExists(): Boolean

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions")
    fun observeBalance(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(-amountPaise), 0) FROM transactions
        WHERE categoryId = :categoryId
          AND amountPaise < 0
          AND occurredAt >= :start
          AND occurredAt < :end
    """)
    fun observeSpentByCategoryInWindow(categoryId: Long, start: Instant, end: Instant): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(-amountPaise), 0) FROM transactions
        WHERE categoryId = :categoryId
          AND amountPaise < 0
          AND occurredAt >= :start
          AND occurredAt < :end
    """)
    suspend fun sumSpentInWindow(categoryId: Long, start: Instant, end: Instant): Long

    @Query("""
        SELECT * FROM transactions
        WHERE amountPaise < 0
          AND occurredAt >= :since
        ORDER BY occurredAt
    """)
    fun observeExpensesSince(since: Instant): Flow<List<Txn>>

    @Insert
    suspend fun insert(txn: Txn): Long

    @Update
    suspend fun update(txn: Txn)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
