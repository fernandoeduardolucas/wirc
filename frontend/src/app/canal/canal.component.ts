import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ChatRoom, UserMessageCount } from '../shared/chat.types';

@Component({
  selector: 'app-canal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './canal.component.html',
  styleUrl: './canal.component.css'
})
export class CanalComponent {
  @Input() room?: ChatRoom;
  @Input({ required: true }) currentUser = '';
  @Input({ required: true }) topUsers: UserMessageCount[] = [];
  @Output() userSelected = new EventEmitter<string>();
}
