import { Injectable, OnDestroy, inject } from '@angular/core';
import { BehaviorSubject, EMPTY, Subject, combineLatest, interval } from 'rxjs';
import { catchError, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';
import { ChatService } from './chat.service';
import { AppUser, ChatMessage, ChatNotification, ChatRoom, RoomStats, UserMessageCount } from './chat.types';

@Injectable({ providedIn: 'root' })
export class ChatStore implements OnDestroy {
  private readonly chatService = inject(ChatService);
  private readonly refreshRooms$ = new BehaviorSubject<void>(undefined);
  private readonly activeRoomIdSubject = new BehaviorSubject<string>('');
  private readonly currentUserSubject = new BehaviorSubject<string>('');
  private readonly searchResultsSubject = new BehaviorSubject<ChatMessage[]>([]);
  private readonly roomMessagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  private readonly roomStatsSubject = new BehaviorSubject<RoomStats | null>(null);
  private readonly topUsersSubject = new BehaviorSubject<UserMessageCount[]>([]);
  private readonly notificationSubject = new BehaviorSubject<string>('');
  private readonly errorSubject = new BehaviorSubject<string>('');
  private readonly destroy$ = new Subject<void>();
  private socket?: WebSocket;

  readonly users$ = this.refreshRooms$.pipe(
    switchMap(() => this.chatService.loadUsers()),
    tap((users) => {
      if (!this.currentUserSubject.value && users.length > 0) {
        this.currentUserSubject.next(users[0].displayName);
      }
    }),
    catchError((error: Error) => this.handleError(error, [] as AppUser[]))
  );

  readonly rooms$ = this.refreshRooms$.pipe(
    switchMap(() => this.chatService.loadRooms()),
    tap((rooms) => {
      if (!this.activeRoomIdSubject.value && rooms.length > 0) {
        this.activeRoomIdSubject.next(rooms[0].id);
      }
    }),
    catchError((error: Error) => this.handleError(error, [] as ChatRoom[]))
  );

  readonly activeRoomId$ = this.activeRoomIdSubject.asObservable();
  readonly currentUser$ = this.currentUserSubject.asObservable();
  readonly messages$ = this.roomMessagesSubject.asObservable();
  readonly searchResults$ = this.searchResultsSubject.asObservable();
  readonly stats$ = this.roomStatsSubject.asObservable();
  readonly topUsers$ = this.topUsersSubject.asObservable();
  readonly notification$ = this.notificationSubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();
  readonly vm$ = combineLatest({
    users: this.users$,
    rooms: this.rooms$,
    activeRoomId: this.activeRoomId$,
    currentUser: this.currentUser$,
    messages: this.messages$,
    searchResults: this.searchResults$,
    stats: this.stats$,
    topUsers: this.topUsers$,
    notification: this.notification$,
    error: this.error$
  });

  constructor() {
    this.activeRoomId$.pipe(
      filter(Boolean),
      distinctUntilChanged(),
      tap((roomId) => this.focusRoom(roomId)),
      switchMap((roomId) => this.chatService.loadMessages(roomId)),
      catchError((error: Error) => this.handleError(error, [] as ChatMessage[]))
    ).subscribe((messages) => this.roomMessagesSubject.next(messages));

    this.activeRoomId$.pipe(
      filter(Boolean),
      distinctUntilChanged(),
      switchMap((roomId) => this.chatService.loadRoomStats(roomId)),
      catchError((error: Error) => this.handleError(error, null))
    ).subscribe((stats) => this.roomStatsSubject.next(stats));

    this.refreshRooms$.pipe(
      switchMap(() => this.chatService.loadTopUsers()),
      catchError((error: Error) => this.handleError(error, [] as UserMessageCount[]))
    ).subscribe((users) => this.topUsersSubject.next(users));

    interval(5000).pipe(
      tap(() => this.refresh()),
      catchError(() => EMPTY)
    ).subscribe();

    try {
      this.socket = this.chatService.connectNotifications((notification) => this.handleNotification(notification));
    } catch {
      this.notificationSubject.next('Modo local ativo: notificações WebSocket indisponíveis.');
    }
  }

  refresh(): void {
    this.refreshRooms$.next();
  }

  selectUser(displayName: string): void {
    this.currentUserSubject.next(displayName);
  }

  selectRoom(roomId: string): void {
    this.notificationSubject.next('');
    this.activeRoomIdSubject.next(roomId);
  }

  runSearch(term: string): void {
    const normalized = term.trim();
    if (!normalized) {
      this.searchResultsSubject.next([]);
      return;
    }

    this.chatService.searchMessages(normalized).pipe(
      catchError((error: Error) => this.handleError(error, [] as ChatMessage[]))
    ).subscribe((results) => this.searchResultsSubject.next(results));
  }

  sendMessage(message: string): void {
    const roomId = this.activeRoomIdSubject.value;
    const user = this.currentUserSubject.value;
    const normalized = message.trim();

    if (!roomId || !user || !normalized) {
      return;
    }

    this.chatService.sendMessage(roomId, user, normalized, true).pipe(
      tap(() => this.refresh()),
      catchError((error: Error) => this.handleError(error, null))
    ).subscribe(() => {
      this.chatService.loadMessages(roomId).subscribe((messages) => this.roomMessagesSubject.next(messages));
      this.chatService.loadRoomStats(roomId).subscribe((stats) => this.roomStatsSubject.next(stats));
    });
  }

  roomName(rooms: ChatRoom[], roomId: string): string {
    return rooms.find((room) => room.id === roomId)?.name ?? roomId;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.socket?.close();
  }

  private focusRoom(roomId: string): void {
    this.chatService.focusRoom(roomId).pipe(catchError(() => EMPTY)).subscribe(() => this.refreshRooms$.next());
  }

  private handleNotification(notification: ChatNotification): void {
    this.notificationSubject.next(`${notification.user ?? 'Sistema'}: ${notification.preview}`);
    this.refresh();
  }

  private handleError<T>(error: Error, fallback: T) {
    this.errorSubject.next(error.message);
    return new BehaviorSubject(fallback);
  }
}
