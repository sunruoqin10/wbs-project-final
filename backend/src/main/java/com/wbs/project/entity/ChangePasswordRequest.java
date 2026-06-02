package com.wbs.project.entity;

import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}
