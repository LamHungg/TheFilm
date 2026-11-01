import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'thefilm-member-profile',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './member-profile.component.html',
  styleUrl: './member-profile.component.scss',
})
export class MemberProfileComponent {}
