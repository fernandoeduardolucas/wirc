package com.wirc.controller;

import com.wirc.model.AppUser;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;
import com.wirc.service.ChatApplicationFacade;
import com.wirc.service.ChatCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatGraphqlController {
    private final ChatApplicationFacade chatFacade;

    @QueryMapping
    public List<AppUser> users() {
        return chatFacade.users();
    }

    @QueryMapping
    public List<ChatRoom> rooms() {
        return chatFacade.rooms();
    }

    @QueryMapping
    public List<ChatMessage> messagesByRoom(@Argument String roomId) {
        return chatFacade.messagesByRoom(roomId);
    }

    @QueryMapping
    public List<ChatMessage> searchMessages(@Argument String term) {
        return chatFacade.searchMessages(term);
    }

    @QueryMapping
    public RoomStats roomStats(@Argument String roomId) {
        return chatFacade.roomStats(roomId);
    }

    @QueryMapping
    public List<UserMessageCount> topUsers() {
        return chatFacade.topUsers();
    }

    @MutationMapping
    public ChatMessage sendMessage(
            @Argument String roomId,
            @Argument String user,
            @Argument String message,
            @Argument boolean focusedRoom
    ) {
        return chatFacade.sendMessage(new ChatCommand(roomId, user, message, focusedRoom));
    }

    @MutationMapping
    public ChatRoom focusRoom(@Argument String roomId) {
        return chatFacade.focusRoom(roomId);
    }
}
