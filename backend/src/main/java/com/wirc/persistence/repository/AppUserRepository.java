package com.wirc.persistence.repository;

import com.wirc.persistence.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUserEntity, String> {
    List<AppUserEntity> findAllByOrderByDisplayNameAsc();
}
