# Simple Snowy Request Chain Learning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the first-version modular monolith with a small, runnable learning lab for HTTP methods, Spring MVC request flow, Sa-Token permissions, DNS, Nginx, and embedded Tomcat.

**Architecture:** Keep one Spring Boot process and the existing Maven module boundaries. Authentication lives in `snowy-plugin-auth`, in-memory CRM behavior lives in `snowy-plugin-crm`, shared request/response contracts live in `snowy-plugin-api`, and request-pipeline infrastructure lives in `snowy-web-app`. Nginx remains an inactive example file and the browser UI remains plain JavaScript.

**Tech Stack:** Java 17, Spring Boot 3.5.9, Spring MVC, Spring AOP, Jakarta Validation, Sa-Token 1.44.0, JUnit 5, native HTML/CSS/JavaScript, optional Nginx example configuration.

---

## File Map

**Shared contracts and errors**

- Modify `snowy-common/src/main/java/com/example/snowydemo/common/CommonResult.java`: support HTTP-aligned success and error bodies.
- Create `snowy-common/src/main/java/com/example/snowydemo/common/CommonException.java`: carry an HTTP status for expected business failures.
- Create `snowy-common/src/main/java/com/example/snowydemo/common/annotation/DemoLog.java`: mark methods observed by the teaching AOP.
- Create request/result records under `snowy-plugin-api/src/main/java/com/example/snowydemo/plugin/api/auth` and `.../crm`.

**Authentication**

- Modify root and auth POM files for Sa-Token 1.44.0 and validation.
- Replace the fixed current-user behavior in `AuthService` with two in-memory accounts.
- Extend `AuthController` with login, logout, and current-user endpoints.
- Create `AuthConfig` to register `SaInterceptor` and provide permission lists.
- Create `GlobalExceptionHandler` with stable JSON responses before testing permission failures.

**CRM methods and permissions**

- Replace immutable sample-list behavior with an in-memory ordered map.
- Extend `CrmCustomerController` with GET, POST, PUT, and DELETE mappings.
- Use annotations for read/create/delete and `StpUtil.checkPermission()` inside the update Service method.

**Request pipeline**

- Create `TraceContext`, `DemoRequestFilter`, `DemoTraceInterceptor`, `RequestPipelineConfig`, and `DemoLogAspect` in `snowy-web-app`.
- Preserve the existing CORS configuration and expose `X-Trace-Id` and `X-Handler` headers.

**Learning UI and deployment notes**

- Replace `snowy-admin-web/index.html` with a compact request lab.
- Create `deploy/nginx/simple-snowy-demo.conf.example` without activating it.
- Update `README.md` with the exact reading order and request flow.

## Task 1: Shared Response, Exception, Annotation, and DTO Contracts

**Files:**

- Modify: `snowy-common/src/main/java/com/example/snowydemo/common/CommonResult.java`
- Create: `snowy-common/src/main/java/com/example/snowydemo/common/CommonException.java`
- Create: `snowy-common/src/main/java/com/example/snowydemo/common/annotation/DemoLog.java`
- Modify: `snowy-common/src/test/java/com/example/snowydemo/common/CommonResultTest.java`
- Modify: `snowy-plugin-api/pom.xml`
- Create: `snowy-plugin-api/src/main/java/com/example/snowydemo/plugin/api/auth/LoginRequestDTO.java`
- Create: `snowy-plugin-api/src/main/java/com/example/snowydemo/plugin/api/auth/LoginResultDTO.java`
- Create: `snowy-plugin-api/src/main/java/com/example/snowydemo/plugin/api/crm/CustomerCreateRequest.java`
- Create: `snowy-plugin-api/src/main/java/com/example/snowydemo/plugin/api/crm/CustomerUpdateRequest.java`

- [ ] **Step 1: Extend the response test first**

Add assertions that `created(data)` uses code 201 and `error(403, message)` preserves the status and null data:

```java
@Test
void createsHttpAlignedResponseBodies() {
    CommonResult<String> created = CommonResult.created("customer");
    CommonResult<Void> denied = CommonResult.error(403, "没有操作权限");

    assertThat(created.getCode()).isEqualTo(201);
    assertThat(created.getData()).isEqualTo("customer");
    assertThat(denied.getCode()).isEqualTo(403);
    assertThat(denied.getMessage()).isEqualTo("没有操作权限");
    assertThat(denied.getData()).isNull();
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -pl snowy-common test
```

Expected: compilation failure because `created` and `error` do not exist.

- [ ] **Step 3: Implement shared types**

Add these factories to `CommonResult`:

