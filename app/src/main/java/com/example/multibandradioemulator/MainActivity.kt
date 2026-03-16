package com.example.multibandradioemulator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.multibandradioemulator.audio.RadioSignalPlayer
import com.example.multibandradioemulator.navigation.BottomNavItem
import com.example.multibandradioemulator.ui.screens.AntennaInfoScreen
import com.example.multibandradioemulator.ui.screens.HomeScreen
import com.example.multibandradioemulator.ui.screens.OptionsScreen
import com.example.multibandradioemulator.ui.theme.MultiBandRadioEmulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiBandRadioEmulatorTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Persisted settings
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var showGraphs by remember { mutableStateOf(prefs.getBoolean("show_graphs", true)) }
    var signalBoost by remember {
        mutableStateOf(prefs.getFloat("signal_boost", RadioSignalPlayer.DEFAULT_GAIN))
    }
    LaunchedEffect(showGraphs) {
        prefs.edit().putBoolean("show_graphs", showGraphs).apply()
    }
    LaunchedEffect(signalBoost) {
        prefs.edit().putFloat("signal_boost", signalBoost).apply()
    }

    // Shared player — lives at app level so playback survives navigation
    val player = remember { RadioSignalPlayer() }
    var isPlaying by remember { mutableStateOf(false) }

    // Update gain dynamically without restarting playback
    LaunchedEffect(signalBoost) {
        player.gain = signalBoost
    }

    // Release player when the activity leaves composition
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                BottomNavItem.items.forEach { item ->
                    val selected = currentDestination?.route == item.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.labelRes)
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    player = player,
                    isPlaying = isPlaying,
                    onPlayingChanged = { isPlaying = it },
                    showGraphs = showGraphs
                )
            }
            composable(BottomNavItem.Info.route) {
                AntennaInfoScreen()
            }
            composable(BottomNavItem.Options.route) {
                OptionsScreen(
                    showGraphs = showGraphs,
                    onShowGraphsChanged = { showGraphs = it },
                    signalBoost = signalBoost,
                    onSignalBoostChanged = { signalBoost = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    MultiBandRadioEmulatorTheme {
        MainApp()
    }
}
