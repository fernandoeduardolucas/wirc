package com.wirc.service;

import com.wirc.common.RoomSession;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;

import java.util.List;

public interface RoomService {
    List<ChatRoom> rooms(String activeUser);

    ChatRoom focusRoom(String roomId, String activeUser);

    RoomStats roomStats(String roomId, String activeUser);

    List<UserMessageCount> topUsers(String activeUser);

    ChatRoom createRoom(String name, String activeUser, List<String> participants);

    ChatRoom addMemberToRoom(String roomId, String member, String activeUser);

    RoomSession requireAccessibleRoom(String roomId, String activeUser);
}
