package com.wirc.gateway;



import com.wirc.model.ChatNotification;
import org.springframework.web.socket.WebSocketSession;



public interface WebSocketNotificationGateway {

    void addSession(WebSocketSession session);
    void removeSession(WebSocketSession session);
    void broadcast(ChatNotification notification);

}
