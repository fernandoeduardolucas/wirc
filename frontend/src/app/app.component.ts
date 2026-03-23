import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { map } from 'rxjs/operators';
import { ChatComponent } from './features/chat/chat.component';
import { UserComponent } from './features/user/user.component';
import { RoomComponent } from './features/room/room.component';
import { ChatStore } from './shared/chat.store';
import { ChatRoom } from './models/room.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatComponent, UserComponent, RoomComponent],
  templateUrl: './app.component.html'
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
