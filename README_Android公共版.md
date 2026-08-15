# 摸鱼工作台 Android v1.0.2

核心修复：迁移不再依赖系统剪贴板。

迁移流程：
旧 爱摸鱼的XCL v3.0.9
→ 将记录压缩为 gzip
→ Base64URL 编码
→ moyuwb://import 深链接携带数据
→ 摸鱼工作台 v1.0.2 直接接收并解压
→ 合并到 moyu-workbench-v1
→ 提示“已迁移 X 条记录”

兼容：
仍保留 clipboard 模式，用于兼容旧迁移补丁。

版本：
versionCode = 3
versionName = 1.0.2
applicationId = com.moyu.workbench

固定签名不变，可覆盖安装 v1.0.0 / v1.0.1 并保留已有数据。
