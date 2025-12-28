# SaaS 短链接系统

基于 Spring Cloud 的短链接生成与管理平台。

## 技术栈

- **后端**: Spring Boot 3.x, Spring Cloud, MyBatis-Plus, ShardingSphere
- **前端**: Vue 3, Element Plus, Vite
- **中间件**: MySQL 8.0, Redis, Nacos
- **部署**: Docker, Docker Compose

---

## 本地开发

### 1. 环境准备

启动本地中间件服务：

| 服务 | 端口 | 备注 |
|-----|------|-----|
| MySQL | 3306 | 导入 `link.sql` 初始化数据库 |
| Redis | 6379 | 设置密码 |
| Nacos | 8848 | 单机模式启动 |

### 2. 配置密码

修改以下配置文件中的密码（与你本地服务一致）：

- `admin/src/main/resources/application.yml` - Redis 密码
- `project/src/main/resources/application.yml` - Redis 密码
- `admin/src/main/resources/shardingsphere-config-dev.yml` - MySQL 密码
- `project/src/main/resources/shardingsphere-config-dev.yml` - MySQL 密码

### 3. 设置环境变量

```bash
# 高德地图 API Key（用于 IP 地理位置解析）
export AMAP_KEY=你的高德Key
```

### 4. 配置 hosts 映射

编辑 `/etc/hosts`（macOS/Linux）或 `C:\Windows\System32\drivers\etc\hosts`（Windows）：

```
127.0.0.1 surl.com
```

### 5. 启动服务

依次启动三个模块（推荐使用 IDE）：

1. `AdminApplication` - 用户管理服务 (端口 8002)
2. `ProjectApplication` - 短链接服务 (端口 8001)
3. `GatewayApplication` - 网关服务 (端口 8000)

### 6. 启动前端

```bash
cd console-vue
npm install
npm run dev
```

访问 http://localhost:5173

---

## 服务器部署

### 1. 准备配置

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置（设置强密码）
vim .env
```

`.env` 配置示例：

```env
MYSQL_ROOT_PASSWORD=YourStr0ng!MySQL@Password
REDIS_PASSWORD=YourStr0ng!Redis@Password
AMAP_KEY=你的高德Key
SHORT_LINK_DOMAIN=s.yourdomain.com
```

> ⚠️ **安全提示**: 服务器密码请使用复杂密码（字母+数字+特殊字符）

### 2. 域名配置

将以下域名指向你的服务器 IP：

| 域名 | 用途 |
|-----|------|
| `yourdomain.com` | 前端控制台 |
| `s.yourdomain.com` | 短链接跳转 |

### 3. 一键部署

```bash
./deploy.sh
```

脚本会自动：
- 读取 `.env` 配置
- 构建所有 Docker 镜像
- 启动全部服务

### 4. 服务端口

| 服务 | 端口 | 说明 |
|-----|------|-----|
| Frontend | 3000 | 前端控制台 |
| Gateway | 8000 | API 网关 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848 | 注册中心 |

### 5. 反向代理配置

使用 Nginx Proxy Manager 或 Nginx 配置域名：

**前端控制台** (`yourdomain.com` → `localhost:3000`)

**短链接跳转** (`s.yourdomain.com` → `localhost:8001`)

### 6. 常用命令

```bash
# 查看容器状态
docker-compose ps

# 查看日志
docker-compose logs -f [服务名]

# 重启服务
docker-compose restart [服务名]

# 停止所有服务
docker-compose down
```

---

## 项目结构

```
shortlink/
├── admin/          # 用户管理模块
├── project/        # 短链接核心模块
├── gateway/        # API 网关
├── console-vue/    # 前端项目
├── docker-compose.yml
├── deploy.sh
└── .env.example
```
