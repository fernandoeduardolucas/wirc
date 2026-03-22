package com.wirc.persistence;

import com.wirc.common.DatabaseChatStateStore;
import com.wirc.common.SchemaInspector;
import com.wirc.repository.ChatMessageRepository;
import com.wirc.repository.ChatRoomRepository;
import com.wirc.repository.RoomSessionStateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class DatabaseChatStateStoreTest {
    @Test
    void saveSkipsPersistenceWhenRequiredTablesAreMissing() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        RoomSessionStateRepository roomSessionStateRepository = mock(RoomSessionStateRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        SchemaInspector schemaInspector = mock(SchemaInspector.class);

        when(schemaInspector.tableExists("room_session_state")).thenReturn(false);

        DatabaseChatStateStore store = new DatabaseChatStateStore(
                chatRoomRepository,
                roomSessionStateRepository,
                chatMessageRepository,
                schemaInspector);

        store.save(List.of());

        verifyNoInteractions(chatRoomRepository, roomSessionStateRepository, chatMessageRepository);
    }
}
