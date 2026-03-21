package com.wirc.persistence.repository;

import com.wirc.persistence.entity.ChatMessageEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {
    List<ChatMessageEntity> findAllByRoom_IdOrderBySentAtAsc(String roomId);

    @Transactional
    void deleteAllByRoom_Id(String roomId);
}
