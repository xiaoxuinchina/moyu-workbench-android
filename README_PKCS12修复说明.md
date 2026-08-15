# 摸鱼工作台 Android v1.0.0 · PKCS12 签名修正版

本版修复 GitHub Actions `Tag number over 30 is not supported`。

原因：发布密钥实际是 PKCS12 格式，旧工程文件名虽然为 `.jks`，但 Gradle signingConfig 没有显式指定 storeType。

修改：
- `app/build.gradle.kts` 增加 `storeType = "PKCS12"`。
- Actions 将密钥恢复为 `moyu-release.p12`。
- 构建前先执行 `keytool -list -storetype PKCS12` 验证密钥和 Secret。
- 原来的 4 个 GitHub Secrets 不需要修改。

上传后重新运行 `Build Signed Moyu Workbench APK` 即可。
