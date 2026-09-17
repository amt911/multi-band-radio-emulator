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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.multibandradioemulator.navigation.BottomNavItem
import com.example.multibandradioemulator.ui.screens.AntennaInfoScreen
import com.example.multibandradioemulator.ui.screens.HomeScreen
import com.example.multibandradioemulator.ui.screens.OptionsScreen
import com.example.multibandradioemulator.ui.theme.MultiBandRadioEmulatorTheme

/** Root composable's Compose test tag, exposed as the Android view resourceId (see [MainApp]). */
const val MAIN_SCAFFOLD_TEST_TAG = "main_scaffold"

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Persisted settings
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var showGraphs by remember { mutableStateOf(prefs.getBoolean("show_graphs", true)) }
    LaunchedEffect(showGraphs) {
        prefs.edit().putBoolean("show_graphs", showGraphs).apply()
    }

    Scaffold(
        // Exposes Compose testTag as the Android view resourceId, so Maestro (which reads the
        // view hierarchy via UiAutomator, not Compose semantics) can select on `id:`.
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(MAIN_SCAFFOLD_TEST_TAG),
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
                HomeScreen(showGraphs = showGraphs)
            }
            composable(BottomNavItem.Info.route) {
                AntennaInfoScreen()
            }
            composable(BottomNavItem.Options.route) {
                OptionsScreen(
                    showGraphs = showGraphs,
                    onShowGraphsChanged = { showGraphs = it }
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