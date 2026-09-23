package com.onewheel.ridetracker.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onewheel.ridetracker.RideViewModel
import com.onewheel.ridetracker.data.BoardEntity
import com.onewheel.ridetracker.ocr.OcrGuess
import com.onewheel.ridetracker.ocr.RideTextParser
import com.onewheel.ridetracker.ocr.recognizeText
import com.onewheel.ridetracker.ui.theme.LocalRideColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(vm: RideViewModel = viewModel()) {
    val colors = LocalRideColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allRides by vm.allRides.collectAsState()
    val visibleRides by vm.visibleRides.collectAsState()
    val boardFilter by vm.boardFilter.collectAsState()
    val showTrendlines by vm.showTrendlines.collectAsState()
    val importMessage by vm.importMessage.collectAsState()
    val boardMessage by vm.boardMessage.collectAsState()
    val boards by vm.boards.collectAsState()
    val boardsLoaded by vm.boardsLoaded.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showManageBoards by remember { mutableStateOf(false) }
    var showAskClaude by remember { mutableStateOf(false) }
    var ocrPrefill by remember { mutableStateOf<OcrGuess?>(null) }
    var scanning by remember { mutableStateOf(false) }

    fun colorFor(boardName: String): Color =
        boards.find { it.name == boardName }?.let { parseHexColor(it.colorHex, colors.textFaint) } ?: colors.textFaint

    LaunchedEffect(importMessage) {
        importMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.clearImportMessage()
        }
    }

    LaunchedEffect(boardMessage) {
        boardMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.clearBoardMessage()
        }
    }

    // No boards set up yet — show the setup screen instead of the (empty) dashboard.
    if (boardsLoaded && boards.isEmpty()) {
        FirstRunSetupScreen(vm)
        return
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanning = true
        scope.launch {
            try {
                val text = recognizeText(context, uri)
                ocrPrefill = RideTextParser.parse(text)
                showAddSheet = true
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't read that screenshot: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                scanning = false
            }
        }
    }

    val jsonPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = readTextFromUri(context, uri)
                vm.importRidesFromJson(text)
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't open that file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text("VESC BATTERY & RIDE LOG", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
                Spacer(Modifier.height(4.dp))
                Text("Ride Telemetry", style = MaterialTheme.typography.headlineSmall, color = colors.text)
            }
            TextButton(onClick = { showManageBoards = true }) { Text("⚙ Boards") }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Efficiency and range across every logged ride. Speed Efficiency tracks speed against consumption; Temperature Efficiency tracks ambient temperature against efficiency.",
            style = MaterialTheme.typography.bodySmall, color = colors.textMuted
        )

        Spacer(Modifier.height(18.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(boards) { b ->
                BoardCard(b, parseHexColor(b.colorHex, colors.textFaint), Modifier.width(200.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip("All rides", boardFilter == null) { vm.setBoardFilter(null) }
            boards.forEach { b ->
                FilterChip(b.name, boardFilter == b.name) { vm.setBoardFilter(b.name) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(if (showTrendlines) "Trendlines: on" else "Trendlines: off", selected = false) { vm.toggleTrendlines() }
            Button(onClick = { ocrPrefill = null; showAddSheet = true }) { Text("+ Log a ride") }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !scanning,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (scanning) "Scanning…" else "📷 From screenshot")
            }
            OutlinedButton(
                onClick = { jsonPicker.launch("application/json") },
                modifier = Modifier.weight(1f)
            ) {
                Text("📄 Import JSON")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showAskClaude = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🤖 Ask Claude")
        }

        Spacer(Modifier.height(16.dp))
        val totalMiles = visibleRides.sumOf { it.miles }
        val totalWh = visibleRides.sumOf { it.whUsed }
        val avgWhMi = if (totalMiles > 0) totalWh / totalMiles else 0.0
        val best = visibleRides.minByOrNull { it.whPerMi }
        val mostRecent = visibleRides.firstOrNull()

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Rides logged", visibleRides.size.toString(), sub = "${"%.1f".format(totalMiles)} total miles", modifier = Modifier.weight(1f))
            StatTile("Average efficiency", "%.1f".format(avgWhMi), sub = "Wh per mile", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                "Best efficiency",
                best?.let { "%.1f".format(it.whPerMi) } ?: "–",
                unit = "Wh/mi",
                sub = best?.let { "${it.board} · ${formatDate(it.date)}" } ?: "",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                "Most recent",
                mostRecent?.let { "%.1f mi".format(it.miles) } ?: "–",
                sub = mostRecent?.let { "${it.board} · ${formatDate(it.date)}" } ?: "",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        ChartCard(
            title = "Speed Efficiency",
            desc = "Efficiency (Wh/mi) vs. average speed (mph) — drag rises faster than speed.",
            rides = visibleRides,
            boards = boards,
            colorOf = ::colorFor,
            xOf = { it.avgSpeed },
            xDomain = 7.0..21.0,
            xTicks = listOf(8.0, 12.0, 16.0, 20.0),
            xUnitLabel = " mph",
            showTrendlines = showTrendlines
        )
        Spacer(Modifier.height(12.dp))
        ChartCard(
            title = "Temperature Efficiency",
            desc = "Efficiency (Wh/mi) vs. ambient temperature (°F) — cold air taxes range.",
            rides = visibleRides,
            boards = boards,
            colorOf = ::colorFor,
            xOf = { it.temp?.toDouble() ?: 0.0 },
            xDomain = 35.0..90.0,
            xTicks = listOf(40.0, 55.0, 70.0, 85.0),
            xUnitLabel = "°F",
            showTrendlines = showTrendlines
        )

        Spacer(Modifier.height(18.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ride log", style = MaterialTheme.typography.titleMedium, color = colors.text)
                    Text("${visibleRides.size} rides", fontSize = 11.sp, color = colors.textFaint)
                }
                Spacer(Modifier.height(6.dp))
                visibleRides.forEachIndexed { index, ride ->
                    RideRow(ride, colorFor(ride.board)) { vm.deleteRide(ride.id) }
                    if (index != visibleRides.lastIndex) {
                        HorizontalDivider(color = colors.border)
                    }
                }
                if (visibleRides.isEmpty()) {
                    Text("No rides match this filter.", color = colors.textFaint, modifier = Modifier.padding(vertical = 16.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddSheet) {
        AddRideSheet(
            boards = boards,
            onDismiss = { showAddSheet = false; ocrPrefill = null },
            onSave = { input -> vm.addRide(input); showAddSheet = false; ocrPrefill = null },
            prefill = ocrPrefill
        )
    }

    if (showManageBoards) {
        ManageBoardsSheet(vm = vm, onDismiss = { showManageBoards = false })
    }

    if (showAskClaude) {
        AskClaudeSheet(
            boards = boards,
            rides = allRides,
            onDismiss = { showAskClaude = false }
        )
    }
}

private suspend fun readTextFromUri(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: throw IllegalStateException("Could not open that file.")

@Composable
private fun ChartCard(
    title: String,
    desc: String,
    rides: List<com.onewheel.ridetracker.data.Ride>,
    boards: List<BoardEntity>,
    colorOf: (String) -> Color,
    xOf: (com.onewheel.ridetracker.data.Ride) -> Double,
    xDomain: ClosedFloatingPointRange<Double>,
    xTicks: List<Double>,
    xUnitLabel: String,
    showTrendlines: Boolean
) {
    val colors = LocalRideColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, shape = RoundedCornerShape(14.dp))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text)
        Spacer(Modifier.height(3.dp))
        Text(desc, fontSize = 11.5.sp, color = colors.textMuted)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            boards.forEachIndexed { index, b ->
                if (index > 0) Spacer(Modifier.width(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(colorOf(b.name), 8.dp); Spacer(Modifier.width(4.dp)); Text(b.name, fontSize = 11.sp, color = colors.textMuted)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        EfficiencyScatterChart(
            rides = rides, xOf = xOf, xDomain = xDomain, xTicks = xTicks,
            xUnitLabel = xUnitLabel, showTrendlines = showTrendlines, colorOf = colorOf
        )
    }
}
