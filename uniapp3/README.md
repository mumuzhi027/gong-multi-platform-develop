# 拱拱 uni-app 3

这是根据仓库中的 Kotlin Compose Multiplatform 客户端复现的独立 uni-app 3（Vue 3）工程。原有 `composeApp`、`iosApp` 与 `back` 目录不受影响。

## 开发与构建

```powershell
npm install
npm run dev:h5
npm run build:mp-weixin
```

微信小程序构建结果位于：

```text
dist/build/mp-weixin
```

用微信开发者工具导入该目录即可预览。

## 上线前配置

1. 在 `src/manifest.json` 的 `mp-weixin.appid` 填入真实小程序 AppID。
2. 将 `src/config/env.js` 中的 API 地址改为已备案且支持 HTTPS 的正式域名。
3. 在微信公众平台的“开发管理 → 开发设置 → 服务器域名”中添加该 HTTPS 域名。
4. 如启用订阅消息，在微信公众平台创建消息模板，并在通知设置页接入真实模板 ID。
5. 将最终版用户协议、隐私政策和权限说明替换到 `src/pages/clause/index.vue`，或配置合法业务域名后使用 `web-view`。

开发者工具中关闭域名校验只能用于本地调试，不能作为正式发布方案。

使用 HBuilderX 时请直接打开本目录（包含 `package.json` 的 `uniapp3` 目录），不要只打开 `src`。工程已声明 `vueVersion: 3`，HBuilderX 会使用项目内的 Vite 编译器。

## 页面

- 登录与离线模式
- 首页、今日课程、考试安排、侧边栏
- 完整周课表与课程详情
- 今天/明天空教室及节次筛选
- 学业总览、排名与分学期成绩表
- 通知设置、认证服务、关于与条款页面

接口封装集中在 `src/api`，服务地址集中在 `src/config/env.js`，状态及本地缓存集中在 `src/stores`。
