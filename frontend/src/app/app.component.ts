import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ChatController,
  ChatMessage,
  ChatNotification,
  ChatRoom,
  RoomStats,
  UserMessageCount
} from './chat.controller';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="layout">
      <aside class="sidebar card">
        <h1>WIRC Multi-room Chat</h1>
        <p class="muted">GraphQL para dados e WebSocket para notificações instantâneas.</p>

        <label class="label">Utilizador</label>
        <input [(ngModel)]="user" placeholder="Ex.: Ana" />

        <label class="label">Pesquisar em conversas</label>
        <div class="row compact">
          <input [(ngModel)]="searchTerm" placeholder="graphql, reunião, ajuda..." />
          <button (click)="runSearch()">Pesquisar</button>
        </div>

        <h2>Salas</h2>
        <button
          *ngFor="let room of rooms"
          class="room-button"
          [class.active]="room.id === activeRoomId"
          (click)="selectRoom(room.id)"
        >
          <span>
            <strong>{{ room.name }}</strong>
            <small>{{ room.participants.join(', ') }}</small>
          </span>
          <span class="badge" *ngIf="room.unreadMessages > 0">{{ room.unreadMessages }}</span>
        </button>

        <div class="card nested" *ngIf="stats">
          <h3>Estatísticas da sala</h3>
          <p>Total de mensagens: <strong>{{ stats.totalMessages }}</strong></p>
          <p>Mensagens com destaque: <strong>{{ stats.highlightedMessages }}</strong></p>
          <p>Utilizador mais ativo: <strong>{{ stats.busiestUser }}</strong></p>
        </div>

        <div class="card nested">
          <h3>Top 3 utilizadores</h3>
          <ol>
            <li *ngFor="let userStat of topUsers">
              {{ userStat.user }} - {{ userStat.totalMessages }} mensagens
            </li>
          </ol>
        </div>
      </aside>

      <main class="content card" *ngIf="activeRoom">
        <header class="content-header">
          <div>
            <h2>{{ activeRoom.name }}</h2>
            <p class="muted">Estado atual: {{ activeRoom.state }}</p>
          </div>
          <div class="pill">{{ activeRoom.participants.length }} participantes</div>
        </header>

        <div class="notification" *ngIf="notificationMessage">
          🔔 {{ notificationMessage }}
        </div>

        <div class="search-results card nested" *ngIf="searchResults.length > 0">
          <h3>Resultados da pesquisa</h3>
          <ul>
            <li *ngFor="let result of searchResults">
              <strong>{{ result.user }}</strong> em <em>{{ roomName(result.roomId) }}</em>: {{ result.message }}
            </li>
          </ul>
        </div>

        <ul class="messages">
          <li *ngFor="let item of messages" [class.highlighted]="item.highlighted">
            <div class="message-header">
              <strong>{{ item.user }}</strong>
              <small>{{ item.sentAt | date:'shortTime' }}</small>
            </div>
            <span>{{ item.message }}</span>
          </li>
        </ul>

        <div class="composer">
          <input
            [(ngModel)]="message"
            placeholder="Escreva uma mensagem..."
            (keydown.enter)="sendMessage()"
          />
          <button (click)="sendMessage()">Enviar</button>
        </div>

        <small *ngIf="error" class="error">Erro: {{ error }}</small>
      </main>
    </div>
  `
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly chatController = inject(ChatController);
  private poller?: ReturnType<typeof setInterval>;
  private socket?: WebSocket;

  user = 'Ana';
  message = '';
  searchTerm = '';
  activeRoomId = '';
  rooms: ChatRoom[] = [];
  messages: ChatMessage[] = [];
  searchResults: ChatMessage[] = [];
  stats?: RoomStats;
  topUsers: UserMessageCount[] = [];
  notificationMessage = '';
  error = '';

  get activeRoom(): ChatRoom | undefined {
    return this.rooms.find((room) => room.id === this.activeRoomId);
  }

  async ngOnInit(): Promise<void> {
    await this.refreshSidebar();
    if (this.rooms.length > 0) {
      await this.selectRoom(this.rooms[0].id);
    }

    this.socket = this.chatController.connectNotifications((notification) => {
      this.handleNotification(notification);
    });

    this.poller = setInterval(() => {
      void this.refreshSidebar();
      if (this.activeRoomId) {
        void this.loadMessages(this.activeRoomId);
      }
    }, 4000);
  }

  ngOnDestroy(): void {
    if (this.poller) {
      clearInterval(this.poller);
    }
    this.socket?.close();
  }

  async selectRoom(roomId: string): Promise<void> {
    this.activeRoomId = roomId;
    this.searchResults = [];
    await this.chatController.focusRoom(roomId);
    await this.refreshSidebar();
    await this.loadMessages(roomId);
    this.stats = await this.chatController.loadRoomStats(roomId);
    this.notificationMessage = '';
  }

  async sendMessage(): Promise<void> {
    const trimmed = this.message.trim();
    if (!trimmed || !this.activeRoomId) {
      return;
    }

    try {
      await this.chatController.sendMessage(this.activeRoomId, this.user.trim() || 'Anónimo', trimmed, true);
      this.message = '';
      await this.loadMessages(this.activeRoomId);
      await this.refreshSidebar();
      this.stats = await this.chatController.loadRoomStats(this.activeRoomId);
      this.error = '';
    } catch (err) {
      this.error = err instanceof Error ? err.message : 'Erro ao enviar mensagem';
    }
  }

  async runSearch(): Promise<void> {
    if (!this.searchTerm.trim()) {
      this.searchResults = [];
      return;
    }

    try {
      this.searchResults = await this.chatController.searchMessages(this.searchTerm.trim());
      this.error = '';
    } catch (err) {
      this.error = err instanceof Error ? err.message : 'Erro ao pesquisar mensagens';
    }
  }

  roomName(roomId: string): string {
    return this.rooms.find((room) => room.id === roomId)?.name ?? roomId;
  }

  private async refreshSidebar(): Promise<void> {
    this.rooms = await this.chatController.loadRooms();
    this.topUsers = await this.chatController.loadTopUsers();
  }

  private async loadMessages(roomId: string): Promise<void> {
    this.messages = await this.chatController.loadMessages(roomId);
    this.stats = await this.chatController.loadRoomStats(roomId);
  }

  private handleNotification(notification: ChatNotification): void {
    if (notification.type === 'CONNECTED') {
      return;
    }

    if (notification.roomId && notification.roomId !== this.activeRoomId) {
      this.notificationMessage = `Nova mensagem de ${notification.user} na sala ${notification.roomName}: ${notification.preview}`;
      void this.refreshSidebar();
    }
  }
}
