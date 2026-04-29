package app.pocketsense.service

object PaymentApps {
    private val labels = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "net.one97.paytm" to "Paytm",
        "com.phonepe.app" to "PhonePe",
        "com.phonepe.app.preprod" to "PhonePe",
        "in.org.npci.upiapp" to "BHIM",
        "com.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.whatsapp" to "WhatsApp Pay",
        "com.freecharge.android" to "Freecharge",
        "com.mobikwik_new" to "MobiKwik",
    )

    val defaultPackages: Set<String> = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",
    )

    val knownPackages: Set<String> = labels.keys

    fun label(pkg: String): String = labels[pkg] ?: pkg
}
