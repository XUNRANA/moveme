# MovieMe 文档总入口

> MovieMe 是一个基于豆瓣 Top250 数据的电影浏览与个性化推荐系统。Spring Boot 3.4 + Vue 3.5 + MySQL 8 + Redis 7。
>
> 当前状态：**Phase A/B/C/D/F 全部完成**——数据库 v2、种子导入、推荐特征框架、REST API、管理后台、用户中心、Vue 3 前端全部上线。Phase F LLM 推荐已接入小米 MiMO 大模型。推荐服务的离线计算默认关闭（`moveme.recommendation.enabled=false`），按需开启。

---

## 阅读顺序

### 第一次进入项目（入门）
1. [01 - 项目概述与架构设计](./01-项目概述与架构设计.md) — 全栈架构、模块划分、设计决策
2. [02 - 环境搭建与运行指南](./02-环境搭建与运行指南.md) — 从 0 把项目跑起来
3. [91 - 开发命令速查](./91-开发命令速查.md) — 日常 Docker/Maven/前端/数据库命令

### 想搞清数据（核心）
4. [03 - 数据库 Schema 详解](./03-数据库Schema详解.md) — 40+ 张表逐张解释
5. [40 - 种子导入器](./40-种子导入器.md) — 把 `top250.json` 灌入 38+ 张表的字段映射与算法

### 想理解推荐系统
6. [04 - 推荐系统总体设计](./04-推荐系统总体设计.md) — 三策略融合、冷启动、缓存
7. [05 - 推荐算法详解](./05-推荐算法详解.md) — TF-IDF / 余弦 / PMI / KMeans 数学与 Java 实现要点
8. [41 - 推荐特征计算](./41-推荐特征计算.md) — Phase C 模块结构与 Calculator 接口
9. [34 - AI 推荐功能说明](./34-AI推荐功能说明.md) — MiMO 大模型接入、聊天 + 一键推荐

### 接口与前端
9. [30 - REST API 参考](./30-REST-API参考.md) — 全部 7 个模块、40+ 端点的可读版接口手册
10. [31 - 前端架构与组件库](./31-前端架构与组件库.md) — Vue 3 + Pinia + Tailwind 4 + 25 个组件 + 设计 token

### 业务使用指南
11. [32 - 管理后台使用指南](./32-管理后台使用指南.md) — AdminDashboard 全功能流程
12. [33 - 用户中心功能说明](./33-用户中心功能说明.md) — UserDashboard 个人档案/收藏/历史/口味档案

### 长期规划与故障排查
13. [20 - 后续路线图](./20-后续路线图.md) — Phase E（A/B 测试 / 向量化 / 社交 / 运营）
14. [90 - 常见问题与故障排查](./90-常见问题与故障排查.md) — 按现象分类的排错手册

### 历史归档
- [历史 - Phase1 基础骨架](./历史-Phase1-基础骨架开发文档.md) — 用户注册登录链路打通的最早期记录
- [历史 - Phase2 豆瓣爬虫](./历史-Phase2-豆瓣爬虫开发文档.md) — 早期 OkHttp + Jsoup 爬虫实现
- [历史 - Phase-A 数据库重建](./历史-Phase-A-数据库重建.md) — v1→v2 schema 推倒重建的里程碑
- [历史 - Phase-B 业务逻辑细节](./历史-Phase-B-业务逻辑细节.md) — 种子导入逐方法字段映射稿
- [历史 - Phase-B 代码实现讲解](./历史-Phase-B-代码实现讲解.md) — Phase B 完成后的"为什么这么写" walkthrough

---

## 项目状态

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 1 | 用户注册登录、JWT、Spring Security 链路 | 已完成 |
| Phase 2 | 豆瓣爬虫（列表 + 详情 + 调度 + 反反爬） | 已完成 |
| Phase A | 数据库 v2 推倒重建（40+ 张表） | 已完成 |
| Phase B | JSON 种子导入器（`top250.json` → DB） | 已完成 |
| Phase C | 推荐特征/相似度/共现计算（Calculator 框架） | 已完成，默认关闭 |
| Phase D | REST API + 管理后台 + 用户中心 + Vue 3 前端 | 已完成 |
| Phase F | LLM 推荐接入（MiMO 大模型，聊天 + 一键推荐） | 已完成 |
| Phase E+ | A/B 测试、向量化、社交、运营工具 | 见 [20-后续路线图](./20-后续路线图.md) |

---

## 核心目录速查

```
moveme/
├── README.md                            # 项目门面（GitHub 入口）
├── docker/
│   ├── docker-compose.yml               # MySQL + Redis 容器编排
│   └── mysql/init/
│       ├── 01-schema.sql                # 40+ 张表 DDL
│       └── 02-seed-data.sql             # 字典 + admin 种子
├── data/
│   ├── top250.json                      # 250 部豆瓣经典种子数据
│   └── images/                          # 250 张本地海报
├── moveme-backend/                      # Spring Boot 3.4 后端
│   └── src/main/java/com/moveme/
│       ├── common/                      # Result / 异常 / JWT 工具 / 常量
│       ├── config/                      # Security / WebMvc / MyBatis-Plus / Redis
│       └── module/
│           ├── user/                    # 注册登录、收藏、评分、历史、口味
│           ├── movie/                   # 电影、人物、榜单、评论
│           ├── admin/                   # 管理后台
│           ├── crawler/                 # 豆瓣爬虫（Java 调度 + Python 执行）
│           ├── recommend/               # AI 推荐（MiMO 大模型接入）
│           └── seed/                    # JSON 种子导入
├── moveme-frontend/                     # Vue 3.5 + Vite 5 + Tailwind 4
│   └── src/
│       ├── views/                       # 11 个页面
│       ├── components/                  # 25 个业务组件（admin/user/movie/layout/common）
│       ├── stores/user.ts               # Pinia 用户状态
│       ├── api/movies.ts                # 全部接口调用层
│       ├── router/index.ts              # 11 条路由 + 守卫
│       └── assets/styles/               # global.css + variables.css 设计 token
└── docs/                                # 你正在看的文档目录
```

---

## 维护约定

- 改 schema 先更新 `03-数据库Schema详解.md`，再动 SQL 与 entity。
- 改接口先更新 `30-REST-API参考.md`，再动 controller。
- 改组件先在 `31-前端架构与组件库.md` 的对应章节补一行说明，再写组件。
- 大版本里程碑（如本次"全面完善 + 死代码清理 + 项目瘦身"）完成后，刷新 `20-后续路线图.md` 顶部的"已完成全景图"。
