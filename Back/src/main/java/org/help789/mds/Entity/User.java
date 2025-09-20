package org.help789.mds.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_account", columnNames = "account"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名（真名）
     */
    @Column(name = "real_name", nullable = false, length = 100)
    private String realName;

    /**
     * 账户（登录名）
     */
    @Column(name = "account", nullable = false, length = 60)
    private String account;

    /**
     * 建议存哈希（如 BCrypt）
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * 角色：如 admin / teacher / student / user 等
     */
    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private String role = "user";
}