```java
public static <T> CommonResult<T> created(T data) {
    return new CommonResult<>(201, "created", data);
}

public static <T> CommonResult<T> error(int code, String message) {
    return new CommonResult<>(code, message, null);
}
```

Create the expected business exception:

```java
public class CommonException extends RuntimeException {
    private final int status;

    public CommonException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
```

Create an annotation with runtime method retention:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DemoLog {
    String value();
}
```

Add `jakarta.validation:jakarta.validation-api` to `snowy-plugin-api`, then create records with these exact constraints:

```java
public record LoginRequestDTO(@NotBlank(message = "账号不能为空") String username) {}

public record LoginResultDTO(String token, LoginUserDTO user) {}

public record CustomerCreateRequest(
        @NotBlank(message = "客户名称不能为空") String name,
        @NotBlank(message = "负责人不能为空") String owner,
        @NotBlank(message = "客户等级不能为空") String level) {}

public record CustomerUpdateRequest(
        @NotBlank(message = "客户名称不能为空") String name,
        @NotBlank(message = "负责人不能为空") String owner,
        @NotBlank(message = "客户等级不能为空") String level) {}
```

Add detailed Chinese class comments that explain DTOs are contracts in `plugin-api`, not response wrappers.

- [ ] **Step 4: Run shared and API tests**

Run:

```bash
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -pl snowy-common,snowy-plugin-api test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit the shared contract layer**

```bash
git add snowy-common snowy-plugin-api
git commit -m "feat: add learning request contracts"
```

## Task 2: Real Sa-Token In-Memory Authentication

**Files:**

- Modify: `pom.xml`
- Modify: `snowy-plugin/snowy-plugin-auth/pom.xml`
- Modify: `snowy-plugin/snowy-plugin-auth/src/main/java/com/example/snowydemo/plugin/auth/service/AuthService.java`
- Modify: `snowy-plugin/snowy-plugin-auth/src/main/java/com/example/snowydemo/plugin/auth/controller/AuthController.java`
- Create: `snowy-plugin/snowy-plugin-auth/src/main/java/com/example/snowydemo/plugin/auth/config/AuthConfig.java`
- Create: `snowy-web-app/src/main/resources/application.properties`
- Create: `snowy-web-app/src/main/java/com/example/snowydemo/webapp/handler/GlobalExceptionHandler.java`
- Create: `snowy-web-app/src/test/java/com/example/snowydemo/webapp/AuthFlowIntegrationTest.java`

- [ ] **Step 1: Write authentication integration tests**

Use `TestRestTemplate` and `ObjectMapper` to prove:

```java
@Test
void adminCanLoginAndReadCurrentUser() throws Exception {
    String token = login("admin");
    ResponseEntity<String> response = getWithToken("/api/auth/me", token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("演示管理员", "admin");
}

@Test
void unknownAccountIsRejected() {
    ResponseEntity<String> response = postJson("/api/auth/login", "{\"username\":\"missing\"}", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("只支持 admin 或 viewer");
}
```

The helper reads `data.token` from the login JSON and sends it using the `token` request header.

- [ ] **Step 2: Run the test and verify RED**

Run:

```bash
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -pl snowy-web-app -am -Dtest=AuthFlowIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: failure because login endpoint and Sa-Token configuration do not exist.

- [ ] **Step 3: Add Sa-Token dependency and configuration**

Add root property and dependency management:

```xml
<sa-token.version>1.44.0</sa-token.version>
```

Add this auth dependency:

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>${sa-token.version}</version>
</dependency>
```

Add `spring-boot-starter-validation` to `snowy-web-app` so the validation annotations from `plugin-api` have a runtime validator implementation.

Configure the teaching token name without Redis:

```properties
server.port=8080
sa-token.token-name=token
sa-token.timeout=3600
sa-token.is-concurrent=true
sa-token.is-share=true
sa-token.is-log=false
```

- [ ] **Step 4: Implement fixed users and permission provider**

`AuthService` stores these users:

```java
private static final Map<String, LoginUserDTO> USERS = Map.of(
        "admin", new LoginUserDTO(1L, "admin", "演示管理员"),
        "viewer", new LoginUserDTO(2L, "viewer", "只读用户")
);
```

`login(username)` throws `new CommonException(400, "只支持 admin 或 viewer")` for an unknown account, calls `StpUtil.login(user.id())` for a known account, and returns `new LoginResultDTO(StpUtil.getTokenValue(), user)`. `getCurrentUser()` resolves the login ID, and `logout()` calls `StpUtil.logout()`.

