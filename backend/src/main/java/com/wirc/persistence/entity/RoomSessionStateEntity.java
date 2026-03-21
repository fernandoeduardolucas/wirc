package com.wirc.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_session_state")
public class RoomSessionStateEntity {
    @Id
    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    @Column(name = "state", nullable = false, length = 50)
    private String state;

    @Column(name = "unread_messages", nullable = false)
    private long unreadMessages;

    protected RoomSessionStateEntity() {
    }

    public RoomSessionStateEntity(String roomId, String state, long unreadMessages) {
        this.roomId = roomId;
        this.state = state;
        this.unreadMessages = unreadMessages;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getState() {
        return state;
    }

    public long getUnreadMessages() {
        return unreadMessages;
    }
}
