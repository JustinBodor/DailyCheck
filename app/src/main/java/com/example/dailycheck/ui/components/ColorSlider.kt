package com.example.dailycheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val RainbowStops = listOf(
    Color(0xFFFF3B30),
    Color(0xFFFF9500),
    Color(0xFFFFCC00),
    Color(0xFF34C759),
    Color(0xFF32ADE6),
    Color(0xFF5856D6),
    Color(0xFFAF52DE),
    Color(0xFFFF2D55)
)

/** 彩虹滑轨取色：横向渐变条 + 可拖动圆形手柄 + 实时色块预览 */
@Composable
fun ColorSlider(
    colorArgb: Long,
    onColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = Color(colorArgb)
    var fraction by remember { mutableFloatStateOf(hueOf(colorArgb) / 360f) }

    LaunchedEffect(colorArgb) {
        fraction = hueOf(colorArgb) / 360f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val density = LocalDensity.current
            val thumbSize = 32.dp
            val thumbPx = with(density) { thumbSize.toPx() }
            val usable = (constraints.maxWidth - thumbPx).coerceAtLeast(1f)

            fun applyX(x: Float) {
                val f = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
                fraction = f
                onColorChange(hsvToArgb(f * 360f))
            }

            // 渐变轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Brush.horizontalGradient(RainbowStops))
            )

            // 拖拽手柄
            Box(
                modifier = Modifier
                    .offset { IntOffset((usable * fraction).roundToInt(), 0) }
                    .size(thumbSize)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(current)
                    .border(3.dp, Color.White, CircleShape)
            )

            // 手势层
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .pointerInput(usable) {
                        detectTapGestures { offset -> applyX(offset.x) }
                    }
                    .pointerInput(usable) {
                        detectDragGestures(
                            onDragStart = { offset -> applyX(offset.x) },
                            onDrag = { change, _ ->
                                change.consume()
                                applyX(change.position.x)
                            }
                        )
                    }
            )
        }
    }
}

private fun hsvToArgb(hue: Float): Long {
    val rgb = android.graphics.Color.HSVToColor(
        floatArrayOf(hue.coerceIn(0f, 360f), 0.72f, 0.95f)
    )
    return 0xFF000000L or (rgb.toLong() and 0xFFFFFFL)
}

private fun hueOf(argb: Long): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb.toInt(), hsv)
    // 接近灰白时退回 0（红），避免手柄乱跳
    return if (hsv[1] < 0.05f) 0f else hsv[0].coerceIn(0f, 360f)
}