`AuthConfig` registers:

```java
registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
```

Its `StpInterface` returns:

```java
if ("1".equals(String.valueOf(loginId))) {
    return List.of("crm:customer:read", "crm:customer:create",
            "crm:customer:update", "crm:customer:delete");
}
return List.of("crm:customer:read");
```

Add Chinese comments explaining that `SaInterceptor` executes after Spring MVC has selected a handler, which is why it can inspect method annotations.

- [ ] **Step 5: Implement stable JSON exception handling**

Create `GlobalExceptionHandler` as `@RestControllerAdvice`. Handle `NotLoginException`, `NotPermissionException`, `MethodArgumentNotValidException`, and `CommonException` with `ResponseEntity<CommonResult<Void>>`. Map them to 401, 403, 400, and the business exception status, using the messages in the design.

- [ ] **Step 6: Implement auth endpoints**

```java
@PostMapping("/api/auth/login")
public CommonResult<LoginResultDTO> login(@RequestBody @Valid LoginRequestDTO request) {
    return CommonResult.data(authService.login(request.username()));
}

@SaCheckLogin
@PostMapping("/api/auth/logout")
public CommonResult<String> logout() {
    authService.logout();
    return CommonResult.ok();
}

@SaCheckLogin
@GetMapping("/api/auth/me")
public CommonResult<LoginUserDTO> me() {
    return CommonResult.data(authService.getCurrentUser());
}
```

- [ ] **Step 7: Run auth tests and commit**

Run the command from Step 2. Expected: both successful login and the invalid-account 400 JSON test pass.

```bash
git add pom.xml snowy-plugin/snowy-plugin-auth snowy-web-app/pom.xml snowy-web-app/src/main
git add snowy-web-app/src/test
git commit -m "feat: add in-memory sa-token login"
```

## Task 3: In-Memory CRM CRUD and Both Permission Styles

**Files:**

- Modify: `snowy-plugin/snowy-plugin-crm/src/main/java/com/example/snowydemo/plugin/crm/service/CrmCustomerService.java`
- Modify: `snowy-plugin/snowy-plugin-crm/src/main/java/com/example/snowydemo/plugin/crm/controller/CrmCustomerController.java`
- Modify: `snowy-plugin/snowy-plugin-crm/pom.xml`
- Create: `snowy-web-app/src/test/java/com/example/snowydemo/webapp/CrmPermissionIntegrationTest.java`

- [ ] **Step 1: Write failing admin/viewer CRUD tests**

Cover the complete contract:

```java
@Test
void adminCanUseAllFourHttpMethods() {
    String token = login("admin");
    assertThat(getCustomers(token).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createCustomer(token).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(updateCustomer(token, 1001L).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(deleteCustomer(token, 1002L).getStatusCode()).isEqualTo(HttpStatus.OK);
}

@Test
void viewerCanReadButCannotChangeCustomers() {
    String token = login("viewer");
    assertThat(getCustomers(token).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createCustomer(token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(updateCustomer(token, 1001L).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(deleteCustomer(token, 1002L).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}
```

- [ ] **Step 2: Run and verify RED**

Run the web-app Maven test command filtered to `CrmPermissionIntegrationTest`. Expected: POST, PUT, and DELETE mappings are missing.

- [ ] **Step 3: Add the CRM compile-time permission dependency**

Add `cn.dev33:sa-token-core` with `${sa-token.version}` to `snowy-plugin-crm`. The CRM plugin needs Sa-Token annotation and utility classes at compile time; the auth plugin still owns login configuration.

- [ ] **Step 4: Implement the in-memory Service**

Use a `LinkedHashMap<Long, CustomerDTO>` initialized with IDs 1001-1003 and `AtomicLong(1004)`. Implement synchronized methods:

```java
@DemoLog("查询客户")
public synchronized List<CustomerDTO> listCustomers()

@DemoLog("新增客户")
public synchronized CustomerDTO create(CustomerCreateRequest request)

@DemoLog("修改客户（Service 手动验权）")
public synchronized CustomerDTO update(Long id, CustomerUpdateRequest request) {
    StpUtil.checkPermission("crm:customer:update");
    return replaceExistingCustomer(id, request);
}

@DemoLog("删除客户")
public synchronized void delete(Long id)
```

Throw `new CommonException(404, "客户不存在")` for unknown IDs. Explain in comments that the update permission check is intentionally in Service to contrast manual and annotation-based permission checks.

- [ ] **Step 5: Implement four HTTP mappings**

