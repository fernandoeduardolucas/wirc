import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppUser, ChatRoom } from '../shared/chat.types';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rooms.component.html',
  styleUrl: './rooms.component.css'
})
export class RoomsComponent {
  @Input({ required: true }) rooms: ChatRoom[] = [];
  @Input({ required: true }) activeRoomId = '';
  @Input({ required: true }) users: AppUser[] = [];
  @Input({ required: true }) authenticatedUser = '';
  @Output() roomSelected = new EventEmitter<string>();
  @Output() roomCreated = new EventEmitter<{ name: string; participants: string[] }>();

  newRoomName = '';
  selectedParticipants: string[] = [];

  toggleParticipant(displayName: string, enabled: boolean): void {
    this.selectedParticipants = enabled
      ? [...new Set([...this.selectedParticipants, displayName])]
      : this.selectedParticipants.filter((participant) => participant !== displayName);
  }

  submitRoom(): void {
    this.roomCreated.emit({ name: this.newRoomName, participants: this.selectedParticipants });
    this.newRoomName = '';
    this.selectedParticipants = [];
  }
}
