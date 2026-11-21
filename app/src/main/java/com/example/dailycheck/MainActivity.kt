package com.example.dailycheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailycheck.data.settings.AppThemeMode
import com.example.dailycheck.notification.NotificationHelper
import com.example.dailycheck.ui.navigation.AppNavHost
import com.example.dailycheck.ui.screens.settings.SettingsViewModel
import com.example.dailycheck.ui.theme.DailyCheckTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    /** 通知点击带过来的 taskId，消费后置空，避免重复滚动 */
    private val pendingTaskId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 明确交给 Compose 处理系统栏 inset，避免状态栏压住标题
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        pendingTaskId.value = intent.extractTaskId()

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            DailyCheckTheme(darkTheme = darkTheme) {
                val taskId by pendingTaskId.collectAsState()
                AppNavHost(
                    highlightTaskId = taskId,
                    onHighlightConsumed = { pendingTaskId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTaskId.value = intent.extractTaskId()
    }

    private fun Intent?.extractTaskId(): Long? =
        this?.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)?.takeIf { it > 0L }
}
