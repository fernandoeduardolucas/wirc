package com.wirc.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatGraphqlController {

    private final List<GraphqlChatMessage> messages = new ArrayList<>();

    @QueryMapping
    public List<GraphqlChatMessage> messages() {
        return List.copyOf(messages);
    }

    @MutationMapping
    public GraphqlChatMessage sendMessage(@Argument String user, @Argument String message) {
        GraphqlChatMessage created = new GraphqlChatMessage(user, message);
        messages.add(created);
        return created;
    }
}
