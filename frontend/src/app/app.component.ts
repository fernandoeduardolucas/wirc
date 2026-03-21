import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { map } from 'rxjs/operators';
import { ChatComponent } from './chat/chat.component';
import { IdentidadeComponent } from './identidade/identidade.component';
import { SalasComponent } from './salas/salas.component';
import { ChatStore } from './shared/chat.store';
import { ChatRoom } from './shared/chat.types';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatComponent, IdentidadeComponent, SalasComponent],
  template: `
    <div class="shell" *ngIf="vm$ | async as vm">
      <aside class="sidebar-panel">
        <div class="brand card">
          <div>
            <p class="eyebrow">WIRC</p>
            <h1>Messenger Relay Chat</h1>
            <p class="muted">Chat com visual clássico e identidade inspirada no logo que enviaste.</p>
          </div>
        </div>

        <app-identidade
          [users]="vm.users"
          [currentUser]="vm.currentUser"
          (userSelected)="store.selectUser($event)"
        />

        <app-salas
          [rooms]="vm.rooms"
          [activeRoomId]="vm.activeRoomId"
          (roomSelected)="store.selectRoom($event)"
        />
      </aside>

      <main class="workspace">
        <section class="hero-banner card">
          <div class="hero-copy">
            <p class="eyebrow">Página principal</p>
            <h2>Nova logo do WIRC</h2>
            <p class="muted">Usei a referência que mandaste: o “m” azul virou um “w” e o boneco amarelo agora está com óculos de sol.</p>
          </div>

          <img
            class="hero-logo"
            src="assets/wirc-logo.svg"
            alt="Logo do WIRC com W azul, irc laranja e boneco amarelo com óculos de sol"
          />
        </section>

        <app-chat
          [room]="activeRoom(vm.rooms, vm.activeRoomId)"
          [messages]="vm.messages"
          [searchResults]="vm.searchResults"
          [stats]="vm.stats"
          [topUsers]="vm.topUsers"
          [currentUser]="vm.currentUser"
          [notification]="vm.notification"
          [error]="vm.error"
          [roomNameResolver]="roomNameResolver(vm.rooms)"
          (searchSubmitted)="store.runSearch($event)"
          (messageSent)="store.sendMessage($event)"
          (userSelected)="store.selectUser($event)"
        />
      </main>
    </div>
  `
})
export class AppComponent {
  readonly store = inject(ChatStore);
  readonly vm$ = this.store.vm$.pipe(map((vm) => ({ ...vm })));

  activeRoom(rooms: ChatRoom[], activeRoomId: string): ChatRoom | undefined {
    return rooms.find((room) => room.id === activeRoomId);
  }

  roomNameResolver(rooms: Array<{ id: string; name: string }>) {
    return (roomId: string) => rooms.find((room) => room.id === roomId)?.name ?? roomId;
  }
}
