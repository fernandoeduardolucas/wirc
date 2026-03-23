import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AppUser, ChatRoom, UserMessageCount } from '../../shared/chat.types';

@Component({
  selector: 'app-room',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './room.component.html',
  styleUrl: './room.component.css'
})
export class RoomComponent {
  @Input({ required: true }) rooms: ChatRoom[] = [];
  @Input() room?: ChatRoom;
  @Input({ required: true }) activeRoomId = '';
  @Input({ required: true }) users: AppUser[] = [];
  @Input({ required: true }) authenticatedUser = '';
  @Input({ required: true }) currentUser = '';
  @Input({ required: true }) topUsers: UserMessageCount[] = [];
  @Output() roomSelected = new EventEmitter<string>();
  @Output() roomCreated = new EventEmitter<{ name: string; participants: string[] }>();
  @Output() userSelected = new EventEmitter<string>();
  @Output() memberAdded = new EventEmitter<string>();

  readonly roomForm = new FormGroup({
    newRoomName: new FormControl('', { nonNullable: true }),
    selectedParticipants: new FormArray<FormControl<string>>([])
  });
  readonly selectedMemberControl = new FormControl('', { nonNullable: true });

  get selectedParticipants(): FormArray<FormControl<string>> {
    return this.roomForm.controls.selectedParticipants;
  }

  isParticipantSelected(displayName: string): boolean {
    return this.selectedParticipants.controls.some((control) => control.value === displayName);
  }

  toggleParticipant(displayName: string, enabled: boolean): void {
    if (enabled && !this.isParticipantSelected(displayName)) {
      this.selectedParticipants.push(new FormControl(displayName, { nonNullable: true }));
      return;
    }

    if (!enabled) {
      const index = this.selectedParticipants.controls.findIndex((control) => control.value === displayName);
      if (index >= 0) {
        this.selectedParticipants.removeAt(index);
      }
    }
  }

  submitRoom(): void {
    this.roomCreated.emit({
      name: this.roomForm.controls.newRoomName.value,
      participants: this.selectedParticipants.getRawValue()
    });
    this.roomForm.reset({ newRoomName: '' });
    this.selectedParticipants.clear();
  }

  addMember(): void {
    this.memberAdded.emit(this.selectedMemberControl.value);
    this.selectedMemberControl.reset('');
  }
}
