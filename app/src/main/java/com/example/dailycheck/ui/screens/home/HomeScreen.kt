@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.dailycheck.ui.screens.home

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailycheck.core.util.DateTimeUtils
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.model.TaskType
import com.example.dailycheck.permission.NotificationPermissionHelper
import com.example.dailycheck.ui.animation.ParticleOverlay
import com.example.dailycheck.ui.animation.rememberParticleController
import com.example.dailycheck.ui.components.AddTaskFab
import com.example.dailycheck.ui.components.EditScreen
import com.example.dailycheck.ui.components.FocusTimerSheet
import com.example.dailycheck.ui.components.EmptyState
import com.example.dailycheck.ui.components.TaskCard
import com.example.dailycheck.ui.screens.settings.SettingsScreen
import com.example.dailycheck.ui.screens.stats.StatsScreen
import com.example.dailycheck.ui.screens.settings.SettingsViewModel
import com.example.dailycheck.ui.theme.LiquidBackground
import com.example.dailycheck.ui.theme.liquidGlass
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    highlightTaskId: Long?,
    onHighlightConsumed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    var highlightedId by remember { mutableStateOf<Long?>(null) }
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationPermissionHelper.hasPermission(context))
    }
    var showSettings by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var focusTask by remember { mutableStateOf<Task?>(null) }

    val particleController = rememberParticleController()
    val accentColor = MaterialTheme.colorScheme.primary
    val goldColor = Color(0xFFFFD66B)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = NotificationPermissionHelper.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationPermissionHelper.hasPermission(context)
        ) {
            permissionLauncher.launch(NotificationPermissionHelper.POST_NOTIFICATIONS_PERMISSION)
        }
    }

    LaunchedEffect(highlightTaskId, uiState.isLoading, uiState.activeTasks, uiState.completedTasks) {
        val targetId = highlightTaskId ?: return@LaunchedEffect
        if (uiState.isLoading) return@LaunchedEffect

        val activeIndex = uiState.activeTasks.indexOfFirst { it.id == targetId }
        val completedIndex = uiState.completedTasks.indexOfFirst { it.id == targetId }
        if (activeIndex < 0 && completedIndex < 0) {
            onHighlightConsumed()
            return@LaunchedEffect
        }
        val targetIndex = if (activeIndex >= 0) activeIndex
        else uiState.activeTasks.size + 1 + completedIndex
        listState.animateScrollToItem(targetIndex)
        highlightedId = targetId
        delay(1600)
        highlightedId = null
        onHighlightConsumed()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("今日打卡", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = DateTimeUtils.formatToday(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                TextButton(onClick = { showStatsSheet = true }) { Text("统计") }
                                TextButton(onClick = { showSettings = true }) { Text("设置") }
                            },
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                    floatingActionButton = {
                        if (uiState.draft == null && !showSettings) {
                            AddTaskFab(onClick = viewModel::startCreate)
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (!hasNotificationPermission) {
                            PermissionBanner(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                onOpenSettings = {
                                    NotificationPermissionHelper.openNotificationSettings(context)
                                }
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = uiState.activeTasks, key = { "active_${it.id}" }) { task ->
                                TaskCard(
                                    task = task,
                                    completed = false,
                                    highlighted = highlightedId == task.id,
                                    timer = uiState.timer,
                                    onToggleComplete = { origin, color ->
                                        viewModel.toggleComplete(task, true)
                                        particleController.burst(
                                            origin = origin,
                                            colors = listOf(color, accentColor, goldColor, Color.White),
                                            count = 24
                                        )
                                    },
                                    onStartTimer = { viewModel.startTimer(task) },
                                    onPauseTimer = viewModel::pauseTimer,
                                    onResumeTimer = viewModel::resumeTimer,
                                    onCancelTimer = viewModel::cancelTimer,
                                    onIncrementCount = { viewModel.incrementCount(task) },
                                    onEdit = { viewModel.startEdit(task) },
                                    onOpenFocus = { focusTask = task },
                                    onRestartTimer = { viewModel.restartTimer(task) },
                                    onUndo = { viewModel.undoTask(task) },
                                    onMakeup = { viewModel.makeupYesterday(task) },
                                    onDelete = { origin, color ->
                                        viewModel.deleteTask(task)
                                        particleController.burst(
                                            origin = origin,
                                            colors = listOf(color, accentColor, goldColor, Color.White),
                                            count = 26
                                        )
                                    }
                                )
                            }

                            if (!uiState.isLoading &&
                                uiState.activeTasks.isEmpty() &&
                                uiState.completedTasks.isEmpty()
                            ) {
                                item(key = "empty") {
                                    EmptyState(
                                        modifier = Modifier.fillMaxWidth().padding(top = 72.dp)
                                    )
                                }
                            }

                            if (uiState.completedTasks.isNotEmpty()) {
                                item(key = "completed_header") {
                                    Text(
                                        text = "已完成 ${uiState.completedTasks.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                items(items = uiState.completedTasks, key = { "completed_${it.id}" }) { task ->
                                    TaskCard(
                                        task = task,
                                        completed = true,
                                        highlighted = highlightedId == task.id,
                                        timer = uiState.timer,
                                        onToggleComplete = { _, _ ->
                                            viewModel.toggleComplete(task, false)
                                        },
                                        onStartTimer = { viewModel.startTimer(task) },
                                        onPauseTimer = viewModel::pauseTimer,
                                        onResumeTimer = viewModel::resumeTimer,
                                        onCancelTimer = viewModel::cancelTimer,
                                        onIncrementCount = { viewModel.incrementCount(task) },
                                        onEdit = { viewModel.startEdit(task) },
                                        onOpenFocus = { focusTask = task },
                                        onRestartTimer = { viewModel.restartTimer(task) },
                                        onUndo = { viewModel.undoTask(task) },
                                    onMakeup = { viewModel.makeupYesterday(task) },
                                        onDelete = { origin, color ->
                                            viewModel.deleteTask(task)
                                            particleController.burst(
                                                origin = origin,
                                                colors = listOf(color, accentColor, goldColor, Color.White),
                                                count = 26
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ParticleOverlay(
        controller = particleController,
        modifier = Modifier.fillMaxSize()
    )

    val draft = uiState.draft
    if (draft != null) {
        EditScreen(
            draft = draft,
            onDraftChange = viewModel::updateDraft,
            onSave = viewModel::saveDraft,
            onCancel = viewModel::cancelCreate
        )
    }

    if (showStatsSheet) {
        StatsScreen(onDismiss = { showStatsSheet = false })
    }

    val focus = focusTask
    if (focus != null) {
        FocusTimerSheet(
            task = focus,
            onDismiss = { focusTask = null },
            onEdit = {
                focusTask = null
                viewModel.startEdit(focus)
            }
        )
    }

    if (showSettings) {
        SettingsScreen(
            themeMode = themeMode,
            onThemeModeChange = settingsViewModel::setThemeMode,
            allowMakeup = uiState.allowMakeup,
            onAllowMakeup = viewModel::setAllowMakeup,
            quietHours = uiState.quietHours,
            onQuietEnabled = viewModel::setQuietEnabled,
            onQuietStart = viewModel::setQuietStart,
            onQuietEnd = viewModel::setQuietEnd,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun PermissionBanner(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "通知权限未开启", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "提醒到点将无法送达，可前往系统设置开启。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onOpenSettings) { Text("去设置") }
    }
}
