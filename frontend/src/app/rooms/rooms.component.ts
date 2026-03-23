import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AppUser, ChatRoom } from '../shared/chat.types';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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

  readonly roomForm = new FormGroup({
    newRoomName: new FormControl('', { nonNullable: true }),
    selectedParticipants: new FormArray<FormControl<string>>([])
  });

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
}
