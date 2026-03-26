import { Injectable } from '@angular/core';
import { ChatMessage, ChatNotification } from './models/chat.models';
import { ChatRoom, RoomStats, UserMessageCount } from './models/room.models';

interface GraphqlResponse<T> {
  data?: T;
  errors?: Array<{ message: string }>;
}

@Injectable({ providedIn: 'root' })
export class ChatController {
  private readonly endpoint = 'http://localhost:8080/wirc';
  private readonly websocketEndpoint = 'ws://localhost:8080/wirc/chat';

  async loadRooms(): Promise<ChatRoom[]> {
    const response = await this.runQuery<{ rooms: ChatRoom[] }>(
      'query { rooms { id name participants state unreadMessages canManageMembers } }'
    );
    return response.data?.rooms ?? [];
  }

  async loadMessages(roomId: string): Promise<ChatMessage[]> {
    const response = await this.runQuery<{ messagesByRoom: ChatMessage[] }>(
      'query($roomId: String!) { messagesByRoom(roomId: $roomId) { id roomId user message sentAt highlighted } }',
      { roomId }
    );
    return response.data?.messagesByRoom ?? [];
  }

  async searchMessages(term: string): Promise<ChatMessage[]> {
    const response = await this.runQuery<{ searchMessages: ChatMessage[] }>(
      'query($term: String!) { searchMessages(term: $term) { id roomId user message sentAt highlighted } }',
      { term }
    );
    return response.data?.searchMessages ?? [];
  }

  async loadRoomStats(roomId: string): Promise<RoomStats> {
    const response = await this.runQuery<{ roomStats: RoomStats }>(
      'query($roomId: String!) { roomStats(roomId: $roomId) { roomId roomName totalMessages highlightedMessages busiestUser } }',
      { roomId }
    );

    if (!response.data?.roomStats) {
      throw new Error('Resposta GraphQL inválida para roomStats.');
    }

    return response.data.roomStats;
  }

  async loadTopUsers(): Promise<UserMessageCount[]> {
    const response = await this.runQuery<{ topUsers: UserMessageCount[] }>(
      'query { topUsers { user totalMessages } }'
    );
    return response.data?.topUsers ?? [];
  }

  async sendMessage(roomId: string, user: string, message: string, focusedRoom: boolean): Promise<ChatMessage> {
    const response = await this.runQuery<{ sendMessage: ChatMessage }>(
      'mutation($roomId: String!, $user: String!, $message: String!, $focusedRoom: Boolean!) { sendMessage(roomId: $roomId, user: $user, message: $message, focusedRoom: $focusedRoom) { id roomId user message sentAt highlighted } }',
      { roomId, user, message, focusedRoom }
    );

    if (!response.data?.sendMessage) {
      throw new Error('Resposta GraphQL inválida para sendMessage.');
    }

    return response.data.sendMessage;
  }

  async focusRoom(roomId: string): Promise<ChatRoom> {
    const response = await this.runQuery<{ focusRoom: ChatRoom }>(
      'mutation($roomId: String!) { focusRoom(roomId: $roomId) { id name participants state unreadMessages canManageMembers } }',
      { roomId }
    );

    if (!response.data?.focusRoom) {
      throw new Error('Resposta GraphQL inválida para focusRoom.');
    }

    return response.data.focusRoom;
  }

  connectNotifications(onNotification: (notification: ChatNotification) => void): WebSocket {
    const socket = new WebSocket(this.websocketEndpoint);
    socket.onmessage = (event) => {
      onNotification(JSON.parse(event.data) as ChatNotification);
    };
    return socket;
  }

  private async runQuery<T>(query: string, variables: Record<string, string | boolean> = {}): Promise<GraphqlResponse<T>> {
    const raw = await fetch(this.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, variables })
    });

    if (!raw.ok) {
      throw new Error(`Falha HTTP ${raw.status}: ${raw.statusText}`);
    }

    const payload = (await raw.json()) as GraphqlResponse<T>;

    if (payload.errors?.length) {
      throw new Error(payload.errors[0].message);
    }

    return payload;
  }
}
