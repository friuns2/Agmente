package com.agmente.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agmente.model.ChatEntry

@Composable
fun ChatTranscript(
    entries: List<ChatEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            when (entry) {
                is ChatEntry.UserText -> UserBubble(entry.text)
                is ChatEntry.AssistantMarkdown -> AssistantBubble(entry.markdown, entry.isStreaming)
                is ChatEntry.AssistantThought -> ThoughtBubble(entry.text)
                is ChatEntry.AssistantPlan -> PlanBubble(entry.explanation, entry.steps)
                is ChatEntry.ToolCallEntry -> ToolCallBubble(
                    entry.toolName, entry.input, entry.output, entry.status
                )
                is ChatEntry.FileChangesEntry -> FileChangesBubble(entry.diff)
                is ChatEntry.SystemMessage -> SystemBubble(entry.text)
                is ChatEntry.ErrorMessage -> ErrorBubble(entry.text)
                is ChatEntry.StreamingIndicator -> StreamingIndicator()
            }
        }
    }
}
