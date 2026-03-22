import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppUser } from '../shared/chat.types';

@Component({
  selector: 'app-identity',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './identity.component.html',
  styleUrl: './identity.component.css'
})
export class IdentityComponent {
  @Input({ required: true }) users: AppUser[] = [];
  @Input({ required: true }) currentUser = '';
  @Input({ required: true }) authenticatedUser = '';
  @Output() userSelected = new EventEmitter<string>();
  @Output() credentialsSubmitted = new EventEmitter<{ user: string; password: string }>();
  @Output() userCreated = new EventEmitter<{ displayName: string; password: string }>();
  @Output() signOutRequested = new EventEmitter<void>();

  password = '';
  newDisplayName = '';
  newPassword = '';

  submitCredentials(): void {
    this.credentialsSubmitted.emit({ user: this.currentUser, password: this.password });
    this.password = '';
  }

  submitUserCreation(): void {
    this.userCreated.emit({ displayName: this.newDisplayName, password: this.newPassword });
    this.newDisplayName = '';
    this.newPassword = '';
  }
}
