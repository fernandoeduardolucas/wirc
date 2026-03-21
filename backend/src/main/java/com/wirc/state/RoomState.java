package com.wirc.state;

import com.wirc.service.RoomSession;

public interface RoomState {
    String name();

    void onMessageSent(RoomSession roomSession, boolean focused);

    void onRoomFocused(RoomSession roomSession);
}
