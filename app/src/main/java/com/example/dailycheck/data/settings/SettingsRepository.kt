package com.example.dailycheck.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dailycheck.ui.components.TaskStyleCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class AppThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色模式"),
    DARK("深色模式")
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "daily_check_settings"
)

/**
 * 顺序持久化模型：
 * - key_icon_order  ：图标索引顺序，如 "0,3,1,2,..."
 * - key_color_order ：颜色十六进制值顺序，如 "FF7C6CF0,FF3AA8FF,..."
 *
 * 仓库在 App 启动时（设置页 ViewModel 创建时）就开始把 DataStore 读进内存，
 * 因此编辑页打开时可以直接用同步缓存做初值，视觉上不会先默认后刷新。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("appearance_mode")
    private val keyIconOrder = stringPreferencesKey("key_icon_order")
    private val keyColorOrder = stringPreferencesKey("key_color_order")

    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _iconOrder = MutableStateFlow<List<Int>>(emptyList())
    val iconOrder: StateFlow<List<Int>> = _iconOrder.asStateFlow()

    private val _colorOrder = MutableStateFlow<List<String>>(emptyList())
    val colorOrder: StateFlow<List<String>> = _colorOrder.asStateFlow()

    init {
        // 进程启动即开始预读，供 UI 做同步初值
        writeScope.launch {
            context.settingsDataStore.data.collect { prefs ->
                _iconOrder.value = prefs[keyIconOrder]
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?: emptyList()

                _colorOrder.value = prefs[keyColorOrder]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            }
        }
    }

    // ---------- 主题 ----------

    val themeMode: Flow<AppThemeMode> = context.settingsDataStore.data
        .map { prefs ->
            prefs[themeKey]?.let { name -> AppThemeMode.entries.firstOrNull { it.name == name } }
                ?: AppThemeMode.SYSTEM
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[themeKey] = mode.name }
    }

    // ---------- 顺序读写 ----------

    fun currentIconOrder(): List<Int> = _iconOrder.value

    fun currentColorOrder(): List<String> = _colorOrder.value

    /** 传图标列表，内部折算成索引后写入 key_icon_order */
    suspend fun saveIconOrder(order: List<String>) {
        val indices = order.mapNotNull { icon -> TaskStyleCatalog.icons.indexOf(icon).takeIf { it >= 0 } }
        val value = if (indices.size == order.size && indices.isNotEmpty()) {
            indices
        } else {
            TaskStyleCatalog.icons.indices.toList()
        }
        context.settingsDataStore.edit { prefs ->
            prefs[keyIconOrder] = value.joinToString(",")
        }
        _iconOrder.value = value
    }

    /** 传颜色十六进制串（如 FF7C6CF0） */
    suspend fun saveColorOrder(order: List<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyColorOrder] = order.joinToString(",")
        }
        _colorOrder.value = order
    }
}
