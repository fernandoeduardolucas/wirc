import { Injectable } from '@angular/core';
import { Observable, defer, from } from 'rxjs';
import { AppError, AppUser, ChatMessage, ChatNotification, ChatRoom, RoomStats, UserMessageCount } from './chat.types';

interface GraphqlResponse<T> {
  data?: T;
  errors?: Array<{
    message: string;
    extensions?: {
      code?: string;
      details?: AppError['details'];
    };
  }>;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly endpoint = 'http://localhost:8080/graphql';
  private readonly websocketEndpoint = 'ws://localhost:8080/ws/chat';

  loadUsers(): Observable<AppUser[]> {
    return defer(() => from(this.runQuery<{ users: AppUser[] }>('query { users { username displayName } }').then((r) => r.data?.users ?? [])));
  }

  loadRooms(): Observable<ChatRoom[]> {
    return defer(() => from(this.runQuery<{ rooms: ChatRoom[] }>('query { rooms { id name participants state unreadMessages } }').then((r) => r.data?.rooms ?? [])));
  }

  loadMessages(roomId: string): Observable<ChatMessage[]> {
    return defer(() => from(this.runQuery<{ messagesByRoom: ChatMessage[] }>(
      'query($roomId: String!) { messagesByRoom(roomId: $roomId) { id roomId user message sentAt highlighted } }',
      { roomId }
    ).then((r) => r.data?.messagesByRoom ?? [])));
  }

  searchMessages(term: string): Observable<ChatMessage[]> {
    return defer(() => from(this.runQuery<{ searchMessages: ChatMessage[] }>(
      'query($term: String!) { searchMessages(term: $term) { id roomId user message sentAt highlighted } }',
      { term }
    ).then((r) => r.data?.searchMessages ?? [])));
  }

  loadRoomStats(roomId: string): Observable<RoomStats> {
    return defer(() => from(this.runQuery<{ roomStats: RoomStats }>(
      'query($roomId: String!) { roomStats(roomId: $roomId) { roomId roomName totalMessages highlightedMessages busiestUser } }',
      { roomId }
    ).then((r) => {
      if (!r.data?.roomStats) {
        throw this.createError('Resposta GraphQL inválida para roomStats.');
      }
      return r.data.roomStats;
    })));
  }

  loadTopUsers(): Observable<UserMessageCount[]> {
    return defer(() => from(this.runQuery<{ topUsers: UserMessageCount[] }>('query { topUsers { user totalMessages } }').then((r) => r.data?.topUsers ?? [])));
  }

  sendMessage(roomId: string, user: string, message: string, focusedRoom: boolean): Observable<ChatMessage> {
    return defer(() => from(this.runQuery<{ sendMessage: ChatMessage }>(
      'mutation($roomId: String!, $user: String!, $message: String!, $focusedRoom: Boolean!) { sendMessage(roomId: $roomId, user: $user, message: $message, focusedRoom: $focusedRoom) { id roomId user message sentAt highlighted } }',
      { roomId, user, message, focusedRoom }
    ).then((r) => {
      if (!r.data?.sendMessage) {
        throw this.createError('Resposta GraphQL inválida para sendMessage.');
      }
      return r.data.sendMessage;
    })));
  }

  focusRoom(roomId: string): Observable<ChatRoom> {
    return defer(() => from(this.runQuery<{ focusRoom: ChatRoom }>(
      'mutation($roomId: String!) { focusRoom(roomId: $roomId) { id name participants state unreadMessages } }',
      { roomId }
    ).then((r) => {
      if (!r.data?.focusRoom) {
        throw this.createError('Resposta GraphQL inválida para focusRoom.');
      }
      return r.data.focusRoom;
    })));
  }

  connectNotifications(onNotification: (notification: ChatNotification) => void): WebSocket {
    const socket = new WebSocket(this.websocketEndpoint);
    socket.onmessage = (event) => onNotification(JSON.parse(event.data) as ChatNotification);
    return socket;
  }

  private async runQuery<T>(query: string, variables: Record<string, string | boolean> = {}): Promise<GraphqlResponse<T>> {
    const raw = await fetch(this.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, variables })
    });

    if (!raw.ok) {
      throw this.createError(`Falha HTTP ${raw.status}: ${raw.statusText}`);
    }

    const payload = (await raw.json()) as GraphqlResponse<T>;
    if (payload.errors?.length) {
      const firstError = payload.errors[0];
      throw this.createError(firstError.message, firstError.extensions?.code, firstError.extensions?.details);
    }
    return payload;
  }

  private createError(message: string, code?: string, details?: AppError['details']): AppError {
    return { message, code, details };
  }
}
