import { Injectable, inject } from '@angular/core';
import { Observable, defer, from, of } from 'rxjs';
import { ChatRoom, UserMessageCount } from '../../shared/chat.types';
import { GraphqlApiService } from '../../shared/graphql-api.service';

@Injectable({ providedIn: 'root' })
export class RoomService {
  private readonly graphqlApi = inject(GraphqlApiService);

  loadRooms(activeUser: string): Observable<ChatRoom[]> {
    if (!activeUser) {
      return of([]);
    }

    return defer(() => from(
      this.graphqlApi.runQuery<{ rooms: ChatRoom[] }>(
        'query($activeUser: String!) { rooms(activeUser: $activeUser) { id name participants state unreadMessages canManageMembers } }',
        { activeUser }
      ).then((response) => response.data?.rooms ?? [])
    ));
  }

  focusRoom(roomId: string, activeUser: string): Observable<ChatRoom> {
    return defer(() => from(
      this.graphqlApi.runQuery<{ focusRoom: ChatRoom }>(
        'mutation($roomId: String!, $activeUser: String!) { focusRoom(roomId: $roomId, activeUser: $activeUser) { id name participants state unreadMessages canManageMembers } }',
        { roomId, activeUser }
      ).then((response) => {
        if (!response.data?.focusRoom) {
          throw this.graphqlApi.createError('Resposta GraphQL inválida para focusRoom.');
        }
        return response.data.focusRoom;
      })
    ));
  }



  loadTopUsers(activeUser: string): Observable<UserMessageCount[]> {
    if (!activeUser) {
      return of([]);
    }

    return defer(() => from(
      this.graphqlApi.runQuery<{ topUsers: UserMessageCount[] }>(
        'query($activeUser: String!) { topUsers(activeUser: $activeUser) { user totalMessages } }',
        { activeUser }
      ).then((response) => response.data?.topUsers ?? [])
    ));
  }

  addMemberToRoom(roomId: string, member: string, activeUser: string): Observable<ChatRoom> {
    return defer(() => from(
      this.graphqlApi.runQuery<{ addMemberToRoom: ChatRoom }>(
        'mutation($roomId: String!, $member: String!, $activeUser: String!) { addMemberToRoom(roomId: $roomId, member: $member, activeUser: $activeUser) { id name participants state unreadMessages canManageMembers } }',
        { roomId, member, activeUser }
      ).then((response) => {
        if (!response.data?.addMemberToRoom) {
          throw this.graphqlApi.createError('Resposta GraphQL inválida para addMemberToRoom.');
        }
        return response.data.addMemberToRoom;
      })
    ));
  }

  createRoom(name: string, activeUser: string, participants: string[]): Observable<ChatRoom> {
    return defer(() => from(
      this.graphqlApi.runQuery<{ createRoom: ChatRoom }>(
        'mutation($name: String!, $activeUser: String!, $participants: [String!]!) { createRoom(name: $name, activeUser: $activeUser, participants: $participants) { id name participants state unreadMessages canManageMembers } }',
        { name, activeUser, participants }
      ).then((response) => {
        if (!response.data?.createRoom) {
          throw this.graphqlApi.createError('Resposta GraphQL inválida para createRoom.');
        }
        return response.data.createRoom;
      })
    ));
  }
}
