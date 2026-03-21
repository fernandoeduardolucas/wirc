package com.wirc.backend.graphql;

import com.wirc.backend.model.ChatMessage;
import com.wirc.backend.model.ChatRoom;
import com.wirc.backend.model.RoomStats;
import com.wirc.backend.model.UserMessageCount;
import com.wirc.backend.service.ChatApplicationFacade;
import com.wirc.backend.service.ChatCommand;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatGraphqlController {
    private final ChatApplicationFacade chatFacade;

    public ChatGraphqlController(ChatApplicationFacade chatFacade) {
        this.chatFacade = chatFacade;
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
