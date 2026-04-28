package app.pocketsense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Wallet::class, Category::class, Allocation::class, Txn::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PocketDb : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun txnDao(): TxnDao
    abstract fun allocationDao(): AllocationDao

    companion object {
        @Volatile private var instance: PocketDb? = null

        fun get(context: Context): PocketDb =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): PocketDb =
            Room.databaseBuilder(
                context.applicationContext,
                PocketDb::class.java,
                DB_NAME,
            )
                .addCallback(SeedCallback)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        private const val DB_NAME = "pocketsense.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN splitCount INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE categories SET archivedAt = datetime('now') WHERE isDefault = 1")
                val defaults = listOf(
                    Triple("Rent", "rent", "#5C6BC0"),
                    Triple("Food", "food", "#FF7043"),
                    Triple("Travel", "travel", "#42A5F5"),
                    Triple("Shopping", "shopping", "#AB47BC"),
                    Triple("Food Orders", "food_orders", "#26A69A"),
                )
                defaults.forEachIndexed { i, (name, key, color) ->
                    db.execSQL(
                        """INSERT INTO categories
                           (name, iconKey, colorHex, isDefault, sortOrder, archivedAt)
                           VALUES (?, ?, ?, 1, ?, NULL)""",
                        arrayOf<Any>(name, key, color, i),
                    )
                }
            }
        }

        private val SeedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO wallet (id, monthlyIncomePaise, cycleStartDay) VALUES (1, 0, 1)"
                )
                val seeds = listOf(
                    Triple("Food", "food", "#FF7043"),
                    Triple("Rent", "rent", "#5C6BC0"),
                    Triple("Travel", "travel", "#42A5F5"),
                    Triple("Shopping", "shopping", "#AB47BC"),
                    Triple("Food Orders", "food_orders", "#26A69A"),
                )
                seeds.forEachIndexed { i, (name, key, color) ->
                    db.execSQL(
                        """INSERT INTO categories
                           (name, iconKey, colorHex, isDefault, sortOrder, archivedAt)
                           VALUES (?, ?, ?, 1, ?, NULL)""",
                        arrayOf<Any>(name, key, color, i),
                    )
                }
            }
        }
    }
}
