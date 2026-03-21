package com.wirc.repository;

import com.wirc.entity.RoomSessionStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSessionStateRepository extends JpaRepository<RoomSessionStateEntity, String> {
}
