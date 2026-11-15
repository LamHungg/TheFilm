import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Cinema } from '@the-film/customer/data-access';

export interface QuickBookingSearchParams {
  cinemaId: string;
  movieSearchTerm: string;
  date: string;
  roomType: string;
}

@Component({
  selector: 'thefilm-quick-booking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './quick-booking.component.html',
  styleUrls: ['./quick-booking.component.scss'],
})
export class QuickBookingComponent {
  private readonly today = new Date();
  @Input() cinemas: Cinema[] = [];
  @Input() selectedCinemaId = '';
  @Input() movieSearchTerm = '';
  @Input() dayVal = String(this.today.getDate()).padStart(2, '0');
  @Input() monthVal = String(this.today.getMonth() + 1).padStart(2, '0');
  @Input() yearVal = String(this.today.getFullYear());
  @Input() selectedRoomType = 'ALL';

  @Output() search = new EventEmitter<QuickBookingSearchParams>();

  onSearch(): void {
    const formattedDate = `${this.yearVal}-${this.monthVal.padStart(2, '0')}-${this.dayVal.padStart(2, '0')}`;
    this.search.emit({
      cinemaId: this.selectedCinemaId,
      movieSearchTerm: this.movieSearchTerm,
      date: formattedDate,
      roomType: this.selectedRoomType,
    });
  }
}
