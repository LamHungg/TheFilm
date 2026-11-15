import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { BadgeComponent } from '@the-film/shared/ui';
import { formatVND } from '@the-film/shared/util';
import { Order, Seat, FnbCombo, BookingService, CinemaService } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-my-tickets',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, BadgeComponent],
  templateUrl: './my-tickets.component.html',
  styleUrl: './my-tickets.component.scss',
})
export class MyTicketsComponent implements OnInit {
  orders: Order[] = [];
  activeTab: 'ACTIVE' | 'HISTORY' = 'ACTIVE';
  categoryFilter: 'ALL' | 'MOVIE' | 'COMBO' = 'ALL';
  searchCode = '';
  modalOrder: Order | null = null;
  private posterByTitle = new Map<string, string>();

  constructor(
    private bookingService: BookingService,
    private cinemaService: CinemaService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.orders = this.bookingService.orders();
    this.syncOrdersFromBackend();

    this.cinemaService.getMovies().subscribe((movies) => {
      movies.forEach((m) => {
        if (m.title && m.posterUrl) {
          this.posterByTitle.set(m.title.trim().toLowerCase(), m.posterUrl);
        }
      });
      this.cdr.markForCheck();
    });
  }

  private syncOrdersFromBackend(): void {
    const storedOrders = this.bookingService.orders().filter((o) => Boolean(o.orderCode));
    if (storedOrders.length === 0) return;
    forkJoin(
      storedOrders.map((o) =>
        this.bookingService.getOrder(o.orderCode).pipe(catchError(() => of(null)))
      )
    ).subscribe(() => {
      this.orders = this.bookingService.orders();
      this.cdr.markForCheck();
    });
  }

  get activeOrders(): Order[] {
    return this.orders.filter((o) => this.isActiveOrder(o));
  }

  get historyOrders(): Order[] {
    return this.orders.filter((o) => !this.isActiveOrder(o));
  }

  isActiveOrder(order: Order): boolean {
    return order.status === 'CONFIRMED' || order.status === 'ISSUED';
  }

  getStatusLabel(order: Order): string {
    switch (order.status) {
      case 'CONFIRMED':
      case 'ISSUED':
        return 'ĐÃ XÁC NHẬN / SẴN SÀNG';
      case 'PENDING':
      case 'PENDING_PAYMENT':
        return 'CHỜ THANH TOÁN';
      case 'CANCELLED':
        return 'ĐÃ HỦY';
      default:
        return 'ĐÃ SỬ DỤNG';
    }
  }

  getQrCodeUrl(order: Order): string {
    if (order.qrCodeUrl) return order.qrCodeUrl;
    const qrData =
      order.tickets?.[0]?.qrData ||
      order.tickets?.[0]?.qrTokenHash ||
      order.tickets?.[0]?.ticketNumber ||
      order.bookingCode ||
      order.orderCode;
    return qrData
      ? `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(qrData)}`
      : '';
  }

  get displayedOrders(): Order[] {
    let list = this.activeTab === 'ACTIVE' ? this.activeOrders : this.historyOrders;
    if (this.categoryFilter === 'COMBO') {
      list = list.filter((o) => o.combos && o.combos.length > 0);
    }
    if (this.searchCode.trim()) {
      const q = this.searchCode.toLowerCase().trim();
      list = list.filter(
        (o) =>
          o.bookingCode.toLowerCase().includes(q) ||
          o.orderCode.toLowerCase().includes(q) ||
          o.movieTitle.toLowerCase().includes(q)
      );
    }
    return list;
  }

  getPosterUrl(order: Order): string {
    return this.posterByTitle.get((order.movieTitle || '').trim().toLowerCase()) || '';
  }

  searchTicket(): void {
    const code = this.searchCode.trim().toUpperCase();
    if (!code.startsWith('TF')) return;
    const alreadyExists = this.orders.some((o) => o.orderCode.toUpperCase() === code);
    if (alreadyExists) return;
    this.bookingService
      .getOrder(code)
      .pipe(catchError(() => of(null)))
      .subscribe((fetched) => {
        if (fetched) {
          this.orders = this.bookingService.orders();
          this.cdr.markForCheck();
        }
      });
  }

  openQrModal(order: Order): void {
    this.modalOrder = order;
  }

  getSeatCodes(seats: Seat[]): string {
    return seats.map((s) => s.code).join(', ');
  }

  getCombosText(combos: FnbCombo[]): string {
    return combos.map((c) => `${c.name} (x${c.quantity})`).join(', ');
  }

  formatMoney(amount: number): string {
    return formatVND(amount);
  }
}
