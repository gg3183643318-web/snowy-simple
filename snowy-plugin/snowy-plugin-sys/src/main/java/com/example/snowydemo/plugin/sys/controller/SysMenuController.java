package com.example.snowydemo.plugin.sys.controller;

import com.example.snowydemo.common.CommonResult;
import com.example.snowydemo.plugin.api.sys.MenuDTO;
import com.example.snowydemo.plugin.sys.service.SysMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SysMenuController {

    private final SysMenuService sysMenuService;

    public SysMenuController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    @GetMapping("/api/sys/menus")
    public CommonResult<List<MenuDTO>> menus() {
        return CommonResult.data(sysMenuService.listMenus());
    }
}
