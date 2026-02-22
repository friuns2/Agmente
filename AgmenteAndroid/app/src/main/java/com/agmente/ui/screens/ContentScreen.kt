package com.agmente.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.SessionSummary
import com.agmente.model.ServerConfiguration
import com.agmente.ui.theme.AccentBlue
import com.agmente.ui.theme.AccentGreen
import com.agmente.ui.theme.AccentRed
import com.agmente.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentScreen(
    appViewModel: AppViewModel,
    onAddServer: () -> Unit,
    onEditServer: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onSettings: () -> Unit
) {
    val servers by appViewModel.servers.collectAsState()
    val selectedServerId by appViewModel.selectedServerId.collectAsState()
    val serverVm by appViewModel.selectedServerViewModel.collectAsState()

    val connectionState = serverVm?.connectionState?.collectAsState()
    val isInitialized = serverVm?.isInitialized?.collectAsState()
    val sessions = serverVm?.sessionSummaries?.collectAsState()
    val agentInfo = serverVm?.agentInfo?.collectAsState()
    val selectedSessionId = serverVm?.selectedSessionId?.collectAsState()

    android.util.Log.d("ContentScreen", "Compose: serverVm=${serverVm != null}, connState=${connectionState?.value}, init=${isInitialized?.value}")

    var showServerPicker by remember { mutableStateOf(false) }
    var lastNavigatedSessionId by remember { mutableStateOf<String?>(null) }

    val currentSessionId = selectedSessionId?.value
    LaunchedEffect(currentSessionId) {
        if (currentSessionId != null && currentSessionId != lastNavigatedSessionId) {
            lastNavigatedSessionId = currentSessionId
            onOpenSession(currentSessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val selectedServer = servers.find { it.id == selectedServerId }
                    Column {
                        Text(
                            text = selectedServer?.name ?: "Agmente",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (agentInfo?.value != null) {
                            Text(
                                text = serverVm?.initializationSummary ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (servers.size > 1) {
                        IconButton(onClick = { showServerPicker = true }) {
                            Icon(Icons.Default.Menu, "Server list")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onAddServer) {
                        Icon(Icons.Default.Dns, "Add server")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isInitialized?.value == true) {
                ExtendedFloatingActionButton(
                    onClick = { serverVm?.sendNewSession() },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New Chat") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (servers.isEmpty()) {
                EmptyServerState(onAddServer)
            } else {
                ConnectionBar(
                    connectionState = connectionState?.value ?: ACPConnectionState.Disconnected,
                    isInitialized = isInitialized?.value ?: false,
                    onConnect = { appViewModel.connectSelectedServer() },
                    onDisconnect = { appViewModel.disconnectSelectedServer() }
                )

                if (isInitialized?.value == true) {
                    val sessionList = sessions?.value ?: emptyList()
                    if (sessionList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No sessions yet. Tap + to create one.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        SessionList(
                            sessions = sessionList,
                            onSessionClick = { onOpenSession(it) },
                            onDeleteSession = { serverVm?.deleteSession(it) }
                        )
                    }
                }
            }
        }
    }

    if (showServerPicker) {
        ServerPickerDialog(
            servers = servers,
            selectedId = selectedServerId,
            onSelect = {
                appViewModel.selectServer(it)
                showServerPicker = false
            },
            onDismiss = { showServerPicker = false }
        )
    }
}

@Composable
fun EmptyServerState(onAddServer: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No servers configured",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add a server to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddServer) {
                Text("Add Server")
            }
        }
    }
}

@Composable
fun ConnectionBar(
    connectionState: ACPConnectionState,
    isInitialized: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusText, statusColor) = when (connectionState) {
                    is ACPConnectionState.Connected ->
                        if (isInitialized) "Initialized" to AccentGreen
                        else "Connected" to AccentBlue
                    is ACPConnectionState.Connecting -> "Connecting..." to AccentBlue
                    is ACPConnectionState.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.onSurfaceVariant
                    is ACPConnectionState.Failed -> "Failed: ${connectionState.error.localizedMessage?.take(40) ?: "unknown"}" to AccentRed
                }
                Icon(
                    Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
            }

            when (connectionState) {
                is ACPConnectionState.Disconnected, is ACPConnectionState.Failed -> {
                    TextButton(onClick = onConnect) { Text("Connect") }
                }
                is ACPConnectionState.Connected -> {
                    if (!isInitialized) {
                        TextButton(onClick = onConnect) { Text("Initialize") }
                    } else {
                        TextButton(onClick = onDisconnect) { Text("Disconnect") }
                    }
                }
                else -> { }
            }
        }
    }
}

@Composable
fun SessionList(
    sessions: List<SessionSummary>,
    onSessionClick: (String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            ListItem(
                headlineContent = {
                    Text(
                        text = session.title ?: session.id.take(16),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    val details = buildList {
                        session.cwd?.let { add(it) }
                        session.updatedAt?.let { add(dateFormat.format(it)) }
                    }
                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" | "),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                trailingContent = {
                    IconButton(onClick = { onDeleteSession(session.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.clickable { onSessionClick(session.id) }
            )
        }
    }
}

@Composable
fun ServerPickerDialog(
    servers: List<ServerConfiguration>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Server") },
        text = {
            LazyColumn {
                items(servers) { server ->
                    ListItem(
                        headlineContent = { Text(server.name.ifEmpty { server.host }) },
                        supportingContent = { Text(server.endpointURLString) },
                        leadingContent = {
                            RadioButton(
                                selected = server.id == selectedId,
                                onClick = { onSelect(server.id) }
                            )
                        },
                        modifier = Modifier.clickable { onSelect(server.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
