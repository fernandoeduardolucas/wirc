import { Injectable, inject } from '@angular/core';
import { Observable, defer, from, of } from 'rxjs';
import { ChatMessage } from '../../models/chat.models';
import { RoomStats } from '../../models/room.models';
import { GraphqlApiService } from '../../shared/graphql-api.service';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly graphqlApi = inject(GraphqlApiService);

  loadMessages(roomId: string, activeUser: string): Observable<ChatMessage[]> {
    if (!roomId) {
      return of([]);
    }

    return defer(() => from(
      this.graphqlApi.runQuery<{ messagesByRoom: ChatMessage[] }>(
        'query($roomId: String!, $activeUser: String!) { messagesByRoom(roomId: $roomId, activeUser: $activeUser) { id roomId user message sentAt highlighted } }',
        { roomId, activeUser }
      ).then((response) => response.data?.messagesByRoom ?? [])
    ));
  }

  searchMessages(term: string, activeUser: string): Observable<ChatMessage[]> {
    return defer(() => from(
      this.graphqlApi.runQuery<{ searchMessages: ChatMessage[] }>(
        'query($term: String!, $activeUser: String!) { searchMessages(term: $term, activeUser: $activeUser) { id roomId user message sentAt highlighted } }',
        { term, activeUser }
      ).then((response) => response.data?.searchMessages ?? [])
    ));
  }

  loadRoomStats(roomId: string, activeUser: string): Observable<RoomStats> {
    return defer(() => from(
      this.graphqlApi.runQuery<{ roomStats: RoomStats }>(
        'query($roomId: String!, $activeUser: String!) { roomStats(roomId: $roomId, activeUser: $activeUser) { roomId roomName totalMessages highlightedMessages busiestUser } }',
        { roomId, activeUser }
      ).then((response) => {
        if (!response.data?.roomStats) {
          throw this.graphqlApi.createError('Resposta GraphQL inválida para roomStats.');
        }
        return response.data.roomStats;
      })
    ));
  }
}
