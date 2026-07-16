package com.example.snowydemo.common;

/**
 * 表示业务代码已经预期并能够向客户端说明的异常。
 *
 * <p>它通常由 Service 层在“账号不存在”“客户不存在”等场景主动抛出，异常会沿着
 * Service -> Controller -> Spring MVC 的调用链向上传递。后续由 web-app 层的统一异常处理器
 * 读取 {@code status} 和异常消息，再分别设置 HTTP 状态码与 {@link CommonResult} 错误响应体。
 * common 模块只定义这个跨模块信号，不依赖 Spring MVC，也不直接负责发送 HTTP 响应。</p>
 */
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
