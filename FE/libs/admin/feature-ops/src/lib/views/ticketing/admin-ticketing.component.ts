import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminOpsService,
  FilmAdminDto,
  ScreeningAdminDto,
} from '../../services/admin-ops.service';

@Component({
  selector: 'thefilm-admin-ticketing',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-ticketing.component.html',
  styleUrl: './admin-ticketing.component.scss',
})
export class AdminTicketingComponent implements OnInit {
  private readonly adminOps = inject(AdminOpsService);

  tabs = [
    { label: 'Bán vé tại quầy', active: true },
    { label: 'Soát QR & Kiểm vé', active: false },
    { label: 'Tra cứu vé phát hành', active: false },
  ];

  films: FilmAdminDto[] = [];
  screenings: ScreeningAdminDto[] = [];

  selectedFilmId: number = 1;
  selectedScreeningId: number = 1;
  selectedMovieTitle = 'Tầng Mây Cuối Cùng';
  selectedShowtime = '17:45 · Standard 03 (Hôm nay)';

  paymentMethod: 'cash' | 'pos' = 'cash';
  ticketCodeInput = 'TF2026111576DE13B0';
  isChecking = false;
  scanResult: { valid: boolean; message: string } | null = null;

  rows = ['A', 'B', 'C', 'D', 'E', 'F'];
  cols = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  selectedSeats = new Set<string>(['D3', 'D4']);
  seatPrice = 110000;
  fnbPrice = 85000;
  hasFnb = true;

  // Modal đơn quầy
  isSuccessModalOpen = false;
  lastOrderCode = '';

  ngOnInit(): void {
    this.loadCatalog();
  }

  loadCatalog(): void {
    this.adminOps.getFilms().subscribe((films) => {
      this.films = films;
      if (films.length > 0) {
        this.selectedFilmId = films[0].filmId;
        this.selectedMovieTitle = films[0].titleLocal;
      }
    });

    this.adminOps.getScreenings(1).subscribe((scs) => {
      this.screenings = scs;
      if (scs.length > 0) {
        this.selectedScreeningId = scs[0].screeningId;
        const start = new Date(scs[0].startsAt);
        const timeStr = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}`;
        this.selectedShowtime = `${timeStr} · ${scs[0].roomName || 'Phòng 01'} (${scs[0].formatCode})`;
      }
    });
  }

  onFilmChange(id: number): void {
    this.selectedFilmId = Number(id);
    const found = this.films.find((f) => f.filmId === this.selectedFilmId);
    if (found) {
      this.selectedMovieTitle = found.titleLocal;
    }
  }

  onScreeningChange(id: number): void {
    this.selectedScreeningId = Number(id);
    const found = this.screenings.find((s) => s.screeningId === this.selectedScreeningId);
    if (found) {
      const start = new Date(found.startsAt);
      const timeStr = `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}`;
      this.selectedShowtime = `${timeStr} · ${found.roomName || 'Phòng 01'} (${found.formatCode})`;
    }
  }

  toggleSeat(seatId: string): void {
    if (this.selectedSeats.has(seatId)) {
      this.selectedSeats.delete(seatId);
    } else {
      this.selectedSeats.add(seatId);
    }
  }

  isSeatSelected(seatId: string): boolean {
    return this.selectedSeats.has(seatId);
  }

  getSelectedSeatsArray(): string[] {
    return Array.from(this.selectedSeats);
  }

  getTotalPrice(): number {
    return this.selectedSeats.size * this.seatPrice + (this.hasFnb ? this.fnbPrice : 0);
  }

  selectTab(index: number): void {
    this.tabs.forEach((t, i) => (t.active = i === index));
  }

  checkTicket(): void {
    if (!this.ticketCodeInput.trim()) return;
    this.isChecking = true;
    this.adminOps.validateTicket(this.ticketCodeInput.trim()).subscribe((res) => {
      this.isChecking = false;
      this.scanResult = res;
    });
  }

  checkout(): void {
    if (this.selectedSeats.size === 0) {
      alert('Vui lòng chọn ít nhất 1 ghế trước khi thanh toán.');
      return;
    }
    const rand = Math.random().toString(36).substring(2, 8).toUpperCase();
    this.lastOrderCode = `POS-${Date.now().toString().substring(6)}-${rand}`;
    this.isSuccessModalOpen = true;
  }

  finishOrder(): void {
    this.isSuccessModalOpen = false;
    this.selectedSeats.clear();
  }
}
