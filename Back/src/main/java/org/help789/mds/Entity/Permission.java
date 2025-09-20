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
    @Column(name = "access_patient_info", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean accessPatientInfo = false;

    @Column(name = "access_research_data", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean accessResearchData = false;

    // —— 功能模块 ——
    @Column(name = "module_stats_analysis", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean moduleStatsAnalysis = false;

    @Column(name = "module_report_generation", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean moduleReportGeneration = false;

    // —— 操作类型 ——
    @Column(name = "op_view", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean opView = false;

    @Column(name = "op_edit", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean opEdit = false;

    @Column(name = "op_delete", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean opDelete = false;
}
