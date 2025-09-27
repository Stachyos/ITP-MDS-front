package org.help789.mds.Entity.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端展示 / 传输权限数据用的 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVo {
    private Long permissionId;             // 权限记录ID
    private Long userId;                   // 用户ID
    private String account;                // 用户账号
    private String email;                  // 用户邮箱
    private Boolean accessLogPage;
    private Boolean accessVisualPage;
    private Boolean accessDisplayPage;
    private Boolean permissionManagement;
    private Boolean optionEdit;
}
