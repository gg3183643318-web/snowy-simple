package com.example.snowydemo.plugin.crm.service;

import com.example.snowydemo.plugin.api.crm.CustomerDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrmCustomerService {

    public List<CustomerDTO> listCustomers() {
        return List.of(
                new CustomerDTO(1001L, "上海星河科技", "李经理", "A"),
                new CustomerDTO(1002L, "杭州云启制造", "王经理", "B"),
                new CustomerDTO(1003L, "深圳青木贸易", "赵经理", "C")
        );
    }
}
