# Docker Compose 一键部署（本地最小可用）

目标：一条命令拉起 **MySQL + Redis + RabbitMQ + MinIO + Backend + Prometheus**（Frontend 先占位，后续接入 #33 构建产物）。

---

## 前置条件

- 安装 Docker Desktop（包含 Docker Compose v2）
- 本机端口未被占用：`3306/6379/5672/15672/9000/9001/8080/9090/5173`

---

## 快速开始（10 分钟内）

```bash
git clone git@github.com:xingyun0812/openInterview.git
cd openInterview
docker compose up -d --build
```

---

## 验证（必做）

- Backend 健康检查：
  - `http://localhost:8080/actuator/health`
- Backend Prometheus 指标：
  - `http://localhost:8080/actuator/prometheus`
- Prometheus UI：
  - `http://localhost:9090`
- RabbitMQ 管理台：
  - `http://localhost:15672`（账号 `admin` / 密码 `123456`）
- MinIO 控制台：
  - `http://localhost:9001`（账号 `minio` / 密码 `minio123456`）

---

## 常用命令

```bash
# 查看服务状态
docker compose ps

# 查看日志（backend 例子）
docker compose logs -f backend

# 重新构建并启动
docker compose up -d --build

# 停止并删除容器（保留数据卷）
docker compose down

# 停止并删除容器 + 删除数据卷（清库/清数据）
docker compose down -v
```

---

## 说明与约定

- MySQL 初始化脚本目录：`./db/migration`（首次启动会自动执行）
- Backend 默认通过环境变量连接各依赖（在 `docker-compose.yml` 里已配置）
- Frontend：
  - 当前为占位 `nginx:alpine`
  - TODO：接入 #33 的前端构建产物（建议 nginx 静态托管 `dist/`）

