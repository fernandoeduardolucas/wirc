package com.wirc.repository;

import com.wirc.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, String> {
    @EntityGraph(attributePaths = {"members", "members.user"})
    List<ChatRoomEntity> findAllByOrderByNameAsc();
}
