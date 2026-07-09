package com.example.snowydemo.plugin.auth.controller;

import com.example.snowydemo.common.CommonResult;
import com.example.snowydemo.plugin.api.auth.LoginUserDTO;
import com.example.snowydemo.plugin.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/auth/me")
    public CommonResult<LoginUserDTO> me() {
        return CommonResult.data(authService.getCurrentUser());
    }
}
