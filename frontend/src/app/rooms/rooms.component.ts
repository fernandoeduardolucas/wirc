import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ChatRoom } from '../shared/chat.types';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rooms.component.html',
  styleUrl: './rooms.component.css'
})
export class RoomsComponent {
  @Input({ required: true }) rooms: ChatRoom[] = [];
  @Input({ required: true }) activeRoomId = '';
  @Output() roomSelected = new EventEmitter<string>();
}
