package com.wirc.state;

import com.wirc.common.RoomSession;

// State pattern: behavior for rooms currently being viewed by the user.
public class FocusedRoomState implements RoomState {
    @Override
    public String name() {
        return "FOCUSED";
    }

    @Override
    public void onMessageSent(RoomSession roomSession, boolean focused) {
        if (focused) {
            roomSession.clearUnread();
            return;
        }

        roomSession.incrementUnread();
        roomSession.changeState(new NotifiedRoomState());
    }

    @Override
    public void onRoomFocused(RoomSession roomSession) {
        roomSession.clearUnread();
    }
}
