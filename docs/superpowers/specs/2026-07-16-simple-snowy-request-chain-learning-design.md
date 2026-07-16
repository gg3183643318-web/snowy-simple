# Simple Snowy 请求链与权限教学设计

## 目标

在现有极简 Snowy 模块化单体项目中加入一条可运行、可观察的 HTTP 请求链，帮助学习以下内容：

- Nginx、DNS、内嵌 Tomcat 和 Spring MVC 的位置关系。
- `Filter -> DispatcherServlet -> HandlerInterceptor -> Controller` 的执行顺序。
- Filter、Interceptor、AOP 的职责区别。
- Sa-Token 登录、注解验权和 `StpUtil` 手动验权。
- GET、POST、PUT、DELETE 的请求语义和前端 `fetch` 写法。

项目继续保持教学性质，不引入数据库、Redis、Node.js 或 Spring Cloud Gateway。

## 已确认方案

采用真实 Sa-Token，登录态和权限数据只保存在应用内存中。前端提供一个小型请求实验区，可以切换管理员和普通用户，并直接调用四种 HTTP 方法。

Nginx 只提供未启用的示例配置文件。项目不会读取、修改或重载电脑已有的 Nginx 配置。

## 模块职责

### snowy-common

- 保留统一响应对象 `CommonResult<T>`。
- 增加无 Spring 依赖的教学日志注解 `@DemoLog`。
- 增加必要的统一失败响应能力。

### snowy-plugin-api

- 保留插件共享 DTO。
- 增加客户新增、修改请求 DTO。
- 增加登录结果 DTO，使前端可以得到 Token 和当前用户信息。

### snowy-plugin-auth

- 引入 Sa-Token Spring Boot 3 Starter。
- 提供固定的 `admin` 和 `viewer` 两个教学账号。
- `admin` 拥有 CRM 查询、新增、修改、删除权限。
- `viewer` 只有 CRM 查询权限。
- 实现 Sa-Token 的权限列表接口。
- 提供登录、退出、当前用户接口。
- 注册 `SaInterceptor`，启用 `@SaCheckLogin` 和 `@SaCheckPermission`。

### snowy-plugin-crm

- 使用内存集合保存客户，应用重启后恢复初始数据。
- 提供 GET、POST、PUT、DELETE 四类接口。
- 查询、新增和删除演示 Sa-Token 注解验权。
- 修改接口在 Service 中调用 `StpUtil.checkPermission()`，演示手动验权。
- Service 方法使用 `@DemoLog`，让 AOP 调用位置可观察。

### snowy-web-app

- 继续作为唯一 Spring Boot 启动模块，使用内嵌 Tomcat。
- 增加 `DemoRequestFilter`，记录进入和离开 Servlet Filter 链。
- 增加 `DemoTraceInterceptor`，记录 Controller 执行前后。
- 增加 `DemoLogAspect`，记录被注解的 Service 方法执行前后。
- 每次请求生成 Trace ID，并通过 `X-Trace-Id` 响应头返回。
- 增加统一异常处理，把未登录、无权限和参数错误转换成清晰 JSON。

### snowy-admin-web

- 继续使用单文件原生 HTML、CSS、JavaScript。
- 增加 `admin`、`viewer` 登录和退出操作。
- Token 保存到浏览器 `localStorage`，请求时通过 `token` 请求头发送。
- 增加客户查询、新增、修改、删除按钮。
- 显示请求方法、URL、HTTP 状态码、Trace ID 和响应 JSON。

### deploy/nginx

- 增加 `simple-snowy-demo.conf.example`。
- 示例监听 `18090`，避免占用常见的 80、81、8080 端口。
- 示例通过 `location /api/` 代理到 `127.0.0.1:8080`。
- 示例通过 `location /` 提供前端静态文件和 SPA 回退。
- 文件内说明 `listen`、`server_name`、`location` 和 `proxy_pass`。
- 不执行安装、复制、软链接、语法检查或 reload 操作。

## HTTP 接口

| 方法 | 路径 | 权限方式 | 用途 |
|---|---|---|---|
| POST | `/api/auth/login` | 无 | 使用固定账号登录 |
| POST | `/api/auth/logout` | 登录校验 | 退出当前会话 |
| GET | `/api/auth/me` | `@SaCheckLogin` | 查询当前登录用户 |
| GET | `/api/crm/customers` | `@SaCheckPermission` | 查询客户列表 |
| POST | `/api/crm/customers` | `@SaCheckPermission` | 新增客户 |
| PUT | `/api/crm/customers/{id}` | Service 手动验权 | 修改客户 |
| DELETE | `/api/crm/customers/{id}` | `@SaCheckPermission` | 删除客户 |

