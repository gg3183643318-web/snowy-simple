package com.example.snowydemo.plugin.api.crm;

import jakarta.validation.constraints.NotBlank;

/**
 * CRM 新增客户接口接收的请求数据合同。
 *
 * <p>它位于 plugin-api 模块，用来约定前端 JSON、CRM Controller 与业务插件之间传递的字段，
 * 不包含新增客户的业务逻辑。Spring MVC 完成请求体绑定后，参数校验流程会检查三个
 * {@link NotBlank} 约束；这个 record 只代表请求内容，不是带 code、message、data 的通用响应包装。</p>
 */
public record CustomerCreateRequest(
        @NotBlank(message = "客户名称不能为空") String name,
        @NotBlank(message = "负责人不能为空") String owner,
        @NotBlank(message = "客户等级不能为空") String level) {
}
