import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppError, ChatMessage, ChatRoom, RoomStats } from '../shared/chat.types';
import { CanalComponent } from '../canal/canal.component';
import { UserMessageCount } from '../shared/chat.types';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, CanalComponent],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css'
})
export class ChatComponent {
  @Input() room?: ChatRoom;
  @Input() messages: ChatMessage[] = [];
  @Input() searchResults: ChatMessage[] = [];
  @Input() stats: RoomStats | null = null;
  @Input() topUsers: UserMessageCount[] = [];
  @Input() currentUser = '';
  @Input() notification = '';
  @Input() error: AppError | null = null;
  @Input() roomNameResolver: (roomId: string) => string = (roomId) => roomId;
  @Output() searchSubmitted = new EventEmitter<string>();
  @Output() messageSent = new EventEmitter<string>();
  @Output() userSelected = new EventEmitter<string>();

  searchTerm = '';
  message = '';

  submitSearch(): void {
    this.searchSubmitted.emit(this.searchTerm);
  }

  submitMessage(): void {
    this.messageSent.emit(this.message);
    this.message = '';
  }
}
