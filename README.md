# Simple Snowy Demo

这是一个极简版 Snowy 架构练习项目。第二版重点展示 HTTP 方法、Spring MVC 请求链、Sa-Token 权限、Nginx 路由和内嵌 Tomcat。

先记住结论：它仍然只有一个真正启动的 Spring Boot 应用 `snowy-web-app`，是“多 Maven 模块的单体应用”，不是 Spring Cloud 微服务。当前推荐运行方式是后端 `8080` 加静态前端 `8091`；仓库里的 Nginx 只是可选且未启用的教学示例，本项目也没有使用 Spring Cloud Gateway。

## 模块关系

```text
snowy-admin-web（原生 HTML/CSS/JavaScript）
  |
  | HTTP
  v
snowy-web-app（唯一启动模块）
  |
  | Maven 本地依赖
  v
snowy-plugin
  |- snowy-plugin-auth
  |- snowy-plugin-sys
  `- snowy-plugin-crm
  |
  | 使用共享类型
  v
snowy-common + snowy-plugin-api
```

- `snowy-web-app`：启动 Spring Boot，并放置 Filter、Interceptor、AOP 和全局异常处理。
- `snowy-common`：公共能力，包括统一返回对象 `CommonResult<T>`。
- `snowy-plugin-api`：插件共享的请求对象和 DTO。
- `snowy-plugin-*`：认证、菜单和 CRM 的 Controller、Service 等业务代码。
- `snowy-admin-web`：原生 JavaScript 请求实验页面。

## 第二版阅读顺序

建议严格按下面顺序读。先看请求从哪里发出，再跟着它一层层进入后端，最后看数据如何包装返回；关键类和前端函数中都有更详细的中文源码注释。

1. `snowy-admin-web/index.html` 中的 `request()`：看前端怎样选择 GET/POST/PUT/DELETE、添加 token 和 JSON 请求头，以及读取状态码、响应头、响应体。
2. `deploy/nginx/simple-snowy-demo.conf.example`：只把它当作可选的中间路线，理解静态文件与 `/api/` 反向代理；当前运行方式并不会启用它。
3. `SimpleSnowyDemoApplication`：`SpringApplication.run(...)` 是 Java 后端入口。
4. `DemoRequestFilter`：请求最先进入的 Servlet Filter，创建 traceId，然后把请求交给后续链路。
5. `DispatcherServlet` 的概念位置：它由 Spring MVC 提供，位于 Filter 之后，负责根据 URI 和 HTTP 方法寻找 Controller；项目中没有手写这个类。
6. `DemoTraceInterceptor` 和 `RequestPipelineConfig`：配置类把拦截器接入 `/api/**`，此时已经能看到选中的 Controller 方法。
7. `AuthConfig` 和 `SaInterceptor`：把 Sa-Token 接入 Spring MVC，在 Controller 执行前检查登录和权限注解。
8. `AuthController`、`SysMenuController`、`CrmCustomerController`：接收参数，把 HTTP 请求交给业务层。
9. `DemoLogAspect`：包裹带 `@DemoLog` 的 Service 方法，观察业务方法调用前后；它不是 HTTP 网关或 MVC 拦截器。
10. `AuthService`、`SysMenuService`、`CrmCustomerService`：执行业务逻辑。本 demo 用内存 Map 代替 Mapper 和 MySQL。
11. `CommonResult`：把成功或错误数据统一包装为 `{code, message, data}`，再由 Spring MVC 转成 JSON 响应。

可以把完整去程简化为：

```text
request()
  -> [可选 Nginx，本次默认不经过]
  -> 内嵌 Tomcat
  -> DemoRequestFilter
  -> DispatcherServlet
  -> DemoTraceInterceptor
  -> SaInterceptor
  -> Controller
  -> DemoLogAspect
  -> Service
  -> CommonResult
  -> JSON 响应沿原链路返回浏览器
```

## DNS、Host 和 URI 怎么分工

假设访问 `http://simple-snowy-demo.local:18090/api/crm/customers`：

1. **域名 -> IP**：DNS（本地教学时也可能是 hosts 文件）把 `simple-snowy-demo.local` 解析成服务器 IP。DNS 不认识 Controller，也不选择 Nginx 的 `server`。
2. **Host -> server_name**：浏览器连接目标 IP 的 `18090` 端口，同时发送 `Host: simple-snowy-demo.local`。Nginx 结合 `listen` 端口和 Host，匹配 `server_name`，选出一个 `server` 块。一个 Nginx 可以有很多个 `server` 块，类似同一入口下管理多个站点。
3. **URI -> location**：`/api/crm/customers` 是 URI 路径。Nginx 在选中的 `server` 内匹配 `location /api/`，然后把请求代理给 `127.0.0.1:8080`；其他路径由 `location /` 查找前端文件。

这三步可以记成：**域名找机器，Host 找站点，URI 找站点里的处理规则**。

## Nginx 和 Spring Cloud Gateway

两者都能转发 HTTP 请求，但所处位置和侧重点不同：

- **Nginx** 通常在 Java 应用外部，是独立进程，擅长统一入口、静态资源、反向代理、TLS 和负载均衡，不依赖 Spring。
- **Spring Cloud Gateway** 本身是一个 Java/Spring Cloud 应用，常放在微服务入口，擅长按服务路由、过滤器、鉴权、限流，并可配合 Nacos 等注册中心发现服务。

真实系统可以先经过 Nginx，再进入 Gateway，然后由 Gateway 转发到多个微服务。但这个 demo 只有一个 Spring Boot 进程，没有注册中心、OpenFeign 远程调用或 Gateway，**本 demo 不使用 Spring Cloud Gateway**。

## 内嵌 Tomcat 从哪里来

`snowy-web-app` 依赖 `spring-boot-starter-web`。这个 starter 会通过 Maven 间接带入 Spring MVC 和内嵌 Tomcat：

1. `SimpleSnowyDemoApplication` 调用 `SpringApplication.run(...)`。
2. Spring Boot 创建并启动内嵌 Tomcat，默认监听 `8080`。
3. Spring Boot 向 Tomcat 注册 `DispatcherServlet`。
4. Tomcat 收到 HTTP 请求后，先走 Servlet Filter，再交给 `DispatcherServlet` 进入 Spring MVC。

因此不需要另外安装 Tomcat，也不需要把 WAR 手动部署到 Tomcat；这里的 Tomcat 就像随 Spring Boot 应用一起启动的 HTTP 容器。

## 账号、权限和接口

账号固定在内存中，只用于学习，没有密码和数据库：

| 账号 | 权限 | 预期行为 |
| --- | --- | --- |
| `admin` | 客户查询、新增、修改、删除 | GET/POST/PUT/DELETE 都可以执行 |
| `viewer` | 只读 | GET 成功；POST/PUT/DELETE 返回 HTTP `403` |

未登录访问受保护接口会返回 HTTP `401`；已登录但权限不足返回 HTTP `403`。前者表示“还不知道你是谁”，后者表示“知道你是谁，但你不能做这件事”。

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| POST | `/api/auth/login` | 使用 `admin` 或 `viewer` 登录 |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/auth/me` | 查询当前账号，未登录时为 `401` |
| GET | `/api/crm/customers` | 查询客户列表 |
| POST | `/api/crm/customers` | 新增客户，成功时为 `201` |
| PUT | `/api/crm/customers/{id}` | 修改指定客户 |
| DELETE | `/api/crm/customers/{id}` | 删除指定客户 |

## 当前运行方式（不使用 Nginx）

开两个终端。第一个终端用 Docker Maven 启动后端：

```bash
cd "$HOME/Desktop/simple-snowy-demo"
docker run --rm \
  -p 8080:8080 \
  -v "$HOME/Desktop/simple-snowy-demo":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn spring-boot:run -pl snowy-web-app -am
```

第二个终端启动静态前端服务器：

```bash
cd "$HOME/Desktop/simple-snowy-demo"
python3 -m http.server 8091 --directory snowy-admin-web
```

打开以下地址：

- 前端实验页：`http://localhost:8091/`
- 后端接口示例：`http://localhost:8080/api/auth/me`

当前 `request()` 的 `baseUrl` 是 `http://localhost:8080`，所以页面从 `8091` 加载后会直接请求后端 `8080`。Nginx 示例不会参与这条链路，也不会改动机器配置。

运行全部 Maven 测试：

```bash
docker run --rm \
  -v "$HOME/Desktop/simple-snowy-demo":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn test
```

## 可选 Nginx 示例

`deploy/nginx/simple-snowy-demo.conf.example` 只是配置片段，`.example` 后缀不会被项目或 Nginx 自动加载，仓库也没有活动配置去 include 它。它的存在不会修改本机 Nginx、hosts、端口或当前 `8080 + 8091` 运行方式。

若以后手动实验，需要先做这些明确的选择：

1. 把 `root` 中的 `/absolute/path/to/simple-snowy-demo` 替换成项目的真实绝对路径。
2. 让 `simple-snowy-demo.local` 指向 Nginx 所在机器；本机实验通常需要你自行决定是否修改 hosts。
3. 由你主动把示例复制到或显式 include 到 Nginx 已加载的配置范围，并先做语法检查。
4. 只有在你确认配置和影响范围后，才由你决定是否加载或重载 Nginx；本项目不会自动执行这些操作。

启用后站点地址设计为 `http://simple-snowy-demo.local:18090/`。不过当前前端 `baseUrl` 仍固定为后端 `8080`，所以页面的 JavaScript 默认会绕过 Nginx 的 `/api/` 代理；要完整实验同源反向代理，还需要把前端请求地址改为同源路径，这不属于本次文档配置任务。
