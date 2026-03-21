package com.wirc.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_room_member")
public class ChatRoomMemberEntity {
    @EmbeddedId
    private ChatRoomMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roomId")
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("username")
    @JoinColumn(name = "username", nullable = false)
    private AppUserEntity user;

    public ChatRoomMemberId getId() {
        return id;
    }

    public ChatRoomEntity getRoom() {
        return room;
    }

    public AppUserEntity getUser() {
        return user;
    }
}
