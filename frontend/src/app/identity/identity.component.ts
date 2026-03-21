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

  password = '';

  submitCredentials(): void {
    this.credentialsSubmitted.emit({ user: this.currentUser, password: this.password });
    this.password = '';
  }
}
