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

interface LocalRoomDraft {
  name: string;
  participantInput: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="shell">
      <aside class="sidebar-panel">
        <div class="brand card mirc-logo">
          <div class="brand-mark">m</div>
          <div>
            <p class="eyebrow">WIRC</p>
            <h1>Messenger Relay Chat</h1>
            <p class="muted">Visual inspirado no mIRC, simples e funcional.</p>
          </div>
        </div>

        <section class="card section-stack">
          <div class="section-title">
            <h2>Identidade</h2>
            <span class="status">online</span>
          </div>

          <label class="label">Nickname ativo</label>
          <input [(ngModel)]="user" placeholder="Ex.: Ana" />

          <div class="mini-grid">
            <div>
              <label class="label">Novo utilizador</label>
              <div class="inline-form">
                <input [(ngModel)]="newUserName" placeholder="Adicionar utilizador" (keydown.enter)="addUser()" />
                <button (click)="addUser()">+</button>
              </div>
            </div>
            <div class="user-list-box">
              <p class="label">Utilizadores</p>
              <button
                *ngFor="let availableUser of availableUsers"
                class="list-chip"
                [class.active]="availableUser === user"
                (click)="selectUser(availableUser)"
              >
                {{ availableUser }}
              </button>
            </div>
          </div>
        </section>

        <section class="card section-stack">
          <div class="section-title">
            <h2>Salas</h2>
            <span class="muted">{{ rooms.length }} abertas</span>
          </div>

          <div class="inline-form room-creator">
            <input [(ngModel)]="newRoom.name" placeholder="#nova-sala" />
            <button (click)="createRoom()">Criar</button>
          </div>
          <input
            [(ngModel)]="newRoom.participantInput"
            placeholder="Participantes: Ana, Rui, Marta"
            (keydown.enter)="createRoom()"
          />

          <button
            *ngFor="let room of rooms"
            class="room-entry"
            [class.active]="room.id === activeRoomId"
            (click)="selectRoom(room.id)"
          >
            <div>
              <strong>{{ room.name }}</strong>
              <small>{{ room.participants.join(', ') || 'Sem participantes' }}</small>
            </div>
            <span class="badge" *ngIf="room.unreadMessages > 0">{{ room.unreadMessages }}</span>
          </button>
        </section>
      </aside>

      <main class="workspace">
        <section class="chat-window card" *ngIf="activeRoom as room">
          <header class="window-titlebar">
            <div class="title-dots">
              <span></span><span></span><span></span>
            </div>
            <div>
              <h2>{{ room.name }}</h2>
              <p class="muted">{{ room.participants.length }} utilizadores ligados</p>
            </div>
            <div class="title-meta">{{ room.state }}</div>
          </header>

          <div class="toolbar card nested-toolbar">
            <div>
              <label class="label">Pesquisar no histórico</label>
              <div class="inline-form">
                <input [(ngModel)]="searchTerm" placeholder="texto a procurar..." />
                <button (click)="runSearch()">Pesquisar</button>
              </div>
            </div>
            <div class="toolbar-stats" *ngIf="stats">
              <span><strong>{{ stats.totalMessages }}</strong> msgs</span>
              <span><strong>{{ stats.highlightedMessages }}</strong> destaque</span>
              <span><strong>{{ stats.busiestUser }}</strong> ativo</span>
            </div>
          </div>

          <div class="notification" *ngIf="notificationMessage">🔔 {{ notificationMessage }}</div>
          <div class="error-banner" *ngIf="error">{{ error }}</div>

          <div class="chat-body">
            <section class="messages-panel">
              <div class="search-results card nested-panel" *ngIf="searchResults.length > 0">
                <h3>Resultados</h3>
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
            </section>

            <aside class="people-panel card nested-panel">
              <div class="section-title compact-title">
                <h3>Canal</h3>
                <span class="muted">{{ room.participants.length }} users</span>
              </div>

              <div class="participant-list">
                <button
                  *ngFor="let participant of room.participants"
                  class="list-chip"
                  [class.active]="participant === user"
                  (click)="selectUser(participant)"
                >
                  {{ participant }}
                </button>
              </div>

              <label class="label">Adicionar utilizador à sala</label>
              <div class="inline-form">
                <select [(ngModel)]="selectedRoomUser">
                  <option value="">Escolher utilizador</option>
                  <option *ngFor="let availableUser of availableUsers" [value]="availableUser">{{ availableUser }}</option>
                </select>
                <button (click)="addUserToActiveRoom()">Adicionar</button>
              </div>

