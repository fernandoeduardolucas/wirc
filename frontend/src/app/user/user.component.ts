import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AppUser } from '../shared/chat.types';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user.component.html',
  styleUrl: './user.component.css'
})
export class UserComponent implements OnChanges {
  @Input({ required: true }) users: AppUser[] = [];
  @Input({ required: true }) currentUser = '';
  @Input({ required: true }) authenticatedUser = '';
  @Output() userSelected = new EventEmitter<string>();
  @Output() credentialsSubmitted = new EventEmitter<{ user: string; password: string }>();
  @Output() userCreated = new EventEmitter<{ displayName: string; password: string }>();
  @Output() signOutRequested = new EventEmitter<void>();

  readonly loginForm = new FormGroup({
    user: new FormControl('', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true })
  });

  readonly createUserForm = new FormGroup({
    displayName: new FormControl('', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true })
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentUser']) {
      this.loginForm.controls.user.setValue(this.currentUser, { emitEvent: false });
    }
  }

  onUserChanged(user: string): void {
    this.userSelected.emit(user);
  }

  submitCredentials(): void {
    this.credentialsSubmitted.emit(this.loginForm.getRawValue());
    this.loginForm.controls.password.reset('');
  }

  submitUserCreation(): void {
    this.userCreated.emit(this.createUserForm.getRawValue());
    this.createUserForm.reset({ displayName: '', password: '' });
  }
}
