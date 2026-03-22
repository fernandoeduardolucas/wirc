package com.wirc.state;

import com.wirc.common.RoomSession;

// State pattern: each room state encapsulates how a room reacts to messages and focus events.
public interface RoomState {
    String name();

    void onMessageSent(RoomSession roomSession, boolean focused);

    void onRoomFocused(RoomSession roomSession);
}
