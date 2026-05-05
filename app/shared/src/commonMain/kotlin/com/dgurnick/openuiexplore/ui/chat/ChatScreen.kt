package com.dgurnick.openuiexplore.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dgurnick.openuiexplore.data.model.ChatMessage
import com.dgurnick.openuiexplore.data.model.Role
import com.dgurnick.openuiexplore.presentation.chat.ChatState
import com.dgurnick.openuiexplore.presentation.chat.QuickAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
        state: ChatState,
        onSend: (String) -> Unit,
        onDismissError: () -> Unit,
        onReset: () -> Unit = {},
        onQuickAction: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Raiffeisen Kosovo") },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color(0xFFFFCC00),
                                        titleContentColor = Color.Black,
                                        actionIconContentColor = Color.Black
                                ),
                        actions = {
                            IconButton(onClick = onReset) {
                                Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Start over"
                                )
                            }
                        }
                )
            }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
            ) { items(state.messages, key = { it.id }) { message -> MessageBubble(message) } }

            if (state.quickActions.isNotEmpty()) {
                QuickActionRow(actions = state.quickActions, onAction = onQuickAction)
            }

            state.error?.let { error ->
                Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            ChatInput(enabled = !state.isStreaming, onSend = onSend)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionRow(actions: List<QuickAction>, onAction: (String) -> Unit) {
    FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        actions.forEach { action ->
            AssistChip(
                    onClick = { onAction(action.prompt) },
                    label = { Text(action.label, style = MaterialTheme.typography.labelMedium) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFF8DC))
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER

    if (!isUser) {
        val htmlBody = if (!message.isStreaming) extractHtml(message.content) else null
        if (htmlBody != null || message.isStreaming) {
            Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                HtmlView(
                        html = htmlBody ?: "",
                        isStreaming = message.isStreaming,
                        modifier = Modifier.fillMaxWidth()
                )
            }
            return
        }
    }

    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
                shape =
                        RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                color =
                        if (isUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                    text = message.content + if (message.isStreaming) " |" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                            if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun ChatInput(enabled: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(shadowElevation = 4.dp) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Describe a UI component...") },
                    enabled = enabled,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSend(text.trim())
                            text = ""
                        }
                    },
                    enabled = enabled && text.isNotBlank()
            ) { Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
        }
    }
}
