import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Cinema } from '@the-film/customer/data-access';
import { SelectComponent, SelectOptionComponent } from '../select/select.component';

export interface QuickBookingSearchParams {
  cinemaId: string;
  movieSearchTerm: string;
  date: string;
  roomType: string;
}

@Component({
  selector: 'thefilm-quick-booking',
  standalone: true,
  imports: [CommonModule, FormsModule, SelectComponent, SelectOptionComponent],
  templateUrl: './quick-booking.component.html',
  styleUrls: ['./quick-booking.component.scss'],
})
export class QuickBookingComponent {
  @Input() cinemas: Cinema[] = [];
  @Input() selectedCinemaId = '';
  @Input() movieSearchTerm = '';
  @Input() dayVal = '01';
  @Input() monthVal = '11';
  @Input() yearVal = '2026';
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
