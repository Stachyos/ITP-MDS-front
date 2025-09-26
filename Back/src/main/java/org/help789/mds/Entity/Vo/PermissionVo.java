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
    private Boolean accessLogPage;     // 是否可访问病人信息
    private Boolean accessResearchData;    // 是否可访问科研数据
    private Boolean moduleReportGeneration;// 是否可使用报告生成模块
    private Boolean moduleStatsAnalysis;   // 是否可使用统计分析模块
    private Boolean optionEdit;
}
