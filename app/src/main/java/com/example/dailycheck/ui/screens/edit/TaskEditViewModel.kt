package com.example.dailycheck.ui.screens.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailycheck.data.repository.TaskRepository
import com.example.dailycheck.data.settings.SettingsRepository
import com.example.dailycheck.ui.components.TaskStyleCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 编辑页辅助数据：分类、图标顺序、颜色顺序。
 *
 * 防闪烁关键点：
 * 1. 图标/颜色列表都有"非空默认值"，UI 首帧即可渲染；
 * 2. 仓库在 App 启动阶段已把 DataStore 预读进内存，
 *    编辑页构造时的初值直接取内存快照，一般就是用户保存过的顺序，
 *    不会出现"先默认、再刷新"的跳变；
 * 3. UI 层没有任何 isLoading 分支。
 */
@HiltViewModel
class TaskEditViewModel @Inject constructor(
    repository: TaskRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val categories: StateFlow<List<String>> = repository.observeCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** 图标：先用内存快照/默认值做初值，DataStore 到达后只在内容不同时才替换 */
    private val _iconList = MutableStateFlow(
        resolveIcons(settingsRepository.currentIconOrder())
    )
    val iconList: StateFlow<List<String>> = _iconList.asStateFlow()

    /** 颜色顺序（十六进制串），stateIn + 非空初值，无闪烁 */
    val colorList: StateFlow<List<String>> = settingsRepository.colorOrder
        .map { stored -> stored.ifEmpty { defaultColorHex() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.currentColorOrder().ifEmpty { defaultColorHex() }
        )

    init {
        viewModelScope.launch {
            settingsRepository.iconOrder.collect { indices ->
                val resolved = resolveIcons(indices)
                if (resolved != _iconList.value) {
                    _iconList.value = resolved
                }
            }
        }
    }

    /**
     * 拖拽过程中的即时排序：只改内存，保证跟手。
     * 落盘在拖拽结束的 persistIconOrder() 里完成（也顺带兜底）。
     */
    fun moveIcon(from: Int, to: Int) {
        val current = _iconList.value
        if (from == to) return
        if (from !in current.indices || to !in current.indices) return

        _iconList.value = current.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** 拖拽结束（onDragStopped）时调用：把最终顺序写入 DataStore */
    fun persistIconOrder() {
        val order = _iconList.value
        viewModelScope.launch {
            settingsRepository.saveIconOrder(order)
        }
    }

    /** 保存任务时把当前颜色提到最前，并持久化颜色顺序 */
    fun rememberColor(argb: Long) {
        val hex = String.format("%08X", argb)
        val current = colorList.value
        val next = (listOf(hex) + current.filter { it != hex }).take(8)
        viewModelScope.launch {
            settingsRepository.saveColorOrder(next)
        }
    }

    /** 颜色顺序保存（如需调整最近颜色顺序时调用） */
    fun persistColorOrder(order: List<String>) {
        viewModelScope.launch {
            settingsRepository.saveColorOrder(order)
        }
    }

    private fun resolveIcons(indices: List<Int>): List<String> {
        if (indices.isEmpty()) return TaskStyleCatalog.icons
        val ordered = indices.mapNotNull { TaskStyleCatalog.icons.getOrNull(it) }
            .distinct()
        val missing = TaskStyleCatalog.icons.filter { it !in ordered }
        return ordered + missing
    }

    private fun defaultColorHex(): List<String> =
        TaskStyleCatalog.colors.map { String.format("%08X", it) }
}
