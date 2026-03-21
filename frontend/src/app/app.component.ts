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
    <div class="shell" *ngIf="vm$ | async as vm">
      <aside class="sidebar-panel">
        <div class="brand card">
          <img
            class="brand-logo"
            src="assets/wirc.png"
            alt="WIRC logo"
          />

          <div>
            <p class="eyebrow">WIRC</p>
            <h1>Messenger Relay Chat</h1>
          </div>
        </div>

        <app-identity
          [users]="vm.users"
          [currentUser]="vm.currentUser"
          (userSelected)="store.selectUser($event)"
        />

        <app-rooms
          [rooms]="vm.rooms"
          [activeRoomId]="vm.activeRoomId"
          (roomSelected)="store.selectRoom($event)"
        />
      </aside>

      <main class="workspace">
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
