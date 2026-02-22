package com.agmente.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agmente.data.db.ServerType
import com.agmente.model.ServerConfiguration
import com.agmente.ui.components.ServerTypePicker
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    onSave: (ServerConfiguration) -> Unit,
    onBack: () -> Unit,
    existingServer: ServerConfiguration? = null
) {
    var name by remember { mutableStateOf(existingServer?.name ?: "") }
    var serverType by remember { mutableStateOf(existingServer?.serverType ?: ServerType.ACP) }
    var scheme by remember { mutableStateOf(existingServer?.scheme ?: "ws") }
    var host by remember { mutableStateOf(existingServer?.host ?: "") }
    var token by remember { mutableStateOf(existingServer?.token ?: "") }
    var workingDirectory by remember { mutableStateOf(existingServer?.workingDirectory ?: "") }
    var cfAccessClientId by remember { mutableStateOf(existingServer?.cfAccessClientId ?: "") }
    var cfAccessClientSecret by remember { mutableStateOf(existingServer?.cfAccessClientSecret ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }

    val isEditing = existingServer != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Server" else "Add Server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val config = ServerConfiguration(
                                id = existingServer?.id ?: UUID.randomUUID().toString(),
                                name = name.ifEmpty { host },
                                scheme = scheme,
                                host = host,
                                token = token,
                                cfAccessClientId = cfAccessClientId,
                                cfAccessClientSecret = cfAccessClientSecret,
                                workingDirectory = workingDirectory.trim(),
                                serverType = serverType
                            )
                            onSave(config)
                        },
                        enabled = host.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ServerTypePicker(
                selectedType = serverType,
                onTypeSelected = { serverType = it }
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Server Name") },
                placeholder = { Text("My Agent") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = scheme,
                    onValueChange = { scheme = it },
                    label = { Text("Scheme") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    placeholder = { Text("localhost:8765") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Bearer Token (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = workingDirectory,
                onValueChange = { workingDirectory = it },
                label = { Text("Working Directory (optional)") },
                placeholder = { Text("/path/to/workspace") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide Advanced" else "Show Advanced")
            }

            if (showAdvanced) {
                OutlinedTextField(
                    value = cfAccessClientId,
                    onValueChange = { cfAccessClientId = it },
                    label = { Text("CF Access Client ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = cfAccessClientSecret,
                    onValueChange = { cfAccessClientSecret = it },
                    label = { Text("CF Access Client Secret") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
