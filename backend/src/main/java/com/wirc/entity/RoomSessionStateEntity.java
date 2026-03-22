package com.wirc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_session_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RoomSessionStateEntity {

    @Id
    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    @Column(name = "state", nullable = false, length = 50)
    private String state;

    @Column(name = "unread_messages", nullable = false)
    private long unreadMessages;

}
