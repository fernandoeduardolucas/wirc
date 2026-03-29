package com.wirc.service;

import com.wirc.model.AppUser;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;

import java.util.List;

public interface WircServiceFacade {
    List<AppUser> users();

    List<ChatRoom> rooms(String activeUser);

    List<ChatMessage> messagesByRoom(String roomId, String activeUser);

    List<ChatMessage> searchMessages(String term, String activeUser);

    RoomStats roomStats(String roomId, String activeUser);

    List<UserMessageCount> topUsers(String activeUser);

    AppUser signIn(String user, String password);

    AppUser createUser(String displayName, String password);

    ChatMessage sendMessage(String roomId, String activeUser, String user, String message, boolean focusedRoom);

    ChatRoom focusRoom(String roomId, String activeUser);

    ChatRoom createRoom(String name, String activeUser, List<String> participants);

    ChatRoom addMemberToRoom(String roomId, String member, String activeUser);
}
