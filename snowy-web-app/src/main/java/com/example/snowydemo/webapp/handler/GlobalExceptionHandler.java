package com.example.snowydemo.webapp.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.example.snowydemo.common.CommonException;
import com.example.snowydemo.common.CommonResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把不同层抛出的可预期异常统一转换成 JSON 响应。
 *
 * <p>参数校验异常通常在 Controller 参数解析阶段产生，Sa-Token 的登录/权限异常通常在拦截器阶段
 * 产生，{@link CommonException} 则通常由 Service 主动抛出。虽然来源不同，它们都会回到
 * DispatcherServlet 的异常解析流程；Spring MVC 在这里找到匹配的 {@link ExceptionHandler} 后，
 * 统一设置 HTTP 状态码，并把 {@link CommonResult} 序列化为 JSON 返回给客户端。</p>
 */
// @RestControllerAdvice 表示这是作用于所有 @RestController 的全局异常处理 Bean。
// 它等于“全局 @ControllerAdvice + 方法返回值自动写成 JSON”。
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 只有异常类型是 NotLoginException（或其子类）时，Spring MVC 才会调用这个方法。
    @ExceptionHandler(NotLoginException.class)
    // exception 是 Sa-Token 抛出的原始异常；本示例不把框架原始消息直接暴露给客户端。
    public ResponseEntity<CommonResult<Void>> handleNotLogin(NotLoginException exception) {
        // 401 表示身份未认证，即客户端没有携带有效 token。
        return error(HttpStatus.UNAUTHORIZED.value(), "请先登录");
    }

    // 匹配“已经识别身份，但当前用户没有所需权限”的 Sa-Token 异常。
    @ExceptionHandler(NotPermissionException.class)
    // Spring MVC 的异常解析器会把捕获到的异常对象作为参数传入。
    public ResponseEntity<CommonResult<Void>> handleNotPermission(NotPermissionException exception) {
        // 403 表示服务器知道用户是谁，但拒绝执行当前操作。
        return error(HttpStatus.FORBIDDEN.value(), "没有操作权限");
    }

    // @RequestBody 转 DTO 后，@Valid 校验失败会抛出 MethodArgumentNotValidException，并匹配到这里。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 此时 Controller 方法体尚未执行，异常来自 Spring MVC 的参数解析和校验阶段。
    public ResponseEntity<CommonResult<Void>> handleValidation(MethodArgumentNotValidException exception) {
        // 从异常携带的 BindingResult 中取得所有字段校验错误，并转成 Stream 方便逐步处理。
        String message = exception.getBindingResult().getFieldErrors().stream()
                // 教学项目只向客户端返回第一条字段错误；没有错误时得到 Optional.empty()。
                .findFirst()
                // 如果存在 FieldError，就取 @NotBlank(message = "...") 中配置的提示语。
                .map(FieldError::getDefaultMessage)
                // 理论上没有字段错误时，使用通用提示作为兜底。
                .orElse("请求参数不合法");
        // 参数内容不符合接口约定，因此返回 HTTP 400 Bad Request。
        return error(HttpStatus.BAD_REQUEST.value(), message);
    }

    // 匹配业务代码主动抛出的 CommonException，例如账号不支持或客户不存在。
    @ExceptionHandler(CommonException.class)
    // Service 抛出的异常会经过 Controller 向上传递，最终由 DispatcherServlet 的异常解析流程送到这里。
    public ResponseEntity<CommonResult<Void>> handleCommon(CommonException exception) {
        // 使用业务异常中保存的状态码和消息，例如 400 或 404，而不是统一写死为 500。
        return error(exception.getStatus(), exception.getMessage());
    }

    // 四个异常处理方法共用的响应构造方法，避免重复编写状态码和 CommonResult 包装逻辑。
    private ResponseEntity<CommonResult<Void>> error(int status, String message) {
        // status(...) 设置真实 HTTP 状态码；body(...) 设置随后由 Jackson 转成 JSON 的响应体。
        return ResponseEntity.status(status).body(CommonResult.error(status, message));
    }
}