登录请求只接受两个账号名：

```json
{
  "username": "admin"
}
```

或：

```json
{
  "username": "viewer"
}
```

这是教学登录，不保存密码，也不模拟真实用户数据库。

## 请求执行顺序

一次受保护的客户请求按以下顺序执行：

```text
浏览器 fetch
  -> Nginx（仅在用户主动启用示例配置时存在）
  -> Spring Boot 内嵌 Tomcat
  -> DemoRequestFilter before
  -> DispatcherServlet 查找 HandlerExecutionChain
  -> DemoTraceInterceptor preHandle
  -> SaInterceptor 读取 HandlerMethod 上的权限注解
  -> Controller
  -> DemoLogAspect before
  -> Service
  -> DemoLogAspect after
  -> DemoTraceInterceptor afterCompletion
  -> DemoRequestFilter after
  -> HTTP JSON 响应
```

DispatcherServlet 本身使用 Spring Boot 提供的实现。项目不复制 Spring 源码，只在学习文档和控制台日志中标出它的位置。

## 可观察性

Filter 在请求开始时生成短 Trace ID，并写入请求属性和响应头。Filter、Interceptor 和 AOP 的日志都携带同一个 Trace ID，例如：

```text
[a1b2c3d4] 1 Filter before: GET /api/crm/customers
[a1b2c3d4] 2 Interceptor before: CrmCustomerController#customers
[a1b2c3d4] 3 AOP before: CrmCustomerService#listCustomers
[a1b2c3d4] 4 AOP after: CrmCustomerService#listCustomers
[a1b2c3d4] 5 Interceptor after
[a1b2c3d4] 6 Filter after: 200
```

前端显示 `X-Trace-Id`，学习者可以用它在后端控制台中找到同一次请求的完整顺序。

## 教学注释要求

第二版新增代码使用较详细的中文注释，重点解释：

- 当前类位于 Filter、Spring MVC、权限或业务调用链的哪一层。
- Spring 在什么时候创建或调用当前对象。
- 请求从上一层如何进入，以及处理结果如何返回下一层。
- Sa-Token 注解验权和手动验权为什么放在不同位置。
- `fetch` 中 `method`、`headers`、`body` 和 Token 分别有什么作用。
- Nginx 示例中每个关键指令负责什么，以及为什么不会自动影响本机配置。

注释不重复翻译显而易见的 Java 语法，不给 getter、简单赋值或普通括号增加无效说明。每个关键类保留一段职责说明，每个教学关键点在对应代码附近解释，学习者可以按请求链顺序阅读源码。

## 错误处理

所有预期错误都返回 JSON，不返回默认 HTML 错误页：

| 场景 | HTTP 状态 | 返回消息 |
|---|---:|---|
| 未登录 | 401 | 请先登录 |
| 没有权限 | 403 | 没有操作权限 |
| 参数校验失败 | 400 | 返回对应字段校验信息 |
| 客户不存在 | 404 | 客户不存在 |

`CommonResult.code` 与 HTTP 状态码保持一致，便于同时学习业务响应体和 HTTP 状态。

## Nginx 与 DNS 边界

一台服务器上的一个 Nginx 可以拥有多个 `server` 配置。不同项目可以通过不同域名、端口或路径区分。

示例配置使用：

```text
listen: 18090
server_name: simple-snowy-demo.local
```

仓库只说明可以手动把 `simple-snowy-demo.local` 映射到 `127.0.0.1`，不会修改 `/etc/hosts`。不启用示例配置时，项目仍然可以继续通过 `localhost:8080` 和静态页面运行。

Spring Cloud Gateway 只在文档中与 Nginx 对比，不添加 Gateway 依赖。当前项目仍然是一个端口、一个 JVM、一个 Spring Boot 应用。

## 测试与验收

- Maven 全模块测试通过。
- Spring Boot 上下文可以启动。
- 管理员可以查询、新增、修改、删除客户。
- 普通用户可以查询客户。
- 普通用户执行新增、修改或删除时得到 HTTP 403。
- 未登录访问受保护接口时得到 HTTP 401。
- 成功和失败响应都带有 `X-Trace-Id`。
- 集成测试覆盖 Filter、Interceptor、注解验权和手动验权。
- 前端在桌面和移动宽度下不出现文字或控件重叠。

## 非目标

- 不增加 MySQL、Redis、Nacos、OpenFeign 或 Gateway。
- 不实现注册、密码、验证码、刷新 Token 等生产级认证功能。
- 不把 Nginx 示例安装到本机。
- 不修改电脑上任何现有项目的 Nginx、DNS 或 hosts 配置。
- 不把教学项目扩展成完整 CRM。
