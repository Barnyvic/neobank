package com.vaultpay.auth.repository;

import com.vaultpay.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.user.id = :userId")
    List<UserRole> findByUserIdWithRole(@Param("userId") Long userId);
}
