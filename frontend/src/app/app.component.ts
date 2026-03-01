import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatController, ChatMessage } from './chat.controller';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <h1>Frontend em Angular 20 + GraphQL</h1>
      <p>Backend GraphQL em <code>http://localhost:8080/graphql</code></p>

      <div class="row">
        <input [(ngModel)]="user" placeholder="Seu nome" />
      </div>

      <div class="row">
        <input
          [(ngModel)]="message"
          placeholder="Digite a mensagem"
          (keydown.enter)="sendMessage()"
        />
        <button (click)="sendMessage()">Enviar</button>
      </div>

      <small *ngIf="error">Erro: {{ error }}</small>

      <ul>
        <li *ngFor="let item of messages">[{{ item.user }}] {{ item.message }}</li>
      </ul>
    </div>
  `
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly chatController = inject(ChatController);
  private poller?: ReturnType<typeof setInterval>;

  user = 'guest';
  message = '';
  messages: ChatMessage[] = [];
  error = '';

  async ngOnInit(): Promise<void> {
    await this.loadMessages();
    this.poller = setInterval(() => {
      void this.loadMessages();
    }, 2500);
  }

  ngOnDestroy(): void {
    if (this.poller) {
      clearInterval(this.poller);
    }
  }

  async sendMessage(): Promise<void> {
    const trimmed = this.message.trim();
    if (!trimmed) {
      return;
    }

    try {
      await this.chatController.sendMessage(this.user || 'guest', trimmed);
      this.message = '';
      await this.loadMessages();
      this.error = '';
    } catch (err) {
      this.error = err instanceof Error ? err.message : 'Erro ao enviar mensagem';
    }
  }

  private async loadMessages(): Promise<void> {
    try {
      this.messages = await this.chatController.loadMessages();
      this.error = '';
    } catch (err) {
      this.error = err instanceof Error ? err.message : 'Erro ao carregar mensagens';
    }
  }
}
