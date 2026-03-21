package com.wirc.state;

import com.wirc.service.RoomSession;

// State pattern: behavior for rooms with unread activity waiting for user attention.
public class NotifiedRoomState implements RoomState {
    @Override
    public String name() {
        return "NOTIFIED";
    }

    @Override
    public void onMessageSent(RoomSession roomSession, boolean focused) {
        if (focused) {
            roomSession.clearUnread();
            roomSession.changeState(new FocusedRoomState());
            return;
        }

        roomSession.incrementUnread();
    }

    @Override
    public void onRoomFocused(RoomSession roomSession) {
        roomSession.clearUnread();
        roomSession.changeState(new FocusedRoomState());
    }
}
