@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.dailycheck.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailycheck.core.util.DateTimeUtils
import com.example.dailycheck.data.settings.AppThemeMode
import com.example.dailycheck.data.timer.QuietHours
import com.example.dailycheck.ui.components.TimeWheelPicker
import com.example.dailycheck.ui.theme.LocalDarkTheme
import com.example.dailycheck.ui.theme.liquidGlass

@Composable
fun SettingsScreen(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    allowMakeup: Boolean,
    onAllowMakeup: (Boolean) -> Unit,
    quietHours: QuietHours,
    onQuietEnabled: (Boolean) -> Unit,
    onQuietStart: (Int) -> Unit,
    onQuietEnd: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalDarkTheme.current
    var pickerTarget by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (dark) Color(0xFF080C18) else Color(0xFFF6F8FE),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.80f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("完成") }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ---------- 外观模式 ----------
            SettingsCard {
                Text(
                    text = "外观模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "切换后立即生效，无需重启",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                ThemeSegmented(
                    selected = themeMode,
                    dark = dark,
                    onSelect = onThemeModeChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------- 打卡 ----------
            SettingsCard {
                SwitchRow(
                    title = "允许补打卡",
                    subtitle = "仅支持补昨天，默认开启",
                    checked = allowMakeup,
                    onCheckedChange = onAllowMakeup
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------- 免打扰 ----------
            SettingsCard {
                SwitchRow(
                    title = "免打扰",
                    subtitle = "该时段内不推送提醒",
                    checked = quietHours.enabled,
                    onCheckedChange = onQuietEnabled
                )
                if (quietHours.enabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TimeRow("开始时间", quietHours.startMinutes) { pickerTarget = 0 }
                    TimeRow("结束时间", quietHours.endMinutes) { pickerTarget = 1 }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    when (pickerTarget) {
        0 -> TimeWheelPicker(
            title = "免打扰开始时间",
            initialMinutes = quietHours.startMinutes,
            onDismiss = { pickerTarget = null },
            onConfirm = { minutes ->
                pickerTarget = null
                onQuietStart(minutes)
            }
        )

        1 -> TimeWheelPicker(
            title = "免打扰结束时间",
            initialMinutes = quietHours.endMinutes,
            onDismiss = { pickerTarget = null },
            onConfirm = { minutes ->
                pickerTarget = null
                onQuietEnd(minutes)
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass()
            .padding(16.dp),
        content = content
    )
}

/** 三选一分段选择器：选中态为高对比胶囊 */
@Composable
private fun ThemeSegmented(
    selected: AppThemeMode,
    dark: Boolean,
    onSelect: (AppThemeMode) -> Unit
) {
    val activeBg = if (dark) Color.White.copy(alpha = 0.94f) else Color.Black.copy(alpha = 0.88f)
    val activeFg = if (dark) Color(0xFF0B0D16) else Color.White

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppThemeMode.entries.forEach { mode ->
            val active = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (active) activeBg
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) activeFg else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = DateTimeUtils.formatMinutes(minutes),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
