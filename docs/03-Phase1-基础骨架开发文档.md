# 03 - Phase 1：基础骨架开发文档

## 概述

Phase 1 目标：搭建可运行的项目骨架，完成用户注册/登录全链路，验证前后端联通。

**完成时间**：2026-03-25
**涉及文件数**：后端 26 个 Java 源文件 + 前端 10 个文件 + 基础设施 5 个文件

---

## 1. 后端工程初始化

### 1.1 Maven 项目结构

```
moveme-backend/
├── pom.xml                          # Maven 依赖管理
├── mvnw / mvnw.cmd                  # Maven Wrapper（免安装 Maven）
├── .mvn/wrapper/
│   ├── maven-wrapper.jar            # Wrapper 可执行文件
│   └── maven-wrapper.properties     # Maven 版本配置
└── src/
    ├── main/
    │   ├── java/com/moveme/
    │   │   ├── MovemeApplication.java
    │   │   ├── config/
    │   │   ├── common/
    │   │   └── module/
    │   └── resources/
    │       └── application.yml
    └── test/
```

### 1.2 核心依赖 (pom.xml)

| 依赖 | 版本 | 用途 |
|---|---|---|
| spring-boot-starter-web | 3.4.4 | Web MVC |
| spring-boot-starter-security | 3.4.4 | 安全框架 |
| spring-boot-starter-validation | 3.4.4 | 参数校验 (@Valid) |
| spring-boot-starter-data-redis | 3.4.4 | Redis 客户端 |
| mybatis-plus-spring-boot3-starter | 3.5.10.1 | ORM 框架 |
| mybatis-plus-jsqlparser | 3.5.10.1 | 分页插件依赖 |
| jjwt-api/impl/jackson | 0.12.6 | JWT 令牌 |
| knife4j-openapi3-jakarta-spring-boot-starter | 4.5.0 | API 文档 |
| okhttp | 4.12.0 | HTTP 客户端（爬虫+LLM） |
| jsoup | 1.18.3 | HTML 解析（爬虫） |
| lombok | managed | 减少样板代码 |

> **踩坑记录**：MyBatis-Plus 3.5.10+ 将 `PaginationInnerInterceptor` 移至 `mybatis-plus-jsqlparser` 模块，需要单独引入。

### 1.3 application.yml 关键配置解读

```yaml
# 数据源：通过环境变量注入，支持开发/Docker 切换
spring.datasource.url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3307}/moveme

# MyBatis-Plus：驼峰映射 + 自动打印 SQL（开发环境）
mybatis-plus.configuration.map-underscore-to-camel-case: true
mybatis-plus.configuration.log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 自定义配置：JWT 密钥和过期时间
moveme.jwt.secret: ${JWT_SECRET:...}
moveme.jwt.access-token-expiration: 7200000    # 2小时
moveme.jwt.refresh-token-expiration: 604800000  # 7天
```

---

## 2. 公共组件实现

### 2.1 统一响应 Result\<T\>

**文件**：`common/result/Result.java`

```java
// 使用方式
Result.success()                           // {"code":200,"message":"操作成功"}
Result.success(data)                       // {"code":200,"message":"操作成功","data":{...}}
Result.error(ResultCode.USER_ALREADY_EXISTS)  // {"code":1001,"message":"用户名已存在"}
```

设计要点：
- 泛型 `<T>` 支持任意类型的 data
- 静态工厂方法，Controller 中一行代码返回结果
- `@Data` (Lombok) 自动生成 getter/setter

### 2.2 错误码枚举 ResultCode

**文件**：`common/result/ResultCode.java`

```
200  - 操作成功
400  - 请求参数错误
401  - 未登录或Token已过期
403  - 无权限访问
404  - 资源不存在
500  - 服务器内部错误
1001 - 用户名已存在
1002 - 邮箱已被注册
1003 - 用户名或密码错误
1004 - 用户已被禁用
2001 - 爬虫任务正在运行中
3001 - LLM服务不可用
```

### 2.3 全局异常处理 GlobalExceptionHandler

**文件**：`common/exception/GlobalExceptionHandler.java`

使用 `@RestControllerAdvice` 拦截所有 Controller 异常：

