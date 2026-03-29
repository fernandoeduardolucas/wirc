package com.wirc.controller;

import com.wirc.model.AppUser;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;
import com.wirc.facade.WircFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WircController {

    private final WircFacade wircFacade;

    @QueryMapping
    public List<AppUser> users() {
        return wircFacade.users();
    }

    @QueryMapping
    public List<ChatRoom> rooms(@Argument String activeUser) {
        return wircFacade.rooms(activeUser);
    }

    @QueryMapping
    public List<ChatMessage> messagesByRoom(@Argument String roomId, @Argument String activeUser) {
        return wircFacade.messagesByRoom(roomId, activeUser);
    }

    @QueryMapping
    public List<ChatMessage> searchMessages(@Argument String term, @Argument String activeUser) {
        return wircFacade.searchMessages(term, activeUser);
    }

    @QueryMapping
    public RoomStats roomStats(@Argument String roomId, @Argument String activeUser) {
        return wircFacade.roomStats(roomId, activeUser);
    }

    @QueryMapping
    public List<UserMessageCount> topUsers(@Argument String activeUser) {
        return wircFacade.topUsers(activeUser);
    }

    @MutationMapping
    public AppUser signIn(@Argument String user, @Argument String password) {
        return wircFacade.signIn(user, password);
    }

    @MutationMapping
    public AppUser createUser(@Argument String displayName, @Argument String password) {
        return wircFacade.createUser(displayName, password);
    }

    @MutationMapping
    public ChatMessage sendMessage(
            @Argument String roomId,
            @Argument String activeUser,
            @Argument String user,
            @Argument String message,
            @Argument boolean focusedRoom
    ) {
        return wircFacade.sendMessage(roomId, activeUser, user, message, focusedRoom);
    }

    @MutationMapping
    public ChatRoom focusRoom(@Argument String roomId, @Argument String activeUser) {
        return wircFacade.focusRoom(roomId, activeUser);
    }

    @MutationMapping
    public ChatRoom createRoom(@Argument String name, @Argument String activeUser, @Argument List<String> participants) {
        return wircFacade.createRoom(name, activeUser, participants);
    }

    @MutationMapping
    public ChatRoom addMemberToRoom(@Argument String roomId, @Argument String member, @Argument String activeUser) {
        return wircFacade.addMemberToRoom(roomId, member, activeUser);
    }
}
