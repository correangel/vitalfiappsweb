package com.whofi.vitalfi.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.whofi.vitalfi.dsp.HeatPoint
import com.whofi.vitalfi.dsp.TrappedVictim
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class RadarViewport(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
) {
    fun zoomIn(): RadarViewport = copy(zoom = (zoom * 1.3f).coerceAtMost(5f))
    fun zoomOut(): RadarViewport = copy(zoom = (zoom / 1.3f).coerceAtLeast(0.5f))
    fun reset(): RadarViewport = RadarViewport()
}

internal val RadarCyan = Color(0xFF00FFCC)
internal val RadarRed = Color(0xFFFF3366)
internal val RadarOrange = Color(0xFFFF9900)
internal val RadarGreen = Color(0xFF66FF99)

fun DrawScope.radarCenter(viewport: RadarViewport): Pair<Float, Float> {
    val cx = size.width / 2f + viewport.panX
    val cy = size.height / 2f + viewport.panY
    return cx to cy
}

fun DrawScope.effectiveRadarScale(divisor: Float, viewport: RadarViewport): Float {
    val half = minOf(size.width, size.height) / 2f
    return half / divisor * viewport.zoom
}

fun Modifier.radarGestures(
    viewport: RadarViewport,
    victims: List<TrappedVictim>,
    projectVictim: (TrappedVictim) -> Offset,
    onVictimClick: (Int) -> Unit,
    onViewportChange: (RadarViewport) -> Unit,
): Modifier = this
    .pointerInput(viewport) {
        detectTransformGestures { _, pan, zoom, _ ->
            onViewportChange(
                viewport.copy(
                    zoom = (viewport.zoom * zoom).coerceIn(0.5f, 5f),
                    panX = viewport.panX + pan.x,
                    panY = viewport.panY + pan.y,
                ),
            )
        }
    }
    .pointerInput(victims, viewport) {
        detectTapGestures { tap ->
            val hit = victims.minByOrNull { victim ->
                val p = projectVictim(victim)
                hypot(tap.x - p.x, tap.y - p.y)
            }
            hit?.let { victim ->
                val p = projectVictim(victim)
                val hitRadius = (36f * viewport.zoom).coerceIn(28f, 56f)
                if (hypot(tap.x - p.x, tap.y - p.y) <= hitRadius) {
                    onVictimClick(victim.id)
                }
            }
        }
    }

