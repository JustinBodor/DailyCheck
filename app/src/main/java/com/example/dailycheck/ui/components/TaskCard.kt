@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dailycheck.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailycheck.core.util.DateTimeUtils
import com.example.dailycheck.core.util.RepeatRuleUtils
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.model.TaskType
import com.example.dailycheck.data.timer.TimerUiState
import com.example.dailycheck.ui.theme.LocalDarkTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class SwipeAction(
    val label: String,
    val color: Color,
    val onClick: () -> Unit
)

private val ACTION_WIDTH = 78.dp
private val CARD_ACTION_GAP = 8.dp
private val CARD_SHAPE = RoundedCornerShape(24.dp)

private val DarkCardColor = Color(0xFF2A2B30)
private val DarkCardBorder = Color.White.copy(alpha = 0.10f)
private val TimerRunningGreen = Color(0xFF4CAF50)
private val TimerPausedRed = Color(0xFFF44336)

private val DarkTextPrimary = Color.White
private val DarkTextSecondary = Color.White.copy(alpha = 0.60f)

@Composable
fun TaskCard(
    task: Task,
    completed: Boolean,
    highlighted: Boolean,
    timer: TimerUiState?,
    onToggleComplete: (Offset, Color) -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onIncrementCount: () -> Unit,
    onEdit: () -> Unit,
    onOpenFocus: () -> Unit,
    onRestartTimer: () -> Unit,
    onUndo: () -> Unit,
    onMakeup: () -> Unit,
    onDelete: (Offset, Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    var showMenu by remember { mutableStateOf(false) }

    // 删除粒子原点：卡片中心
    val deleteOrigin = cardBounds.center

    val timerActive = !completed && timer != null && timer.taskId == task.id

    // 严格点击规则：
    // 已完成 -> 编辑（保存即重置）
    // 计时型 + 计时中/暂停 -> 专注抽屉，绝不进编辑
    // 其余（含未开始的计时型） -> 编辑
    val onCardClick: () -> Unit = when {
        completed -> onEdit
        task.type == TaskType.TIMED && timerActive -> onOpenFocus
        else -> onEdit
    }

    // 补打卡只对未完成的“完成型”开放；计时型只有删除
    // 右滑仅完成型有“补打卡”；计时型禁止右滑
    // 完成型（无论今天是否已完成）右滑补打卡；计时型/计数型不提供
    val leading = if (task.type == TaskType.COMPLETE) {
        listOf(SwipeAction("补打卡", Color(0xFF32B39A), onMakeup))
    } else {
        emptyList()
    }
    val trailing = listOf(
        SwipeAction("删除", Color(0xFFFF453A)) {
            performTick(context)
            onDelete(deleteOrigin, Color(task.colorArgb))
        }
    )

    Box {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = if (LocalDarkTheme.current) Color(0xFF2A2B30) else Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            if (completed && task.type != TaskType.TIMED) {
                DropdownMenuItem(
                    text = { Text("撤销打卡") },
                    onClick = {
                        showMenu = false
                        onUndo()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showMenu = false
                    onDelete(deleteOrigin, Color(task.colorArgb))
                }
            )
        }

        SwipeActionCard(
        leading = leading,
        trailing = trailing,
        onClick = onCardClick,
        onLongClick = { showMenu = true },
        modifier = modifier.onGloballyPositioned { coords ->
            val pos = coords.positionInRoot()
            cardBounds = Rect(
                pos.x,
                pos.y,
                pos.x + coords.size.width,
                pos.y + coords.size.height
            )
        }
    ) {
        TaskCardContent(
            task = task,
            completed = completed,
            highlighted = highlighted,
            timer = timer,
            onToggleComplete = onToggleComplete,
            onRestartTimer = onRestartTimer,
            onStartTimer = onStartTimer,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer,
            onCancelTimer = onCancelTimer,
            onIncrementCount = onIncrementCount
        )
        }
    }
}

@Composable
private fun TaskCardContent(
    task: Task,
    completed: Boolean,
    highlighted: Boolean,
    timer: TimerUiState?,
    onToggleComplete: (Offset, Color) -> Unit,
    onRestartTimer: () -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onIncrementCount: () -> Unit
) {
    val dark = LocalDarkTheme.current
    val timerActive = timer != null && timer.taskId == task.id
    val accent = Color(task.colorArgb)

    val contentAlpha by animateFloatAsState(
        targetValue = if (completed) 0.65f else 1f,
        animationSpec = tween(200),
        label = "taskAlpha"
    )

    val primaryText = if (dark) DarkTextPrimary else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (dark) DarkTextSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val cardModifier = if (dark) {
        Modifier
            .clip(CARD_SHAPE)
            .background(DarkCardColor, CARD_SHAPE)
            .border(
                width = 1.dp,
                color = if (highlighted) accent.copy(alpha = 0.55f) else DarkCardBorder,
                shape = CARD_SHAPE
            )
    } else {
        Modifier
            .shadow(
                elevation = 16.dp,
                shape = CARD_SHAPE,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(CARD_SHAPE)
            .background(Color.White, CARD_SHAPE)
            .border(
                width = 1.dp,
                color = if (highlighted) accent.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.05f),
                shape = CARD_SHAPE
            )
    }

    CompositionLocalProvider(LocalContentColor provides primaryText) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha }
                .then(cardModifier)
                .padding(if (task.type == TaskType.TIMED) 20.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = if (dark) 0.22f else 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = task.iconKey, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dark) secondaryText else primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (completed) TextDecoration.LineThrough else null
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (task.type == TaskType.TIMED) {
                            formatDuration(task.targetDurationSeconds)
                        } else {
                            task.category ?: "每日"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = secondaryText.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 计时型没有直接打勾；完成后可再次点击撤销
                if (task.type != TaskType.TIMED) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CheckCircle(
                        completed = completed,
                        accent = accent,
                        dark = dark,
                        onClick = { center -> onToggleComplete(center, accent) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                completed -> CompletedBody(
                    task = task,
                    accent = accent,
                    secondaryText = secondaryText,
                    dark = dark,
                    onRestartTimer = onRestartTimer
                )

                task.type == TaskType.TIMED -> TimedBody(
                    task = task,
                    timer = timer,
                    timerActive = timerActive,
                    accent = accent,
                    dark = dark,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onStartTimer = onStartTimer,
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                    onCancelTimer = onCancelTimer
                )

                task.type == TaskType.COUNT -> CountBody(
                    task = task,
                    accent = accent,
                    dark = dark,
                    secondaryText = secondaryText,
                    completed = completed,
                    onIncrementCount = onIncrementCount
                )

                else -> Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "连续 ${task.streak} 天",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 计时任务不支持重复，不显示“每天”
                if (task.type != TaskType.TIMED) {
                    MetaChip(RepeatRuleUtils.describe(task.repeatRule), dark, secondaryText)
                }
                task.category?.let { MetaChip(it, dark, secondaryText) }
                task.reminderMinutes?.let { MetaChip(DateTimeUtils.formatMinutes(it), dark, secondaryText) }
            }
        }
    }
}