| 异常类型 | HTTP 状态码 | 处理方式 |
|---|---|---|
| `BusinessException` | 200 (业务码在 body) | 返回自定义错误码 |
| `MethodArgumentNotValidException` | 400 | 拼接所有字段校验错误 |
| `ConstraintViolationException` | 400 | 返回约束违反信息 |
| `BadCredentialsException` | 401 | 用户名或密码错误 |
| `AccessDeniedException` | 403 | 无权限 |
| `Exception` | 500 | 兜底，记录日志 |

### 2.4 自定义业务异常 BusinessException

**文件**：`common/exception/BusinessException.java`

```java
// Service 层抛出
throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
throw new BusinessException(ResultCode.BAD_REQUEST, "自定义消息");
```

---

## 3. 配置类详解

### 3.1 SecurityConfig - Spring Security 配置

**文件**：`config/SecurityConfig.java`

关键配置：
```java
http
    .csrf(disable)                          // 前后端分离不需要 CSRF
    .sessionManagement(STATELESS)           // 无状态，不创建 Session
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()      // 登录注册公开
        .requestMatchers(GET, "/api/v1/movies/**").permitAll() // 电影浏览公开
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")  // 管理员接口
        .anyRequest().authenticated()                          // 其余需认证
    )
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
```

密码编码器：`BCryptPasswordEncoder(12)`

### 3.2 JwtAuthenticationFilter - JWT 认证过滤器

**文件**：`config/JwtAuthenticationFilter.java`

每个请求经过此过滤器：
1. 从 `Authorization: Bearer xxx` 头提取 Token
2. 验证 Token 有效性和过期时间
3. 解析出 userId、username、role
4. 构建 `UsernamePasswordAuthenticationToken` 设置到 `SecurityContext`
5. 后续 Controller 通过 `Authentication.getPrincipal()` 获取 userId

### 3.3 JwtUtil - JWT 工具类

**文件**：`common/util/JwtUtil.java`

| 方法 | 用途 |
|---|---|
| `generateAccessToken(userId, username, role)` | 生成访问令牌 (2h) |
| `generateRefreshToken(userId)` | 生成刷新令牌 (7d) |
| `parseToken(token)` | 解析 Token 获取 Claims |
| `isTokenValid(token)` | 验证 Token 是否有效 |
| `getUserId(token)` | 从 Token 提取用户 ID |

Token 结构 (Payload)：
```json
{
    "userId": 1,
    "username": "admin",
    "role": "ADMIN",
    "iat": 1774436394,
    "exp": 1774443594
}
```

### 3.4 其他配置类

| 文件 | 用途 |
|---|---|
| `MyBatisPlusConfig.java` | 注册分页插件 `PaginationInnerInterceptor` |
| `RedisConfig.java` | 配置 RedisTemplate 序列化方式 (Jackson JSON) |
| `WebMvcConfig.java` | CORS 跨域配置，允许前端 `localhost:5173` 访问 |
| `MetaObjectHandlerConfig.java` | MyBatis-Plus 自动填充 `createdAt`、`updatedAt` |

---

## 4. 用户模块实现

### 4.1 文件清单

```
module/user/
├── entity/User.java              # 数据库实体
├── dto/UserRegisterDTO.java      # 注册请求参数
├── dto/UserLoginDTO.java         # 登录请求参数
├── vo/UserVO.java                # 用户信息响应（脱敏）
├── vo/TokenVO.java               # JWT Token 响应
├── mapper/UserMapper.java        # MyBatis-Plus Mapper
├── service/UserService.java      # 服务接口
├── service/impl/UserServiceImpl.java  # 服务实现
└── controller/UserController.java     # REST 控制器
```

### 4.2 注册流程

```
POST /api/v1/auth/register
Body: {"username":"test", "password":"test123", "email":"test@test.com"}

1. @Valid 参数校验（用户名非空、密码≥6位、邮箱格式）
2. 检查用户名唯一 → 不唯一则抛 BusinessException(1001)
3. 检查邮箱唯一 → 不唯一则抛 BusinessException(1002)
4. BCrypt 加密密码
5. 插入 users 表（role=0, status=1）
6. 返回 Result.success()
```

### 4.3 登录流程

