package com.example.snowydemo.plugin.auth.service;

import com.example.snowydemo.plugin.api.auth.LoginUserDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public LoginUserDTO getCurrentUser() {
        return new LoginUserDTO(1L, "demo_admin", "演示管理员");
    }
}
