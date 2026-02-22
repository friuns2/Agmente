package com.agmente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agmente.ui.screens.*
import com.agmente.ui.theme.AgmenteTheme
import com.agmente.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgmenteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AgmenteNavHost()
                }
            }
        }
    }
}

@Composable
fun AgmenteNavHost() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            ContentScreen(
                appViewModel = appViewModel,
                onAddServer = { navController.navigate("add_server") },
                onEditServer = { navController.navigate("edit_server/$it") },
                onOpenSession = { sessionId ->
                    navController.navigate("session/$sessionId")
                },
                onSettings = { navController.navigate("settings") }
            )
        }

        composable("add_server") {
            AddServerScreen(
                onSave = { config ->
                    appViewModel.addServer(config)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("edit_server/{serverId}") { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: return@composable
            val servers by appViewModel.servers.collectAsState()
            val server = servers.find { it.id == serverId }
            if (server != null) {
                AddServerScreen(
                    existingServer = server,
                    onSave = { config ->
                        appViewModel.updateServer(config)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("session/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val currentVm = appViewModel.selectedServerViewModel.collectAsState().value
            if (currentVm != null) {
                currentVm.openSession(sessionId)
                val sessionVm = currentVm.currentSessionViewModel
                if (sessionVm != null) {
                    SessionDetailScreen(
                        serverViewModel = currentVm,
                        sessionViewModel = sessionVm,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composable("settings") {
            val devMode by appViewModel.devMode.collectAsState()
            SettingsScreen(
                devMode = devMode,
                onToggleDevMode = { appViewModel.toggleDevMode() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