```
POST /api/v1/auth/login
Body: {"username":"test", "password":"test123"}

1. 根据 username 查询用户
2. BCrypt 比对密码 → 不匹配则抛 BusinessException(1003)
3. 检查 status → 被禁用则抛 BusinessException(1004)
4. 生成 accessToken + refreshToken
5. refreshToken 存入 Redis（key=auth:refresh_token:{userId}, TTL=7天）
6. 返回 Result.success(TokenVO)
```

### 4.4 API 测试命令

```bash
# 注册
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123","email":"test@test.com"}'

# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}'

# 获取用户信息（需要 Token）
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>"

# 未认证访问（应返回 401）
curl http://localhost:8080/api/v1/users/me
```

---

## 5. 前端工程初始化

### 5.1 技术选型

| 库 | 版本 | 用途 |
|---|---|---|
| Vue 3 | 3.5.x | MVVM 框架 |
| Vite 5 | 5.4.x | 构建工具（开发快，HMR） |
| TypeScript | 5.6.x | 类型安全 |
| Element Plus | 2.9.x | UI 组件库 |
| Vue Router | 4.5.x | 路由管理 |
| Pinia | 2.3.x | 状态管理（替代 Vuex） |
| Axios | 1.7.x | HTTP 客户端 |

### 5.2 关键文件说明

#### `src/utils/request.ts` - Axios 封装

- **请求拦截器**：自动从 Pinia Store 读取 `accessToken`，添加到 `Authorization` 头
- **响应拦截器**：
  - `code !== 200` → 弹出 ElMessage 错误提示
  - `code === 401` → 清除 Token，跳转登录页
  - 网络错误 → 提示"网络错误"

#### `src/stores/user.ts` - 用户状态管理

- `accessToken` / `refreshToken` — 持久化到 `localStorage`
- `userInfo` — 当前用户信息
- `isLoggedIn` — 计算属性，判断是否登录
- `login()` / `register()` / `logout()` / `fetchUserInfo()` — 动作方法

#### `src/router/index.ts` - 路由守卫

```typescript
router.beforeEach((to, _from, next) => {
    // 公开页面：/login, /register, /
    // 其他页面需要登录，未登录跳转 /login
})
```

### 5.3 已创建页面

| 页面 | 路径 | 说明 |
|---|---|---|
| Login.vue | /login | 登录表单 + 表单校验 |
| Register.vue | /register | 注册表单 + 表单校验 |
| Home.vue | / | 首页框架（搜索栏 + 统计卡片 + 占位） |

---

## 6. Docker 基础设施

### 6.1 docker-compose.yml

```yaml
services:
  mysql:
    image: mysql:8.0
    ports: ["3307:3306"]       # 映射到 3307 避免冲突
    volumes:
      - ./mysql/init:/docker-entrypoint-initdb.d  # 自动执行建表 SQL
      - mysql-data:/var/lib/mysql                  # 持久化数据

  redis:
    image: redis:7-alpine
    ports: ["6380:6379"]       # 映射到 6380 避免冲突
```

### 6.2 数据库初始化

Docker MySQL 启动时自动执行 `/docker-entrypoint-initdb.d/` 目录下的 SQL 文件：

1. `01-schema.sql` — 创建数据库和 10 张表
2. `02-seed-data.sql` — 预置 20 个电影类型 + admin 管理员账号

---

## 7. 踩坑记录

| 问题 | 原因 | 解决方案 |
|---|---|---|
| PaginationInnerInterceptor 找不到 | MyBatis-Plus 3.5.10+ 拆分了模块 | 添加 `mybatis-plus-jsqlparser` 依赖 |
| maven-wrapper.jar 没有主清单属性 | v3.2.0 的 jar 没有 Main-Class | 用 `-cp` + 显式指定主类方式运行 |
| Docker MySQL 端口冲突 | 本地已安装 MySQL 占用 3306 | 映射到 3307 端口 |
| Docker Redis 端口冲突 | 本地已安装 Redis 占用 6379 | 映射到 6380 端口 |
| admin 密码无法登录 | seed data 中的 BCrypt hash 不正确 | 通过注册接口生成正确 hash 后更新 |
| create-vite 9.x 报错 | Node.js 20.11.1 不满足 >=20.19.0 | 手动创建 Vue 项目文件 |