@Composable
private fun CheckCircle(
    completed: Boolean,
    accent: Color,
    dark: Boolean,
    onClick: (Offset) -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    var center by remember { mutableStateOf(Offset.Zero) }

    val checkScale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "checkScale"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(150),
        label = "checkAlpha"
    )

    val ringColor = if (completed) {
        accent
    } else if (dark) {
        Color.White.copy(alpha = 0.70f)
    } else {
        Color.Black.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .size(54.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                center = Offset(
                    pos.x + coords.size.width / 2f,
                    pos.y + coords.size.height / 2f
                )
            }
            .pressScale(interactionSource, pressedScale = 0.88f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (!completed) performTick(context)
                onClick(center)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (completed) accent else if (dark) Color.White.copy(alpha = 0.06f) else Color.Transparent
                )
                .border(width = if (dark) 2.dp else 1.8.dp, color = ringColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Text(
                    text = "✓",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        alpha = checkAlpha
                        scaleX = checkScale
                        scaleY = checkScale
                    }
                )
            }
        }
    }
}

@Composable
private fun SwipeActionCard(
    leading: List<SwipeAction>,
    trailing: List<SwipeAction>,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val maxCount = maxOf(leading.size, trailing.size).coerceAtLeast(1)
    val maxPx = with(density) { (ACTION_WIDTH * maxCount).toPx() }
    val gapPx = with(density) { CARD_ACTION_GAP.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(24.dp)
    val dark = LocalDarkTheme.current

    // 右滑仅在有 leading 动作（完成型补打卡）时可用；计时型 leading 为空 -> 最大位移 0
    val allowStartToEnd = leading.isNotEmpty()
    val allowEndToStart = trailing.isNotEmpty()
    val maxRight = if (allowStartToEnd) maxPx else 0f
    val maxLeft = if (allowEndToStart) -maxPx else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(maxPx, allowStartToEnd, allowEndToStart) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offset.snapTo((offset.value + dragAmount).coerceIn(maxLeft, maxRight))
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            val target = when {
                                maxRight > 0f && offset.value > maxRight * 0.45f -> maxRight
                                maxLeft < 0f && offset.value < maxLeft * 0.45f -> maxLeft
                                else -> 0f
                            }
                            offset.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    }
                )
            }
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            val exposed = offset.value
            if (exposed > 1f && leading.isNotEmpty()) {
                ActionStrip(
                    actions = leading,
                    widthDp = with(density) { (exposed - gapPx).coerceAtLeast(0f).toDp() },
                    align = Alignment.CenterStart,
                    dark = dark,
                    onAction = { action ->
                        scope.launch {
                            offset.animateTo(0f, tween(150))
                            action.onClick()
                        }
                    }
                )
            }
            if (exposed < -1f && trailing.isNotEmpty()) {
                ActionStrip(
                    actions = trailing,
                    widthDp = with(density) { (-exposed - gapPx).coerceAtLeast(0f).toDp() },
                    align = Alignment.CenterEnd,
                    dark = dark,
                    onAction = { action ->
                        scope.launch {
                            offset.animateTo(0f, tween(150))
                            action.onClick()
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .clip(shape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (offset.value != 0f) {
                            scope.launch { offset.animateTo(0f, tween(150)) }
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = onLongClick
                )
        ) {
            content()
        }
    }
}

@Composable
private fun BoxScope.ActionStrip(
    actions: List<SwipeAction>,
    widthDp: Dp,
    align: Alignment,
    dark: Boolean,
    onAction: (SwipeAction) -> Unit
) {
    val stripBg = if (dark) Color(0xFF2A2A2A) else Color.White

    Row(
        modifier = Modifier
            .align(align)
            .width(widthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(stripBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onAction(action) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = action.color,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompletedBody(
    task: Task,
    accent: Color,
    secondaryText: Color,
    dark: Boolean,
    onRestartTimer: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "已完成",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = accent.copy(alpha = 0.85f)
        )
        if (task.type != TaskType.TIMED) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "连续 ${task.streak} 天",
                style = MaterialTheme.typography.labelMedium,
                color = secondaryText.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (task.type == TaskType.TIMED) {
            CapsuleAction(
                text = "重新计时",
                filled = true,
                dark = dark,
                onClick = onRestartTimer,
                containerColor = accent,
                compact = true
            )
        }
    }
}

@Composable
private fun TimedBody(
    task: Task,
    timer: TimerUiState?,
    timerActive: Boolean,
    accent: Color,
    dark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit
) {
    val targetSeconds = task.targetDurationSeconds.coerceAtLeast(1)
    val remaining = if (timerActive) timer!!.remainingSeconds else targetSeconds
    val ratio = 1f - remaining.toFloat() / targetSeconds

    val running = timerActive && timer!!.running
    val paused = timerActive && !running

    // 进行中=绿，暂停=红，未开始=主题色
    val stateColor = when {
        running -> TimerRunningGreen
        paused -> TimerPausedRed
        else -> accent
    }

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatClock(remaining),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = when {
                    paused -> TimerPausedRed
                    running -> TimerRunningGreen
                    else -> primaryText
                },
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    running -> "计时中"
                    paused -> "已暂停"
                    else -> formatDuration(task.targetDurationSeconds)
                },
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        GradientProgress(progress = ratio, color = stateColor)

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                !timerActive -> CapsuleAction(
                    "开始", true, dark, onStartTimer, containerColor = accent, compact = true
                )

                running -> CapsuleAction(
                    "暂停", true, dark, onPauseTimer, containerColor = TimerRunningGreen, compact = true
                )

                else -> CapsuleAction(
                    "继续", true, dark, onResumeTimer, containerColor = TimerPausedRed, compact = true
                )
            }
            if (timerActive) {
                CapsuleAction("取消", false, dark, onCancelTimer, compact = true)
            }
        }
    }
}

