import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AppUser } from '../shared/chat.types';

@Component({
  selector: 'app-identity',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './identity.component.html',
  styleUrl: './identity.component.css'
})
export class IdentityComponent {
  @Input({ required: true }) users: AppUser[] = [];
  @Input({ required: true }) currentUser = '';
  @Output() userSelected = new EventEmitter<string>();
}
