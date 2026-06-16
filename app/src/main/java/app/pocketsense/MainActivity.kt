package app.pocketsense

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.pocketsense.data.DarkModePref
import app.pocketsense.data.PocketRepository
import app.pocketsense.data.Preferences
import app.pocketsense.service.AndroidBudgetAlertSink
import app.pocketsense.service.ExpensePromptService
import app.pocketsense.ui.Dest
import app.pocketsense.ui.auth.AuthScreen
import app.pocketsense.ui.categories.CategoriesScreen
import app.pocketsense.ui.categories.CategoryDetailScreen
import app.pocketsense.ui.home.HomeScreen
import app.pocketsense.ui.insights.CycleDetailScreen
import app.pocketsense.ui.insights.InsightsScreen
import app.pocketsense.ui.quickadd.QuickAddSheet
import app.pocketsense.ui.settings.SettingsScreen
import app.pocketsense.ui.theme.PocketSenseTheme

class MainActivity : ComponentActivity() {

    private var triggerAppPackage: String? by mutableStateOf(null)
    private var deepLinkCategoryId: Long? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        consumeIntent(intent)
        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as PocketSenseApp
            val container = app.container
            val prefs: Preferences = container.preferences
            val authRepo = container.authRepository

            var darkMode by remember { mutableStateOf(prefs.darkMode()) }
            var watchedApps by remember { mutableStateOf(prefs.watchedPaymentApps()) }
            val firebaseUser by authRepo.observeAuthUser().collectAsState(initial = authRepo.currentUser())

            val darkTheme = when (darkMode) {
                DarkModePref.SYSTEM -> isSystemInDarkTheme()
                DarkModePref.LIGHT -> false
                DarkModePref.DARK -> true
            }
            PocketSenseTheme(darkTheme = darkTheme) {
                if (firebaseUser == null) {
                    AuthScreen(authRepository = authRepo, darkMode = darkMode)
                } else {
                    val userDisplayName = firebaseUser?.displayName
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: firebaseUser?.email
                            ?.substringBefore("@")
                            ?.takeIf { it.isNotBlank() }
                        ?: "there"
                    AppRoot(
                        repo = container.repository,
                        triggerAppPackage = triggerAppPackage,
                        onTriggerConsumed = { triggerAppPackage = null },
                        deepLinkCategoryId = deepLinkCategoryId,
                        onDeepLinkConsumed = { deepLinkCategoryId = null },
                        userDisplayName = userDisplayName,
                        darkMode = darkMode,
                        onDarkModeChange = { newPref ->
                            prefs.setDarkMode(newPref)
                            darkMode = newPref
                        },
                        watchedApps = watchedApps,
                        onWatchedAppsChange = { packages ->
                            prefs.setWatchedPaymentApps(packages)
                            watchedApps = prefs.watchedPaymentApps()
                        },
                        onSignOut = { authRepo.signOut() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        intent ?: return
        val pkg = intent.getStringExtra(ExpensePromptService.EXTRA_TRIGGER_PACKAGE)
        if (pkg != null) {
            triggerAppPackage = pkg
            intent.removeExtra(ExpensePromptService.EXTRA_TRIGGER_PACKAGE)
        }
        val catId = intent.getLongExtra(AndroidBudgetAlertSink.EXTRA_OPEN_CATEGORY_ID, -1L)
        if (catId > 0L) {
            deepLinkCategoryId = catId
            intent.removeExtra(AndroidBudgetAlertSink.EXTRA_OPEN_CATEGORY_ID)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    repo: PocketRepository,
    triggerAppPackage: String?,
    onTriggerConsumed: () -> Unit,
    deepLinkCategoryId: Long?,
    onDeepLinkConsumed: () -> Unit,
    userDisplayName: String,
    darkMode: DarkModePref,
    onDarkModeChange: (DarkModePref) -> Unit,
    watchedApps: Set<String>,
    onWatchedAppsChange: (Set<String>) -> Unit,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val bottomTabs = remember { listOfNotNull(Dest.Home, Dest.Categories, Dest.Insights) }
    var manualAddOpen by remember { mutableStateOf(false) }
    val showSheet = triggerAppPackage != null || manualAddOpen
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Dest.Home.route
    val tabRoutes = remember(bottomTabs) { bottomTabs.map { it.route }.toSet() }
    val isTabRoute = currentRoute in tabRoutes
    val showFab = currentRoute == Dest.Home.route || currentRoute == Dest.Categories.route

    LaunchedEffect(deepLinkCategoryId) {
        if (deepLinkCategoryId != null) {
            navController.navigate(Dest.CategoryDetail.build(deepLinkCategoryId))
            onDeepLinkConsumed()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTabRoute) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentRoute) {
                                Dest.Home.route -> "PocketSense"
                                Dest.Categories.route -> "Categories"
                                Dest.Insights.route -> "Insights"
                                else -> ""
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Dest.Settings.route) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (isTabRoute) {
                BottomNav(
                    navController = navController,
                    currentRoute = currentRoute,
                    tabs = bottomTabs,
                )
            }
        },
        floatingActionButton = {
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = { manualAddOpen = true },
                    text = { Text("Add expense") },
                    icon = { Icon(Icons.Default.Add, null) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    repo = repo,
                    userDisplayName = userDisplayName,
                    onCategoryClick = { id -> navController.navigate(Dest.CategoryDetail.build(id)) },
                )
            }
            composable(Dest.Categories.route) {
                CategoriesScreen(
                    repo = repo,
                    onCategoryClick = { id -> navController.navigate(Dest.CategoryDetail.build(id)) },
                )
            }
            composable(Dest.Insights.route) {
                InsightsScreen(
                    repo = repo,
                    onSeeCycleDetails = { navController.navigate(Dest.CycleDetail.route) },
                )
            }
            composable(Dest.CycleDetail.route) {
                CycleDetailScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Dest.CategoryDetail.route,
                arguments = listOf(navArgument(Dest.CategoryDetail.ARG) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(Dest.CategoryDetail.ARG) ?: return@composable
                CategoryDetailScreen(
                    categoryId = id,
                    repo = repo,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Dest.Settings.route) {
                SettingsScreen(
                    repo = repo,
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    watchedApps = watchedApps,
                    onWatchedAppsChange = onWatchedAppsChange,
                    onSignOut = onSignOut,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    if (showSheet) {
        QuickAddSheet(
            repo = repo,
            triggerAppPackage = triggerAppPackage,
            onDismiss = {
                manualAddOpen = false
                if (triggerAppPackage != null) onTriggerConsumed()
            },
        )
    }
}

@Composable
private fun BottomNav(
    navController: NavHostController,
    currentRoute: String,
    tabs: List<Dest>,
) {
    NavigationBar {
        tabs.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    if (currentRoute != dest.route) {
                        navController.navigate(dest.route) {
                            popUpTo(Dest.Home.route) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { dest.icon?.let { Icon(it, contentDescription = dest.label) } },
                label = { Text(dest.label) },
            )
        }
    }
}
