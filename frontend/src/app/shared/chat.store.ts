import { Injectable, OnDestroy, inject } from '@angular/core';
import { BehaviorSubject, EMPTY, Subject, combineLatest } from 'rxjs';
import { catchError, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';
import { ChatService } from './chat.service';
import { AppError, AppUser, ChatMessage, ChatNotification, ChatRoom, RoomStats, UserMessageCount } from './chat.types';

@Injectable({ providedIn: 'root' })
export class ChatStore implements OnDestroy {
  private readonly chatService = inject(ChatService);
  private readonly refreshRooms$ = new BehaviorSubject<void>(undefined);
  private readonly activeRoomIdSubject = new BehaviorSubject<string>('');
  private readonly currentUserSubject = new BehaviorSubject<string>('');
  private readonly authenticatedUserSubject = new BehaviorSubject<string>('');
  private readonly searchResultsSubject = new BehaviorSubject<ChatMessage[]>([]);
  private readonly roomMessagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  private readonly roomStatsSubject = new BehaviorSubject<RoomStats | null>(null);
  private readonly topUsersSubject = new BehaviorSubject<UserMessageCount[]>([]);
  private readonly notificationSubject = new BehaviorSubject<string>('');
  private readonly errorSubject = new BehaviorSubject<AppError | null>(null);
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
    switchMap(() => this.chatService.loadRooms(this.authenticatedUserSubject.value)),
    tap((rooms) => {
      const activeRoomId = this.activeRoomIdSubject.value;
      if (!activeRoomId && rooms.length > 0) {
        this.activeRoomIdSubject.next(rooms[0].id);
        return;
      }
      if (activeRoomId && !rooms.some((room) => room.id === activeRoomId) && rooms.length > 0) {
        this.activeRoomIdSubject.next(rooms[0].id);
      }
    }),
    catchError((error: Error) => this.handleError(error, [] as ChatRoom[]))
  );

  readonly activeRoomId$ = this.activeRoomIdSubject.asObservable();
  readonly currentUser$ = this.currentUserSubject.asObservable();
  readonly authenticatedUser$ = this.authenticatedUserSubject.asObservable();
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
    authenticatedUser: this.authenticatedUser$,
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

  authenticate(displayName: string, password: string): void {
    const normalizedUser = displayName.trim();
    const normalizedPassword = password.trim();
    if (!normalizedUser || normalizedUser !== normalizedPassword) {
      this.errorSubject.next({ message: 'Credenciais inválidas. Use user/password iguais, por exemplo Ana / Ana.' });
      return;
    }

    this.currentUserSubject.next(normalizedUser);
    this.authenticatedUserSubject.next(normalizedUser);
    this.errorSubject.next(null);
    this.notificationSubject.next(`Identidade assumida: ${normalizedUser}`);
    this.refresh();
  }

  selectRoom(roomId: string): void {
    this.notificationSubject.next('');
    this.errorSubject.next(null);
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
      catchError((error: Error) => this.handleError(error, null))
    ).subscribe();
  }

  createRoom(name: string, participants: string[]): void {
    const activeUser = this.authenticatedUserSubject.value;
    if (!activeUser) {
      this.errorSubject.next({ message: 'Assuma primeiro uma identidade válida para criar canais.' });
      return;
    }

    this.chatService.createRoom(name.trim(), activeUser, participants).pipe(
      catchError((error: Error) => this.handleError(error, null))
    ).subscribe((room) => {
      if (!room) {
        return;
      }
      this.activeRoomIdSubject.next(room.id);
      this.notificationSubject.next(`Canal criado: ${room.name}`);
      this.refresh();
    });
  }

  addMemberToActiveRoom(member: string): void {
    const roomId = this.activeRoomIdSubject.value;
    const activeUser = this.authenticatedUserSubject.value;
    if (!roomId || !activeUser) {
      this.errorSubject.next({ message: 'Selecione um canal e assuma uma identidade antes de adicionar membros.' });
      return;
    }

    this.chatService.addMemberToRoom(roomId, member.trim(), activeUser).pipe(
      catchError((error: Error) => this.handleError(error, null))
    ).subscribe((room) => {
      if (!room) {
        return;
      }
      this.notificationSubject.next(`Membro adicionado ao canal ${room.name}.`);
      this.refresh();
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
    this.chatService.focusRoom(roomId, this.authenticatedUserSubject.value).pipe(catchError(() => EMPTY)).subscribe(() => this.refreshRooms$.next());
  }

  private handleNotification(notification: ChatNotification): void {
    if (notification.type === 'CONNECTED') {
      this.notificationSubject.next(notification.preview);
      return;
    }

    const activeRoomId = this.activeRoomIdSubject.value;
    const currentUser = this.currentUserSubject.value.trim().toLowerCase();
    const notificationUser = notification.user?.trim().toLowerCase();
    const isOwnMessage = notification.type === 'NEW_MESSAGE' && currentUser !== '' && notificationUser === currentUser;

    if (notification.type === 'NEW_MESSAGE' && notification.roomId && notification.messageId && notification.sentAt) {
      const liveMessage: ChatMessage = {
        id: notification.messageId,
        roomId: notification.roomId,
        user: notification.user ?? 'Sistema',
        message: notification.preview,
        sentAt: notification.sentAt,
        highlighted: notification.highlighted
      };

      if (notification.roomId === activeRoomId) {
        this.appendLiveMessage(liveMessage);
        this.refreshRoomStats(activeRoomId);
      }
    }

    if (!isOwnMessage) {
      this.notificationSubject.next(`${notification.user ?? 'Sistema'}: ${notification.preview}`);
    }

    this.refreshRooms$.next();
  }

  private appendLiveMessage(message: ChatMessage): void {
    const messages = this.roomMessagesSubject.value;
    if (messages.some((existingMessage) => existingMessage.id === message.id)) {
      return;
    }

    this.roomMessagesSubject.next([...messages, message]);
  }

  private refreshRoomStats(roomId: string): void {
    this.chatService.loadRoomStats(roomId).pipe(
      catchError((error: Error) => this.handleError(error, null))
    ).subscribe((stats) => this.roomStatsSubject.next(stats));
  }

  private handleError<T>(error: Error, fallback: T) {
    this.errorSubject.next(this.toAppError(error));
    return new BehaviorSubject(fallback);
  }

  private toAppError(error: Error | AppError): AppError {
    if ('code' in error || 'details' in error) {
      return error as AppError;
    }

    return { message: error.message };
  }
}