              <div class="card mini-stats">
                <h3>Top utilizadores</h3>
                <ol>
                  <li *ngFor="let userStat of topUsers">
                    {{ userStat.user }} — {{ userStat.totalMessages }} msgs
                  </li>
                </ol>
              </div>
            </aside>
          </div>

          <footer class="composer-box">
            <input
              [(ngModel)]="message"
              placeholder="Escreva uma mensagem para {{ room.name }}"
              (keydown.enter)="sendMessage()"
            />
            <button (click)="sendMessage()">Enviar</button>
          </footer>
        </section>
      </main>
    </div>
  `
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly chatController = inject(ChatController);
  private poller?: ReturnType<typeof setInterval>;
  private socket?: WebSocket;
  private localRooms = new Set<string>();
  private localMessages = new Map<string, ChatMessage[]>();

  user = 'Ana';
  newUserName = '';
  selectedRoomUser = '';
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
  availableUsers = ['Ana', 'Rui', 'Marta', 'SysOp'];
  newRoom: LocalRoomDraft = {
    name: '',
    participantInput: 'Ana, Rui'
  };

  get activeRoom(): ChatRoom | undefined {
    return this.rooms.find((room) => room.id === this.activeRoomId);
  }

  async ngOnInit(): Promise<void> {
    await this.refreshSidebar();
    if (this.rooms.length > 0) {
      await this.selectRoom(this.rooms[0].id);
    }

    try {
      this.socket = this.chatController.connectNotifications((notification) => {
        this.handleNotification(notification);
      });
    } catch {
      this.notificationMessage = 'Modo local ativo: notificações WebSocket indisponíveis.';
    }

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

    if (!this.localRooms.has(roomId)) {
      try {
        await this.chatController.focusRoom(roomId);
      } catch {
        // keep UI usable in local/demo mode
      }
    }

    await this.refreshSidebar();
    await this.loadMessages(roomId);
    this.notificationMessage = '';
    this.error = '';
  }

  addUser(): void {
    const candidate = this.newUserName.trim();
    if (!candidate) {
      return;
    }

    if (!this.availableUsers.includes(candidate)) {
      this.availableUsers = [...this.availableUsers, candidate].sort((a, b) => a.localeCompare(b));
    }

    this.user = candidate;
    this.newUserName = '';
  }

  selectUser(userName: string): void {
    this.user = userName;
  }

  createRoom(): void {
    const rawName = this.newRoom.name.trim();
    if (!rawName) {
      this.error = 'Indica um nome para a sala.';
      return;
    }

    const normalizedName = rawName.startsWith('#') ? rawName : `#${rawName}`;
    const roomId = normalizedName.toLowerCase().replace(/[^a-z0-9]+/gi, '-');
    if (this.rooms.some((room) => room.id === roomId)) {
      this.error = 'Já existe uma sala com esse nome.';
      return;
    }

