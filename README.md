# 小企鹅输入法（短信验证码插件）
[![License](https://img.shields.io/github/license/jiuyuan-light/fcitx5-android-SMSVerificationCode-plugin)](LICENSE)
[![Build Status](https://github.com/jiuyuan-light/fcitx5-android-SMSVerificationCode-plugin/workflows/Auto%20Release/badge.svg)](https://github.com/jiuyuan-light/fcitx5-android-SMSVerificationCode-plugin/actions)

一个为 [Fcitx5 Android](https://github.com/fcitx5-android/fcitx5-android) 设计的插件：接收到短信或通知时自动提取验证码，并复制到系统剪贴板。

## 功能特点
- 短信监听：通过 `SMS_RECEIVED` 广播提取验证码并复制到剪贴板。
- 通知监听：可选开启通知监听权限，从通知内容提取验证码。
- 关键字配置：在插件界面配置关键字（逗号分隔），提升匹配准确度。

## 使用方法
1. 安装 **Fcitx5 Android** 主程序。
2. 下载并安装本插件 APK。
3. 在 **Fcitx5 输入法设置 → 插件** 中打开本插件页面，授予短信权限，需要时开启通知监听权限。
4. 在插件界面配置验证码关键字（逗号分隔）。
5. 在 Fcitx5 输入法设置中，确保已启用剪贴板功能。
6. 当收到验证码短信或通知时，验证码会自动出现在 Fcitx5 的剪贴板历史或候选词中（取决于主程序实现）。

提示：如需从通知提取验证码，请在系统设置中开启通知监听权限。关键字可在插件界面配置并保存。

## 默认关键字
默认关键字来自 `default_keywords.txt`（支持 `#` 开头的注释行），如误操作可直接从下方复制恢复（逗号分隔）：
一次性, 口令, 动态码, 取件码, 提货码, 校验码, 确认码, 验证码

## 构建指南
本项目使用 Gradle 构建。构建前请先设置 `JAVA_HOME` 与 `ANDROID_HOME`，或在 `local.properties` 中配置 `org.gradle.java.home` 与 `sdk.dir`（可参考 `local.properties.templete`）。

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

说明：`src/main/assets/descriptor.json` 为占位文件，`sha256` 与 `files` 会在发布或打包时更新。`src/main/res/xml/plugin.xml` 仅包含插件元信息。

## 贡献与致谢
- [fcitx5-android](https://github.com/fcitx5-android/fcitx5-android)
- [SmsForwarder](https://github.com/pppscn/SmsForwarder)
- [SMS2Clipboard](https://github.com/silica-github/SMS2Clipboard)
- [SmsCodeHelper](https://github.com/RikkaW/SmsCodeHelper)
- [otphelper](https://github.com/jd1378/otphelper)

## 许可证
本项目采用 [MIT License](LICENSE) 开源。
