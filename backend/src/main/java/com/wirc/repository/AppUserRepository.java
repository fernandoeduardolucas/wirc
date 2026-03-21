package com.wirc.repository;

import com.wirc.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repository pattern: abstracts app user persistence and lookup operations.
public interface AppUserRepository extends JpaRepository<AppUserEntity, String> {
    List<AppUserEntity> findAllByOrderByDisplayNameAsc();

    Optional<AppUserEntity> findByUsernameIgnoreCase(String username);

    Optional<AppUserEntity> findByDisplayNameIgnoreCase(String displayName);
}
