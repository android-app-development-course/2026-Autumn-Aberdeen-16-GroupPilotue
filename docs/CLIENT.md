# Mycoursepractice · GroupPilot 界面演示

CS4001 人机交互 · Group 16 的课程作业界面演示。**只做 UI，不做主功能**：所有数据在内存里，重启即重置。

设计说明（配色、字体、页面清单、动效规范、演示脚本、截图索引）见 [UI设计说明.md](UI设计说明.md)。

## 跑起来

```powershell
.\gradlew.bat --offline :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.mycoursepractice/.MainActivity
```

- 需要 JDK 25（本机用 Android Studio 自带的 JBR：`C:\Program Files\Android\Android Studio\jbr`），已在 `%USERPROFILE%\.gradle\gradle.properties` 里配好 `org.gradle.java.installations.paths`。
- 全离线构建：依赖都来自本机 Gradle 缓存，不要去掉 `--offline`。
- 设备按 1080×2400 / 420dpi 调过版。

## 打包成安装包（release）

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat --offline --no-configuration-cache :app:assembleRelease
# 产物：app\build\outputs\apk\release\app-release.apk
```

- 签名配置读 `signing/signing.properties`（keystore `signing/grouppilot.keystore`，别名 `grouppilot`，口令 `GroupPilot2026`）。这个目录已被 `.gitignore` 忽略。
- **同一包名只能用同一签名升级**：keystore 丢了就只能卸载重装。
- `release` 里刻意关掉了 AGP 9 的 `optimization { packageScope = … }`——它会对 `androidx/kotlin` 重打包，实测导致启动崩溃（`IllegalAccessError: Class kotlin.collections.h extended by class s6 is inaccessible`）。
- `lint { checkReleaseBuilds = false }`：离线环境下 `lint-gradle:32.5.1` 不在缓存里，不关会直接构建失败。
- 现成安装包：`dist/GroupPilot-1.0.apk`（4.89 MB），说明见 `dist/安装说明.md`。


## 模拟器（已装到标准位置）

```powershell
# 标准位置（已从 MyApplication2 的工程内 SDK 复制过来）
C:\Users\stev\AppData\Local\Android\Sdk\emulator\emulator.exe

# 直接启动（自带冷启动参数，避开 quickboot 快照）
powershell -NoProfile -ExecutionPolicy Bypass -File tools\start-emulator.ps1
```

- AVD 名：`medium_phone`（Android 16 / android-36 / x86_64），数据仍在 `MyApplication2\.android_user\avd\medium_phone.avd`，靠 `C:\Users\stev\.android\avd\medium_phone.ini` 注册。
- 系统镜像：`C:\Users\stev\AppData\Local\Android\Sdk\system-images\android-36\google_apis_playstore\x86_64`。
- 卡死排查与 `start-emulator` 的来龙去脉见 [模拟器卡死排查.md](模拟器卡死排查.md)。

## 演示路径（点这些能看到状态真的在变）

**账号层**

1. 打开就是 `项目库`：3 门课、4 个项目，顶部一条 `周楚涵 邀请你` 的邀请
2. 点邀请 / `加入项目` → 输码 `R3F8K2` → 预览 → `加入项目`，进去后成员从 4 人变 5 人
3. `创建项目`：选课（名称会自动填）→ 选模板 `按交付物` → `创建并进入`，4 条 20/30/30/20 的工作项直接生成
4. 项目内任意 tab 左上角 `←` 回项目库；右上齿轮是应用设置

**项目层**

5. `板` → 点 `待确认` 刻度 = 筛选，再点取消
6. 点 `08 动效与转场` → 详情（编号做共享元素放大）
7. `打开链接并定位` → 证据页高亮原文
8. `确认完成` → 选 50% / 100%，看积分、总分、待确认数三个数一起变
9. `确认` → 详情盖 `已确认`，印章落下 → `查看结算单`
10. 结算单 → 点任一成员 → 逐条算式
11. `项目设置 ›` → 邀请码 / 是否需确认 / 成员；成员视角看不到邀请码
12. 右上齿轮 `设置`（每个页面都有）→ 改名字、切换字号（大 / 中 / 小）；名字会同步到成员栏和结算单
13. 板页右上 `+` → `新建工作项`：填标题、分值、分摊 → `添加到板`，编号 13 落在「待开始」组
14. 详情页右上 `编辑` → 改分值 → `保存修改`；标题下方出现 `上次改动`，底部 `改动记录` 可展开看 `分值 5 → 8`
15. 待确认（如 08）点 `编辑`：分值/分摊/负责人置灰，只允许改标题与说明；已确认的项直接只读

## 目录

```
app/src/main/java/.../MainActivity.kt      外壳：项目库 / 项目两种模式 + 4 tab
app/src/main/java/.../ui/                  字体 / 动效 / 导航 / SegmentBar / SettingsSheet
app/src/main/java/.../data/Demo.kt         3 门课 · 4 个项目 · 邀请码 · 计分
app/src/main/java/.../data/Model.kt        Course / Project / Invitation
app/src/main/java/.../data/Types.kt        WorkItem / Evidence / 改动记录
app/src/main/java/.../data/AppSettings.kt  名字 + 字号（SharedPreferences）
app/src/main/java/.../screens/             17 个 Fragment
app/src/main/res/layout/                   29 个布局
shots/                                     50+ 张截图 + 5 段动效录屏
docs/UI设计说明.md                         设计文档
```

> 字号默认中档（1.00×）；大档 1.15×、小档 0.88×，写在 `data/AppSettings.kt` 里，`attachBaseContext` 套 `Configuration.fontScale` 生效。
