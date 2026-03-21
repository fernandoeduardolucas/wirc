import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AppUser } from '../shared/chat.types';

@Component({
  selector: 'app-identidade',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './identidade.component.html',
  styleUrl: './identidade.component.css'
})
export class IdentidadeComponent {
  @Input({ required: true }) users: AppUser[] = [];
  @Input({ required: true }) currentUser = '';
  @Output() userSelected = new EventEmitter<string>();
}
