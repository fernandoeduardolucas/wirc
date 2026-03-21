package com.wirc.persistence.repository;

import com.wirc.persistence.entity.RoomSessionStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSessionStateRepository extends JpaRepository<RoomSessionStateEntity, String> {
}
