package com.example.snowydemo.plugin.crm.controller;

import com.example.snowydemo.common.CommonResult;
import com.example.snowydemo.plugin.api.crm.CustomerDTO;
import com.example.snowydemo.plugin.crm.service.CrmCustomerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CrmCustomerController {

    private final CrmCustomerService crmCustomerService;

    public CrmCustomerController(CrmCustomerService crmCustomerService) {
        this.crmCustomerService = crmCustomerService;
    }

    @GetMapping("/api/crm/customers")
    public CommonResult<List<CustomerDTO>> customers() {
        return CommonResult.data(crmCustomerService.listCustomers());
    }
}
