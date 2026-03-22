package com.wirc.common;

import com.wirc.model.ChatMessage;
import com.wirc.model.RoomSessionSnapshot;
import com.wirc.state.RoomState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RoomSession {
    private final String id;
    private final String name;
    private final Set<String> participants = new LinkedHashSet<>();
    private final List<ChatMessage> messages = new ArrayList<>();
    private RoomState state;
    private long unreadMessages;

    public RoomSession(String id, String name, RoomState initialState, List<String> initialParticipants) {
        this(id, name, initialState, initialParticipants, 0);
    }

    public RoomSession(String id, String name, RoomState initialState, List<String> initialParticipants, long unreadMessages) {
        this.id = id;
        this.name = name;
        this.state = initialState;
        this.unreadMessages = unreadMessages;
        this.participants.addAll(initialParticipants);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Set<String> participants() {
        return participants;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public RoomState state() {
        return state;
    }

    public void changeState(RoomState nextState) {
        this.state = nextState;
    }

    public long unreadMessages() {
        return unreadMessages;
    }

    public void incrementUnread() {
        unreadMessages++;
    }

    public void clearUnread() {
        unreadMessages = 0;
    }

    public RoomSessionSnapshot snapshot() {
        return new RoomSessionSnapshot(
                id,
                name,
                new ArrayList<>(participants),
                state.name(),
                unreadMessages,
                new ArrayList<>(messages));
    }
}
