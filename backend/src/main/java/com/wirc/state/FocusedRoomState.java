package com.wirc.state;

import com.wirc.service.RoomSession;

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
