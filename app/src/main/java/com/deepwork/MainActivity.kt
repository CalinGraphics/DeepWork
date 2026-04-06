package com.deepwork

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.deepwork.core.ui.theme.DeepWorkTheme
import com.deepwork.feature.analytics.AchievementsScreen
import com.deepwork.feature.analytics.AnalyticsScreen
import com.deepwork.feature.pcremote.PcRemoteScreen
import com.deepwork.feature.settings.BlockedAppsPickerScreen
import com.deepwork.feature.settings.SettingsScreen
import com.deepwork.feature.tasks.TaskScreen
import com.deepwork.feature.timer.TimerScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var unlockSeqPos = 0
    private var unlockSeqLastAt = 0L
    @Volatile
    internal var unlockUntilMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Safety net: iesim din lock task daca a ramas activ dintr-o versiune veche.
        runCatching { stopLockTask() }
        enableEdgeToEdge()
        setContent {
            DeepWorkTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
                val prefs by mainViewModel.preferences.collectAsState()
                val strictFocusActive by mainViewModel.strictFocusActive.collectAsState()
                if (!prefs.onboardingCompleted) {
                    KaraOnboardingScreen(
                        onComplete = { mainViewModel.completeOnboarding() },
                        onSkip = { mainViewModel.completeOnboarding() }
                    )
                } else {
                    StrictFocusGuard(
                        strictFocusActive = strictFocusActive,
                        unlockUntilMsProvider = { unlockUntilMs }
                    )
                    DeepWorkAppContent()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Combinație: VolUp, VolDown, VolUp, VolDown în max 3 secunde.
            val expected = intArrayOf(
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN
            )
            val now = System.currentTimeMillis()
            if (now - unlockSeqLastAt > 3000L) unlockSeqPos = 0
            unlockSeqLastAt = now

            if (event.keyCode == expected[unlockSeqPos]) {
                unlockSeqPos++
                if (unlockSeqPos >= expected.size) {
                    unlockSeqPos = 0
                    runCatching { stopLockTask() }
                    unlockUntilMs = System.currentTimeMillis() + 6_000L
                    Toast.makeText(this, "Kara: focus unlock", Toast.LENGTH_SHORT).show()
                    return true
                }
            } else {
                unlockSeqPos = if (event.keyCode == expected[0]) 1 else 0
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

sealed class BottomDestination(
    val route: String,
    val label: String
) {
    data object Timer : BottomDestination("timer", "Timer")
    data object Tasks : BottomDestination("tasks", "Tasks")
    data object Analytics : BottomDestination("analytics", "Analytics")
    data object Settings : BottomDestination("settings", "Settings")
}

private const val ROUTE_PC_REMOTE = "pcremote"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_BLOCKED_APPS = "blocked_apps"

@Composable
private fun StrictFocusGuard(
    strictFocusActive: Boolean,
    unlockUntilMsProvider: () -> Long
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val unlockUntilMs = unlockUntilMsProvider()
    val lockedNow = strictFocusActive && System.currentTimeMillis() > unlockUntilMs

    // Back blocat cât timp sesiunea e activă și nu e deblocat temporar.
    BackHandler(enabled = lockedNow) {}

    LaunchedEffect(strictFocusActive, unlockUntilMs) {
        if (strictFocusActive) {
            if (System.currentTimeMillis() > unlockUntilMs) {
                runCatching { activity.startLockTask() }
            }
        } else {
            runCatching { activity.stopLockTask() }
        }
    }
}

@Composable
fun DeepWorkAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val items = listOf(
        BottomDestination.Timer,
        BottomDestination.Tasks,
        BottomDestination.Analytics,
        BottomDestination.Settings
    )

    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in items.map { it.route }

    val navigateFromDrawer: (String) -> Unit = { route ->
        navController.navigate(route) {
            if (route == ROUTE_PC_REMOTE) {
                launchSingleTop = true
            } else {
                popUpTo(BottomDestination.Timer.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DeepWorkDrawerSheet(
                currentRoute = currentRoute,
                onNavigate = navigateFromDrawer
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    DeepWorkBottomNavigation(navController, items)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomDestination.Timer.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomDestination.Timer.route) {
                    TimerScreen(
                        onOpenTaskList = {
                            navController.navigate(BottomDestination.Tasks.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenNotifications = {
                            navController.navigate(ROUTE_NOTIFICATIONS) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenMenu = {
                            scope.launch { drawerState.open() }
                        }
                    )
                }
                composable(BottomDestination.Tasks.route) { TaskScreen() }
                composable(BottomDestination.Analytics.route) {
                    AnalyticsScreen(
                        onOpenAchievements = { navController.navigate("achievements") }
                    )
                }
                composable("achievements") {
                    AchievementsScreen(onBack = { navController.popBackStack() })
                }
                composable(ROUTE_NOTIFICATIONS) { NotificationsScreen() }
                composable(BottomDestination.Settings.route) {
                    SettingsScreen(
                        onOpenDesktopPairing = { navController.navigate(ROUTE_PC_REMOTE) },
                        onOpenBlockedAppsPicker = {
                            navController.navigate(ROUTE_BLOCKED_APPS) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(ROUTE_BLOCKED_APPS) {
                    BlockedAppsPickerScreen(onBack = { navController.popBackStack() })
                }
                composable(ROUTE_PC_REMOTE) { PcRemoteScreen() }
            }
        }
    }
}

@Composable
private fun DeepWorkDrawerSheet(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.kara_logo),
                contentDescription = "Kara",
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(96.dp)
                    .alpha(0.92f),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(8.dp))
            NavigationDrawerItem(
                label = { Text(BottomDestination.Timer.label) },
                selected = currentRoute == BottomDestination.Timer.route,
                onClick = { onNavigate(BottomDestination.Timer.route) },
                icon = {
                    Icon(Icons.Rounded.Timer, contentDescription = null)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
            NavigationDrawerItem(
                label = { Text(BottomDestination.Tasks.label) },
                selected = currentRoute == BottomDestination.Tasks.route,
                onClick = { onNavigate(BottomDestination.Tasks.route) },
                icon = {
                    Icon(Icons.Rounded.TaskAlt, contentDescription = null)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
            NavigationDrawerItem(
                label = { Text(BottomDestination.Analytics.label) },
                selected = currentRoute == BottomDestination.Analytics.route,
                onClick = { onNavigate(BottomDestination.Analytics.route) },
                icon = {
                    Icon(Icons.Rounded.BarChart, contentDescription = null)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
            NavigationDrawerItem(
                label = { Text(BottomDestination.Settings.label) },
                selected = currentRoute == BottomDestination.Settings.route,
                onClick = { onNavigate(BottomDestination.Settings.route) },
                icon = {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.08f)
            )
            NavigationDrawerItem(
                label = { Text("Conectare remote") },
                selected = currentRoute == ROUTE_PC_REMOTE,
                onClick = { onNavigate(ROUTE_PC_REMOTE) },
                icon = {
                    Icon(Icons.Rounded.DesktopWindows, contentDescription = null)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DeepWorkBottomNavigation(
    navController: NavHostController,
    items: List<BottomDestination>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(modifier = Modifier.navigationBarsPadding()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.06f)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            items.forEach { dest ->
                val selected = currentRoute == dest.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(dest.route) {
                            popUpTo(BottomDestination.Timer.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        when (dest) {
                            BottomDestination.Timer ->
                                Icon(Icons.Rounded.Timer, contentDescription = dest.label)
                            BottomDestination.Tasks ->
                                Icon(Icons.Rounded.TaskAlt, contentDescription = dest.label)
                            BottomDestination.Analytics ->
                                Icon(Icons.Rounded.BarChart, contentDescription = dest.label)
                            BottomDestination.Settings ->
                                Icon(Icons.Rounded.Settings, contentDescription = dest.label)
                        }
                    },
                    label = {
                        Text(
                            dest.label.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
