package com.example.snowydemo.plugin.sys.service;

import com.example.snowydemo.plugin.api.sys.MenuDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuService {

    public List<MenuDTO> listMenus() {
        return List.of(
                new MenuDTO("home", "首页", "/"),
                new MenuDTO("crm_customer", "客户管理", "/crm/customers"),
                new MenuDTO("sys_settings", "系统设置", "/sys/settings")
        );
    }
}
