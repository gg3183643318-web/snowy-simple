package com.example.snowydemo.plugin.api.crm;

import jakarta.validation.constraints.NotBlank;

/**
 * CRM 修改客户接口接收的请求数据合同。
 *
 * <p>它放在 plugin-api 模块中，让调用方与 CRM 插件对修改请求体中的 name、owner、level 保持一致。
 * 客户 id 属于请求路径参数，因此不重复放进这个请求体；Spring MVC 绑定 JSON 后会继续执行
 * {@link NotBlank} 校验。它只定义修改操作的输入数据，不承担通用响应包装或异常处理职责。</p>
 */
public record CustomerUpdateRequest(
        @NotBlank(message = "客户名称不能为空") String name,
        @NotBlank(message = "负责人不能为空") String owner,
        @NotBlank(message = "客户等级不能为空") String level) {
}
