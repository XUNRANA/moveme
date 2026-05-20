# MovieMe

> 电影浏览 + 个性化推荐 + 管理后台的全栈学习项目。
>
> Spring Boot 3.4 · Vue 3.5 · MySQL 8 · Redis 7 · Docker · MiMO LLM · 40+ 张表 · 25 个前端组件 · 40+ REST 端点

---

## 这是什么

基于豆瓣 Top250 数据的电影站，含：

- **电影库**：列表 / 详情 / 全文搜索 / 多榜单（Top250 / 分类 / 年度 / 专题）
- **人物档案**：导演/演员/编剧三档作品归类
- **评论体系**：豆瓣源（爬虫导入）+ 站内源（用户撰写）+ 点赞 + 热度/时间排序
- **用户中心**：个人档案、收藏（想看/已看）、评分、浏览历史、口味档案（类型/演员偏好）
- **管理后台**：系统统计、用户管理（角色/状态）、爬虫任务面板、导入/推荐日志
- **推荐系统**：Content-Based + Item-Based CF + LLM 三策略融合的离线计算框架（默认关闭）
- **AI 推荐**：接入小米 MiMO 大模型，支持多轮聊天对话和基于用户画像的一键推荐
- **爬虫**：Java 调度 + Python 执行，4 个定时任务（榜单/评论/年度/数据增强）

---

## 技术栈

| 层 | 核心依赖 |
|---|---|
| 前端 | Vue 3.5.13 · Vue Router 4.5 · Pinia 2.3 · Axios 1.7 · TypeScript 5.6 · Vite 5.4 · Tailwind CSS 4.1 |
| 后端 | Spring Boot 3.4.4 · Spring Security · MyBatis-Plus 3.5.10 · JJWT 0.12 · Knife4j 4.5 · Lombok |
| 数据 | MySQL 8 · Redis 7 |
| 爬虫 | Python 3.x（由 Java 端 `ProcessBuilder` 拉起） |
| LLM 可选 | MiMO（小米大模型，默认） / Gemini / OpenAI / Claude（按 API key 可用降级） |
| 部署 | Docker Compose（MySQL + Redis） |
| JDK | 17+（Maven 用仓库自带的 mvnw wrapper） |

---

## 快速启动

```bash
git clone <repo-url>
cd moveme

# 1. 启动数据库（首次启动会自动执行 docker/mysql/init/01-schema.sql + 02-seed-data.sql）
cd docker && docker compose up -d && cd ..

# 等 MySQL ready
docker exec moveme-mysql mysqladmin ping -uroot -proot123 --silent

# 2. 启动后端（Windows 用 mvnw.cmd，Linux/Mac 用 ./mvnw）
cd moveme-backend && ./mvnw spring-boot:run

# 3. 启动前端
cd ../moveme-frontend && npm install && npm run dev
```

打开浏览器：
- 前端：<http://localhost:5173>
- 后端 API：<http://localhost:8080/api/v1/movies/genres>
- Knife4j 接口文档：<http://localhost:8080/doc.html>

完整步骤（环境变量、端口规划、Windows mvnw 坑、管理员账号）见 [docs/02-环境搭建与运行指南.md](./docs/02-环境搭建与运行指南.md)。

---

## 端口

| 服务 | 端口 |
|---|---|
| 前端 Vite | 5173 |
| 后端 Spring Boot | 8080 |
| MySQL (Docker) | 3307 → 容器内 3306 |
| Redis (Docker) | 6380 → 容器内 6379 |

非标准端口是为了避免与本机已装的 MySQL/Redis 冲突。

---

## 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | 管理员 |

> 首次启动后建议立即修改密码。若要把已注册的普通用户升级为管理员，见 [docs/02 第 2.7 节](./docs/02-环境搭建与运行指南.md)。

---

## 项目结构

