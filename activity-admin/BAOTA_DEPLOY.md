# 宝塔部署：gong-admin.mumuzhi.cloud

## 1. 后端环境变量

在 `back/GongGong-develop/docker-compose.yaml` 同级的 `.env` 中设置：

```env
ACTIVITY_ADMIN_TOKEN=替换为足够长的随机令牌
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

保存后在宝塔中重载 Nginx。管理端会自动使用 `https://gong-admin.mumuzhi.cloud/api` 作为后端地址；登录令牌就是 `.env` 中的 `ACTIVITY_ADMIN_TOKEN`。

## 4. 验证

```bash
curl http://127.0.0.1:8000/activities
curl -I https://gong-admin.mumuzhi.cloud
```

登录后，侧栏的“后端日志”可以查看最近请求、HTTP 状态、耗时和错误摘要。登录事件会记录提交登录的学号、成功或失败状态及失败原因；不会记录密码、管理员令牌、其他请求参数或请求正文。
