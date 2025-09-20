package org.help789.mds.Entity.vo;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecordShow {
    private Long recordId;
    private String name;
    private String sex;     // 男/女/其他/未知
    private Integer age;

    private BigDecimal hdlC;
    private BigDecimal ldlC;
    private BigDecimal vldlC;
    private BigDecimal triglyceride;
    private BigDecimal totalCholesterol;

    private Integer pulse;
    private Integer diastolicBp;
    private Boolean hypertensionHistory;

    private BigDecimal bun;
    private BigDecimal uricAcid;
    private BigDecimal creatinine;
}