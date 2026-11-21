# DailyCheck · 每日打卡

一个完全本地化的 Android 每日打卡 App：Kotlin + Jetpack Compose + Room + Hilt + WorkManager + Glance 桌面小组件。

- 无账号、无网络、无广告，所有数据只存在本机 Room 数据库。
- 支持「完成型 / 计数型 / 计时型」三类任务。
- 提供统计日历热力图、专注计时前台服务、桌面小组件。

---

## 功能总览

### 今日打卡
- 完成型任务：点击右侧圆圈打卡；再次点击撤销；右滑补昨天；左滑删除。
- 计数型任务：`+1` 累计，达到目标自动完成。
- 计时型任务：点击卡片进入专注抽屉，开始 / 暂停 / 结束；倒计时归零自动写入打卡记录并震动。
- 已完成区域常驻在列表底部，按完成时间倒序（后完成的更靠上）。
- 打卡 / 撤销 / 删除带粒子动画与触感反馈。

### 新建 / 编辑任务
- 底部抽屉式编辑页，24dp 圆角、24dp 内边距，与设置页风格一致。
- 图标：20 个常用 Emoji，每屏 6 个，长按拖拽排序并持久化。
- 颜色：彩虹滑轨取色（300ms 线性补间），保存时记录最近颜色。
- 计时任务：数字 + 单位（秒/分钟/小时/天）自由组合，统一换算为秒入库。
- 分类：读取 Room 中历史分类做下拉补全。
- 名称限制 8 个字，超出以红色小字提示。
- 计时任务隐藏「重复」与「提醒」模块。

### 统计
- 顶部：总打卡次数、当前连续天数。
- 月历：完整日期网格，今天蓝紫高亮、历史打卡按次数做紫色深浅热力。
- 点击有记录的日期弹出极简浮窗：图标 + 最多 8 字任务名。
- 以底部抽屉（ModalBottomSheet）呈现，与设置页一致。

### 设置
- 三种主题：跟随系统 / 浅色 / 深色（DataStore 持久化，切换即时刷新桌面组件）。
- 允许补打卡开关。
- 免打扰时段（默认 23:00–07:00），提醒到点自动跳过。

### 桌面小组件（Glance，4x2）
- 左侧：当前月完整日历，今天圆点标记、历史打卡紫色热力。
- 右侧：不透明中性灰卡片，显示「今日任务」与巨大居中数字。
- 点击组件直接打开 App 首页。
- 打卡 / 主题切换后主动刷新所有组件实例。

### 提醒与计时
- WorkManager + HiltWorker 定时提醒，支持免打扰。
- 计时使用 `specialUse` 前台服务 + 常驻通知，退后台 / 锁屏不中断。
- 计时状态存 DataStore（`TimerStateStore`），进程被杀后可恢复。

---

## 技术栈

| 领域 | 选型 |
|---|---|
| 语言 | Kotlin 2.2.20 |
| UI | Jetpack Compose（BOM 2025.06.01）、Material3 |
| 架构 | MVVM + Repository + UseCase + Hilt |
| 本地存储 | Room 2.8.5（KSP）、DataStore Preferences |
| 后台 | WorkManager 2.12.0（HiltWorker）、Foreground Service |
| 桌面组件 | Glance AppWidget 1.1.1 |
| 导航 | Navigation Compose |
| 拖拽排序 | sh.calvin.reorderable 2.5.0 |
| 构建 | AGP 8.13.0 / Gradle 8.13 / JDK 17，minSdk 26、targetSdk 35 |

---

## 目录结构

```
app/src/main/java/com/example/dailycheck/
├── core/di/                 Hilt 模块（Database / Repository）
├── core/util/               日期、重复规则工具
├── data/
│   ├── local/               Room：TaskEntity、CheckInRecordEntity、DAO、v1→v3 迁移
│   ├── model/               Task / RepeatRule / TaskType
│   ├── repository/          TaskRepository、StatsRepository
│   ├── settings/            SettingsRepository（主题、图标顺序、颜色顺序）
│   └── timer/               TimerBus、TimerStateStore
├── domain/usecase/          CheckInUseCase
├── notification/            通知渠道、提醒调度、Worker
├── permission/              通知权限工具
├── service/                 TaskTimerService（计时前台服务）
├── ui/
│   ├── animation/           粒子系统
│   ├── components/          TaskCard、EditScreen、FocusTimerSheet、ColorSlider、TimeWheelPicker…
│   ├── navigation/          AppNavHost / Routes
│   ├── screens/home/        HomeScreen、HomeViewModel、TaskDraft
│   ├── screens/edit/        TaskEditViewModel（分类 / 图标顺序 / 颜色）
│   ├── screens/settings/    SettingsScreen、SettingsViewModel
│   ├── screens/stats/       StatsScreen、StatsViewModel（月历）
│   ├── screens/timer/       TimerViewModel
│   └── theme/               Theme、Typography、GlassModifier
└── widget/                  Glance 小组件（DailyCheckWidget / TodayWidget）

app/src/main/res/            launcher 图标（自适应）、widget provider XML
docs/DEV_FLOW.md             完整开发流程记录
```

---

## 构建

1. 安装 JDK 17、Android SDK（platform 35、build-tools 35）。
2. 在项目根目录创建 `local.properties`：
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```
3. 构建 Debug APK：
   ```bash
   ./gradlew assembleDebug
   ```
   产物：`app/build/outputs/apk/debug/app-debug.apk`

Release 需自行配置签名（`*.jks` 已加入 `.gitignore`，不会上传）。

---

## 数据与隐私

- 所有任务与打卡记录仅存储在本机 Room 数据库。
- 小组件与计时服务只读本机数据，不发起任何网络请求。
- 卸载 App 即删除全部数据。

---

## 开发流程

详见 [docs/DEV_FLOW.md](docs/DEV_FLOW.md)：从 P0 基础打卡、P1 计时 / 统计 / 组件，到后续多轮 UI 与逻辑打磨的完整记录。
