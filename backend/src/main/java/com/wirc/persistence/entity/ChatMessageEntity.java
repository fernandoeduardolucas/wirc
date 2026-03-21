package com.wirc.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {
    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomEntity room;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "highlighted", nullable = false)
    private boolean highlighted;

    protected ChatMessageEntity() {
    }

    public ChatMessageEntity(String id, ChatRoomEntity room, String username, String message, Instant sentAt, boolean highlighted) {
        this.id = id;
        this.room = room;
        this.username = username;
        this.message = message;
        this.sentAt = sentAt;
        this.highlighted = highlighted;
    }

    public String getId() {
        return id;
    }

    public ChatRoomEntity getRoom() {
        return room;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
}
