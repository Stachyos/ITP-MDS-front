package org.help789.mds.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_time", columnList = "event_time"),
                @Index(name = "idx_audit_operator", columnList = "operator_username")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /**
     * 事件时间：数据库默认 CURRENT_TIMESTAMP，这里也自动填充
     */
    @CreationTimestamp
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    /**
     * 操作人用户名（与 users.account/real_name 任选其一保持一致即可）
     */
    @Column(name = "operator_username", nullable = false, length = 100)
    private String operatorUsername;

    /**
     * 做了什么事情
     */
    @Lob
    @Column(name = "action", nullable = false)
    private String action;
}
