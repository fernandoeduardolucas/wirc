package com.wirc.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ChatRoomMemberId implements Serializable {
    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    protected ChatRoomMemberId() {
    }

    public ChatRoomMemberId(String roomId, String username) {
        this.roomId = roomId;
        this.username = username;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChatRoomMemberId that)) {
            return false;
        }
        return Objects.equals(roomId, that.roomId) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, username);
    }
}
