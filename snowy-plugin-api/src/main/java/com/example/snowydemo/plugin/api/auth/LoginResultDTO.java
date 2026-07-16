package com.example.snowydemo.plugin.api.auth;

/**
 * 登录成功后由认证插件交给上层的结果数据合同。
 *
 * <p>plugin-api 模块用它固定 token 和当前用户信息的字段结构，使认证 Service、Controller 与前端
 * 不需要互相依赖具体实现。它表示统一响应中 data 部分的业务数据，不负责 HTTP 状态码和提示消息；
 * Controller 返回结果时，外层仍应使用 CommonResult 一类的通用响应包装。</p>
 */
public record LoginResultDTO(String token, LoginUserDTO user) {
}
