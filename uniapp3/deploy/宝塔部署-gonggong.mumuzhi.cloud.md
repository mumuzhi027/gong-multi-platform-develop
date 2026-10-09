# 宝塔部署：gonggong.mumuzhi.cloud

已准备的最新前端部署包：仓库内 `tmp/deploy/gong-pwa-ios3-88802ecaaabfe198.zip`。本版针对反馈中的屏幕 874px、视口 812px、顶部安全区 62px 组合补上兼容处理：仅在 iOS 桌面模式且缺少的高度恰好等于顶部安全区时，恢复页面高度；导航随页面贴底。顶部加入完整实色安全区，在受影响的 Safari 27 桌面模式中额外避让 16px，尝试避开系统模糊边缘。保留上一版安全区、内部滚动和登录修复。压缩包包含当前已构建的全部 184 个网站文件，内容已逐一与构建产物核对。以下步骤需要在你的宝塔面板完成，本次没有连接或修改服务器。

iPhone 用户接受更新后，从多任务界面关闭拱拱，再从桌面图标重新打开。浏览器布局检查不能验证 iOS 系统模糊层，仍需问题手机实测。如果仍异常，先回首页，再通过左上角菜单进入“设置 → 界面显示反馈”，选择“复制信息”，将内容和新截图提供给维护者。界面版本应显示 `20261009-ios3`，激活 worker 应为 `88802ecaaabfe198`。反馈新增 vh/dvh/lvh 实测值和高度恢复状态，区分原始视口与修正后的页面高度。对于上述异常组合，预期页面高度和导航底部都是 874px、导航底部留白 34px；视口原值仍可能是 812px。不读取账号、密码、个人数据，不自动上传，也不清理用户数据。

## 1. 域名与网站

在 `mumuzhi.cloud` 的 DNS 管理中，确认主机记录 `gonggong` 指向这台宝塔云服务器公网 IP。通常使用 A 记录。服务器安全组和防火墙放通 80、443。

在宝塔“网站”中添加站点：

- 域名：`gonggong.mumuzhi.cloud`
- 网站目录：`/www/wwwroot/gonggong.mumuzhi.cloud`（如果面板指定其他目录，以实际目录为准）
- PHP 版本：纯静态
- 数据库：不创建
- Web 服务：Nginx

该域名的站点已存在时，使用已有站点设置。需要让 Nginx 从本步骤指定的目录读取 PWA 文件。

## 2. 上传

在宝塔文件管理中进入该网站目录，上传前端部署包并解压。解压后的目录应为：

```text
/www/wwwroot/gonggong.mumuzhi.cloud/
  index.html
  sw.js
  manifest.webmanifest
  assets/
  static/
  pwa/
```

`index.html` 应直接位于网站目录，不能多套一层 `h5/`。这个包已经编译好了，不需要在宝塔上安装 Node 或运行 `npm run dev:h5`。

## 3. HTTPS

进入该网站设置中的 SSL 选项，申请有效证书（例如 Let's Encrypt），部署证书并开启强制 HTTPS。证书申请时的 DNS/HTTP 验证按面板提示完成。

证书已配置时沿用原证书。保留宝塔生成的证书路径和证书续期配置。

## 4. 接入现有后端

打开同目录的 `gonggong.mumuzhi.cloud.locations.conf` 文件，将里面的 `location` 配置加入该网站配置文件的 `server { ... }` 内。

注意位置：不要粘贴到整个配置文件的最后一个 `}` 外面。如果已有相同的 `location /`、`location /api/`、`location /web/`、`location /static/` 等配置，要合并或替换为本片段对应配置，不能重复定义。不要把片段粘贴进“伪静态”输入框。

保留原配置中的 `server_name`、`root`、SSL 证书、证书验证与日志设置。

此配置默认转发到项目正在使用的后端：

```text
浏览器：https://gonggong.mumuzhi.cloud/api/login
后端：  http://101.43.27.78:8000/login
```

已有海报地址和条款网站也通过同域代理加载。两处 `proxy_pass` 末尾的 `/` 必须保留。如果后端实际上在同一台服务器的本机 8000 端口，把两处后端地址改成 `http://127.0.0.1:8000/`，前提是该地址确实能连接后端。

保存配置，确保检查通过，然后重载 Nginx。若提示 `duplicate location`，查找并合并重复块；若提示 `location directive is not allowed here`，检查片段是否位于 `server` 内。

## 5. 检查与安装

访问 https://gonggong.mumuzhi.cloud/ ，确认登录页显示正确，然后用真实账号验证课程、成绩、空教室和图片。

另外检查：

- https://gonggong.mumuzhi.cloud/sw.js 应返回脚本。
- https://gonggong.mumuzhi.cloud/manifest.webmanifest 应返回 JSON。
- https://gonggong.mumuzhi.cloud/pwa/apple-touch-icon.png 应返回拱拱图标。
- 真实登录不能返回网页 HTML 或 502。502 通常需要检查服务器能否连接后端 8000 端口；页面正常但登录失败也要查看接口实际响应。

在 iPhone Safari 中打开正式地址，通过分享菜单选择“添加到主屏幕”；如果出现“作为 Web App 打开”，保持开启。之后从桌面图标打开并完成登录，使用独立 App 窗口。

首次联网打开后等待离线资源缓存完成，再检查断网重开。离线查看的是已经缓存的课程等数据，未加载过的远程图片不保证断网可用。

## 6. 后续更新

在完整项目的 `uniapp3` 目录重新执行 `npm run build:h5`，上传 `dist/build/h5` 内的全部最新文件。

发布顺序：资源目录 → `index.html` 与 manifest → 最后 `sw.js`。保留上一版带哈希文件名的 `assets/` 文件。用户联网打开后收到更新提示，点“立即更新”；也可以在设置里检查更新。无需重新添加桌面图标。

若使用 CDN，刷新 HTML、manifest、Service Worker 及 `static/`、`pwa/` 的缓存。

当前后台通知尚未接入；上线网站不会自动产生课程或考试后台提醒。正式发布前仍需核对完整条款内容和 iPhone 真机体验。

参考：[Apple 主屏幕安装说明](https://support.apple.com/en-lamr/guide/iphone/iphea86e5236/ios)、[Nginx 代理规则](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)。
