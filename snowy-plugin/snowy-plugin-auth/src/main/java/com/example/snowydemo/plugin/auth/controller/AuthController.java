package com.example.snowydemo.plugin.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.snowydemo.common.CommonResult;
import com.example.snowydemo.plugin.api.auth.LoginRequestDTO;
import com.example.snowydemo.plugin.api.auth.LoginResultDTO;
import com.example.snowydemo.plugin.api.auth.LoginUserDTO;
import com.example.snowydemo.plugin.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

//111111111111111111111111111111111111111
//222
/**
 * 认证接口的 HTTP 入口。
 *
 * <p>请求先到 DispatcherServlet，Spring MVC 根据路径和请求方法找到这里的 Controller 方法。
 * 对登录接口来说，{@link RequestBody} 会让 Jackson 把 JSON 请求体绑定成 {@link LoginRequestDTO}，
 * {@link Valid} 再触发 DTO 上的校验注解；校验通过后才会进入方法并调用 Service。Service 返回的业务数据
 * 会被 {@link CommonResult} 包装，随后由 Spring MVC 序列化成 JSON 响应。</p>
 *
 * <p>登录成功后，客户端需要把返回的 token 放到后续请求名为 {@code token} 的请求头中。
 * 标有 {@link SaCheckLogin} 的接口会在 Controller 执行前由 Sa-Token 注解拦截器检查这个请求头；
 * token 有效才继续调用 Controller，无效或缺失则抛出未登录异常，交给统一异常处理器生成 JSON。</p>
 */
//33333
    //test1cs
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 接收形如 {@code {"username":"admin"}} 的 JSON。JSON 绑定和参数校验发生在方法执行之前，
     * 所以空账号不会进入 AuthService；合法账号则由 Service 建立 Sa-Token 登录会话并返回 token。
     */
    // HandlerMapping 使用“POST + /api/auth/login”找到这个 Controller 方法，此时方法还没有执行。
    @PostMapping("/api/auth/login")
    // DispatcherServlet 随后通过 HandlerAdapter 解析参数：
    // 1. @RequestBody 选择 Jackson，把 HTTP 请求体 JSON 转成 LoginRequestDTO；
    // 2. @Valid 调用 Validator，检查 DTO 字段上的 @NotBlank；
    // 3. 两步都成功后，才把 request 传进来并执行下面的方法体。
    public CommonResult<LoginResultDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        // 取出 DTO 中的 username，调用 Service 完成登录，再把 token 和用户信息包装成统一响应。
        return CommonResult.data(authService.login(request.username()));
    }

    /**
     * Sa-Token 先根据 token 请求头确认当前请求已经登录，再执行退出逻辑。
     * 退出后当前 token 对应的会话被注销，客户端继续使用同一个 token 将无法通过登录检查。
     */
    @SaCheckLogin
    @PostMapping("/api/auth/logout")
    public CommonResult<String> logout() {
        authService.logout();
        return CommonResult.ok();
    }

    /**
     * 请求携带的 token 先通过注解检查，随后 Service 从 Sa-Token 会话中取得登录 ID，
     * 再把 ID 还原为当前演示用户，最终作为统一响应的 data 字段返回给客户端。
     */
    @SaCheckLogin
    @GetMapping("/api/auth/me")
    public CommonResult<LoginUserDTO> me() {
        return CommonResult.data(authService.getCurrentUser());
    }
}
