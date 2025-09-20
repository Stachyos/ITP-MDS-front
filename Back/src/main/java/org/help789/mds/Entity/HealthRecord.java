package org.help789.mds.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "health_records",
        indexes = {
                @Index(name = "idx_health_records_name", columnList = "name")
        }
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

    /**
     * 姓名（作为查询索引）
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 性别：男/女/其他/未知（存字符串更通用）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sex", length = 10)
    @Builder.Default
    private Sex sex = Sex.未知;

    /**
     * 年龄（岁）
     */
    @Column(name = "age")
    private Integer age;

    // 血脂相关（单位自定；两位小数）
    @Column(name = "hdl_c", precision = 6, scale = 2)
    private BigDecimal hdlC;              // 高密度脂蛋白胆固醇
    @Column(name = "ldl_c", precision = 6, scale = 2)
    private BigDecimal ldlC;              // 低密度脂蛋白胆固醇
    @Column(name = "vldl_c", precision = 6, scale = 2)
    private BigDecimal vldlC;             // 极低密度脂蛋白胆固醇
    @Column(name = "triglyceride", precision = 6, scale = 2)
    private BigDecimal triglyceride; // 甘油三酯
    @Column(name = "total_cholesterol", precision = 6, scale = 2)
    private BigDecimal totalCholesterol; // 总胆固醇

    // 生命体征 / 血压
    @Column(name = "pulse")
    private Integer pulse;               // 脉搏（次/分）
    @Column(name = "diastolic_bp")
    private Integer diastolicBp;  // 舒张压（mmHg）

    @Column(name = "hypertension_history", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean hypertensionHistory = false;                  // 高血压史

    // 肾功指标
    @Column(name = "bun", precision = 6, scale = 2)
    private BigDecimal bun;        // 尿素氮
    @Column(name = "uric_acid", precision = 7, scale = 2)
    private BigDecimal uricAcid;   // 尿酸
    @Column(name = "creatinine", precision = 7, scale = 2)
    private BigDecimal creatinine; // 肌酐

    public enum Sex {男, 女, 其他, 未知}
}
