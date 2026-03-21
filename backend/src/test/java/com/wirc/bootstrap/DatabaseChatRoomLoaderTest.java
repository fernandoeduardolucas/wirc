package com.wirc.bootstrap;

import com.wirc.model.RoomSessionSnapshot;
import com.wirc.persistence.SchemaInspector;
import com.wirc.entity.AppUserEntity;
import com.wirc.entity.ChatRoomEntity;
import com.wirc.entity.ChatRoomMemberEntity;
import com.wirc.repository.ChatMessageRepository;
import com.wirc.repository.ChatRoomRepository;
import com.wirc.repository.RoomSessionStateRepository;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DatabaseChatRoomLoaderTest {
    @Test
    void loadRoomsFallsBackToDefaultsWhenOptionalTablesAreMissing() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        RoomSessionStateRepository roomSessionStateRepository = mock(RoomSessionStateRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        SchemaInspector schemaInspector = mock(SchemaInspector.class);

        when(schemaInspector.tableExists("room_session_state")).thenReturn(false);
        when(schemaInspector.tableExists("chat_message")).thenReturn(false);
        when(chatRoomRepository.findAllByOrderByNameAsc()).thenReturn(List.of(room("room-equipa", "Equipa Projeto", "Ana", "Bruno")));

        DatabaseChatRoomLoader loader = new DatabaseChatRoomLoader(
                chatRoomRepository,
                roomSessionStateRepository,
                chatMessageRepository,
                schemaInspector);

        List<RoomSessionSnapshot> snapshots = loader.loadRooms();

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().state()).isEqualTo("FOCUSED");
        assertThat(snapshots.getFirst().unreadMessages()).isZero();
        assertThat(snapshots.getFirst().messages()).isEmpty();
        assertThat(snapshots.getFirst().participants()).containsExactly("Ana", "Bruno");
        verify(roomSessionStateRepository, never()).findAll();
        verify(chatMessageRepository, never()).findAll();
    }

    private ChatRoomEntity room(String id, String name, String... displayNames) {
        ChatRoomEntity room = mock(ChatRoomEntity.class);
        when(room.getId()).thenReturn(id);
        when(room.getName()).thenReturn(name);

        LinkedHashSet<ChatRoomMemberEntity> members = new LinkedHashSet<>();
        for (String displayName : displayNames) {
            ChatRoomMemberEntity member = mock(ChatRoomMemberEntity.class);
            AppUserEntity user = mock(AppUserEntity.class);
            when(user.getDisplayName()).thenReturn(displayName);
            when(member.getUser()).thenReturn(user);
            members.add(member);
        }
        when(room.getMembers()).thenReturn(members);
        return room;
    }
}
