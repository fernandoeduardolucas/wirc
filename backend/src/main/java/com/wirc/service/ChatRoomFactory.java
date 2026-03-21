package com.wirc.service;

import com.wirc.model.RoomSessionSnapshot;
import com.wirc.state.FocusedRoomState;
import com.wirc.state.NotifiedRoomState;
import com.wirc.state.RoomState;
import org.springframework.stereotype.Component;

@Component
// Factory pattern: centralizes RoomSession creation and state instantiation from persisted snapshots.
public class ChatRoomFactory {

    public RoomSession createFromSnapshot(RoomSessionSnapshot snapshot) {
        RoomSession roomSession = new RoomSession(
                snapshot.id(),
                snapshot.name(),
                toState(snapshot.state()),
                snapshot.participants(),
                snapshot.unreadMessages());
        roomSession.messages().addAll(snapshot.messages());
        return roomSession;
    }

    private RoomState toState(String state) {
        return switch (state) {
            case "NOTIFIED" -> new NotifiedRoomState();
            case "FOCUSED" -> new FocusedRoomState();
            default -> throw new IllegalArgumentException("Estado de sala desconhecido: " + state);
        };
    }
}
