@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.dailycheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailycheck.core.util.DateTimeUtils
import com.example.dailycheck.data.model.TaskType
import com.example.dailycheck.ui.screens.edit.TaskEditViewModel
import com.example.dailycheck.ui.screens.home.DurationUnit
import com.example.dailycheck.ui.screens.home.RepeatType
import com.example.dailycheck.ui.screens.home.TaskDraft
import com.example.dailycheck.ui.theme.AppCardBorderDark
import com.example.dailycheck.ui.theme.AppCardBorderLight
import com.example.dailycheck.ui.theme.AppCardDark
import com.example.dailycheck.ui.theme.LocalDarkTheme
import java.time.DayOfWeek
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

object TaskStyleCatalog {
    /** 20 个常见打卡场景图标 */
    val icons: List<String> = listOf(
        "⭐", "💪", "📖", "💧", "🌙", "🍎",
        "🏃", "🧘", "🎯", "💰", "🛏️", "🚭",
        "🦷", "🧹", "🎨", "🎧", "☀️", "🐶",
        "💊", "📝"
    )

    /** 默认配色（12 个），用于颜色初值 */
    val colors: List<Long> = listOf(
        0xFF7C6CF0L, 0xFF3AA8FFL, 0xFF35C08AL,
        0xFFFFB020L, 0xFFFF6B6BL, 0xFFE86FB4L,
        0xFF00B8D9L, 0xFF8E7CFFL, 0xFFFF8A3DL,
        0xFF4CD964L, 0xFFAF52DEL, 0xFF6B7A99L
    )

    val defaultIconKey: String = icons.first()
    val defaultColorArgb: Long = colors.first()
}

private val WeekDayOptions: List<Pair<DayOfWeek, String>> = listOf(
    DayOfWeek.MONDAY to "一", DayOfWeek.TUESDAY to "二", DayOfWeek.WEDNESDAY to "三",
    DayOfWeek.THURSDAY to "四", DayOfWeek.FRIDAY to "五", DayOfWeek.SATURDAY to "六",
    DayOfWeek.SUNDAY to "日"
)

private val TypeLabels = mapOf(
    TaskType.COMPLETE to "完成",
    TaskType.COUNT to "计数",
    TaskType.TIMED to "计时"
)

/** 与设置页一致的抽屉底色 */
private val SheetDark = Color(0xFF080C18)
private val SheetLight = Color(0xFFF6F8FE)