@Composable
private fun CountBody(
    task: Task,
    accent: Color,
    dark: Boolean,
    secondaryText: Color,
    completed: Boolean,
    onIncrementCount: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${task.todayCount}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = accent,
                maxLines = 1
            )
            Text(
                text = " / ${task.targetCount}",
                style = MaterialTheme.typography.titleLarge,
                color = secondaryText,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            CapsuleAction("+1", true, dark, onIncrementCount, enabled = !completed)
        }

        Spacer(modifier = Modifier.height(12.dp))

        GradientProgress(task.todayCount.toFloat() / task.targetCount.coerceAtLeast(1), accent)
    }
}

@Composable
private fun CapsuleAction(
    text: String,
    filled: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color? = null,
    compact: Boolean = false
) {
    val container = containerColor ?: if (filled) {
        if (dark) Color.White.copy(alpha = if (enabled) 0.92f else 0.4f)
        else Color.Black.copy(alpha = if (enabled) 0.88f else 0.3f)
    } else {
        if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    }
    val content = if (containerColor != null) {
        Color.White
    } else if (filled) {
        if (dark) Color(0xFF0B0D16) else Color.White
    } else {
        if (dark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) 16.dp else 20.dp,
                vertical = if (compact) 8.dp else 10.dp
            )
    ) {
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.labelMedium
            else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
private fun GradientProgress(progress: Float, color: Color) {
    // 每秒一次的状态更新用 1s 线性补间，进度条连续匀速推进
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "progress"
    )

    val track = if (LocalDarkTheme.current) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(percent = 50))
                .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.65f))))
        )
    }
}

@Composable
private fun MetaChip(text: String, dark: Boolean, secondaryText: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = secondaryText,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (dark) Color.White.copy(alpha = 0.08f)
                else Color.Black.copy(alpha = 0.05f)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        maxLines = 1
    )
}

private fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(1)
    return when {
        s % 86400 == 0 -> "${s / 86400} 天"
        s % 3600 == 0 -> "${s / 3600} 小时"
        s % 60 == 0 -> "${s / 60} 分钟"
        else -> "$s 秒"
    }
}

private fun formatClock(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(java.util.Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

/** 清脆的 50ms 短震动 */
private fun performTick(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(
        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
    )
}
