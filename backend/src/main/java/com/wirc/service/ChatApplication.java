package com.wirc.service;


import com.wirc.model.*;
import java.util.List;



 public interface ChatApplication {

     List<AppUser> users();
     AppUser signIn(String user, String password);
     List<ChatRoom> rooms(String activeUser);
     List<ChatMessage> messagesByRoom(String roomId);
     List<ChatMessage> searchMessages(String term);
     ChatMessage sendMessage(ChatCommand command);
     ChatRoom focusRoom(String roomId, String activeUser);
     RoomStats roomStats(String roomId);
     List<UserMessageCount> topUsers();
     ChatRoom createRoom(String name, String activeUser, List<String> participants);
     ChatRoom addMemberToRoom(String roomId, String member, String activeUser);

}
