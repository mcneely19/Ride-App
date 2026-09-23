package com.onewheel.ridetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onewheel.ridetracker.RideViewModel
import com.onewheel.ridetracker.data.BOARD_COLOR_PALETTE
import com.onewheel.ridetracker.data.BoardEntity
import com.onewheel.ridetracker.ui.theme.LocalRideColors

/** The shared add/edit fields — used inside a bottom sheet (Manage Boards) and full-screen (first run). */
@Composable
fun BoardEditorForm(
    initial: BoardEntity?,
    existingNames: Set<String>,
    suggestedColorHex: String,
    onCancel: (() -> Unit)?,
    onSave: (BoardEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val colors = LocalRideColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var capacity by remember {
        mutableStateOf(
            initial?.capacityWh?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var subtitle by remember { mutableStateOf(initial?.subtitle ?: "") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: suggestedColorHex) }
    var error by remember { mutableStateOf<String?>(null) }
    val isEditingName = initial != null

    Column(Modifier.fillMaxWidth()) {
        Text(
            if (initial == null) "Add a board" else "Edit board",
            style = MaterialTheme.typography.titleMedium, color = colors.text
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.uppercase() },
            label = { Text("Board name") },
            singleLine = true,
            enabled = !isEditingName,
            modifier = Modifier.fillMaxWidth()
        )
        if (isEditingName) {
            Spacer(Modifier.height(4.dp))
            Text("Name can't be changed once rides are logged to it.", fontSize = 11.sp, color = colors.textFaint)
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = capacity,
            onValueChange = { capacity = it },
            label = { Text("Total battery capacity (Wh)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = subtitle,
            onValueChange = { subtitle = it },
            label = { Text("Subtitle (optional) — cells, motor, etc.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        Text("Color", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BOARD_COLOR_PALETTE.forEach { hex ->
                val swatch = parseHexColor(hex)
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(if (colorHex == hex) 3.dp else 0.dp, colors.text, CircleShape)
                        .clickable { colorHex = hex }
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = colors.bad, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete", color = colors.bad) }
            }
            Spacer(Modifier.weight(1f))
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
            }
            Button(onClick = {
                val cap = capacity.toDoubleOrNull()
                val trimmedName = name.trim()
                when {
                    trimmedName.isBlank() -> error = "Enter a board name."
                    !isEditingName && trimmedName in existingNames -> error = "A board named \"$trimmedName\" already exists."
                    cap == null || cap <= 0 -> error = "Enter a positive battery capacity in Wh."
                    else -> onSave(BoardEntity(trimmedName, cap, subtitle.trim(), colorHex))
                }
            }) { Text("Save") }
        }
    }
}

/** Add-or-edit-one-board bottom sheet, used from Manage Boards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardEditorSheet(
    initial: BoardEntity?,
    existingNames: Set<String>,
    suggestedColorHex: String,
    onDismiss: () -> Unit,
    onSave: (BoardEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val colors = LocalRideColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surface) {
        Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            BoardEditorForm(
                initial = initial,
                existingNames = existingNames,
                suggestedColorHex = suggestedColorHex,
                onCancel = onDismiss,
                onSave = onSave,
                onDelete = onDelete
            )
        }
    }
}

/** Lists all configured boards; tap one to edit/delete it, or add a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBoardsSheet(vm: RideViewModel, onDismiss: () -> Unit) {
    val colors = LocalRideColors.current
    val boards by vm.boards.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editing by remember { mutableStateOf<BoardEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun nextSuggestedColor(): String {
        val used = boards.map { it.colorHex }.toSet()
        return BOARD_COLOR_PALETTE.firstOrNull { it !in used } ?: BOARD_COLOR_PALETTE.first()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Manage boards", style = MaterialTheme.typography.titleMedium, color = colors.text)
                TextButton(onClick = { creating = true }) { Text("+ Add board") }
            }
            Spacer(Modifier.height(10.dp))

            if (boards.isEmpty()) {
                Text("No boards yet — add one to start logging rides.", color = colors.textFaint, modifier = Modifier.padding(vertical = 12.dp))
            }

            boards.forEach { b ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editing = b }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dot(parseHexColor(b.colorHex), 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.name, color = colors.text, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${b.capacityWh.toInt()} Wh" + if (b.subtitle.isNotBlank()) " · ${b.subtitle}" else "",
                            fontSize = 12.sp, color = colors.textMuted
                        )
                    }
                    Text("Edit", fontSize = 12.sp, color = colors.textFaint)
                }
                if (b != boards.last()) HorizontalDivider(color = colors.border)
            }
        }
    }

    if (creating) {
        BoardEditorSheet(
            initial = null,
            existingNames = boards.map { it.name }.toSet(),
            suggestedColorHex = nextSuggestedColor(),
            onDismiss = { creating = false },
            onSave = { board -> vm.saveBoard(board); creating = false }
        )
    }

    editing?.let { board ->
        BoardEditorSheet(
            initial = board,
            existingNames = boards.map { it.name }.toSet(),
            suggestedColorHex = board.colorHex,
            onDismiss = { editing = null },
            onSave = { updated -> vm.saveBoard(updated); editing = null },
            onDelete = { vm.deleteBoard(board.name); editing = null }
        )
    }
}

/** Shown full-screen when there are no boards configured yet (first run, or a fresh setup for someone else). */
@Composable
fun FirstRunSetupScreen(vm: RideViewModel) {
    val colors = LocalRideColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Text("Welcome to Ride Telemetry", style = MaterialTheme.typography.headlineSmall, color = colors.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "Let's set up your first board — name it, tell the app its total battery capacity, and pick a color. You can add more boards or edit this one anytime from Manage Boards.",
            style = MaterialTheme.typography.bodyMedium, color = colors.textMuted
        )
        Spacer(Modifier.height(24.dp))
        BoardEditorForm(
            initial = null,
            existingNames = emptySet(),
            suggestedColorHex = BOARD_COLOR_PALETTE.first(),
            onCancel = null,
            onSave = { board -> vm.saveBoard(board) }
        )
    }
}
