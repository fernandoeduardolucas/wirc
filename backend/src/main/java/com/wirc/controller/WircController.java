package com.wirc.controller;

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
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WircController {

    private final UserService userService;
    private final RoomService roomService;
    private final MessageService messageService;

    @QueryMapping
    public List<AppUser> users() {
        return userService.users();
    }

    @QueryMapping
    public List<ChatRoom> rooms(@Argument String activeUser) {
        return roomService.rooms(activeUser);
    }

    @QueryMapping
    public List<ChatMessage> messagesByRoom(@Argument String roomId, @Argument String activeUser) {
        return messageService.messagesByRoom(roomId, activeUser);
    }

    @QueryMapping
    public List<ChatMessage> searchMessages(@Argument String term, @Argument String activeUser) {
        return messageService.searchMessages(term, activeUser);
    }

    @QueryMapping
    public RoomStats roomStats(@Argument String roomId, @Argument String activeUser) {
        return roomService.roomStats(roomId, activeUser);
    }

    @QueryMapping
    public List<UserMessageCount> topUsers(@Argument String activeUser) {
        return roomService.topUsers(activeUser);
    }

    @MutationMapping
    public AppUser signIn(@Argument String user, @Argument String password) {
        return userService.signIn(user, password);
    }

    @MutationMapping
    public AppUser createUser(@Argument String displayName, @Argument String password) {
        return userService.createUser(displayName, password);
    }

    @MutationMapping
    public ChatMessage sendMessage(
            @Argument String roomId,
            @Argument String activeUser,
            @Argument String user,
            @Argument String message,
            @Argument boolean focusedRoom
    ) {
        return messageService.sendMessage(new ChatCommand(roomId, activeUser, user, message, focusedRoom));
    }

    @MutationMapping
    public ChatRoom focusRoom(@Argument String roomId, @Argument String activeUser) {
        return roomService.focusRoom(roomId, activeUser);
    }

    @MutationMapping
    public ChatRoom createRoom(@Argument String name, @Argument String activeUser, @Argument List<String> participants) {
        return roomService.createRoom(name, activeUser, participants);
    }

    @MutationMapping
    public ChatRoom addMemberToRoom(@Argument String roomId, @Argument String member, @Argument String activeUser) {
        return roomService.addMemberToRoom(roomId, member, activeUser);
    }
}
