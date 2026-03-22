import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppError, ChatMessage, ChatRoom, RoomStats } from '../shared/chat.types';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css'
})
export class ChatComponent {
  @Input() room?: ChatRoom;
  @Input() messages: ChatMessage[] = [];
  @Input() searchResults: ChatMessage[] = [];
  @Input() stats: RoomStats | null = null;
  @Input() currentUser = '';
  @Input() authenticatedUser = '';
  @Input() notification = '';
  @Input() error: AppError | null = null;
  @Input() roomNameResolver: (roomId: string) => string = (roomId) => roomId;
  @Output() searchSubmitted = new EventEmitter<string>();
  @Output() messageSent = new EventEmitter<string>();

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
