import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AppError, ChatMessage, ChatRoom, RoomStats } from '../shared/chat.types';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe],
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

  readonly searchForm = new FormGroup({
    searchTerm: new FormControl('', { nonNullable: true })
  });

  readonly messageForm = new FormGroup({
    message: new FormControl('', { nonNullable: true })
  });

  submitSearch(): void {
    this.searchSubmitted.emit(this.searchForm.controls.searchTerm.value);
  }

  submitMessage(): void {
    const message = this.messageForm.controls.message.value;
    this.messageSent.emit(message);
    this.messageForm.reset({ message: '' });
  }
}