```
moveme/
├── docker/
│   ├── docker-compose.yml             # MySQL + Redis
│   └── mysql/init/
│       ├── 01-schema.sql              # 40+ 张表 DDL
│       └── 02-seed-data.sql           # 字典 + admin 种子
├── data/
│   ├── top250.json                    # 250 部豆瓣经典种子数据
│   ├── images/                        # 250 张本地海报
│   ├── posters/ avatars/              # 运行时落盘（/static/* 暴露）
├── crawler/                           # Python 爬虫脚本
├── moveme-backend/                    # Spring Boot 3.4
│   ├── pom.xml
│   └── src/main/java/com/moveme/
│       ├── common/                    # Result / 异常 / JWT
│       ├── config/                    # Security / WebMvc / MyBatis-Plus
│       └── module/
│           ├── user/                  # 注册/登录/收藏/评分/历史/口味
│           ├── movie/                 # 电影/人物/榜单/评论
│           ├── admin/                 # 管理后台
│           ├── crawler/               # 豆瓣爬虫（Java 调度 + Python 执行）
│           ├── recommend/             # AI 推荐（MiMO 大模型接入）
│           └── seed/                  # JSON 种子导入
├── moveme-frontend/                   # Vue 3.5 + Vite 5 + Tailwind 4
│   ├── package.json
│   └── src/
│       ├── views/                     # 12 个页面
│       ├── components/                # 25 个业务组件（admin/user/movie/layout/common）
│       ├── stores/user.ts             # Pinia
│       ├── api/movies.ts              # API 层
│       ├── router/index.ts            # 12 条路由 + 守卫
│       ├── directives/vAnimate.ts     # 滚动触发动画
│       └── assets/styles/             # 设计 token
└── docs/                              # 文档（入口 docs/README.md）
```

---

## 文档导航

入口：[docs/README.md](./docs/README.md)

按主题：

| 类别 | 文档 |
|---|---|
| 项目概述 | [01-项目概述与架构设计](./docs/01-项目概述与架构设计.md) |
| 环境搭建 | [02-环境搭建与运行指南](./docs/02-环境搭建与运行指南.md) |
| 数据库 | [03-数据库 Schema 详解](./docs/03-数据库Schema详解.md) · [40-种子导入器](./docs/40-种子导入器.md) |
| 推荐系统 | [04-推荐系统总体设计](./docs/04-推荐系统总体设计.md) · [05-推荐算法详解](./docs/05-推荐算法详解.md) · [41-推荐特征计算](./docs/41-推荐特征计算.md) · [34-AI推荐功能说明](./docs/34-AI推荐功能说明.md) |
| 接口与前端 | [30-REST API 参考](./docs/30-REST-API参考.md) · [31-前端架构与组件库](./docs/31-前端架构与组件库.md) |
| 业务指南 | [32-管理后台使用指南](./docs/32-管理后台使用指南.md) · [33-用户中心功能说明](./docs/33-用户中心功能说明.md) |
| 运维 | [90-常见问题与故障排查](./docs/90-常见问题与故障排查.md) · [91-开发命令速查](./docs/91-开发命令速查.md) |
| 路线图 | [20-后续路线图](./docs/20-后续路线图.md) |

---

## 项目状态

| 阶段 | 内容 | 状态 |
|---|---|---|
| 用户认证 + JWT + Spring Security | 完成 |
| 豆瓣爬虫（4 个 cron 定时任务） | 完成 |
| 数据库 v2（40+ 张表 / 8 域） | 完成 |
| JSON 种子导入（`top250.json` → 38+ 表） | 完成 |
| 推荐特征 / 相似度 / 共现 Calculator | 完成（默认关闭） |
| REST API（40+ 端点 / 6 个 Controller） | 完成 |
| 管理后台 + 用户中心 + Vue 3 前端 | 完成 |
| LLM 推荐接入（MiMO 大模型，聊天 + 一键推荐） | 完成 |
| A/B 测试 / 在线推荐 | 路线图（见 [20-后续路线图](./docs/20-后续路线图.md)） |

---

## 测试与构建

```bash
# 后端
cd moveme-backend
./mvnw test                # 单元测试
./mvnw package -DskipTests # 打 jar 到 target/

# 前端
cd moveme-frontend
npm run build              # vue-tsc 类型检查 + Vite 构建到 dist/
npm run preview            # 本地预览生产构建
```

---

## 学习目标

这个项目是为练手而生的，把以下知识串成一个完整业务：

- Spring Boot 3 全套（JWT / Security / MyBatis-Plus / Scheduling / Validation / 文件上传）
- 复杂数据库 schema（外键、JSON、FULLTEXT、ENUM、关联表多对多）
- 推荐系统理论与工程落地（TF-IDF / 余弦 / PMI / 协同过滤 / 冷启动 / 缓存）
- 爬虫工程（反反爬、调度、Java + Python 混合架构）
- Vue 3 全栈前端（Composition API / Pinia / Router 守卫 / Tailwind 设计 token / 自定义指令）

每篇文档都会附"为什么这么写"的设计取舍，方便边读边学。

---

## 许可证

MIT License。仅供学习使用，不持有任何电影/海报版权，所有数据来自公开抓取。
