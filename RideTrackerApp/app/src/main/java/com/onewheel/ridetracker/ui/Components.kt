package com.onewheel.ridetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onewheel.ridetracker.data.BoardEntity
import com.onewheel.ridetracker.data.Ride
import com.onewheel.ridetracker.ui.theme.LocalRideColors
import java.text.SimpleDateFormat
import java.util.Locale

fun formatDate(iso: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
    SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed!!)
} catch (e: Exception) { iso }

/** Parses a "#RRGGBB" string into a Compose Color, falling back to [fallback] if malformed. */
fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { fallback }

@Composable
fun Dot(color: Color, size: androidx.compose.ui.unit.Dp = 9.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun BoardCard(board: BoardEntity, accent: Color, modifier: Modifier = Modifier) {
    val colors = LocalRideColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(accent)
            Spacer(Modifier.width(8.dp))
            Text(board.name, style = MaterialTheme.typography.titleMedium, color = colors.text)
        }
        if (board.subtitle.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(board.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Spacer(Modifier.height(4.dp))
        Text("${board.capacityWh.toInt()} Wh pack", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
    }
}

@Composable
fun StatTile(label: String, value: String, unit: String? = null, sub: String, modifier: Modifier = Modifier) {
    val colors = LocalRideColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 12.sp, color = colors.textFaint, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalRideColors.current
    val bg = if (selected) colors.text else colors.surface
    val fg = if (selected) colors.bg else colors.textMuted
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, if (selected) colors.text else colors.border, RoundedCornerShape(999.dp))
            .clickable(indication = null, interactionSource = interactionSource, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, fontSize = 12.5.sp)
    }
}

@Composable
fun RideRow(ride: Ride, accent: Color, onDelete: () -> Unit) {
    val colors = LocalRideColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(accent, 7.dp)
                Spacer(Modifier.width(6.dp))
                Text(ride.board, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDate(ride.date) + if (ride.session.isNotBlank()) " · ${ride.session}" else "",
                    fontSize = 12.sp, color = colors.textFaint
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "${"%.1f".format(ride.miles)} mi · ${"%.1f".format(ride.whPerMi)} Wh/mi · ${"%.1f".format(ride.avgSpeed)} mph avg",
                fontSize = 12.5.sp, color = colors.text
            )
            if (ride.notes.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(ride.notes, fontSize = 11.5.sp, color = colors.textMuted, maxLines = 1)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Delete ride", tint = colors.textFaint, modifier = Modifier.size(16.dp))
        }
    }
}
