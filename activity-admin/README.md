# 拱拱活动管理台

## 启动

```bash
npm install
npm run dev
```

后端需要设置管理员令牌：

```bash
ACTIVITY_ADMIN_TOKEN=replace-with-a-long-random-token
```

浏览器进入管理台时填写后端地址和同一令牌。令牌仅保存在当前浏览器会话中。管理端还提供受同一令牌保护的后端请求日志页面，用于查看状态码、耗时和错误摘要。

活动海报用于安卓端首页概览、活动列表和详情页头图，海报主标题和副标题叠加显示在海报上；活动内容图片独立显示在详情页“活动图片”区域。没有海报时使用拱拱系统卡片色。

活动的开始、结束日期会自动换算持续天数。结束日期当天仍显示在“近期活动”，从次日起自动归入安卓端和管理端的“往期活动”；往期活动不会再出现在首页活动概览中。

## 构建

```bash
npm run build
```

将 `dist/` 部署到任意静态站点即可。后端接口包括公开的 `GET /activities`，以及需要管理员令牌的 `/admin/activities` 和 `/admin/activity-posters`。

`gong-admin.mumuzhi.cloud` 的宝塔和 Nginx 配置见 [BAOTA_DEPLOY.md](./BAOTA_DEPLOY.md)。
