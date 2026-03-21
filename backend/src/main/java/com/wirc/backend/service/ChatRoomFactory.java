package com.wirc.backend.service;

import com.wirc.backend.state.FocusedRoomState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatRoomFactory {

    public RoomSession create(String id, String name, List<String> participants) {
        // Design Pattern: Factory -> centraliza a criação consistente de salas.
        return new RoomSession(id, name, new FocusedRoomState(), participants);
    }
}
