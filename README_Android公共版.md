# 摸鱼工作台 Android v1.0.1

更新内容：
- versionCode 2 / versionName 1.0.1
- 与 v1.0.0 使用相同 applicationId：com.moyu.workbench
- 继续使用同一套固定 PKCS12 签名 Secrets，可直接覆盖安装并保留原数据
- 新增 WebView 文件选择器支持，允许用户从手机相册上传专属头像
- 迁移流程改为静默检测；成功后只显示“已迁移 X 条记录”
- 迁移采用合并策略，不覆盖新版里已有记录
- 迁移成功后自动清除剪贴板中的迁移包，避免再次检测

部署：
覆盖上传到 moyu-workbench-android，GitHub Actions 会生成 摸鱼工作台-v1.0.1.apk。
