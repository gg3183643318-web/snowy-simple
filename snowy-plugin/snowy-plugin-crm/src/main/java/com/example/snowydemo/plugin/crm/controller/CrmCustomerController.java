package com.example.snowydemo.plugin.crm.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.snowydemo.common.CommonResult;
import com.example.snowydemo.plugin.api.crm.CustomerCreateRequest;
import com.example.snowydemo.plugin.api.crm.CustomerDTO;
import com.example.snowydemo.plugin.api.crm.CustomerUpdateRequest;
import com.example.snowydemo.plugin.crm.service.CrmCustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 把四种常见 HTTP 方法映射成客户 CRUD 操作。
 *
 * <p>带 {@link SaCheckPermission} 或 {@link SaCheckLogin} 的方法会先经过 SaInterceptor。
 * 拦截器在 Controller 执行前读取注解，检查失败时请求不会进入 Controller。PUT 这里只检查是否登录，
 * 它的具体更新权限由 Controller 调用 Service 后手动检查，用来对比两种权限校验发生的位置。</p>
 */
@RestController
public class CrmCustomerController {

    private final CrmCustomerService crmCustomerService;

    public CrmCustomerController(CrmCustomerService crmCustomerService) {
        this.crmCustomerService = crmCustomerService;
    }

    /**
     * GET 只查询资源，不需要请求体；查询结果放在统一响应的 data 中返回。
     */
    @SaCheckPermission("crm:customer:read")
    @GetMapping("/api/crm/customers")
    public CommonResult<List<CustomerDTO>> customers() {
        return CommonResult.data(crmCustomerService.listCustomers());
    }

    /**
     * POST 表示创建新资源，客户字段来自 JSON 请求体；{@code @Valid} 在进入方法前校验字段。
     */
    @SaCheckPermission("crm:customer:create")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/crm/customers")
    public CommonResult<CustomerDTO> createCustomer(@RequestBody @Valid CustomerCreateRequest request) {
        return CommonResult.created(crmCustomerService.create(request));
    }

    /**
     * PUT 的目标客户 ID 放在 URL 路径中，替换后的 name、owner、level 放在 JSON 请求体中。
     * Controller 注解只要求已登录，真正的 crm:customer:update 权限在 Service 内手动检查。
     */
    @SaCheckLogin
    @PutMapping("/api/crm/customers/{id}")
    public CommonResult<CustomerDTO> updateCustomer(
            @PathVariable Long id, @RequestBody @Valid CustomerUpdateRequest request) {
        return CommonResult.data(crmCustomerService.update(id, request));
    }

    /**
     * DELETE 没有 JSON 请求体，路径变量直接指出要删除哪一个客户。
     */
    @SaCheckPermission("crm:customer:delete")
    @DeleteMapping("/api/crm/customers/{id}")
    public CommonResult<String> deleteCustomer(@PathVariable Long id) {
        crmCustomerService.delete(id);
        return CommonResult.ok();
    }
}
