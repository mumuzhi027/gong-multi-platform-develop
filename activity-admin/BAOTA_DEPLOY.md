# 宝塔部署：gong-admin.mumuzhi.cloud

## 1. 后端环境变量

在 `back/GongGong-develop/docker-compose.yaml` 同级的 `.env` 中设置：

```env
ACTIVITY_SUPERADMIN_NAME=三翼青融媒体中心
ACTIVITY_SUPERADMIN_PASSWORD=替换为足够长的随机密码
ACTIVITY_ADMIN_ACCOUNTS_JSON={"三翼":"替换为至少10位密码","示例部门":"替换为至少10位密码"}
ACTIVITY_ADMIN_ORIGIN=https://gong-admin.mumuzhi.cloud
```

重新构建代理服务：

```bash
cd /www/wwwroot/GongGong-develop
docker compose up -d --build proxy
docker compose logs --tail=100 proxy
```

## 2. 静态网站

将管理端 `dist/` 内的文件解压到：

```text
/www/wwwroot/gong-activity-admin
```

宝塔中新建站点 `gong-admin.mumuzhi.cloud`，网站目录选择上述目录，申请并开启 SSL。

## 3. Nginx 配置

在该站点 Nginx 配置的 `server { ... }` 内加入：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8000/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 60s;
    client_max_body_size 9m;
}

location / {
    try_files $uri $uri/ /index.html;
}
```

保存后重建代理容器并在宝塔中重载 Nginx。最高管理员账号和密码分别来自 `ACTIVITY_SUPERADMIN_NAME`、`ACTIVITY_SUPERADMIN_PASSWORD`；普通组织账号来自单行 JSON 变量 `ACTIVITY_ADMIN_ACCOUNTS_JSON`。修改这些变量后需要重建或重启代理容器才会生效。旧部署若暂未设置新密码变量，可临时用旧 `ACTIVITY_ADMIN_TOKEN` 的值作为最高管理员密码；它不再作为 API Bearer 令牌。

部署时保留 Docker 的 `proxy_files` 数据卷：组织账号、活动、功能开关和操作日志保存在该卷。需同时部署新后端和新管理端；旧静态管理页的令牌登录不再适用。

## 4. 验证

```bash
curl http://127.0.0.1:8000/activities
curl -I https://gong-admin.mumuzhi.cloud
```

登录后，侧栏的“后端日志”可以查看最近请求、HTTP 状态、耗时和错误摘要。登录事件会记录提交登录的学号、成功或失败状态及失败原因；不会记录密码、管理员令牌、其他请求参数或请求正文。
