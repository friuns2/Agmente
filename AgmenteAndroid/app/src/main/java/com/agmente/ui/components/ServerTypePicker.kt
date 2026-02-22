package com.agmente.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agmente.data.db.ServerType

@Composable
fun ServerTypePicker(
    selectedType: ServerType,
    onTypeSelected: (ServerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Server Type",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == ServerType.ACP,
                onClick = { onTypeSelected(ServerType.ACP) },
                label = { Text("ACP") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedType == ServerType.CODEX_APP_SERVER,
                onClick = { onTypeSelected(ServerType.CODEX_APP_SERVER) },
                label = { Text("Codex") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (selectedType) {
                ServerType.ACP -> "Agent Client Protocol (ACP). Connect to ACP-compatible agents like Gemini CLI, Claude Code, or Qwen."
                ServerType.CODEX_APP_SERVER -> "OpenAI Codex app-server protocol. Connect to a Codex CLI app-server instance."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
