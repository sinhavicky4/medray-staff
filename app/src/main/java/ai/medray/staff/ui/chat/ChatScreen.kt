package ai.medray.staff.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.data.model.ChatMessage
import ai.medray.staff.data.model.ChatPendingAction
import ai.medray.staff.data.model.chatFieldLabel
import ai.medray.staff.data.model.extractChatText
import ai.medray.staff.data.model.formatIsoTimeLocal
import ai.medray.staff.ui.theme.*

private val ACTION_LABELS = mapOf(
    "propose_create_patient" to "Register new patient",
    "propose_register_queue_entry" to "Add to today's queue",
    "propose_record_vitals" to "Update vitals",
    "propose_book_appointment" to "Book appointment"
)

private val SUGGESTED_PROMPTS = listOf(
    "Register a new patient",
    "Add someone to today's queue",
    "Book an appointment"
)

/**
 * Full-screen AI chat assistant — mirrors the web app's /chat page
 * (ChatConversation.tsx): message bubbles, suggested prompts on an empty
 * thread, a confirm/dismiss card for any pending agent action, and a
 * bottom input row. State lives in StaffNavGraph (same pattern as every
 * other screen here), this composable is purely presentational.
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    sending: Boolean,
    pendingAction: ChatPendingAction?,
    confirming: Boolean,
    error: String?,
    onSend: () -> Unit,
    onSuggestedPrompt: (String) -> Unit,
    onConfirmAction: () -> Unit,
    onDismissAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, pendingAction, sending) {
        if (messages.isNotEmpty() || pendingAction != null) {
            listState.animateScrollToItem((messages.size + 2).coerceAtLeast(0))
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Slate50)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                item { EmptyStateBubble(onSuggestedPrompt) }
            }

            items(messages) { message ->
                val text = extractChatText(message.content)
                if (text.isNotBlank()) {
                    MessageBubble(isUser = message.role == "user", text = text, timestamp = message.timestamp)
                }
            }

            if (sending) {
                item { TypingIndicator() }
            }

            pendingAction?.let { action ->
                item {
                    PendingActionCard(
                        action = action,
                        confirming = confirming,
                        onConfirm = onConfirmAction,
                        onDismiss = onDismissAction
                    )
                }
            }

            error?.let { message ->
                item { ErrorBanner(message) }
            }
        }

        HorizontalDivider(color = Slate200)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Type a message…", color = Slate400) },
                enabled = !sending,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MedRayBluePrimary,
                    unfocusedBorderColor = Slate200
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSend,
                enabled = !sending && input.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (!sending && input.isNotBlank()) MedRayBluePrimary else Slate200,
                        CircleShape
                    )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PureWhite, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BotAvatar() {
    Surface(color = MedRayBluePrimary, shape = CircleShape, modifier = Modifier.size(28.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Forum, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun EmptyStateBubble(onSuggestedPrompt: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotAvatar()
            Surface(color = Slate100, shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)) {
                Text(
                    "Hi! Ask me to register a patient, add someone to the queue, record vitals, or book an appointment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(start = 36.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            SUGGESTED_PROMPTS.forEach { prompt ->
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    onClick = { onSuggestedPrompt(prompt) }
                ) {
                    Text(
                        prompt,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String, timestamp: String?) {
    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                if (!isUser) BotAvatar()
                Surface(
                    color = if (isUser) MedRayBluePrimary else PureWhite,
                    shape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    shadowElevation = if (isUser) 0.dp else 1.dp,
                    modifier = if (isUser) Modifier.weight(1f, fill = false) else Modifier
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) PureWhite else Slate700,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
        val time = formatIsoTimeLocal(timestamp)
        if (time.isNotBlank()) {
            Text(
                time,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = if (isUser) 0.dp else 36.dp,
                    end = if (isUser) 4.dp else 0.dp
                )
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotAvatar()
        Surface(color = Slate100, shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)) {
            Text("…", style = MaterialTheme.typography.bodyMedium, color = Slate400, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun PendingActionCard(
    action: ChatPendingAction,
    confirming: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = MedRayBlueLight,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
        modifier = Modifier.padding(start = 36.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                ACTION_LABELS[action.name] ?: action.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MedRayBlueDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            action.input.entries
                .filter { (_, v) -> v != null && v != "" }
                .forEach { (key, value) ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(chatFieldLabel(key), style = MaterialTheme.typography.labelSmall, color = Slate500)
                        Text(
                            if (value is List<*>) value.joinToString(", ") else value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800,
                            textAlign = TextAlign.End
                        )
                    }
                }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConfirm,
                    enabled = !confirming,
                    colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(if (confirming) "Saving…" else "Confirm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !confirming,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Dismiss", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = StatusErrorBg,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusErrorBorder),
        modifier = Modifier.padding(start = 36.dp)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = StatusErrorText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
