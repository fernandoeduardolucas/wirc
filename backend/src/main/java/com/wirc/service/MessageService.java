package com.wirc.service;

import com.wirc.model.ChatCommand;
import com.wirc.model.ChatMessage;

import java.util.List;

public interface MessageService {
    List<ChatMessage> messagesByRoom(String roomId, String activeUser);

    List<ChatMessage> searchMessages(String term, String activeUser);

    ChatMessage sendMessage(ChatCommand command);
}
