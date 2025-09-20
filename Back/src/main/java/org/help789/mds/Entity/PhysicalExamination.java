package org.help789.mds.Entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

@Data
@ToString(exclude = {"project"})
@Entity
@Table(
        name = "t_physical_examination",
        indexes = {
                @Index(name = "idx_pe_user_id", columnList = "user_id"),
                @Index(name = "idx_pe_project_id", columnList = "project_id")
        }
)
@Comment("体检记录（和鲸体检数据集映射）")
public class PhysicalExamination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "physical_examination_id")
    @Comment("体检记录ID")
    private Long physicalExaminationId;

    @Column(name = "user_id", length = 64)
    @Comment("用户标识（源字段：User_ID）")
    private String userId;

    @Column(name = "sex", length = 10)
    @Comment("性别")
    private String sex;

    @Column(name = "age")
    @Comment("年龄")
    private Integer age;

    @Column(name = "hdl_cholesterol", precision = 10, scale = 2)
    @Comment("高密度脂蛋白胆固醇（HDL）")
    private BigDecimal hdlCholesterol;

    @Column(name = "ldl_cholesterol", precision = 10, scale = 2)
    @Comment("低密度脂蛋白胆固醇（LDL）")
    private BigDecimal ldlCholesterol;

    @Column(name = "vldl_cholesterol", precision = 10, scale = 2)
    @Comment("极低密度脂蛋白胆固醇（VLDL）")
    private BigDecimal vldlCholesterol;

    @Column(name = "triglyceride", precision = 10, scale = 2)
    @Comment("甘油三酯")
    private BigDecimal triglyceride;

    @Column(name = "total_cholesterol", precision = 10, scale = 2)
    @Comment("总胆固醇")
    private BigDecimal totalCholesterol;

    @Column(name = "pulse")
    @Comment("脉搏（次/分）")
    private Integer pulse;

    @Column(name = "diastolic_bp")
    @Comment("舒张压（mmHg）")
    private Integer diastolicBp;

    @Column(name = "hypertension_history")
    @Comment("高血压史（是否）")
    private Boolean hypertensionHistory;

    @Column(name = "bun", precision = 10, scale = 2)
    @Comment("尿素氮（BUN）")
    private BigDecimal bun;

    @Column(name = "uric_acid", precision = 10, scale = 2)
    @Comment("尿酸")
    private BigDecimal uricAcid;

    @Column(name = "creatinine", precision = 10, scale = 2)
    @Comment("肌酐")
    private BigDecimal creatinine;

    @Column(name = "weight_result", precision = 10, scale = 2)
    @Comment("体重检查结果（kg）")
    private BigDecimal weightResult;

    @Column(name = "diabetes")
    @Comment("是否糖尿病")
    private Boolean diabetes;

}