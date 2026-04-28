package app.pocketsense.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconKeys {
    const val RENT = "rent"
    const val FOOD = "food"
    const val FOOD_ORDERS = "food_orders"
    const val GROCERIES = "groceries"
    const val TRANSPORT = "transport"
    const val FUEL = "fuel"
    const val SHOPPING = "shopping"
    const val ENTERTAINMENT = "entertainment"
    const val GAMING = "gaming"
    const val HEALTH = "health"
    const val EDUCATION = "education"
    const val GIFTS = "gifts"
    const val TRAVEL = "travel"
    const val BILLS = "bills"
    const val OTHER = "other"

    val commonDefaults = listOf(RENT, FOOD, TRAVEL, SHOPPING, FOOD_ORDERS)
}

object CategoryPalette {
    val colors = listOf(
        "#FF7043", "#EF5350", "#EC407A", "#AB47BC",
        "#7E57C2", "#5C6BC0", "#42A5F5", "#26C6DA",
        "#26A69A", "#66BB6A", "#9CCC65", "#FFCA28",
        "#FFA726", "#8D6E63", "#78909C",
    )
}

object CategoryEmojis {
    val library = listOf(
        "🏠", "🍽️", "🧳", "🛍️", "🛵", "🧾", "🎬", "💊", "🎓", "🎁",
        "🚗", "⛽", "🧺", "🐶", "👶", "🏋️", "📱", "💡", "🧼", "🎮",
        "☕", "🍕", "🥗", "✈️", "🚆", "🚕", "🏥", "📚", "🎉", "💰",
        "🥳", "🍻", "🍺", "🥂", "🚬", "❤️", "💘", "💑", "🏍️", "🛣️",
    )
}

fun emojiKey(emoji: String): String = "emoji:$emoji"

fun emojiForIconKey(key: String?): String = when {
    key.isNullOrBlank() -> "🏷️"
    key.startsWith("emoji:") -> key.removePrefix("emoji:")
    key == CategoryIconKeys.RENT -> "🏠"
    key == CategoryIconKeys.FOOD -> "🍽️"
    key == CategoryIconKeys.FOOD_ORDERS -> "🛵"
    key == CategoryIconKeys.TRAVEL -> "🧳"
    key == CategoryIconKeys.SHOPPING -> "🛍️"
    key == CategoryIconKeys.GROCERIES -> "🧺"
    key == CategoryIconKeys.TRANSPORT -> "🚗"
    key == CategoryIconKeys.FUEL -> "⛽"
    key == CategoryIconKeys.ENTERTAINMENT -> "🎬"
    key == CategoryIconKeys.GAMING -> "🎮"
    key == CategoryIconKeys.HEALTH -> "💊"
    key == CategoryIconKeys.EDUCATION -> "📚"
    key == CategoryIconKeys.GIFTS -> "🎁"
    key == CategoryIconKeys.BILLS -> "🧾"
    else -> "🏷️"
}

fun iconForKey(key: String?): ImageVector = when (key) {
    CategoryIconKeys.FOOD -> Icons.Filled.Restaurant
    CategoryIconKeys.TRANSPORT -> Icons.Filled.DirectionsCar
    CategoryIconKeys.SHOPPING -> Icons.Filled.ShoppingBag
    CategoryIconKeys.ENTERTAINMENT -> Icons.Filled.Movie
    CategoryIconKeys.GROCERIES -> Icons.Filled.LocalGroceryStore
    CategoryIconKeys.FUEL -> Icons.Filled.LocalGasStation
    CategoryIconKeys.HEALTH -> Icons.Filled.LocalHospital
    CategoryIconKeys.EDUCATION -> Icons.Filled.MenuBook
    CategoryIconKeys.GAMING -> Icons.Filled.SportsEsports
    CategoryIconKeys.GIFTS -> Icons.Filled.CardGiftcard
    CategoryIconKeys.TRAVEL -> Icons.Filled.FlightTakeoff
    CategoryIconKeys.BILLS -> Icons.Filled.Receipt
    else -> Icons.Filled.Category
}

fun parseColorOrFallback(hex: String?, fallback: Color): Color {
    if (hex == null) return fallback
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}
