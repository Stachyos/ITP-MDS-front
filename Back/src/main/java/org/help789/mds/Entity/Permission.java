package org.help789.mds.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "permissions",
        indexes = {
                @Index(name = "idx_permissions_user_id", columnList = "user_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    /**
     * 仅记录用户ID，不建立外键关系
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // —— 数据访问 ——
    @Column(name = "access_log_page", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean accessLogPage = false;

    @Column(name = "access_visual_page", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean accessVisualPage = false;

    // —— 功能模块 ——
    @Column(name = "access_display_page", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean accessDisplayPage = false;

    @Column(name = "permission_management", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean permissionManagement = false;

    // —— 操作类型 ——

    @Column(name = "option_edit", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean optionEdit = false;

}
