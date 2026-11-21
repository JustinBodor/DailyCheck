# 开发流程记录（DEV FLOW）

按时间顺序记录从零到可运行 App 的完整迭代过程，包含关键决策与踩坑。

---

## 阶段 0 · 环境与工程搭建

1. 使用 Android Studio 新建 Compose 工程，包名 `com.example.dailycheck`。
2. 固定工具链：
   - Kotlin 2.2.20 + KSP 2.2.20-2.0.4
   - AGP 8.13.0 + Gradle 8.13 + JDK 17
   - compileSdk / targetSdk 35，minSdk 26
   - Compose BOM 2025.06.01、Room 2.8.5、Hilt 2.57.1、WorkManager 2.12.0
3. 构建脚本由 Kotlin DSL 改为 Groovy：避免构建时额外下载 Gradle 源码包导致卡死。
4. `gradle.properties` 固定 `org.gradle.java.home` 为 JDK 17，解决 Gradle 与 JDK 25 不兼容。

---

## 阶段 1 · P0 最小可用

### 数据层
- `TaskEntity`（任务）、`CheckInRecordEntity`（打卡记录，(taskId, epochDay) 唯一）。
- `TaskDao` / `CheckInRecordDao` 全部返回 Flow。
- `AppDatabase` v2，附 `MIGRATION_1_2`（新增任务类型与计时字段、建打卡表）。

### 首页
- `HomeViewModel` 合并任务表 + 打卡记录 + 计数状态，拆成「待打卡 / 已完成」。
- `TaskCard`：自定义滑动卡片，右滑补打卡、左滑删除、点击编辑。
- `EditScreen`：底部抽屉表单（名称、类型、图标、颜色、分类、备注、重复、提醒）。

### 提醒
- `ReminderWorker`（HiltWorker）+ `ReminderScheduler`，支持免打扰时段。

---

## 阶段 2 · 计时 / 视觉 / 撤销

- 新增计时型任务：`TaskTimerService`（specialUse 前台服务 + 常驻通知 + 每秒 tick）。
- `TimerStateStore`（DataStore）持久化计时状态，进程被杀后可恢复。
- 粒子系统：Canvas 全屏光点，打卡 / 删除时从圆圈或卡片中心爆发。
- 撤销打卡：再次点击圆圈、左滑「撤销打卡」、长按菜单。
- 全局主题：纯色背景 + 卡片 token（`#1A1B1F` / `#F7F8FA`、`#2A2B30` / White）。

---

## 阶段 3 · 编辑体验打磨

- 图标库扩到 20 个 Emoji，`LazyRow` 每屏 6 个，`sh.calvin.reorderable` 长按拖拽排序，DataStore 持久化顺序。
- 颜色选择从色块列表改为彩虹滑轨（`ColorSlider`），HSV 取色，300ms 线性补间。
- 分类读取 Room 历史分类做 `ExposedDropdownMenu` 补全。
- 名称限制 8 字 + 红色提示。
- 计时任务支持数字 + 单位（秒/分钟/小时/天），统一换算为秒入库（v3 迁移）。

---

## 阶段 4 · 统计页

- `StatsRepository` + `StatsViewModel`：总打卡次数、连续天数、当月每日完成次数与任务颜色/名称。
- 月历组件：完整日期网格、星期表头、今天与打卡热力、点击弹窗（图标 + 8 字任务名）。
- 以 ModalBottomSheet 呈现，避免导航跳转与闪烁。

---

## 阶段 5 · 桌面小组件（Glance）

- 2x2 极简组件（待办数量 + 鼓励语，点击进首页）。
- 4x2 中号组件：左侧月历热力、右侧今日任务摘要；后续多轮调整为「左日历 + 右不透明灰卡 + 巨大数字」。
- 主题：组件读取 `uiMode`；App 内切换主题通过 `AppCompatDelegate.setDefaultNightMode` + `GlanceAppWidgetManager` 主动刷新。
- 打卡数据变化后 `refreshTodayWidget()` 刷新所有实例。

---

## 阶段 6 · 缺陷修复与细节

- 修复系统栏遮挡：编辑页/统计页统一 `statusBarsPadding` 与抽屉内边距。
- 修复粒子过脏、飞行过远：强阻尼 + 短生命周期 + 半径上限。
- 修复日期计算错位、月份最后一行被裁剪：按真实 `leadingBlanks` 计算上月/下月日期，压缩格高保证 6 行完整。
- 修复计时与打卡冲突：计时型任务移除直接打勾，只能通过专注抽屉开始/暂停/结束/完成。
- 修复图标顺序 / 颜色顺序重启丢失：写入走 App 级作用域，启动预读，StateFlow 非空初值消除闪烁。
- 修复主题切换后小组件不刷新：AppCompatDelegate + Glance 强制刷新双保险。

---

## 构建与验证

```bash
./gradlew assembleDebug     # 生成 debug APK
./gradlew assembleRelease   # 未签名 release APK
```

每轮改动后均在本地完整编译通过；真机安装存在 USB 安装限制与签名不一致问题，需卸载旧版后重装。
