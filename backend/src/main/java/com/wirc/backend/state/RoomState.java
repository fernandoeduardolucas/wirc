package com.wirc.backend.state;

import com.wirc.backend.service.RoomSession;

public interface RoomState {
    String name();

    void onMessageSent(RoomSession roomSession, boolean focused);

    void onRoomFocused(RoomSession roomSession);
}
