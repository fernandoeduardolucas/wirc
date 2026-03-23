import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { AppUser, ChatRoom, UserMessageCount } from '../shared/chat.types';

@Component({
  selector: 'app-canal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './canal.component.html',
  styleUrl: './canal.component.css'
})
export class CanalComponent {
  @Input() room?: ChatRoom;
  @Input({ required: true }) currentUser = '';
  @Input({ required: true }) authenticatedUser = '';
  @Input({ required: true }) users: AppUser[] = [];
  @Input({ required: true }) topUsers: UserMessageCount[] = [];
  @Output() userSelected = new EventEmitter<string>();
  @Output() memberAdded = new EventEmitter<string>();

  readonly selectedMemberControl = new FormControl('', { nonNullable: true });

  addMember(): void {
    this.memberAdded.emit(this.selectedMemberControl.value);
    this.selectedMemberControl.reset('');
  }
}
