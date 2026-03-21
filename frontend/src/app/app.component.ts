import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { map } from 'rxjs/operators';
import { ChatComponent } from './chat/chat.component';
import { IdentityComponent } from './identity/identity.component';
import { RoomsComponent } from './rooms/rooms.component';
import { ChatStore } from './shared/chat.store';
import { ChatRoom } from './shared/chat.types';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatComponent, IdentityComponent, RoomsComponent],
  template: `
    @if (vm$ | async; as vm) {
    <div class="shell">
      <aside class="sidebar-panel">
        <div class="brand card">
          <img class="brand-logo" [src]="wircLogo" alt="WIRC logo" />

          <div>
            <p class="eyebrow">WIRC</p>
            <h1>Messenger Relay Chat</h1>
          </div>
        </div>

        <app-identity
          [users]="vm.users"
          [currentUser]="vm.currentUser"
          [authenticatedUser]="vm.authenticatedUser"
          (userSelected)="store.selectUser($event)"
          (credentialsSubmitted)="store.authenticate($event.user, $event.password)"
        />

        <app-rooms
          [rooms]="vm.rooms"
          [activeRoomId]="vm.activeRoomId"
          [users]="vm.users"
          [authenticatedUser]="vm.authenticatedUser"
          (roomSelected)="store.selectRoom($event)"
          (roomCreated)="store.createRoom($event.name, $event.participants)"
        />
      </aside>

      @if (vm.authenticatedUser) {
      <main class="workspace">
        <section class="hero card">
          <img class="hero-logo" [src]="wircLogo" alt="WIRC mascot" />
          <div>
            <p class="eyebrow">Welcome to WIRC</p>
            <h2>Classic chat, front and center.</h2>
          </div>
        </section>

        <app-chat
          [room]="activeRoom(vm.rooms, vm.activeRoomId)"
          [messages]="vm.messages"
          [searchResults]="vm.searchResults"
          [stats]="vm.stats"
          [users]="vm.users"
          [topUsers]="vm.topUsers"
          [currentUser]="vm.currentUser"
          [authenticatedUser]="vm.authenticatedUser"
          [notification]="vm.notification"
          [error]="vm.error"
          [roomNameResolver]="roomNameResolver(vm.rooms)"
          (searchSubmitted)="store.runSearch($event)"
          (messageSent)="store.sendMessage($event)"
          (userSelected)="store.selectUser($event)"
          (memberAdded)="store.addMemberToActiveRoom($event)"
        />
      </main>
      } @else {
      <main class="workspace">
        <section class="hero card">
          <img class="hero-logo" [src]="wircLogo" alt="WIRC mascot" />
          <div>
            <p class="eyebrow">Autenticação obrigatória</p>
            <h2>Sem identidade autenticada não pode ver nada exceto autenticar-se.</h2>
          </div>
        </section>
      </main>
      }
    </div>
    }
  `
})
export class AppComponent {
  readonly store = inject(ChatStore);
  readonly vm$ = this.store.vm$.pipe(map((vm) => ({ ...vm })));
  readonly wircLogo = 'assets/wirc.png';

  activeRoom(rooms: ChatRoom[], activeRoomId: string): ChatRoom | undefined {
    return rooms.find((room) => room.id === activeRoomId);
  }

  roomNameResolver(rooms: Array<{ id: string; name: string }>) {
    return (roomId: string) => rooms.find((room) => room.id === roomId)?.name ?? roomId;
  }
}