fun DrawScope.drawRadarGrid2D(
    divisor: Float,
    viewport: RadarViewport,
    labelPaint: android.graphics.Paint,
) {
    val (cx, cy) = radarCenter(viewport)
    val scale = effectiveRadarScale(divisor, viewport)

    for (r in listOf(1.5f, 3f, 5f)) {
        drawCircle(
            Color(0xFF1A3A1A),
            r * scale,
            Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f),
        )
        drawContext.canvas.nativeCanvas.drawText(
            "${r}m",
            cx + 4f,
            cy - r * scale - 4f,
            labelPaint,
        )
    }

    drawLine(Color(0xFF2A4A2A), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
    drawLine(Color(0xFF2A4A2A), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
    drawContext.canvas.nativeCanvas.drawText("Y+", cx + 6f, 18f, labelPaint)
    drawContext.canvas.nativeCanvas.drawText("X+", size.width - 28f, cy - 6f, labelPaint)
    drawContext.canvas.nativeCanvas.drawText("(0,0)", cx + 8f, cy + 22f, labelPaint)
}

fun DrawScope.drawVictims2D(
    victims: List<TrappedVictim>,
    divisor: Float,
    viewport: RadarViewport,
    selectedVictimId: Int?,
    labelPaint: android.graphics.Paint,
    tagPaint: android.graphics.Paint,
) {
    val (cx, cy) = radarCenter(viewport)
    val scale = effectiveRadarScale(divisor, viewport)

    for (victim in victims) {
        val tx = cx + victim.x.toFloat() * scale
        val ty = cy - victim.y.toFloat() * scale
        val selected = victim.id == selectedVictimId
        val color = victimColor(victim)
        val radius = if (selected) 14f else 11f
        drawCircle(color.copy(alpha = 0.25f), radius + 10f, Offset(tx, ty))
        drawCircle(color, radius, Offset(tx, ty))
        if (selected) {
            drawCircle(RadarCyan, radius + 6f, Offset(tx, ty), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
        }
        drawLine(color.copy(alpha = 0.45f), Offset(cx, cy), Offset(tx, ty), strokeWidth = if (selected) 2.5f else 1.5f)
        drawContext.canvas.nativeCanvas.drawText(victim.label, tx - 12f, ty - radius - 8f, tagPaint)
        drawContext.canvas.nativeCanvas.drawText(
            "(${formatCoord(victim.x)},${formatCoord(victim.y)})",
            tx - 34f,
            ty + radius + 16f,
            labelPaint,
        )
    }
}

fun DrawScope.drawHeatmap2D(
    heatmap: List<HeatPoint>,
    divisor: Float,
    viewport: RadarViewport,
) {
    val (cx, cy) = radarCenter(viewport)
    val scale = effectiveRadarScale(divisor, viewport)
    for (pt in heatmap) {
        val px = cx + pt.x.toFloat() * scale
        val py = cy - pt.y.toFloat() * scale
        val alpha = (0.15f + 0.55f * pt.intensity.coerceIn(0.0, 1.0)).toFloat()
        drawCircle(Color(0xFFFF6600).copy(alpha = alpha), 6f, Offset(px, py))
    }
}

fun DrawScope.drawVictims3D(
    victims: List<TrappedVictim>,
    selectedVictimId: Int?,
    project: (Double, Double, Double) -> Offset,
    origin: Offset,
    tagPaint: android.graphics.Paint,
    labelPaint: android.graphics.Paint,
) {
    for (victim in victims) {
        val target = project(victim.x, victim.y, victim.depth)
        val selected = victim.id == selectedVictimId
        val color = victimColor(victim)
        val radius = if (selected) 11f else 8f
        drawCircle(color.copy(alpha = 0.2f), radius + 8f, target)
        drawCircle(color, radius, target)
        if (selected) {
            drawCircle(RadarCyan, radius + 5f, target, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }
        drawLine(color.copy(alpha = 0.4f), origin, target, strokeWidth = if (selected) 2f else 1.5f)
        drawContext.canvas.nativeCanvas.drawText(victim.label, target.x - 10f, target.y - radius - 6f, tagPaint)
        drawContext.canvas.nativeCanvas.drawText(
            "Z=${formatCoord(-victim.depth)}m",
            target.x - 28f,
            target.y + radius + 14f,
            labelPaint,
        )
    }
}

fun projectRadar3D(
    canvasWidth: Float,
    canvasHeight: Float,
    viewAzim: Double,
    viewElev: Double,
    x: Double,
    y: Double,
    z: Double,
    viewport: RadarViewport,
    divisor: Float = 7f,
): Offset {
    val cx = canvasWidth / 2f + viewport.panX
    val cy = canvasHeight / 2f + viewport.panY
    val half = minOf(canvasWidth, canvasHeight) / 2f
    val scale = half / divisor * viewport.zoom
    val az = Math.toRadians(viewAzim)
    val el = Math.toRadians(viewElev)
    val x1 = x * cos(az) - y * sin(az)
    val y1 = x * sin(az) + y * cos(az)
    val z1 = z
    val y2 = y1 * cos(el) - z1 * sin(el)
    val z2 = y1 * sin(el) + z1 * cos(el)
    return Offset(cx + x1.toFloat() * scale, cy - y2.toFloat() * scale - z2.toFloat() * scale * 0.35f)
}

private fun formatCoord(value: Double): String = "%.1f".format(value)

fun victimColor(victim: TrappedVictim): Color = when {
    victim.isBreathing && victim.heartBeating -> RadarRed
    victim.isBreathing -> RadarRed.copy(alpha = 0.85f)
    victim.heartBeating -> Color(0xFFFF6699)
    victim.isActivity -> RadarOrange
    else -> RadarOrange.copy(alpha = 0.7f)
}
