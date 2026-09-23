# 拱拱活动管理台

## 启动

```bash
npm install
npm run dev
```

后端首次部署需在服务器 `.env` 设置超级管理员初始密码：

```bash
ACTIVITY_SUPERADMIN_NAME=三翼青融媒体中心
ACTIVITY_SUPERADMIN_PASSWORD=replace-with-a-long-random-password
ACTIVITY_ADMIN_ACCOUNTS_JSON={"三翼":"replace-with-another-long-password","示例部门":"replace-with-a-third-long-password"}
```

浏览器使用账号 `三翼青融媒体中心` 和初始密码登录。超级管理员可创建组织账号、重置密码和删除无活动的账号；普通账号只可修改自己发布的活动，首页推荐与排序值仅超级管理员可改。管理端会话令牌仅保存在当前浏览器会话中。所有账号可查看活动操作日志，只有超级管理员可查看后端请求日志与用户统计。

拱拱 AI 客户端入口由超级管理员的功能开关控制，默认关闭；关闭时后端 AI 接口也返回不可用。活动组织筛选由已注册账号及其近期/往期已发布活动自动生成。

活动海报用于安卓端首页概览、活动列表和详情页头图，海报主标题和副标题叠加显示在海报上；活动内容图片独立显示在详情页“活动图片”区域。没有海报时使用拱拱系统卡片色。

活动的开始、结束日期会自动换算持续天数。结束日期当天仍显示在“近期活动”，从次日起自动归入安卓端和管理端的“往期活动”；往期活动不会再出现在首页活动概览中。

## 构建

```bash
npm run build
```

将 `dist/` 部署到任意静态站点即可。后端接口包括公开的 `GET /activities`、`GET /activity-organizers`、`GET /public-features`，以及会话鉴权的 `/admin/activities` 和 `/admin/activity-posters`。

`gong-admin.mumuzhi.cloud` 的宝塔和 Nginx 配置见 [BAOTA_DEPLOY.md](./BAOTA_DEPLOY.md)。