```java
@SaCheckPermission("crm:customer:read")
@GetMapping("/api/crm/customers")

@SaCheckPermission("crm:customer:create")
@ResponseStatus(HttpStatus.CREATED)
@PostMapping("/api/crm/customers")

@SaCheckLogin
@PutMapping("/api/crm/customers/{id}")

@SaCheckPermission("crm:customer:delete")
@DeleteMapping("/api/crm/customers/{id}")
```

POST and PUT accept validated JSON request records. DELETE uses a path variable. Add comments that distinguish request body, path variable, and each HTTP method.

- [ ] **Step 6: Run tests and commit**

Expected: all admin and viewer assertions pass, including the 403 response produced by the exception handler from Task 2.

```bash
git add snowy-plugin/snowy-plugin-crm snowy-web-app/src/test
git commit -m "feat: add permission-aware crm methods"
```

## Task 4: Filter, Interceptor, AOP, and Trace Headers

**Files:**

- Modify: `snowy-web-app/pom.xml`
- Create: `snowy-web-app/src/main/java/com/example/snowydemo/webapp/trace/TraceContext.java`
- Create: `snowy-web-app/src/main/java/com/example/snowydemo/webapp/filter/DemoRequestFilter.java`
- Create: `snowy-web-app/src/main/java/com/example/snowydemo/webapp/interceptor/DemoTraceInterceptor.java`
- Create: `snowy-web-app/src/main/java/com/example/snowydemo/webapp/config/RequestPipelineConfig.java`
- Create: `snowy-web-app/src/main/java/com/example/snowydemo/webapp/aspect/DemoLogAspect.java`
- Modify: `snowy-web-app/src/test/java/com/example/snowydemo/webapp/SimpleSnowyDemoApplicationTest.java`

- [ ] **Step 1: Write failing pipeline and error tests**

Assert that a successful controller response contains nonblank `X-Trace-Id` and a handler name in `X-Handler`. Assert unauthenticated and unauthorized calls return JSON with HTTP 401 and 403 respectively.

```java
assertThat(response.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
assertThat(response.getHeaders().getFirst("X-Handler")).contains("CrmCustomerController");
assertThat(denied.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
assertThat(denied.getBody()).contains("\"code\":403", "没有操作权限");
```

- [ ] **Step 2: Run and verify RED**

Run all `snowy-web-app` tests. Expected: trace headers and unified error JSON are missing.

- [ ] **Step 3: Implement TraceContext and Filter**

`TraceContext` wraps a `ThreadLocal<String>` with `set`, `get`, and `clear`. `DemoRequestFilter` must:

1. Generate `UUID.randomUUID().toString().substring(0, 8)`.
2. Set `X-Trace-Id` before `filterChain.doFilter`.
3. Log Filter before/after in `try/finally`.
4. Clear the ThreadLocal in `finally` to avoid Tomcat thread-pool leakage.

Detailed comments explain that Filter runs in the Servlet layer before DispatcherServlet and can also observe requests that never reach a Controller.

- [ ] **Step 4: Implement the MVC Interceptor**

In `preHandle`, when `handler instanceof HandlerMethod`, set:

```java
String handlerName = handlerMethod.getBeanType().getSimpleName()
        + "#" + handlerMethod.getMethod().getName();
response.setHeader("X-Handler", handlerName);
```

Log before and in `afterCompletion`. Register it for `/api/**`. Comments explain that HandlerMapping has selected the method, but the Controller has not executed yet.

- [ ] **Step 5: Implement teaching AOP**

Add `spring-boot-starter-aop`, then use:

```java
@Around("@annotation(demoLog)")
public Object around(ProceedingJoinPoint joinPoint, DemoLog demoLog) throws Throwable {
    log.info("[{}] 3 AOP before: {}", TraceContext.get(), demoLog.value());
    try {
        return joinPoint.proceed();
    } finally {
        log.info("[{}] 4 AOP after: {}", TraceContext.get(), demoLog.value());
    }
}
```

Comments explain that AOP wraps Spring Bean method execution and can work without HTTP, unlike Filter and MVC Interceptor.

- [ ] **Step 6: Run all backend tests and commit**

Run:

```bash
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace maven:3.9.9-eclipse-temurin-17 mvn test
```

Expected: BUILD SUCCESS with all tests passing.

```bash
git add snowy-web-app
git commit -m "feat: expose spring mvc request pipeline"
```

## Task 5: Plain-JavaScript Request Lab

**Files:**

- Modify: `snowy-admin-web/index.html`

- [ ] **Step 1: Replace automatic-only loading with explicit learning controls**

Keep one HTML file and add:

