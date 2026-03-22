package com.wirc.service;


import com.wirc.model.*;
import java.util.List;



 public interface ChatApplication {

     List<AppUser> users();
     AppUser signIn(String user, String password);
     AppUser createUser(String displayName, String password);
     List<ChatRoom> rooms(String activeUser);
     List<ChatMessage> messagesByRoom(String roomId, String activeUser);
     List<ChatMessage> searchMessages(String term, String activeUser);
     ChatMessage sendMessage(ChatCommand command);
     ChatRoom focusRoom(String roomId, String activeUser);
     RoomStats roomStats(String roomId, String activeUser);
     List<UserMessageCount> topUsers(String activeUser);
     ChatRoom createRoom(String name, String activeUser, List<String> participants);
     ChatRoom addMemberToRoom(String roomId, String member, String activeUser);

}
