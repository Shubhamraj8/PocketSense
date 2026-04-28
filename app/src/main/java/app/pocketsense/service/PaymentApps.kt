package app.pocketsense.service

object PaymentApps {
    private val labels = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "net.one97.paytm" to "Paytm",
        "com.phonepe.app" to "PhonePe",
        "com.phonepe.app.preprod" to "PhonePe",
        "in.org.npci.upiapp" to "BHIM",
    )

    val packages: Set<String> = labels.keys

    fun label(pkg: String): String = labels[pkg] ?: pkg
}