    const participants = Array.from(
      new Set(
        this.newRoom.participantInput
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean)
          .concat(this.user)
      )
    );

    participants.forEach((participant) => {
      if (!this.availableUsers.includes(participant)) {
        this.availableUsers = [...this.availableUsers, participant];
      }
    });

    const room: ChatRoom = {
      id: roomId,
      name: normalizedName,
      participants,
      state: 'FOCUSED',
      unreadMessages: 0
    };

    this.localRooms.add(roomId);
    this.rooms = [room, ...this.rooms];
    this.localMessages.set(roomId, [
      {
        id: `local-${roomId}-welcome`,
        roomId,
        user: 'SysOp',
        message: `Sala ${normalizedName} criada. Podes começar a conversar.`,
        sentAt: new Date().toISOString(),
        highlighted: false
      }
    ]);

    this.newRoom = { name: '', participantInput: participants.join(', ') };
    this.error = '';
    void this.selectRoom(roomId);
  }

  addUserToActiveRoom(): void {
    if (!this.activeRoom || !this.selectedRoomUser) {
      return;
    }

    if (this.activeRoom.participants.includes(this.selectedRoomUser)) {
      this.error = 'Esse utilizador já está na sala atual.';
      return;
    }

    this.rooms = this.rooms.map((room) =>
      room.id === this.activeRoomId
        ? { ...room, participants: [...room.participants, this.selectedRoomUser] }
        : room
    );

    this.error = '';
    this.selectedRoomUser = '';
  }

  async sendMessage(): Promise<void> {
    const trimmed = this.message.trim();
    if (!trimmed || !this.activeRoomId) {
      return;
    }

    if (this.localRooms.has(this.activeRoomId)) {
      const current = this.localMessages.get(this.activeRoomId) ?? [];
      current.push({
        id: `local-${Date.now()}`,
        roomId: this.activeRoomId,
        user: this.user.trim() || 'Anónimo',
        message: trimmed,
        sentAt: new Date().toISOString(),
        highlighted: /^@|!/i.test(trimmed)
      });
      this.localMessages.set(this.activeRoomId, current);
      this.message = '';
      await this.loadMessages(this.activeRoomId);
      this.refreshDerivedTopUsers();
      this.error = '';
      return;
    }

    try {
      await this.chatController.sendMessage(this.activeRoomId, this.user.trim() || 'Anónimo', trimmed, true);
      this.message = '';
      await this.loadMessages(this.activeRoomId);
      await this.refreshSidebar();
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

    if (this.localRooms.size > 0) {
      const term = this.searchTerm.trim().toLowerCase();
      this.searchResults = Array.from(this.localMessages.values())
        .flat()
        .filter((item) => item.message.toLowerCase().includes(term) || item.user.toLowerCase().includes(term));
    }

    try {
      const remoteResults = await this.chatController.searchMessages(this.searchTerm.trim());
      const merged = [...this.searchResults, ...remoteResults];
      this.searchResults = merged.filter(
        (item, index) => merged.findIndex((candidate) => candidate.id === item.id) === index
      );
      this.error = '';
    } catch (err) {
      if (this.searchResults.length === 0) {
        this.error = err instanceof Error ? err.message : 'Erro ao pesquisar mensagens';
      }
    }
  }

  roomName(roomId: string): string {
    return this.rooms.find((room) => room.id === roomId)?.name ?? roomId;
  }

  private async refreshSidebar(): Promise<void> {
    try {
      const remoteRooms = await this.chatController.loadRooms();
      this.rooms = [...this.rooms.filter((room) => this.localRooms.has(room.id)), ...remoteRooms.filter((room) => !this.localRooms.has(room.id))];
    } catch {
      this.rooms = this.rooms.filter((room) => this.localRooms.has(room.id));
    }

    try {
      this.topUsers = await this.chatController.loadTopUsers();
    } catch {
      this.refreshDerivedTopUsers();
    }

    if (this.localRooms.size > 0) {
      this.refreshDerivedTopUsers();
    }
  }

  private async loadMessages(roomId: string): Promise<void> {
    if (this.localRooms.has(roomId)) {
      this.messages = this.localMessages.get(roomId) ?? [];
      this.stats = this.buildLocalStats(roomId, this.messages);
      return;
    }

    try {
      this.messages = await this.chatController.loadMessages(roomId);
      this.stats = await this.chatController.loadRoomStats(roomId);
    } catch (err) {
      this.messages = [];
      this.stats = undefined;
      this.error = err instanceof Error ? err.message : 'Erro ao carregar mensagens';
    }
  }

  private refreshDerivedTopUsers(): void {
    const counts = new Map<string, number>();
    Array.from(this.localMessages.values())
      .flat()
      .forEach((message) => {
        counts.set(message.user, (counts.get(message.user) ?? 0) + 1);
      });

    const localTop = Array.from(counts.entries())
      .map(([localUser, totalMessages]) => ({ user: localUser, totalMessages }))
      .sort((a, b) => b.totalMessages - a.totalMessages)
      .slice(0, 3);

    if (localTop.length > 0) {
      const merged = [...localTop, ...this.topUsers]
        .filter((item, index, array) => array.findIndex((candidate) => candidate.user === item.user) === index)
        .sort((a, b) => b.totalMessages - a.totalMessages)
        .slice(0, 3);
      this.topUsers = merged;
    }
  }

  private buildLocalStats(roomId: string, messages: ChatMessage[]): RoomStats {
    const counts = new Map<string, number>();
    messages.forEach((entry) => {
      counts.set(entry.user, (counts.get(entry.user) ?? 0) + 1);
    });

    const busiestUser = Array.from(counts.entries()).sort((a, b) => b[1] - a[1])[0]?.[0] ?? 'N/A';

    return {
      roomId,
      roomName: this.roomName(roomId),
      totalMessages: messages.length,
      highlightedMessages: messages.filter((entry) => entry.highlighted).length,
      busiestUser
    };
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
