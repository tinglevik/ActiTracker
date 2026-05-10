package com.example.actitracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.actitracker.service.ActivityTrackerService
import com.example.actitracker.ui.screens.LicensesScreen
import com.example.actitracker.ui.screens.LowContrastWarningDrawer
import com.example.actitracker.ui.screens.ManageActivitiesScreen
import com.example.actitracker.ui.screens.ReportScreen
import com.example.actitracker.ui.screens.SettingsScreen
import com.example.actitracker.ui.screens.TodayScreen
import com.example.actitracker.ui.theme.ActitrackerTheme
import com.example.actitracker.viewmodel.ReportViewModel
import com.example.actitracker.viewmodel.ReportViewModelFactory
import com.example.actitracker.viewmodel.SettingsViewModel
import com.example.actitracker.viewmodel.SettingsViewModelFactory
import com.example.actitracker.viewmodel.TodayViewModel
import com.example.actitracker.viewmodel.TodayViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as ActiTrackerApplication
        val repository = app.repository
        val backupManager = com.example.actitracker.data.DataBackupManager(
            app.database.activityDao(),
            app.settingsDataStore
        )
        val todayFactory = TodayViewModelFactory(repository, app.settingsDataStore)
        val settingsFactory = SettingsViewModelFactory(app.settingsDataStore, backupManager)

        val serviceIntent = Intent(this, ActivityTrackerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        setContent {
            ActitrackerTheme {
                val navController = rememberNavController()

                val todayViewModel: TodayViewModel = viewModel(factory = todayFactory)
                val activities by todayViewModel.activitiesWithStats.collectAsState()
                val tags by todayViewModel.tags.collectAsState()
                val goals by todayViewModel.goals.collectAsState()

                val isCreating by todayViewModel.isCreating.collectAsState()
                val activeActivityId by todayViewModel.activeActivityId.collectAsState()

                val reportViewModel: ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(
                        todayViewModel.activitiesWithStats,
                        todayViewModel.tags,
                        todayViewModel.activeActivityId,
                        todayViewModel.activeStartTime,
                        repository
                    )
                )

                val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)
                val backgroundColor by settingsViewModel.backgroundColor.collectAsState()
                val contentColor by settingsViewModel.contentColor.collectAsState()
                val showWarningDrawer by settingsViewModel.showWarningDrawer.collectAsState()
                val snackbarMessage by settingsViewModel.snackbarMessage.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        settingsViewModel.clearSnackbar()
                    }
                }

                Scaffold(
                    bottomBar = { BottomNavBar(navController) },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {

                        val statusBarHeight = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(statusBarHeight)
                                .background(Color(0xE6000000))
                        )

                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            NavHost(
                                navController = navController,
                                startDestination = "today",
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .background(backgroundColor)
                            ) {
                                composable("today") {
                                    val ticker by todayViewModel.ticker.collectAsState()

                                    TodayScreen(
                                        activities = activities,
                                        activeActivityId = activeActivityId,
                                        ticker = ticker,
                                        onStartActivity = { todayViewModel.startActivity(it) },
                                        onStopActivity = { todayViewModel.stopActivity(it) },
                                        onManageClick = {
                                            navController.navigate("manage") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onCreateStart = { todayViewModel.startCreating() },
                                        onCreateDismiss = { todayViewModel.stopCreating() },
                                        onCreateSave = { todayViewModel.addActivity(it) },
                                        isCreating = isCreating,
                                        allTags = tags,
                                        backgroundColor = backgroundColor,
                                        contentColor = contentColor,
                                        onQuickPanelToggle = { todayViewModel.toggleQuickPanel(it) },
                                        onReorderActivities = { todayViewModel.reorderActivities(it) }
                                    )
                                }

                                composable("manage") {
                                    ManageActivitiesScreen(
                                        navController = navController,
                                        activities = activities,
                                        onActivityUpdate = { todayViewModel.updateActivity(it) },
                                        onActivityCreate = { todayViewModel.addActivity(it) },
                                        onActivityDelete = { todayViewModel.deleteActivity(it) },
                                        onReorderActivities = { todayViewModel.reorderActivities(it) },
                                        tags = tags,
                                        onTagUpdate = { todayViewModel.updateTag(it) },
                                        onTagCreate = { todayViewModel.addTag(it) },
                                        onTagDelete = { todayViewModel.deleteTag(it) },
                                        onReorderTags = { todayViewModel.reorderTags(it) },
                                        goals = goals,
                                        onGoalUpdate = { todayViewModel.updateGoal(it) },
                                        onGoalCreate = { todayViewModel.addGoal(it) },
                                        onGoalDelete = { todayViewModel.deleteGoal(it) },
                                        dialogBackgroundColor = backgroundColor,
                                        dialogContentColor = contentColor
                                    )
                                }

                                composable("report") {
                                    ReportScreen(
                                        viewModel = reportViewModel,
                                        contentColor = contentColor,
                                        backgroundColor = backgroundColor
                                    )
                                }

                                composable("settings?highlight={highlight}") { backStackEntry ->
                                    val highlight = backStackEntry.arguments?.getString("highlight") == "true"
                                    SettingsScreen(
                                        settingsViewModel = settingsViewModel,
                                        onNavigateToLicenses = {
                                            navController.navigate("licenses")
                                        },
                                        contentColor = contentColor,
                                        shouldHighlight = highlight
                                    )
                                }

                                composable("licenses") {
                                    LicensesScreen(
                                        onBack = { navController.popBackStack() },
                                        backgroundColor = backgroundColor,
                                        contentColor = contentColor
                                    )
                                }
                            }
                        }

                        if (showWarningDrawer) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            LowContrastWarningDrawer(
                                onRevert = { settingsViewModel.revertChanges(context) },
                                onKeep = { settingsViewModel.keepChanges(context) },
                                onTimeout = { settingsViewModel.revertChanges(context) }
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BottomNavBar(navController: NavHostController) {
        val items = listOf(
            BottomNavItem.Today to R.string.nav_today,
            BottomNavItem.Report to R.string.nav_report,
            BottomNavItem.Settings to R.string.nav_settings
        )

        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            NavigationBar(
                modifier = Modifier.height(48.dp),
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { (item, labelRes) ->
                    val label = stringResource(labelRes)
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.popBackStack("manage", inclusive = true)
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val isSelected = currentRoute == item.route
                            Icon(
                                painter = painterResource(id = if (isSelected) item.selectedIcon else item.unselectedIcon),
                                contentDescription = label,
                                modifier = Modifier
                                    .size(20.dp)
                                    .offset(y = 8.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = LocalContentColor.current,
                            selectedTextColor = LocalContentColor.current,
                            unselectedIconColor = LocalContentColor.current.copy(alpha = 0.6f),
                            unselectedTextColor = LocalContentColor.current.copy(alpha = 0.6f)
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                }
            }
        }
    }

    sealed class BottomNavItem(
        val route: String,
        @param:DrawableRes val selectedIcon: Int,
        @param:DrawableRes val unselectedIcon: Int
    ) {
        object Today : BottomNavItem(
            "today",
            R.drawable.ic_today_filled,
            R.drawable.ic_today_outline
        )

        object Report : BottomNavItem(
            "report",
            R.drawable.ic_pie_chart_filled,
            R.drawable.ic_pie_chart_outline
        )

        object Settings : BottomNavItem(
            "settings", R.drawable.ic_settings_filled, R.drawable.ic_settings_outline
        )
    }
}
