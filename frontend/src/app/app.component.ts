import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { map } from 'rxjs/operators';
import { ChatComponent } from './chat/chat.component';
import { UserComponent } from './user/user.component';
import { RoomComponent } from './room/room.component';
import { ChatStore } from './shared/chat.store';
import { ChatRoom } from './shared/chat.types';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatComponent, UserComponent, RoomComponent],
  template: `
    @if (vm$ | async; as vm) {
    <div class="shell three-column-shell">
      <aside class="user-column">
        <div class="brand card">
          <img class="brand-logo" [src]="wircLogo" alt="WIRC logo" />

          <div>
            <p class="eyebrow">WIRC</p>
            <h1>Messenger Relay Chat</h1>
          </div>
        </div>

        <app-user
          [users]="vm.users"
          [currentUser]="vm.currentUser"
          [authenticatedUser]="vm.authenticatedUser"
          (userSelected)="store.selectUser($event)"
          (credentialsSubmitted)="store.authenticate($event.user, $event.password)"
          (userCreated)="store.createUser($event.displayName, $event.password)"
          (signOutRequested)="store.signOut()"
        />
      </aside>

      <main class="chat-column">
        @if (vm.authenticatedUser) {
        <app-chat
          [room]="activeRoom(vm.rooms, vm.activeRoomId)"
          [messages]="vm.messages"
          [searchResults]="vm.searchResults"
          [stats]="vm.stats"
          [currentUser]="vm.currentUser"
          [authenticatedUser]="vm.authenticatedUser"
          [notification]="vm.notification"
          [error]="vm.error"
          [roomNameResolver]="roomNameResolver(vm.rooms)"
          (searchSubmitted)="store.runSearch($event)"
          (messageSent)="store.sendMessage($event)"
        />
        } @else {
        <section class="hero card empty-state-panel">
          <img class="hero-logo" [src]="wircLogo" alt="WIRC mascot" />
          <div>
            <p class="eyebrow">Autenticação obrigatória</p>
            <h2>Autentique-se ou crie um utilizador para abrir o chat.</h2>
          </div>
        </section>
        }
      </main>

      @if (vm.authenticatedUser) {
      <aside class="channel-column">
        <app-room
          [rooms]="vm.rooms"
          [room]="activeRoom(vm.rooms, vm.activeRoomId)"
          [activeRoomId]="vm.activeRoomId"
          [users]="vm.users"
          [authenticatedUser]="vm.authenticatedUser"
          [currentUser]="vm.currentUser"
          [topUsers]="vm.topUsers"
          (roomSelected)="store.selectRoom($event)"
          (roomCreated)="store.createRoom($event.name, $event.participants)"
          (userSelected)="store.selectUser($event)"
          (memberAdded)="store.addMemberToActiveRoom($event)"
        />

      </aside>
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
