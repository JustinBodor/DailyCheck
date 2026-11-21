package com.example.dailycheck.ui.screens.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailycheck.data.settings.AppThemeMode
import com.example.dailycheck.data.settings.SettingsRepository
import com.example.dailycheck.widget.refreshTodayWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = repository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppThemeMode.SYSTEM
        )

    fun setThemeMode(mode: AppThemeMode) {
        // 1) 先改系统级 night mode，触发全进程 Configuration 变更 → 小组件收到重绘
        val nightMode = when (mode) {
            AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        // 2) 写入 DataStore（主程序主题来源）
        viewModelScope.launch {
            repository.setThemeMode(mode)

            // 3) 保险：主动刷新所有 Glance 实例
            refreshTodayWidget(appContext)
        }
    }
}