@Composable
fun EditScreen(
    draft: TaskDraft,
    onDraftChange: (TaskDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val dark = LocalDarkTheme.current
    val editViewModel: TaskEditViewModel = hiltViewModel()
    val categories by editViewModel.categories.collectAsStateWithLifecycle()
    val iconList by editViewModel.iconList.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    var showReminderPicker by remember { mutableStateOf(false) }
    var nameTooLong by remember { mutableStateOf(false) }

    LaunchedEffect(draft.nameError) {
        if (draft.nameError) snackbarHostState.showSnackbar("请输入任务名称")
    }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (dark) SheetDark else SheetLight,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.62f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    // 键盘弹出时整体上推，保证保存按钮可点
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 抽屉标题 + 关闭
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (draft.id > 0L) "编辑任务" else "新建任务",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCancel) { Text("关闭") }
                }

                Spacer(modifier = Modifier.height(18.dp))

                FormCard(dark) {
                    FormField(
                        value = draft.name,
                        onValueChange = { value ->
                            nameTooLong = value.length > 8
                            onDraftChange(
                                draft.copy(
                                    name = if (value.length > 8) value.take(8) else value,
                                    nameError = false
                                )
                            )
                        },
                        label = "任务名称",
                        dark = dark,
                        isError = draft.nameError || nameTooLong,
                        supportingText = when {
                            draft.nameError -> "名称不能为空"
                            nameTooLong -> "最多 8 个字"
                            else -> null
                        }
                    )

                    Text(
                        text = "类型",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TypeSelector(
                        selected = draft.type,
                        dark = dark,
                        onSelect = { type ->
                            onDraftChange(
                                draft.copy(
                                    type = type,
                                    // 计时任务不需要额外提醒
                                    reminderEnabled = if (type == TaskType.TIMED) false
                                    else draft.reminderEnabled
                                )
                            )
                        }
                    )

                    when (draft.type) {
                        TaskType.COMPLETE -> Unit
                        TaskType.COUNT -> StepperRow(
                            label = "目标", value = draft.targetCount, suffix = "次", range = 1..50
                        ) { value -> onDraftChange(draft.copy(targetCount = value)) }

                        TaskType.TIMED -> TimedDurationField(
                            taskId = draft.id,
                            totalSeconds = draft.targetDurationSeconds,
                            dark = dark,
                            onChange = { seconds ->
                                onDraftChange(draft.copy(targetDurationSeconds = seconds))
                            }
                        )
                    }

                    Text(
                        text = "图标",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconRow(
                        icons = iconList,
                        selected = draft.iconKey,
                        dark = dark,
                        onSelect = { icon -> onDraftChange(draft.copy(iconKey = icon)) },
                        onMove = { from, to -> editViewModel.moveIcon(from, to) },
                        onDragStopped = { editViewModel.persistIconOrder() }
                    )

                    Text(
                        text = "颜色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ColorSlider(
                        colorArgb = draft.colorArgb,
                        onColorChange = { color -> onDraftChange(draft.copy(colorArgb = color)) }
                    )

                    CategoryField(
                        value = draft.category,
                        onValueChange = { value -> onDraftChange(draft.copy(category = value)) },
                        categories = categories,
                        dark = dark
                    )

                    FormField(
                        value = draft.note,
                        onValueChange = { value -> onDraftChange(draft.copy(note = value)) },
                        label = "备注",
                        dark = dark
                    )
                }

                // 计时任务是一次性执行，不参与每日循环，整块隐藏重复规则
                if (draft.type != TaskType.TIMED) {
                    Spacer(modifier = Modifier.height(16.dp))

                    FormCard(dark) {
                        Text(
                            text = "重复",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        RepeatRuleEditor(draft = draft, dark = dark, onDraftChange = onDraftChange)
                    }
                }

                // 计时任务自身就会到点提醒，不提供额外提醒设置
                if (draft.type != TaskType.TIMED) {
                    Spacer(modifier = Modifier.height(16.dp))

                    FormCard(dark) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "提醒",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (draft.reminderEnabled) {
                                        "每天 ${DateTimeUtils.formatMinutes(draft.reminderMinutes)}"
                                    } else {
                                        "不提醒"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            if (draft.reminderEnabled) {
                                TextButton(onClick = { showReminderPicker = true }) { Text("选择时间") }
                            }
                            Switch(
                                checked = draft.reminderEnabled,
                                onCheckedChange = { onDraftChange(draft.copy(reminderEnabled = it)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 表单末尾的主按钮
                Button(
                    onClick = {
                        if (draft.name.isNotBlank()) {
                            editViewModel.rememberColor(draft.colorArgb)
                        }
                        onSave()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("保存")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }

    if (showReminderPicker) {
        TimeWheelPicker(
            title = "提醒时间",
            initialMinutes = draft.reminderMinutes,
            onDismiss = { showReminderPicker = false },
            onConfirm = { minutes ->
                showReminderPicker = false
                onDraftChange(draft.copy(reminderMinutes = minutes))
            }
        )
    }
}

/**
 * 图标行：每屏正好 6 个，LazyRow 吸附滚动，长按拖拽排序。
 */
@Composable
private fun IconRow(
    icons: List<String>,
    selected: String,
    dark: Boolean,
    onSelect: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragStopped: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.index, to.index)
    }
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 一屏 6 格，格子宽度 = 可用宽度 / 6
        val cellWidth = maxWidth / 6

        LazyRow(
            state = lazyListState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(icons, key = { _, icon -> icon }) { _, icon ->
                ReorderableItem(reorderableState, key = icon) { isDragging ->
                    val isSelected = icon == selected
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = if (isDragging) 1.1f else 1f
                                    scaleY = if (isDragging) 1.1f else 1f
                                }
                                .shadow(
                                    elevation = if (isDragging) 8.dp else 0.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    clip = false,
                                    ambientColor = Color.Black.copy(alpha = 0.20f),
                                    spotColor = Color.Black.copy(alpha = 0.25f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    else if (dark) Color.White.copy(alpha = 0.06f)
                                    else Color.Black.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (dark) Color.White.copy(alpha = 0.10f)
                                    else Color.Black.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .longPressDraggableHandle(
                                    onDragStopped = onDragStopped
                                )
                                .clickable { onSelect(icon) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = icon, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryField(
    value: String,
    onValueChange: (String) -> Unit,
    categories: List<String>,
    dark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (categories.isNotEmpty()) expanded = it }
    ) {
        FormField(
            value = value,
            onValueChange = { input ->
                expanded = false
                onValueChange(input)
            },
            label = "分类",
            dark = dark,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (dark) Color(0xFF2A2B30) else Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onValueChange(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FormCard(dark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val bg = if (dark) AppCardDark else Color.White
    val border = if (dark) AppCardBorderDark else AppCardBorderLight
    val shadowModifier = if (dark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(shadowModifier)
            .clip(shape)
            .background(bg)
            .border(width = 1.dp, color = border, shape = shape)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    dark: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val fieldBg = if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFFF2F3F7)
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        dark -> Color.White.copy(alpha = 0.12f)
        else -> Color.Black.copy(alpha = 0.05f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(fieldBg)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
        if (supportingText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TypeSelector(selected: TaskType, dark: Boolean, onSelect: (TaskType) -> Unit) {
    val activeBg = if (dark) Color.White.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.86f)
    val activeFg = if (dark) Color(0xFF0B0D16) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskType.entries.forEach { type ->
            val active = type == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (active) activeBg
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(
                    text = TypeLabels[type] ?: type.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) activeFg else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RepeatRuleEditor(
    draft: TaskDraft,
    dark: Boolean,
    onDraftChange: (TaskDraft) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepeatType.entries.forEach { type ->
                val active = draft.repeatType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                        .clickable { onDraftChange(draft.copy(repeatType = type)) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when (draft.repeatType) {
            RepeatType.DAILY -> Unit

            RepeatType.WEEKLY -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekDayOptions.forEach { (day, label) ->
                    val selected = draft.weeklyDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                            .clickable {
                                val next = if (selected) draft.weeklyDays - day else draft.weeklyDays + day
                                if (next.isNotEmpty()) onDraftChange(draft.copy(weeklyDays = next))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            RepeatType.MONTHLY -> StepperRow(
                label = "每月", value = draft.monthlyDay, suffix = "日", range = 1..31
            ) { value -> onDraftChange(draft.copy(monthlyDay = value)) }

            RepeatType.INTERVAL -> StepperRow(
                label = "每", value = draft.intervalDays, suffix = "天", range = 2..30
            ) { value -> onDraftChange(draft.copy(intervalDays = value)) }
        }
    }
}

/** 计时任务：数字输入 + 单位下拉，最终都换算成秒 */
@Composable
private fun TimedDurationField(
    taskId: Long,
    totalSeconds: Int,
    dark: Boolean,
    onChange: (Int) -> Unit
) {
    val initial = remember(taskId) { DurationUnit.bestFor(totalSeconds) }
    var unit by remember(taskId) { mutableStateOf(initial.first) }
    var text by remember(taskId) { mutableStateOf(initial.second.toString()) }
    var expanded by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(16.dp)
    val fieldBg = if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFFF2F3F7)
    val borderColor = if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f)

    fun commit(newText: String, newUnit: DurationUnit) {
        text = newText
        val value = (newText.toIntOrNull() ?: 1).coerceAtLeast(1)
        onChange((value * newUnit.seconds).coerceAtMost(30 * 86400))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "目标时长",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value = text,
                onValueChange = { input ->
                    commit(input.filter { it.isDigit() }.take(5), unit)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(fieldBg)
                    .border(width = 1.dp, color = borderColor, shape = shape)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            )

            Box(modifier = Modifier.width(120.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(shape)
                        .background(fieldBg)
                        .border(width = 1.dp, color = borderColor, shape = shape)
                        .clickable { expanded = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = unit.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "▾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = if (dark) Color(0xFF2A2B30) else Color.White,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 10.dp
                ) {
                    DurationUnit.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                expanded = false
                                unit = option
                                commit(text, option)
                            }
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    suffix: String,
    range: IntRange,
    step: Int = 1,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        TextButton(
            onClick = { onChange((value - step).coerceAtLeast(range.first)) },
            enabled = value > range.first
        ) { Text("－") }
        Text(
            text = "$value $suffix",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(
            onClick = { onChange((value + step).coerceAtMost(range.last)) },
            enabled = value < range.last
        ) { Text("＋") }
    }
}
