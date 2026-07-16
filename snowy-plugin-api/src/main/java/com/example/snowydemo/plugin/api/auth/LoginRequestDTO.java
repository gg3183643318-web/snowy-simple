package com.example.snowydemo.plugin.api.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录接口接收的请求数据合同。
 *
 * <p>它放在 plugin-api 模块中，供调用方、Controller 和认证插件共同约定 JSON 请求体的结构；
 * 请求进入 Spring MVC 后，JSON 中的 {@code username} 会绑定到这个 record，并由参数校验流程读取
 * {@link NotBlank}。它只描述一次登录请求需要哪些字段，不是包含 code、message、data 的通用响应包装类。</p>
 */
public record LoginRequestDTO(
        @NotBlank(message = "账号不能为空") String username) {
}
