package app.pocketsense.data

import android.content.Context
import app.pocketsense.service.AndroidBudgetAlertSink

class AppContainer(context: Context) {
    val preferences: Preferences = Preferences(context)
    private val db = PocketDb.get(context)
    private val alertSink = AndroidBudgetAlertSink(context)
    val repository: PocketRepository = PocketRepository(
        walletDao = db.walletDao(),
        categoryDao = db.categoryDao(),
        txnDao = db.txnDao(),
        allocationDao = db.allocationDao(),
        alertSink = alertSink,
    )
}
