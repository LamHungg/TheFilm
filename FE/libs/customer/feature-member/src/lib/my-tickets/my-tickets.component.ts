import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BadgeComponent } from '@the-film/shared/ui';
import { formatVND } from '@the-film/shared/util';
import { Order, Seat, FnbCombo, BookingService } from '@the-film/customer/data-access';

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
  searchCode = '';
  modalOrder: Order | null = null;

  constructor(private bookingService: BookingService) {}

  ngOnInit(): void {
    this.orders = this.bookingService.orders();
  }

  get activeOrders(): Order[] {
    return this.orders.filter((o) => o.status === 'CONFIRMED');
  }

  get historyOrders(): Order[] {
    return this.orders.filter((o) => o.status !== 'CONFIRMED');
  }

  get displayedOrders(): Order[] {
    let list = this.activeTab === 'ACTIVE' ? this.activeOrders : this.historyOrders;
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

  searchTicket(): void {
    // Triggers getter recalculation
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
