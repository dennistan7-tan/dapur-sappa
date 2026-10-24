package com.dapursappa.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged

/** Isi bisa diperbesar dengan cubit 2 jari dan digeser (panning) saat sudah diperbesar. Ukuran kotak tetap. */
@Composable
fun PinchZoomBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var ox by remember { mutableStateOf(0f) }
    var oy by remember { mutableStateOf(0f) }
    var w by remember { mutableStateOf(0f) }
    var h by remember { mutableStateOf(0f) }
    Box(
        modifier.fillMaxWidth().clipToBounds()
            .onSizeChanged { w = it.width.toFloat(); h = it.height.toFloat() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val ev = awaitPointerEvent()
                        val multi = ev.changes.count { it.pressed } >= 2
                        if (multi || scale > 1.01f) {
                            val zoom = if (multi) ev.calculateZoom() else 1f
                            val pan = ev.calculatePan()
                            val cen = ev.calculateCentroid()
                            val ns = (scale * zoom).coerceIn(1f, 4f)
                            val k = ns / scale
                            var nx = cen.x - (cen.x - ox) * k + pan.x
                            var ny = cen.y - (cen.y - oy) * k + pan.y
                            nx = nx.coerceIn(w * (1f - ns), 0f)
                            ny = ny.coerceIn(h * (1f - ns), 0f)
                            if (ns <= 1.01f) { nx = 0f; ny = 0f }
                            scale = ns; ox = nx; oy = ny
                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (ev.changes.any { it.pressed })
                }
            },
    ) {
        Box(
            Modifier.graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = ox; translationY = oy
                transformOrigin = TransformOrigin(0f, 0f)
            },
        ) { content() }
    }
}
