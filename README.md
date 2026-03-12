# 微信视频总结

一个 Android 应用，通过 Accessibility Service 读取微信视频号内容，自动发送到 OpenClaw 进行总结。

## 功能

- 自动检测微信视频号界面
- 提取视频标题和文字内容
- 一键发送到 OpenClaw API 进行 AI 总结

## 安装

1. 下载最新的 APK 文件（从 [Releases](../../releases) 页面）
2. 在 Android 手机上安装（需要允许未知来源应用）
3. 打开应用，点击「开启无障碍服务」
4. 在系统设置中找到「微信视频总结」并开启
5. 返回应用，点击「开始捕获」
6. 去微信看视频
7. 看完后返回应用，点击「停止并总结」

## 配置

打开 `WeChatVideoService.kt`，修改 `OPENCLAW_API` 常量为你的 OpenClaw 服务地址：

```kotlin
private const val OPENCLAW_API = "http://你的IP:18789/api/summarize"
```

## 构建

```bash
./gradlew assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 权限

- 无障碍服务（读取屏幕内容）
- 网络权限（发送数据到 OpenClaw）

## 隐私声明

- 仅读取微信视频号的公开文字内容
- 不会进行任何自动化操作（点赞、评论等）
- 数据仅发送到你指定的 OpenClaw 服务器

## 兼容性

- Android 8.0+ (API 26)
- 测试设备：一加 11 (PHB110)