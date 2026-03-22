import { Injectable } from '@angular/core';
import { Observable, defer, from, of } from 'rxjs';
import { AppError, AppUser, ChatMessage, ChatNotification, ChatRoom,  OutboundChatMessage, RoomStats, UserMessageCount } from './chat.types';

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
  private readonly pendingSocketMessages: OutboundChatMessage[] = [];

  loadUsers(activeUser: string): Observable<AppUser[]> {
    if (!activeUser) {
      return of([]);
    }
    return defer(() => from(this.runQuery<{ users: AppUser[] }>('query { users { username displayName } }').then((r) => r.data?.users ?? [])));
  }

  createUser(displayName: string, password: string): Observable<AppUser> {
    return defer(() => from(this.runQuery<{ createUser: AppUser }>(
      'mutation($displayName: String!, $password: String!) { createUser(displayName: $displayName, password: $password) { username displayName } }',
      { displayName, password }
    ).then((r) => {
      if (!r.data?.createUser) {
        throw this.createError('Resposta GraphQL inválida para createUser.');
      }
      return r.data.createUser;
    })));
  }

  signIn(user: string, password: string): Observable<AppUser> {
    return defer(() => from(this.runQuery<{ signIn: AppUser }>(
      'mutation($user: String!, $password: String!) { signIn(user: $user, password: $password) { username displayName } }',
      { user, password }
    ).then((r) => {
      if (!r.data?.signIn) {
        throw this.createError('Resposta GraphQL inválida para signIn.');
      }
      return r.data.signIn;
    })));
  }

  loadRooms(activeUser: string): Observable<ChatRoom[]> {
    if (!activeUser) {
      return of([]);
    }
    return defer(() => from(this.runQuery<{ rooms: ChatRoom[] }>(
      'query($activeUser: String!) { rooms(activeUser: $activeUser) { id name participants state unreadMessages canManageMembers } }',
      { activeUser }
    ).then((r) => r.data?.rooms ?? [])));
  }

  loadMessages(roomId: string, activeUser: string): Observable<ChatMessage[]> {
    if (!roomId) {
      return of([]);
    }
    return defer(() => from(this.runQuery<{ messagesByRoom: ChatMessage[] }>(
      'query($roomId: String!, $activeUser: String!) { messagesByRoom(roomId: $roomId, activeUser: $activeUser) { id roomId user message sentAt highlighted } }',
      { roomId, activeUser }
    ).then((r) => r.data?.messagesByRoom ?? [])));
  }

  searchMessages(term: string, activeUser: string): Observable<ChatMessage[]> {
    return defer(() => from(this.runQuery<{ searchMessages: ChatMessage[] }>(
      'query($term: String!, $activeUser: String!) { searchMessages(term: $term, activeUser: $activeUser) { id roomId user message sentAt highlighted } }',
      { term, activeUser }
    ).then((r) => r.data?.searchMessages ?? [])));
  }

  loadRoomStats(roomId: string, activeUser: string): Observable<RoomStats> {
    return defer(() => from(this.runQuery<{ roomStats: RoomStats }>(
      'query($roomId: String!, $activeUser: String!) { roomStats(roomId: $roomId, activeUser: $activeUser) { roomId roomName totalMessages highlightedMessages busiestUser } }',
      { roomId, activeUser }
    ).then((r) => {
      if (!r.data?.roomStats) {
        throw this.createError('Resposta GraphQL inválida para roomStats.');
      }
      return r.data.roomStats;
    })));
  }

  loadTopUsers(activeUser: string): Observable<UserMessageCount[]> {
    if (!activeUser) {
      return of([]);
    }
    return defer(() => from(this.runQuery<{ topUsers: UserMessageCount[] }>(
      'query($activeUser: String!) { topUsers(activeUser: $activeUser) { user totalMessages } }',
      { activeUser }
    ).then((r) => r.data?.topUsers ?? [])));
  }

  focusRoom(roomId: string, activeUser: string): Observable<ChatRoom> {
    return defer(() => from(this.runQuery<{ focusRoom: ChatRoom }>(
      'mutation($roomId: String!, $activeUser: String!) { focusRoom(roomId: $roomId, activeUser: $activeUser) { id name participants state unreadMessages canManageMembers } }',
      { roomId, activeUser }
    ).then((r) => {
      if (!r.data?.focusRoom) {
        throw this.createError('Resposta GraphQL inválida para focusRoom.');
      }
      return r.data.focusRoom;
    })));
  }

  createRoom(name: string, activeUser: string, participants: string[]): Observable<ChatRoom> {
    return defer(() => from(this.runQuery<{ createRoom: ChatRoom }>(
      'mutation($name: String!, $activeUser: String!, $participants: [String!]!) { createRoom(name: $name, activeUser: $activeUser, participants: $participants) { id name participants state unreadMessages canManageMembers } }',
      { name, activeUser, participants }
    ).then((r) => {
      if (!r.data?.createRoom) {
        throw this.createError('Resposta GraphQL inválida para createRoom.');
      }
      return r.data.createRoom;
    })));
  }

  addMemberToRoom(roomId: string, member: string, activeUser: string): Observable<ChatRoom> {
    return defer(() => from(this.runQuery<{ addMemberToRoom: ChatRoom }>(
      'mutation($roomId: String!, $member: String!, $activeUser: String!) { addMemberToRoom(roomId: $roomId, member: $member, activeUser: $activeUser) { id name participants state unreadMessages canManageMembers } }',
      { roomId, member, activeUser }
    ).then((r) => {
      if (!r.data?.addMemberToRoom) {
        throw this.createError('Resposta GraphQL inválida para addMemberToRoom.');
      }
      return r.data.addMemberToRoom;
    })));
  }

  connectNotifications(onNotification: (notification: ChatNotification) => void): WebSocket {
    const socket = new WebSocket(this.websocketEndpoint);
    socket.onopen = () => this.flushPendingSocketMessages(socket);
    socket.onmessage = (event) => onNotification(JSON.parse(event.data) as ChatNotification);
    return socket;
  }

  sendMessage(socket: WebSocket | undefined, roomId: string, user: string, message: string, focusedRoom: boolean): void {
    const payload: OutboundChatMessage = { type: 'SEND_MESSAGE', roomId, user, message, focusedRoom };
    if (!socket || socket.readyState === WebSocket.CONNECTING) {
      this.pendingSocketMessages.push(payload);
      return;
    }

    if (socket.readyState !== WebSocket.OPEN) {
      throw this.createError('Ligação WebSocket indisponível para enviar mensagens.');
    }

    socket.send(JSON.stringify(payload));
  }

  private flushPendingSocketMessages(socket: WebSocket): void {
    while (this.pendingSocketMessages.length > 0 && socket.readyState === WebSocket.OPEN) {
      const payload = this.pendingSocketMessages.shift();
      if (!payload) {
        return;
      }
      socket.send(JSON.stringify(payload));
    }
  }

  private async runQuery<T>(query: string, variables: Record<string, string | boolean | string[]> = {}): Promise<GraphqlResponse<T>> {
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
