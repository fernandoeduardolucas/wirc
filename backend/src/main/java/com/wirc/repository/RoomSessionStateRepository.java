package com.wirc.repository;

import com.wirc.entity.RoomSessionStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository pattern: abstracts persisted room session state storage.
public interface RoomSessionStateRepository extends JpaRepository<RoomSessionStateEntity, String> {
}
