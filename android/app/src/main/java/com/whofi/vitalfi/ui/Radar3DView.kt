package com.whofi.vitalfi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.whofi.vitalfi.dsp.HeatPoint
import com.whofi.vitalfi.dsp.TrappedVictim
import kotlin.math.cos
import kotlin.math.sin

data class TrailPoint3D(val x: Double, val y: Double, val z: Double)

@Composable
fun Radar3DView(
    bearingDeg: Double,
    victims: List<TrappedVictim>,
    selectedVictimId: Int?,
    heatmap: List<HeatPoint>,
    trail: List<TrailPoint3D>,
    viewAzim: Double,
    viewElev: Double,
    viewport: RadarViewport,
    onVictimClick: (Int) -> Unit,
    onViewportChange: (RadarViewport) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D140D)),
        modifier = modifier,
    ) {
        val labelPaint = remember {
            android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
            }
        }
        val tagPaint = remember {
            android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 24f
                isFakeBoldText = true
            }
        }
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        fun project(x: Double, y: Double, z: Double): Offset =
            projectRadar3D(
                canvasSize.width.toFloat(),
                canvasSize.height.toFloat(),
                viewAzim,
                viewElev,
                x,
                y,
                z,
                viewport,
            )

        Canvas(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .radarGestures(
                    viewport = viewport,
                    victims = victims,
                    projectVictim = { victim -> project(victim.x, victim.y, victim.depth) },
                    onVictimClick = onVictimClick,
                    onViewportChange = onViewportChange,
                ),
        ) {
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())

            for (r in listOf(1.5, 3.0, 5.0)) {
                val pts = (0..48).map { i ->
                    val t = i / 48.0 * 2 * Math.PI
                    project(r * cos(t), r * sin(t), 0.0)
                }
                val path = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, Color(0xFF1A3A1A), style = Stroke(1.5f))
                val labelPt = project(r, 0.0, 0.0)
                drawContext.canvas.nativeCanvas.drawText("${r}m", labelPt.x + 4f, labelPt.y - 4f, labelPaint)
            }

            val gridN = 5
            for (i in -gridN..gridN) {
                val a = project(i.toDouble(), -gridN.toDouble(), 0.0)
                val b = project(i.toDouble(), gridN.toDouble(), 0.0)
                drawLine(Color(0xFF1A2A1A), a, b, strokeWidth = 0.5f)
                val c = project(-gridN.toDouble(), i.toDouble(), 0.0)
                val d = project(gridN.toDouble(), i.toDouble(), 0.0)
                drawLine(Color(0xFF1A2A1A), c, d, strokeWidth = 0.5f)
            }

            for (pt in heatmap) {
                val p = project(pt.x, pt.y, -0.4)
                val alpha = (0.2f + 0.5f * pt.intensity.coerceIn(0.0, 1.0)).toFloat()
                drawCircle(Color(0xFFFF6600).copy(alpha = alpha), 5f, p)
            }

            if (trail.size >= 2) {
                val path = Path()
                trail.forEachIndexed { i, t ->
                    val p = project(t.x, t.y, t.z)
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                drawPath(path, Color(0xFFFF6699).copy(alpha = 0.5f), style = Stroke(2f))
            }

            val laptop = project(0.0, 0.0, 1.0)
            drawCircle(RadarCyan, 12f, laptop)

            val bearingRad = Math.toRadians(bearingDeg).toFloat()
            val dirEnd = project(sin(bearingRad) * 1.4, cos(bearingRad) * 1.4, 1.0)
            drawLine(RadarCyan, laptop, dirEnd, strokeWidth = 3f)

            drawVictims3D(victims, selectedVictimId, ::project, laptop, tagPaint, labelPaint)

            drawContext.canvas.nativeCanvas.apply {
                drawText("Radar 3D — pellizca zoom | arrastra mover", 8f, size.height - 8f, labelPaint)
            }
        }
    }
}
