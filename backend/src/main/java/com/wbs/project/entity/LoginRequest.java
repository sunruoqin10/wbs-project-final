package com.wbs.project.entity;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    private String userId; // 用户ID
    private String password; // 明文密码
}
