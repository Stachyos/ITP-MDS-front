package org.help789.mds.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Auditlog")
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;             // 谁
    private String userAccount;

    private String action;           // 操作名（注解 value）
    @Column(columnDefinition = "TEXT")
    private String detail;           // 详情（SpEL 渲染的文案）

    private Boolean success;         // 是否成功
    @Column(columnDefinition = "TEXT")
    private String errorMsg;         // 异常摘要（失败时）

    private LocalDateTime time;      // 发生时间
}
