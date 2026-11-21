@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.dailycheck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Button
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.ui.screens.timer.TimerViewModel
import com.example.dailycheck.ui.theme.LocalDarkTheme
import java.util.Locale

private val TimerRunningGreen = Color(0xFF4CAF50)
private val TimerPausedWarm = Color(0xFFF59E0B)

private val FocusSheetDark = Color(0xFF080C18)
private val FocusSheetLight = Color(0xFFF6F8FE)

/** 专注计时抽屉：大号倒计时 + 开始/暂停/结束。 */
@Composable
fun FocusTimerSheet(
    task: Task,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val viewModel: TimerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(task.id) { viewModel.bind(task) }

    // 计时完成：短暂展示"已完成"后自动收起
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (dark) FocusSheetDark else FocusSheetLight,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.62f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.taskName.ifEmpty { task.name },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (state.finished) "已完成 · 已自动打卡" else "专注计时",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = if (state.finished) onDismiss else onEdit
                ) {
                    Text(if (state.finished) "关闭" else "编辑")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            CountdownRing(
                remainingSeconds = state.remainingSeconds,
                targetSeconds = state.targetSeconds.coerceAtLeast(1),
                finished = state.finished,
                running = state.running,
                paused = state.paused,
                dark = dark
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.finished) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("关闭")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            when {
                                state.running -> viewModel.pause(context)
                                state.paused -> viewModel.resume(context)
                                else -> viewModel.start(context, task)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                state.running -> TimerRunningGreen
                                state.paused -> TimerPausedWarm
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when {
                                state.running -> "暂停"
                                state.paused -> "继续"
                                else -> "开始"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.finishAndComplete(context, task) },
                        enabled = state.running || state.paused,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("结束")
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownRing(
    remainingSeconds: Int,
    targetSeconds: Int,
    finished: Boolean,
    running: Boolean,
    paused: Boolean,
    dark: Boolean
) {
    val targetProgress = if (targetSeconds <= 0) {
        0f
    } else {
        (1f - remainingSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    }
    // 每秒的剩余秒数变化用 1s 线性补间，圆环匀速连续推进
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "timerRing"
    )
    val accent = when {
        finished -> MaterialTheme.colorScheme.primary
        running -> TimerRunningGreen
        paused -> TimerPausedWarm
        else -> MaterialTheme.colorScheme.primary
    }
    val track = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = Modifier.size(232.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (finished) "00:00" else formatClock(remainingSeconds),
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    finished -> MaterialTheme.colorScheme.primary
                    paused -> TimerPausedWarm
                    running -> TimerRunningGreen
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (finished) "本轮专注已记录" else "剩余时间",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatClock(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}
