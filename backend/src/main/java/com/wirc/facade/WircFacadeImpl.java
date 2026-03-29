package com.wirc.facade;

import com.wirc.model.AppUser;
import com.wirc.model.ChatCommand;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;
import com.wirc.service.MessageService;
import com.wirc.service.RoomService;
import com.wirc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WircFacadeImpl implements WircFacade {

    private final UserService userService;
    private final RoomService roomService;
    private final MessageService messageService;

    @Override
    public List<AppUser> users() {
        return userService.users();
    }

    @Override
    public List<ChatRoom> rooms(String activeUser) {
        return roomService.rooms(activeUser);
    }

    @Override
    public List<ChatMessage> messagesByRoom(String roomId, String activeUser) {
        return messageService.messagesByRoom(roomId, activeUser);
    }

    @Override
    public List<ChatMessage> searchMessages(String term, String activeUser) {
        return messageService.searchMessages(term, activeUser);
    }

    @Override
    public RoomStats roomStats(String roomId, String activeUser) {
        return roomService.roomStats(roomId, activeUser);
    }

    @Override
    public List<UserMessageCount> topUsers(String activeUser) {
        return roomService.topUsers(activeUser);
    }

    @Override
    public AppUser signIn(String user, String password) {
        return userService.signIn(user, password);
    }

    @Override
    public AppUser createUser(String displayName, String password) {
        return userService.createUser(displayName, password);
    }

    @Override
    public ChatMessage sendMessage(String roomId, String activeUser, String user, String message, boolean focusedRoom) {
        return messageService.sendMessage(new ChatCommand(roomId, activeUser, user, message, focusedRoom));
    }

    @Override
    public ChatRoom focusRoom(String roomId, String activeUser) {
        return roomService.focusRoom(roomId, activeUser);
    }

    @Override
    public ChatRoom createRoom(String name, String activeUser, List<String> participants) {
        return roomService.createRoom(name, activeUser, participants);
    }

    @Override
    public ChatRoom addMemberToRoom(String roomId, String member, String activeUser) {
        return roomService.addMemberToRoom(roomId, member, activeUser);
    }
}
