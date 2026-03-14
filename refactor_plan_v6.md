# Fcitx5-Android 短信验证码插件重构计划 (第六阶段 - 极限精简)

## 1. 目标
挑战代码库的物理极限，移除所有非运行必须的辅助脚本和配置，将 Kotlin 代码逻辑压缩到最简。

## 2. 重构任务

### 2.1 移除资产与脚本
- [ ] 保留 `src/main/assets/descriptor.json`（Fcitx5-Android 会读取它；可使用最小空描述符）。
- [ ] 移除 `check_upstream.sh` 脚本（对插件运行无贡献）。

### 2.2 极致精简 `SmsPlugin.kt`
- [ ] 合并正则表达式为单一 Pattern，减少循环。
- [ ] 移除 `Log` 打印，进一步减少代码行数（或仅保留 Error Log）。
- [ ] 简化 `MainService` 的 `IBinder` 获取方式。

### 2.3 极致简化配置文件
- [ ] 清理 `gradle.properties`，仅保留 `android.useAndroidX`。
- [ ] 极简 `settings.gradle.kts`，移除冗余的仓库管理块（若 `build.gradle.kts` 能完全覆盖）。

### 2.4 清单文件属性压缩
- [ ] 移除 `AndroidManifest.xml` 中的 `android:allowBackup="false"` 等非功能性默认属性。

## 3. 验证计划
1. **构建回归**：确保在移除资产和脚本后，构建流程依然完整。
2. **虚拟机回归**：验证 Fcitx5 是否依然能正确加载 `org.fcitx.sms`。
3. **性能验证**：确保正则合并后提取逻辑依然准确。
