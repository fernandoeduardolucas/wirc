import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ChatRoom } from '../shared/chat.types';

@Component({
  selector: 'app-salas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './salas.component.html',
  styleUrl: './salas.component.css'
})
export class SalasComponent {
  @Input({ required: true }) rooms: ChatRoom[] = [];
  @Input({ required: true }) activeRoomId = '';
  @Output() roomSelected = new EventEmitter<string>();
}
