package com.wirc.controller;

import com.wirc.model.AppUser;
import com.wirc.model.ChatCommand;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;
import com.wirc.service.ChatApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WircController {

    private final ChatApplication chatApplication;

    @QueryMapping
    public List<AppUser> users() {
        return chatApplication.users();
    }

    @QueryMapping
    public List<ChatRoom> rooms(@Argument String activeUser) {
        return chatApplication.rooms(activeUser);
    }

    @QueryMapping
    public List<ChatMessage> messagesByRoom(@Argument String roomId) {
        return chatApplication.messagesByRoom(roomId);
    }

    @QueryMapping
    public List<ChatMessage> searchMessages(@Argument String term) {
        return chatApplication.searchMessages(term);
    }

    @QueryMapping
    public RoomStats roomStats(@Argument String roomId) {
        return chatApplication.roomStats(roomId);
    }

    @QueryMapping
    public List<UserMessageCount> topUsers() {
        return chatApplication.topUsers();
    }

    @MutationMapping
    public AppUser signIn(@Argument String user, @Argument String password) {
        return chatApplication.signIn(user, password);
    }

    @MutationMapping
    public ChatMessage sendMessage(
            @Argument String roomId,
            @Argument String user,
            @Argument String message,
            @Argument boolean focusedRoom
    ) {
        return chatApplication.sendMessage(new ChatCommand(roomId, user, message, focusedRoom));
    }

    @MutationMapping
    public ChatRoom focusRoom(@Argument String roomId, @Argument String activeUser) {
        return chatApplication.focusRoom(roomId, activeUser);
    }

    @MutationMapping
    public ChatRoom createRoom(@Argument String name, @Argument String activeUser, @Argument List<String> participants) {
        return chatApplication.createRoom(name, activeUser, participants);
    }

    @MutationMapping
    public ChatRoom addMemberToRoom(@Argument String roomId, @Argument String member, @Argument String activeUser) {
        return chatApplication.addMemberToRoom(roomId, member, activeUser);
    }
}
