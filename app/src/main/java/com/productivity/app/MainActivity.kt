package com.productivity.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.productivity.app.ui.navigation.NavGraph
import com.productivity.app.ui.navigation.Screen
import com.productivity.app.ui.theme.ProductivityTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    private var reminderIdParam by mutableStateOf<Long?>(null)
    private var eventIdParam by mutableStateOf<Long?>(null)
    private var openPersonalManager by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()
        handleIntent(intent)

        setContent {
            ProductivityTheme {
                MainScreen(
                    alarmReminderId = reminderIdParam,
                    alarmEventId = eventIdParam,
                    openPersonalManager = openPersonalManager,
                    onClearAlarm = {
                        reminderIdParam = null
                        eventIdParam = null
                    },
                    onClearOpenPersonalManager = {
                        openPersonalManager = false
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val reminderId = intent?.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L) ?: -1L
        val eventId = intent?.getLongExtra(AlarmManagerHelper.EXTRA_EVENT_ID, -1L) ?: -1L
        
        if (reminderId != -1L) {
            reminderIdParam = reminderId
        }
        if (eventId != -1L) {
            eventIdParam = eventId
        }
        if (intent?.getBooleanExtra("extra_open_personal_manager", false) == true) {
            openPersonalManager = true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainScreen(
    alarmReminderId: Long?,
    alarmEventId: Long?,
    openPersonalManager: Boolean,
    onClearAlarm: () -> Unit,
    onClearOpenPersonalManager: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(alarmReminderId, alarmEventId, openPersonalManager) {
        if (alarmReminderId != null || alarmEventId != null) {
            navController.navigate(Screen.AlarmActive.createRoute(alarmReminderId, alarmEventId)) {
                launchSingleTop = true
            }
            onClearAlarm()
        } else if (openPersonalManager) {
            navController.navigate(Screen.PersonalManager.route) {
                launchSingleTop = true
            }
            onClearOpenPersonalManager()
        }
    }

    // Only show bottom bar on top-level destinations
    val showBottomBar = Screen.bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon
                                                  else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController = navController)
        }
    }
}
