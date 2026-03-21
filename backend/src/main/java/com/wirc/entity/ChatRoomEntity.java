package com.wirc.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor
public class ChatRoomEntity {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomMemberEntity> members = new LinkedHashSet<>();

    public ChatRoomEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addMember(AppUserEntity user) {
        boolean exists = members.stream().anyMatch(member -> member.getUser().getUsername().equalsIgnoreCase(user.getUsername()));
        if (!exists) {
            members.add(new ChatRoomMemberEntity(this, user));
        }
    }
}
