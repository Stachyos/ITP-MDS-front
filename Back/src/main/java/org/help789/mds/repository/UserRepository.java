package org.help789.mds.repository;

import org.help789.mds.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAccount(String account);
    boolean existsByAccount(String account);
    boolean existsByEmail(String email);
}
