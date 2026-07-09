# Simple Snowy Demo

这是一个极简版 Snowy 架构练习项目，用来理解“多 Maven 模块”不等于“分布式系统”。

它只有一个真正启动的 Spring Boot 应用：`snowy-web-app`。

## 模块关系

```text
snowy-admin-web
  |
  | HTTP fetch
  v
snowy-web-app
  |
  | Maven dependency
  v
snowy-plugin
  |-snowy-plugin-auth
  |-snowy-plugin-sys
  `-snowy-plugin-crm
  |
  | uses
  v
snowy-common + snowy-plugin-api
```

## 每个模块负责什么

`snowy-web-app`：主启动模块，只负责启动 Spring Boot。

`snowy-common`：公共能力，这里只有统一返回对象 `CommonResult<T>`。

`snowy-plugin-api`：插件之间共享的 DTO，这里有 `LoginUserDTO`、`MenuDTO`、`CustomerDTO`。

`snowy-plugin`：业务插件聚合模块，下面放具体业务插件。

`snowy-admin-web`：前端页面，用原生 JavaScript 调后端接口。

## 请求怎么走

```text
浏览器打开 snowy-admin-web/index.html
  |
  | GET http://localhost:8080/api/crm/customers
  v
snowy-web-app 启动的 Spring Boot 应用
  |
  | 扫描到 crm 插件里的 Controller
  v
CrmCustomerController
  |
  v
CrmCustomerService
  |
  v
CommonResult<List<CustomerDTO>>
  |
  v
浏览器显示客户列表
```

## 本机 Maven 运行

如果你的电脑已经安装 JDK 17+ 和 Maven：

```bash
cd ~/Desktop/simple-snowy-demo
mvn spring-boot:run -pl snowy-web-app -am
```

后端接口：

```text
http://localhost:8080/api/auth/me
http://localhost:8080/api/sys/menus
http://localhost:8080/api/crm/customers
```

前端页面：

```text
~/Desktop/simple-snowy-demo/snowy-admin-web/index.html
```

## Docker Maven 运行

如果本机没有 JDK 或 Maven，可以用 Docker：

```bash
docker run --rm \
  -p 8080:8080 \
  -v "$HOME/Desktop/simple-snowy-demo":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn spring-boot:run -pl snowy-web-app -am
```

## 测试

```bash
docker run --rm \
  -v "$HOME/Desktop/simple-snowy-demo":/workspace \
  -v "$HOME/.m2":/root/.m2 \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn test
```

## 学习重点

这个 demo 不是 Spring Cloud 微服务。

它更像：

```text
一个 Spring Boot 进程
一个端口
多个 Maven 模块
多个业务插件包
模块之间本地 Java 调用
```

真正的分布式系统通常会有多个独立启动的服务、多个端口、注册中心、网关、OpenFeign 远程调用。
