# 小企鹅输入法（短信验证码插件）

[![License](https://img.shields.io/github/license/jiuyuan-light/fcitx5-android-SMSVerificationCode-plugin)](LICENSE)
[![Build Status](https://github.com/jiuyuan-light/fcitx5-android-SMSVerificationCode-plugin/workflows/Auto%20Release/badge.svg)](https://github.com/jiuyuan-light/fcitx5-android-SMSVerificationCode-plugin/actions)

一个为 [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android) 设计的插件：接收到短信或通知时自动提取验证码，并复制到系统剪贴板。

## ✨ 功能特性

- **多渠道监听**：
  - 广播监听：通过 `SMS_RECEIVED` 广播捕获短信内容。
  - 通知监听：通过通知栏监听服务（Notification Listener）作为兜底方案，提取通知内容中的验证码。
- **无感体验**：提取成功后自动写入剪贴板，Fcitx5 输入法剪贴板面板可直接调用。

## 🚀 使用方法

1. 安装 **Fcitx5 Android** 主程序。
2. 下载并安装本插件 APK。
3. 在 **Fcitx5 输入法设置 → 插件** 中打开本插件页面，授予以下权限：
   - **短信权限**：用于接收短信验证码。
   - **通知权限**（可选）：用于从通知栏提取验证码（推荐开启以获得更好兼容性）。
4. 在 Fcitx5 输入法设置中，确保已启用剪贴板功能。
5. 当收到验证码短信时，验证码将自动出现在 Fcitx5 的剪贴板历史或候选词中（取决于主程序实现）。

## 🛠️ 构建指南

本项目使用 Gradle 构建。
构建前请先设置 `JAVA_HOME` 和 `ANDROID_HOME`，或在 `local.properties` 中配置 `org.gradle.java.home` 与 `sdk.dir`（可参考 `local.properties.templete`）。

### 环境要求
- JDK 11+
- Android SDK Platform & Build-Tools 35

### 编译命令
```bash
# 克隆仓库
git clone https://github.com/jiuyuan-light/fcitx5-android-SMSVerificationCode-plugin.git
cd fcitx5-android-SMSVerificationCode-plugin

# 编译 Debug 包
./gradlew assembleDebug

# 编译 Release 包
./gradlew assembleRelease -x lint
```

## 🤝 贡献与致谢

本项目灵感来源于以下开源项目：
- [fcitx5-android](https://github.com/fcitx5-android/fcitx5-android)
- [SmsForwarder](https://github.com/pppscn/SmsForwarder)
- [SMS2Clipboard](https://github.com/silica-github/SMS2Clipboard)
- [SmsCodeHelper](https://github.com/RikkaW/SmsCodeHelper)
- [otphelper](https://github.com/jd1378/otphelper)

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源。
