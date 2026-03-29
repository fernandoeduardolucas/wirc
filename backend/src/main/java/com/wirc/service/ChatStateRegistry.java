package com.wirc.service;

import com.wirc.bootstrap.DatabaseChatRoomLoader;
import com.wirc.common.RoomSession;
import com.wirc.factory.ChatRoomFactory;
import com.wirc.validation.MessageLengthValidationHandler;
import com.wirc.validation.MessageValidationHandler;
import com.wirc.validation.ParticipantValidationHandler;
import com.wirc.validation.RequiredFieldValidationHandler;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatStateRegistry {

    private final Map<String, RoomSession> rooms = new ConcurrentHashMap<>();
    private final MessageValidationHandler validationChain;

    public ChatStateRegistry(ChatRoomFactory roomFactory, DatabaseChatRoomLoader databaseChatRoomLoader) {
        loadRooms(roomFactory, databaseChatRoomLoader);
        this.validationChain = buildValidationChain();
    }

    public Map<String, RoomSession> rooms() {
        return rooms;
    }

    public MessageValidationHandler validationChain() {
        return validationChain;
    }

    public List<RoomSession> sortedRooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(RoomSession::name))
                .toList();
    }

    public RoomSession requireRoom(String roomId) {
        RoomSession room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Sala não encontrada: " + roomId);
        }
        return room;
    }

    private void loadRooms(ChatRoomFactory roomFactory, DatabaseChatRoomLoader databaseChatRoomLoader) {
        databaseChatRoomLoader.loadRooms()
                .forEach(snapshot -> rooms.put(snapshot.id(), roomFactory.createFromSnapshot(snapshot)));
    }

    private MessageValidationHandler buildValidationChain() {
        MessageValidationHandler required = new RequiredFieldValidationHandler();
        required
                .linkWith(new ParticipantValidationHandler(rooms))
                .linkWith(new MessageLengthValidationHandler());
        return required;
    }
}
