package com.onewheel.ridetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onewheel.ridetracker.NewRideInput
import com.onewheel.ridetracker.data.Board
import com.onewheel.ridetracker.ui.theme.LocalRideColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRideSheet(onDismiss: () -> Unit, onSave: (NewRideInput) -> Unit) {
    val colors = LocalRideColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var board by remember { mutableStateOf(Board.XRV) }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var wh by remember { mutableStateOf("") }
    var miles by remember { mutableStateOf("") }
    var avg by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var psi by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val whD = wh.toDoubleOrNull()
    val milesD = miles.toDoubleOrNull()
    val computed = if (whD != null && whD > 0 && milesD != null && milesD > 0) {
        val whMi = whD / milesD
        val range = board.capacityWh / whMi
        val pct = whD / board.capacityWh * 100
        "%.2f Wh/mi · %.1f mi estimated range · %.1f%% of pack used".format(whMi, range, pct)
    } else "Wh/mi and estimated range appear once Wh used and miles are set."

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Log a ride", style = MaterialTheme.typography.titleMedium, color = colors.text)
            Spacer(Modifier.height(4.dp))
            Text(
                "Wh/mi and estimated range are calculated automatically from the pack size.",
                style = MaterialTheme.typography.bodySmall, color = colors.textMuted
            )
            Spacer(Modifier.height(16.dp))

            Text("Board", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Board.values().forEach { b ->
                    FilterChip(label = "${b.label} — ${b.capacityWh.toInt()} Wh", selected = board == b, onClick = { board = b })
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = date, onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = wh, onValueChange = { wh = it }, label = { Text("Wh used") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = miles, onValueChange = { miles = it }, label = { Text("Miles") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = avg, onValueChange = { avg = it }, label = { Text("Avg mph") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = max, onValueChange = { max = it }, label = { Text("Max mph") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = temp, onValueChange = { temp = it }, label = { Text("Ambient °F") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = psi, onValueChange = { psi = it }, label = { Text("Tire PSI (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = notes, onValueChange = { notes = it }, label = { Text("Notes") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Text(computed, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)

            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = colors.bad, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val tempI = temp.toIntOrNull()
                    if (whD == null || whD <= 0 || milesD == null || milesD <= 0 ||
                        avg.toDoubleOrNull() == null || max.toDoubleOrNull() == null || tempI == null
                    ) {
                        error = "Fill in every required field with a positive number."
                        return@Button
                    }
                    onSave(
                        NewRideInput(
                            board = board.name,
                            date = date,
                            whUsed = whD,
                            miles = milesD,
                            avgSpeed = avg.toDouble(),
                            maxSpeed = max.toDouble(),
                            temp = tempI,
                            psi = psi.toDoubleOrNull(),
                            notes = notes.trim()
                        )
                    )
                }) { Text("Save ride") }
            }
        }
    }
}
