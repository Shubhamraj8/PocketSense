package app.pocketsense.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Dest(val route: String, val label: String, val icon: ImageVector?) {
    object Home : Dest("home", "Home", Icons.Filled.Home)
    object Categories : Dest("categories", "Categories", Icons.Filled.Category)
    object Insights : Dest("insights", "Insights", Icons.Filled.BarChart)
    object CategoryDetail : Dest("category/{categoryId}", "Category", null) {
        const val ARG = "categoryId"
        fun build(id: Long) = "category/$id"
    }
    object Settings : Dest("settings", "Settings", null)

    companion object {
        val bottomTabs = listOf(Home, Categories, Insights)
    }
}
