package com.example.dailycheck.ui.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 光点：1000ms 生命周期，分三段运动
 * 0-300ms 快速展开 → 300-700ms 漂移 → 700-1000ms 淡出。
 * 通过 maxRadius 把扩散半径限制在 100-150dp 内。
 */
data class Particle(
    val x: Float,
    val y: Float,
    val originX: Float,
    val originY: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float,
    val maxRadius: Float
) {
    fun update(dt: Float): Particle {
        val age = maxLife - life

        // 三段阻力：展开段几乎无阻力，漂移段快速减速，淡出段几乎停住
        val damp = when {
            age < 0.30f -> 0.8f
            age < 0.70f -> 5.0f
            else -> 7.0f
        }
        // 重力也分三段递减，整体很轻
        val gravity = when {
            age < 0.30f -> 140f
            age < 0.70f -> 40f
            else -> 8f
        }

        val d = (1f - damp * dt).coerceAtLeast(0f)
        var nvx = vx * d
        var nvy = vy * d + gravity * dt
        var nx = x + nvx * dt
        var ny = y + nvy * dt

        // 限制最大扩散半径（超出后贴边并衰减径向速度）
        val dx = nx - originX
        val dy = ny - originY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > maxRadius) {
            val k = maxRadius / dist
            nx = originX + dx * k
            ny = originY + dy * k
            nvx *= 0.25f
            nvy *= 0.25f
        }

        return copy(
            x = nx,
            y = ny,
            vx = nvx,
            vy = nvy,
            life = life - dt
        )
    }
}

class ParticleController {

    /** 由 rememberParticleController 注入：1dp 对应的像素数 */
    internal var densityScale: Float = 1f

    internal val particles = mutableStateListOf<Particle>()

    fun burst(
        origin: Offset,
        colors: List<Color>,
        count: Int = 28
    ) {
        if (colors.isEmpty()) return
        repeat(count) { index ->
            val angle = (Random.nextFloat() * 2f * Math.PI).toFloat()
            // 速度放缓：700-1100 px/s，在 300ms 内扩到 100dp 左右
            val speed = 700f + Random.nextFloat() * 400f
            val life = 1.0f
            val isThin = index % 4 == 0

            particles.add(
                Particle(
                    x = origin.x + (Random.nextFloat() - 0.5f) * 10f,
                    y = origin.y + (Random.nextFloat() - 0.5f) * 10f,
                    originX = origin.x,
                    originY = origin.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 40f,
                    size = if (isThin) 2.8f + Random.nextFloat() * 1.2f else 1.6f + Random.nextFloat() * 1.6f,
                    color = colors.random(),
                    life = life,
                    maxLife = life,
                    // 100-150dp：按密度换算成像素
                    maxRadius = 130f * densityScale
                )
            )
        }
    }
}

/**
 * 只绘制、不拦截手势。
 */
@Composable
fun ParticleOverlay(
    controller: ParticleController,
    modifier: Modifier = Modifier
) {
    val particles = controller.particles
    val density = LocalDensity.current
    val unit = with(density) { 1.dp.toPx() }

    if (particles.isEmpty()) return

    LaunchedEffect(particles.isNotEmpty()) {
        var lastFrame = 0L
        while (particles.isNotEmpty()) {
            withFrameNanos { now ->
                if (lastFrame == 0L) lastFrame = now
                val dt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrame = now

                var i = 0
                while (i < particles.size) {
                    val next = particles[i].update(dt)
                    if (next.life <= 0f) {
                        particles.removeAt(i)
                    } else {
                        particles[i] = next
                        i++
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            val age = 1f - progress

            // 前 70% 保持可见，最后 30% 线性淡出
            val alpha = if (age < 0.7f) 1f else ((1f - age) / 0.3f).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * unit * 0.5f,
                center = Offset(p.x, p.y)
            )
        }
    }
}

@Composable
fun rememberParticleController(): ParticleController {
    val density = LocalDensity.current
    return remember(density) {
        ParticleController().also { it.densityScale = density.density }
    }
}
