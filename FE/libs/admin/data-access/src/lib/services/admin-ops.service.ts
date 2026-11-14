import { Injectable, signal } from '@angular/core';
import {
  MOCK_ADMIN_ORDERS,
  MOCK_ADMIN_PROMOTIONS,
  MOCK_ADMIN_SUPPORT,
  MOCK_CINEMA_OCCUPANCY,
  MOCK_DASHBOARD_STATS,
} from '../data/admin-mock';
import {
  AdminOrder,
  AdminPromotion,
  AdminSupportCase,
  CinemaOccupancy,
  DashboardStats,
  OrderStatus,
  PromoStatus,
  SupportStatus,
} from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminOpsService {
  private readonly ordersState = signal<AdminOrder[]>([...MOCK_ADMIN_ORDERS]);
  private readonly promotionsState = signal<AdminPromotion[]>([
    ...MOCK_ADMIN_PROMOTIONS,
  ]);
  private readonly supportState = signal<AdminSupportCase[]>([
    ...MOCK_ADMIN_SUPPORT,
  ]);
  private nextPromoId =
    Math.max(...MOCK_ADMIN_PROMOTIONS.map((p) => p.id)) + 1;

  readonly orders = this.ordersState.asReadonly();
  readonly promotions = this.promotionsState.asReadonly();
  readonly supportCases = this.supportState.asReadonly();
  readonly dashboardStats = signal<DashboardStats>({ ...MOCK_DASHBOARD_STATS });
  readonly cinemaOccupancy = signal<CinemaOccupancy[]>([
    ...MOCK_CINEMA_OCCUPANCY,
  ]);

  setOrderStatus(id: number, status: OrderStatus): void {
    this.ordersState.update((list) =>
      list.map((o) => (o.id === id ? { ...o, status } : o))
    );
  }

  upsertPromotion(
    payload: Omit<AdminPromotion, 'id' | 'usageCount'> & {
      id?: number;
      usageCount?: number;
    }
  ): AdminPromotion {
    if (payload.id) {
      const current = this.promotionsState().find((p) => p.id === payload.id)!;
      const updated: AdminPromotion = {
        ...current,
        ...payload,
        id: payload.id,
        usageCount: payload.usageCount ?? current.usageCount,
      };
      this.promotionsState.update((list) =>
        list.map((p) => (p.id === updated.id ? updated : p))
      );
      return updated;
    }
    const created: AdminPromotion = {
      ...payload,
      id: this.nextPromoId++,
      usageCount: 0,
    };
    this.promotionsState.update((list) => [created, ...list]);
    return created;
  }

  setPromotionStatus(id: number, status: PromoStatus): void {
    this.promotionsState.update((list) =>
      list.map((p) => (p.id === id ? { ...p, status } : p))
    );
  }

  setSupportStatus(id: number, status: SupportStatus): void {
    this.supportState.update((list) =>
      list.map((c) =>
        c.id === id
          ? { ...c, status, updatedAt: new Date().toISOString() }
          : c
      )
    );
  }
}
