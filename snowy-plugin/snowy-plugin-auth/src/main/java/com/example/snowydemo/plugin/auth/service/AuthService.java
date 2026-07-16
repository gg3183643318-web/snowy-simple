package com.example.snowydemo.plugin.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.snowydemo.common.CommonException;
import com.example.snowydemo.plugin.api.auth.LoginResultDTO;
import com.example.snowydemo.plugin.api.auth.LoginUserDTO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private static final Map<String, LoginUserDTO> USERS_BY_USERNAME = Map.of(
            "admin", new LoginUserDTO(1L, "admin", "演示管理员"),
            "viewer", new LoginUserDTO(2L, "viewer", "只读用户")
    );

    private static final Map<Long, LoginUserDTO> USERS_BY_ID = Map.of(
            1L, USERS_BY_USERNAME.get("admin"),
            2L, USERS_BY_USERNAME.get("viewer")
    );

    public LoginResultDTO login(String username) {
        LoginUserDTO user = USERS_BY_USERNAME.get(username);
        if (user == null) {
            throw new CommonException(400, "只支持 admin 或 viewer");
        }

        StpUtil.login(user.id());
        return new LoginResultDTO(StpUtil.getTokenValue(), user);
    }

    public LoginUserDTO getCurrentUser() {
        final long loginId;
        try {
            loginId = StpUtil.getLoginIdAsLong();
        } catch (RuntimeException exception) {
            throw new CommonException(401, "请先登录");
        }

        LoginUserDTO user = USERS_BY_ID.get(loginId);
        if (user == null) {
            throw new CommonException(401, "请先登录");
        }
        return user;
    }

    public void logout() {
        StpUtil.logout();
    }
}
