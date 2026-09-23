package com.onewheel.ridetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.onewheel.ridetracker.claude.ClaudeClient
import com.onewheel.ridetracker.claude.ClaudeResult
import com.onewheel.ridetracker.claude.ClaudeSettings
import com.onewheel.ridetracker.claude.RidePromptBuilder
import com.onewheel.ridetracker.data.BoardEntity
import com.onewheel.ridetracker.data.Ride
import com.onewheel.ridetracker.ui.theme.LocalRideColors
import kotlinx.coroutines.launch

private data class ChatTurn(val question: String, val answer: String?, val error: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskClaudeSheet(
    boards: List<BoardEntity>,
    rides: List<Ride>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalRideColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var storedKey by remember { mutableStateOf(ClaudeSettings.getApiKey(context)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Ask Claude", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            if (storedKey == null) {
                ApiKeyEntry(
                    onSaved = { key ->
                        ClaudeSettings.setApiKey(context, key)
                        storedKey = key
                    }
                )
            } else {
                ChatArea(
                    apiKey = storedKey!!,
                    boards = boards,
                    rides = rides,
                    scope = scope,
                    onClearKey = {
                        ClaudeSettings.clearApiKey(context)
                        storedKey = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiKeyEntry(onSaved: (String) -> Unit) {
    var keyText by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }

    Text(
        "This uses your own Anthropic API key, billed to your Anthropic Console account — separate " +
            "from a claude.ai subscription. Your key is stored only on this device and is sent only to " +
            "api.anthropic.com when you ask a question. Everything else in this app works fully offline.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = keyText,
        onValueChange = { keyText = it },
        label = { Text("Anthropic API key") },
        placeholder = { Text("sk-ant-...") },
        singleLine = true,
        // Plain text (not KeyboardType.Password) — some keyboards block long-press paste on
        // password-flagged fields. We hide the value visually instead via visualTransformation.
        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        trailingIcon = {
            IconButton(onClick = { keyVisible = !keyVisible }) {
                Icon(
                    if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (keyVisible) "Hide key" else "Show key"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Get a key at console.anthropic.com (API Keys). This is a different site/account than claude.ai.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { if (keyText.isNotBlank()) onSaved(keyText.trim()) },
        enabled = keyText.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save key")
    }
}

@Composable
private fun ChatArea(
    apiKey: String,
    boards: List<BoardEntity>,
    rides: List<Ride>,
    scope: kotlinx.coroutines.CoroutineScope,
    onClearKey: () -> Unit
) {
    val turns = remember { mutableStateListOf<ChatTurn>() }
    // Raw (role, text) history actually sent to the API — first user turn carries the full
    // ride/board data dump, follow-ups are just the plain question, since the API is stateless
    // and we resend this whole list each call.
    val apiHistory = remember { mutableStateListOf<Pair<String, String>>() }
    var question by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(turns.size, turns.lastOrNull()?.answer, turns.lastOrNull()?.error) {
        if (turns.isNotEmpty()) scrollState.animateScrollTo(scrollState.maxValue)
    }

    fun send(q: String) {
        if (q.isBlank() || sending) return
        sending = true
        val promptForApi = if (apiHistory.isEmpty()) {
            RidePromptBuilder.build(boards, rides, q)
        } else {
            q
        }
        apiHistory.add("user" to promptForApi)
        val turnIndex = turns.size
        turns.add(ChatTurn(question = q, answer = null))
        scope.launch {
            when (val result = ClaudeClient.sendConversation(apiKey, apiHistory.toList())) {
                is ClaudeResult.Success -> {
                    apiHistory.add("assistant" to result.text)
                    turns[turnIndex] = turns[turnIndex].copy(answer = result.text)
                }
                is ClaudeResult.Failure -> {
                    // Drop the failed turn from history sent back to the API next time.
                    apiHistory.removeAt(apiHistory.lastIndex)
                    turns[turnIndex] = turns[turnIndex].copy(error = result.message)
                }
            }
            sending = false
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Uses your rides + boards as context. Key stored on-device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClearKey) { Text("Change key") }
    }
    Spacer(Modifier.height(8.dp))

    if (turns.isEmpty()) {
        OutlinedButton(
            onClick = { send("Give me an overview of my riding efficiency trends and any suggestions.") },
            enabled = !sending,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✨ Analyze my rides")
        }
        Spacer(Modifier.height(12.dp))
    }

    if (turns.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(scrollState)
        ) {
            turns.forEach { turn ->
                Text(
                    turn.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                when {
                    turn.answer != null -> Text(turn.answer, style = MaterialTheme.typography.bodyMedium)
                    turn.error != null -> Text(
                        "Error: ${turn.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> Text(
                        "Thinking…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(if (turns.isEmpty()) "Ask a question" else "Ask a follow-up") },
            placeholder = { Text("e.g. Is my XRV efficiency getting worse in the cold?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                val q = question; question = ""; send(q)
            }),
            enabled = !sending,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = { val q = question; question = ""; send(q) },
            enabled = !sending && question.isNotBlank()
        ) {
            if (sending) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }

    if (rides.isEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            "You don't have any rides logged yet — Claude will only be able to talk about your boards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
