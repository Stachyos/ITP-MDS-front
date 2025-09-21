package org.help789.mds.repository;

import org.help789.mds.Entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByUserId(Long userId); // 只保留这一类
}
