package org.help789.mds.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

@Entity
@Table(
        name = "health_records",
        indexes = { @Index(name = "idx_health_records_name", columnList = "name") }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    /** 姓名（作为查询索引） */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 性别（0/1 存储） */
    @Column(
            name = "sex",
            nullable = false,
            columnDefinition = "TINYINT(1) DEFAULT 0"
    )
    @Builder.Default
    private Integer sex = 0;

    /** 年龄（岁） */
    @Column(name = "age")
    private Integer age;

    // —— 血脂相关 ——
    @Column(name = "hdl_c", precision = 6, scale = 2)
    @Comment("高密度脂蛋白胆固醇")
    private BigDecimal hdlC;

    @Column(name = "ldl_c", precision = 6, scale = 2)
    @Comment("低密度脂蛋白胆固醇")
    private BigDecimal ldlC;

    @Column(name = "vldl_c", precision = 6, scale = 2)
    @Comment("极低密度脂蛋白胆固醇")
    private BigDecimal vldlC;

    @Column(name = "triglyceride", precision = 6, scale = 2)
    @Comment("甘油三酯")
    private BigDecimal triglyceride;

    @Column(name = "total_cholesterol", precision = 6, scale = 2)
    @Comment("总胆固醇")
    private BigDecimal totalCholesterol;

    // —— 生命体征 / 血压 ——
    @Column(name = "pulse")
    @Comment("脉搏")
    private Integer pulse;

    @Column(name = "diastolic_bp")
    @Comment("舒张压")
    private Integer diastolicBp;

    @Column(name = "hypertension_history", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    @Comment("高血压史")
    private Boolean hypertensionHistory = false;

    // —— 肾功指标 ——
    @Column(name = "bun", precision = 6, scale = 2)
    @Comment("尿素氮")
    private BigDecimal bun;

    @Column(name = "uric_acid", precision = 7, scale = 2)
    @Comment("尿酸")
    private BigDecimal uricAcid;

    @Column(name = "creatinine", precision = 7, scale = 2)
    @Comment("肌酐")
    private BigDecimal creatinine;
}
