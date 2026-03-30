package com.wirc.service;

import com.wirc.common.RoomSession;
import com.wirc.validation.MessageValidationHandler;

import java.util.List;
import java.util.Map;

public interface ChatStateRegistry {

    Map<String, RoomSession> rooms();

    MessageValidationHandler validationChain();

    List<RoomSession> sortedRooms();

    RoomSession requireRoom(String roomId);
}