- Login buttons for `admin` and `viewer`, plus logout.
- Current identity and permission explanation.
- Four method controls: GET list, POST sample, PUT first customer, DELETE last customer.
- A customer table.
- A request inspector showing method, URL, HTTP status, Trace ID, handler, and response JSON.

- [ ] **Step 2: Implement one reusable fetch function**

Use this request shape and add detailed Chinese comments beside each option:

```javascript
async function request(path, options = {}) {
    const token = localStorage.getItem('simple-snowy-token');
    const headers = {...(options.headers || {})};

    if (token) headers.token = token;
    if (options.body) headers['Content-Type'] = 'application/json';

    const response = await fetch(`${baseUrl}${path}`, {...options, headers});
    const result = await response.json();

    renderInspector({
        method: options.method || 'GET',
        path,
        status: response.status,
        traceId: response.headers.get('X-Trace-Id'),
        handler: response.headers.get('X-Handler'),
        result
    });

    if (!response.ok) throw new Error(result.message);
    return result;
}
```

POST and PUT call `JSON.stringify(...)`; DELETE sends the ID in the URL and has no body. Login stores `result.data.token`; logout and 401 responses clear it.

- [ ] **Step 3: Verify static syntax and responsive layout**

Run a static server on an unused port and open the page at desktop and mobile widths. Verify no overlapping text, controls, or table columns and that the request inspector wraps long JSON.

- [ ] **Step 4: Commit the frontend lab**

```bash
git add snowy-admin-web/index.html
git commit -m "feat: add browser request learning lab"
```

## Task 6: Inactive Nginx Example and Learning Documentation

**Files:**

- Create: `deploy/nginx/simple-snowy-demo.conf.example`
- Modify: `README.md`

- [ ] **Step 1: Add the inactive example configuration**

Use an isolated teaching server block:

```nginx
server {
    listen 18090;
    server_name simple-snowy-demo.local;

    root /absolute/path/to/simple-snowy-demo/snowy-admin-web;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Add Chinese comments explaining every key directive, that one Nginx can contain many `server` blocks, and that this `.example` file is never loaded automatically.

- [ ] **Step 2: Update the README reading order**

Document:

1. First read Filter, then pipeline config, interceptor, auth config, Controller, AOP, and Service.
2. DNS resolves a domain to an IP; Nginx chooses a `server` by port and `Host`, then chooses `location` by URI.
3. Nginx and Gateway are different layers, and this project does not use Gateway.
4. Spring Boot Starter Web supplies embedded Tomcat and DispatcherServlet.
5. Exact Docker Maven commands, browser URL, fixed accounts, and expected 401/403 behavior.

- [ ] **Step 3: Check docs and commit**

Run `git diff --check`, verify the example is not referenced by any active Nginx include, then commit:

```bash
git add README.md deploy/nginx/simple-snowy-demo.conf.example
git commit -m "docs: explain nginx and request routing"
```

## Task 7: Final Verification and GitHub Comparison Preparation

**Files:**

- Modify only files required by verification findings.

- [ ] **Step 1: Run the complete Maven suite**

```bash
docker run --rm -v "$PWD":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace maven:3.9.9-eclipse-temurin-17 mvn test
```

Expected: BUILD SUCCESS and zero failing tests.

- [ ] **Step 2: Start the backend and exercise real HTTP requests**

Start `snowy-web-app`, then verify login, admin CRUD, viewer GET, viewer 403, and unauthenticated 401. Confirm each response has `X-Trace-Id` and Controller responses have `X-Handler`.

- [ ] **Step 3: Verify the frontend in a real browser**

Open the static page through a local server, test both accounts and all four method buttons at desktop and mobile widths, and save a current screenshot under `snowy-admin-web/` only if it replaces the existing learning screenshot intentionally.

- [ ] **Step 4: Verify the version boundary**

Run:

```bash
git diff --stat main...feature_request_chain_learning
git diff --name-status main...feature_request_chain_learning
git status --short
```

Expected: the feature diff contains only the second-version learning design, implementation, tests, docs, and UI. `Spring三件套常用注解.md` remains untracked unless the user separately requests it in Git.

- [ ] **Step 5: Push after GitHub SSH permission is fixed**

```bash
git push -u origin main
git push -u origin feature_request_chain_learning
```

Expected GitHub comparison URL:

```text
https://github.com/gg3183643318-web/snowy-simple/compare/main...feature_request_chain_learning
```

If GitHub still reports `denied to deploy key`, do not change the repository or global SSH configuration automatically. Report that the current public key needs write access or a personal GitHub SSH identity must be selected.
