import { Movie } from './movie.model';
import { Cinema } from './cinema.model';
import { Showtime } from './showtime.model';
import { Seat } from './seat.model';
import { FnbCombo } from './combo.model';

export interface BuyerInfo {
  fullName: string;
  email: string;
  phone: string;
  needInvoice: boolean;
  companyName?: string;
  taxCode?: string;
  companyAddress?: string;
}

export interface BookingState {
  movie: Movie | null;
  cinema: Cinema | null;
  showtime: Showtime | null;
  selectedSeats: Seat[];
  selectedCombos: FnbCombo[];
  buyerInfo: BuyerInfo;
  voucherCode: string;
  discountAmount: number;
  usePoints: boolean;
  pointsDiscount: number;
  holdExpiresAt: string | null; // Backend OffsetDateTime
}

/** Payload accepted by POST /api/v1/public/bookings/orders. */
export interface CreateOrderPayload {
  screeningId: number;
  showSeatIds: number[];
  idempotencyKey: string;
  voucherCode?: string;
  guestName?: string;
  guestEmail?: string;
  guestPhone?: string;
  fnbItems?: Array<{
    productId: number;
    productName: string;
    productCode?: string;
    quantity: number;
    unitPrice: number;
  }>;
}

export interface HoldSeatsPayload {
  screeningId: number;
  showSeatIds: number[];
  /** Existing backend contract requires a sales order before seats can be held. */
  salesOrderId: number;
}

export interface HoldSeatsResponse {
  salesOrderId: number;
  expiresAt: string;
  showSeatIds: number[];
}
