package com.stefansturm.ripple

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stefansturm.ripple.core.designsystem.RippleTheme
import com.stefansturm.ripple.core.domain.IntakeSource
import com.stefansturm.ripple.core.domain.LogIntake
import com.stefansturm.ripple.core.domain.LogIntakeCommand
import com.stefansturm.ripple.core.domain.Milliliters
import com.stefansturm.ripple.feature.history.DayDetailRoute
import com.stefansturm.ripple.feature.history.HistoryRoute
import com.stefansturm.ripple.feature.onboarding.OnboardingRoute
import com.stefansturm.ripple.feature.settings.SettingsRoute
import com.stefansturm.ripple.feature.stats.StatsRoute
import com.stefansturm.ripple.feature.today.TodayRoute
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private val incomingIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as RippleApplication).graph.let { graph ->
            setContent { RippleApp(graph, incomingIntents) }
        }
        intent?.let { incomingIntents.tryEmit(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntents.tryEmit(intent)
    }
}

private data class RootDestination(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun RippleApp(graph: RippleAppGraph, incomingIntents: SharedFlow<Intent>) {
    val navController = rememberNavController()
    val profile by graph.repository.observeProfile().collectAsStateWithLifecycle(initialValue = com.stefansturm.ripple.core.domain.UserProfile())
    val context = LocalContext.current
    val startDestination = if (profile.onboardingComplete) "today" else "onboarding"
    RippleTheme {
        LaunchedEffect(profile.onboardingComplete) {
            if (profile.onboardingComplete) {
                navController.navigate("today") {
                    popUpTo("onboarding") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        LaunchedEffect(incomingIntents) {
            incomingIntents.collect { intent ->
                when (intent.action) {
                    "de.stefansturm.ripple.OPEN_TODAY" -> navController.navigate("today") { launchSingleTop = true }
                    "de.stefansturm.ripple.LOG_DEFAULT",
                    "de.stefansturm.ripple.LOG_DEFAULT_INTENT" -> {
                        val snapshot = graph.repository.observeToday().first()
                        val source = intent.getStringExtra("source")?.let(IntakeSource::fromRaw)
                            ?: if (intent.action == "de.stefansturm.ripple.LOG_DEFAULT_INTENT") IntakeSource.INTENT else IntakeSource.CONTROL
                        LogIntake(graph.repository)(LogIntakeCommand(snapshot.defaultAddMl, source, containerId = snapshot.containers.firstOrNull { it.isDefault }?.id))
                        navController.navigate("today") { launchSingleTop = true }
                    }
                    "de.stefansturm.ripple.LOG_AMOUNT" -> {
                        val amount = intent.getIntExtra("amount_ml", 0)
                        if (amount > 0) LogIntake(graph.repository)(LogIntakeCommand(Milliliters(amount), IntakeSource.INTENT))
                        navController.navigate("today") { launchSingleTop = true }
                    }
                }
            }
        }
        if (startDestination == "onboarding") {
            NavHost(navController = navController, startDestination = "onboarding", modifier = Modifier.fillMaxSize()) {
                composable("onboarding") {
                    OnboardingRoute(
                        repository = graph.repository,
                        onFinished = { navController.navigate("today") { popUpTo("onboarding") { inclusive = true } } },
                        onRequestHealthConnect = { runCatching { context.startActivity(graph.healthConnect.settingsIntent()) } }
                    )
                }
            }
        } else {
            AdaptiveShell(navController) { padding ->
                RippleNavHost(graph, navController, padding)
            }
        }
    }
}

@Composable
private fun RippleNavHost(graph: RippleAppGraph, navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "today", modifier = modifier) {
        composable("today") {
            TodayRoute(graph.repository, onOpenHistory = { navController.navigate("history") }, onOpenSettings = { navController.navigate("settings") })
        }
        composable("history") {
            HistoryRoute(graph.repository, onOpenDay = { date -> navController.navigate("day/${date}") })
        }
        composable("day/{date}", arguments = listOf(navArgument("date") { type = NavType.StringType })) { entry ->
            val date = LocalDate.parse(entry.arguments?.getString("date"))
            DayDetailRoute(graph.repository, date, onBack = { navController.popBackStack() }, onAdd = { navController.navigate("today") })
        }
        composable("stats") { StatsRoute(graph.repository) }
        composable("settings") {
            SettingsRoute(
                repository = graph.repository,
                onShare = { text, mime ->
                    val send = Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_TEXT, text) }
                    graph.context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                onOpenHealthConnect = { runCatching { graph.context.startActivity(graph.healthConnect.settingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            )
        }
    }
}

@Composable
private fun AdaptiveShell(navController: NavHostController, content: @Composable (Modifier) -> Unit) {
    val destinations = remember {
        listOf(
            RootDestination("today", R.string.nav_today, Icons.Outlined.Home),
            RootDestination("history", R.string.nav_history, Icons.Outlined.History),
            RootDestination("stats", R.string.nav_stats, Icons.Outlined.ShowChart),
            RootDestination("settings", R.string.nav_settings, Icons.Outlined.Settings)
        )
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route?.substringBefore('/')
    BoxWithConstraints(Modifier.fillMaxSize()) {
        when {
            maxWidth < 600.dp -> Scaffold(
                bottomBar = {
                    NavigationBar {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = current == destination.route,
                                onClick = { navigateRoot(navController, destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(androidx.compose.ui.res.stringResource(destination.labelRes)) }
                            )
                        }
                    }
                }
            ) { padding -> content(Modifier.padding(padding)) }
            maxWidth < 840.dp -> Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    destinations.forEach { destination ->
                        NavigationRailItem(
                            selected = current == destination.route,
                            onClick = { navigateRoot(navController, destination.route) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(androidx.compose.ui.res.stringResource(destination.labelRes)) }
                        )
                    }
                }
                content(Modifier.weight(1f))
            }
            else -> ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        Column(Modifier.padding(16.dp)) {
                            destinations.forEach { destination ->
                                NavigationDrawerItem(
                                    selected = current == destination.route,
                                    onClick = { navigateRoot(navController, destination.route) },
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    label = { Text(androidx.compose.ui.res.stringResource(destination.labelRes)) }
                                )
                            }
                        }
                    }
                }
            ) { content(Modifier) }
        }
    }
}

private fun navigateRoot(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo("today") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
