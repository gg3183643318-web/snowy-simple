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
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<CommonResult<Void>> handleNotLogin(NotLoginException exception) {
        return error(HttpStatus.UNAUTHORIZED.value(), "请先登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<CommonResult<Void>> handleNotPermission(NotPermissionException exception) {
        return error(HttpStatus.FORBIDDEN.value(), "没有操作权限");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResult<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不合法");
        return error(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<CommonResult<Void>> handleCommon(CommonException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    private ResponseEntity<CommonResult<Void>> error(int status, String message) {
        return ResponseEntity.status(status).body(CommonResult.error(status, message));
    }
}
