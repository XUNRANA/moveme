# MovieMe - 电影推荐系统

基于 Spring Boot 3.4 + Vue 3 的前后端分离电影推荐系统，集成豆瓣数据爬虫与 AI 大模型智能推荐。

## 技术栈

**后端：** Java 17 / Spring Boot 3.4.4 / MyBatis-Plus / Spring Security + JWT / Redis / MySQL 8.0

**前端：** Vue 3 / Vite 5 / Element Plus / Pinia / Axios / TypeScript

**基础设施：** Docker Compose / Nginx

## 核心功能

- 用户系统：注册、登录、JWT 认证
- 豆瓣爬虫：定时爬取豆瓣电影数据，反反爬策略
- 电影浏览：分页列表、多条件过滤、全文搜索
- 评分与收藏：用户评分、短评、想看/已看收藏
- 推荐引擎：协同过滤 + 内容推荐 + LLM 智能推荐（策略模式）
- LLM 集成：Gemini / ChatGPT / Claude 多 Provider 降级链

## 快速启动

```bash
# 1. 启动 MySQL + Redis（Docker）
cd docker && docker compose up -d

# 2. 启动后端
cd moveme-backend
./mvnw spring-boot:run

# 3. 启动前端
cd moveme-frontend
npm install && npm run dev
```

详细部署指南见 [docs/02-环境搭建与运行指南.md](docs/02-环境搭建与运行指南.md)

## 项目结构

```
moveme/
├── moveme-backend/          # Spring Boot 后端
│   ├── src/main/java/com/moveme/
│   │   ├── config/          # 配置类 (Security, Redis, MyBatis-Plus)
│   │   ├── common/          # 公共组件 (Result, Exception, Util)
│   │   └── module/          # 业务模块
│   │       ├── user/        # 用户模块 ✅
│   │       ├── movie/       # 电影模块 (实体已创建)
│   │       ├── rating/      # 评分模块
│   │       ├── favorite/    # 收藏模块
│   │       ├── crawler/     # 爬虫模块 ✅
│   │       ├── recommend/   # 推荐引擎
│   │       └── llm/         # LLM 集成
│   └── src/main/resources/
│       └── application.yml
├── moveme-frontend/         # Vue 3 前端
│   └── src/
│       ├── views/           # 页面组件
│       ├── stores/          # Pinia 状态管理
│       ├── router/          # 路由
│       └── utils/           # 工具 (Axios 封装)
├── docker/                  # Docker Compose + 数据库初始化
├── docs/                    # 开发文档
└── .env                     # 环境变量 (不提交到 Git)
```

## 开发文档

| 文档 | 说明 |
|---|---|
| [01-项目概述与架构设计](docs/01-项目概述与架构设计.md) | 系统架构、设计模式、数据库设计 |
| [02-环境搭建与运行指南](docs/02-环境搭建与运行指南.md) | 环境依赖、启动步骤、常见问题 |
| [03-Phase1-基础骨架开发文档](docs/03-Phase1-基础骨架开发文档.md) | Phase 1 实现详情、代码说明、API 接口 |
| [04-开发计划与实现指南](docs/04-开发计划与实现指南.md) | Phase 2-7 实现指南、核心算法、学习路线 |
| [05-Phase2-豆瓣爬虫开发文档](docs/05-Phase2-豆瓣爬虫开发文档.md) | 爬虫模块实现、反爬策略、踩坑记录 |

## 开发进度

- [x] Phase 1：基础骨架（项目初始化、用户认证、Docker 部署）
- [x] Phase 2：豆瓣爬虫
- [ ] Phase 3：电影浏览与搜索
- [ ] Phase 4：评分与收藏
- [ ] Phase 5：传统推荐算法
- [ ] Phase 6：LLM 智能推荐
- [ ] Phase 7：收尾与文档

## 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | 管理员 |
