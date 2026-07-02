package com.whofi.vitalfi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.whofi.vitalfi.VitalFiUiState

private val BgDark = Color(0xFF0A0F0A)
private val Cyan = Color(0xFF00FFCC)

@Composable
fun RadarPanel(
    state: VitalFiUiState,
    modifier: Modifier = Modifier,
    onVictimClick: (Int) -> Unit,
    onViewportChange: (RadarViewport) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onViewAzimLeft: () -> Unit,
    onViewAzimRight: () -> Unit,
    onViewElevUp: () -> Unit,
    onViewElevDown: () -> Unit,
    onResetView3D: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RadarToolbar(
            viewport = state.radarViewport,
            isFullscreen = false,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onResetView = onResetView,
            onToggleFullscreen = onToggleFullscreen,
        )
        if (state.showRadar3D) {
            Radar3DView(
                bearingDeg = state.bearingDeg,
                victims = state.victims,
                selectedVictimId = state.selectedVictim?.id,
                heatmap = state.heatmap,
                trail = state.trail,
                viewAzim = state.viewAzim,
                viewElev = state.viewElev,
                viewport = state.radarViewport,
                onVictimClick = onVictimClick,
                onViewportChange = onViewportChange,
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onViewAzimLeft) { Text("◀ Q", color = Cyan) }
                TextButton(onClick = onViewElevUp) { Text("▲ W", color = Cyan) }
                TextButton(onClick = onResetView3D) { Text("V reset", color = Color.Gray) }
                TextButton(onClick = onViewElevDown) { Text("▼ S", color = Cyan) }
                TextButton(onClick = onViewAzimRight) { Text("E ▶", color = Cyan) }
            }
        } else {
            RadarView(
                bearingDeg = state.bearingDeg,
                victims = state.victims,
                selectedVictimId = state.selectedVictim?.id,
                heatmap = state.heatmap,
                viewport = state.radarViewport,
                onVictimClick = onVictimClick,
                onViewportChange = onViewportChange,
                modifier = Modifier.fillMaxWidth().height(280.dp),
            )
        }
        Text(
            "Pellizca para zoom · arrastra para mover · toca P1, P2…",
            color = Color(0xFF666666),
            fontSize = 11.sp,
        )
    }
}

@Composable
fun RadarFullscreenOverlay(
    state: VitalFiUiState,
    onVictimClick: (Int) -> Unit,
    onViewportChange: (RadarViewport) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    onCloseFullscreen: () -> Unit,
    onViewAzimLeft: () -> Unit,
    onViewAzimRight: () -> Unit,
    onViewElevUp: () -> Unit,
    onViewElevDown: () -> Unit,
    onResetView3D: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(BgDark)
            .zIndex(10f),
    ) {
        Column(Modifier.fillMaxSize()) {
            RadarToolbar(
                viewport = state.radarViewport,
                isFullscreen = true,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onResetView = onResetView,
                onToggleFullscreen = onCloseFullscreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D140D))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            if (state.showRadar3D) {
                Radar3DView(
                    bearingDeg = state.bearingDeg,
                    victims = state.victims,
                    selectedVictimId = state.selectedVictim?.id,
                    heatmap = state.heatmap,
                    trail = state.trail,
                    viewAzim = state.viewAzim,
                    viewElev = state.viewElev,
                    viewport = state.radarViewport,
                    onVictimClick = onVictimClick,
                    onViewportChange = onViewportChange,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextButton(onClick = onViewAzimLeft) { Text("◀ Q", color = Cyan) }
                    TextButton(onClick = onViewElevUp) { Text("▲ W", color = Cyan) }
                    TextButton(onClick = onResetView3D) { Text("V reset", color = Color.Gray) }
                    TextButton(onClick = onViewElevDown) { Text("▼ S", color = Cyan) }
                    TextButton(onClick = onViewAzimRight) { Text("E ▶", color = Cyan) }
                }
            } else {
                RadarView(
                    bearingDeg = state.bearingDeg,
                    victims = state.victims,
                    selectedVictimId = state.selectedVictim?.id,
                    heatmap = state.heatmap,
                    viewport = state.radarViewport,
                    onVictimClick = onVictimClick,
                    onViewportChange = onViewportChange,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }

        if (state.victims.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC101810)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Personas: ${state.victims.size} | Zoom ${"%.1f".format(state.radarViewport.zoom)}x | Toca punto para datos",
                    modifier = Modifier.padding(10.dp),
                    color = RadarGreen,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun RadarToolbar(
    viewport: RadarViewport,
    isFullscreen: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isFullscreen) "Radar pantalla completa" else "Radar",
            color = Cyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${"%.1f".format(viewport.zoom)}x",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 4.dp),
            )
            IconButton(onClick = onZoomOut) {
                Icon(Icons.Default.ZoomOut, "Alejar", tint = Cyan)
            }
            IconButton(onClick = onZoomIn) {
                Icon(Icons.Default.ZoomIn, "Acercar", tint = Cyan)
            }
            TextButton(onClick = onResetView) {
                Text("Reset", color = Color.Gray, fontSize = 11.sp)
            }
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    if (isFullscreen) "Salir pantalla completa" else "Pantalla completa",
                    tint = Cyan,
                )
            }
        }
    }
}
