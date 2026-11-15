import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { BadgeComponent } from '@the-film/shared/ui';
import { formatVND } from '@the-film/shared/util';
import {
  AuthService,
  Order,
  Seat,
  FnbCombo,
  BookingService,
  CinemaService,
} from '@the-film/customer/data-access';

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
  isLoading = true;
  private posterByTitle = new Map<string, string>();

  readonly authService = inject(AuthService);
  private readonly bookingService = inject(BookingService);
  private readonly cinemaService = inject(CinemaService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.syncOrders();
    this.loadPosters();
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  private loadPosters(): void {
    this.cinemaService.getMovies().subscribe((movies) => {
      movies.forEach((m) => {
        if (m.title && m.posterUrl) {
          this.posterByTitle.set(m.title.trim().toLowerCase(), m.posterUrl);
        }
      });
      this.cdr.markForCheck();
    });
  }

  private syncOrders(): void {
    this.isLoading = true;

    if (this.authService.isAuthenticated()) {
      // 1. Fetch real orders strictly belonging to this account from backend!
      this.bookingService.getMyOrders().subscribe({
        next: (backendOrders) => {
          this.isLoading = false;
          this.orders = backendOrders;

          // Clean up stale or non-owned orders from localStorage
          const localStored = this.bookingService.orders();
          const backendCodes = new Set(backendOrders.map((b) => b.orderCode));

          localStored.forEach((stored) => {
            if (!backendCodes.has(stored.orderCode)) {
              // Verify if this local order exists and belongs to this user
              this.bookingService.getOrder(stored.orderCode).subscribe({
                next: (verified) => {
                  if (verified) {
                    this.orders = [
                      verified,
                      ...this.orders.filter((o) => o.orderCode !== verified.orderCode),
                    ];
                    this.cdr.markForCheck();
                  }
                },
                error: (err: { status?: number; error?: { error?: { code?: string } } }) => {
                  // If 404 RESOURCE_NOT_FOUND (order belongs to another account or deleted), purge it!
                  if (err?.status === 404 || err?.error?.error?.code === 'RESOURCE_NOT_FOUND') {
                    this.bookingService.removeOrder(stored.orderCode);
                  }
                },
              });
            }
          });

          this.cdr.markForCheck();
        },
        error: () => {
          this.isLoading = false;
          this.orders = this.bookingService.orders();
          this.cdr.markForCheck();
        },
      });
    } else {
      // Unauthenticated mode: only show valid local orders
      this.isLoading = false;
      const storedOrders = this.bookingService.orders().filter((o) => Boolean(o.orderCode));
      this.orders = storedOrders;

      // Verify each stored order; remove any stale 404s
      storedOrders.forEach((o) => {
        this.bookingService.getOrder(o.orderCode).subscribe({
          error: (err: { status?: number; error?: { error?: { code?: string } } }) => {
            if (err?.status === 404 || err?.error?.error?.code === 'RESOURCE_NOT_FOUND') {
              this.bookingService.removeOrder(o.orderCode);
              this.orders = this.orders.filter((item) => item.orderCode !== o.orderCode);
              this.cdr.markForCheck();
            }
          },
        });
      });
      this.cdr.markForCheck();
    }
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
          this.orders = [fetched, ...this.orders.filter((o) => o.orderCode !== fetched.orderCode)];
          this.cdr.markForCheck();
        }
      });
  }

  openQrModal(order: Order): void {
    this.modalOrder = order;
  }

  getSeatCodes(seats: Seat[]): string {
    return (seats || []).map((s) => s.code).join(', ');
  }

  getCombosText(combos: FnbCombo[]): string {
    return (combos || []).map((c) => `${c.name} (x${c.quantity})`).join(', ');
  }

  formatMoney(amount: number): string {
    return formatVND(amount);
  }
}
