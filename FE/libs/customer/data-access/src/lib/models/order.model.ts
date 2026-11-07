import { Seat } from './seat.model';
import { FnbCombo } from './combo.model';
import { BuyerInfo } from './booking.model';

export type PaymentMethod = 'VNPAY_QR' | 'DOMESTIC_CARD' | 'INTERNATIONAL_CARD' | 'E_WALLET';
export type OrderStatus = 'PENDING' | 'PENDING_PAYMENT' | 'CONFIRMED' | 'ISSUED' | 'CANCELLED' | 'USED';

export interface Order {
  id: string;
  orderCode: string;
  bookingCode: string;
  qrCodeUrl: string;
  movieTitle: string;
  movieRating: string;
  cinemaName: string;
  cinemaAddress: string;
  roomName: string;
  format: string;
  showDate: string;
  showTime: string;
  seats: Seat[];
  combos: FnbCombo[];
  ticketTotal: number;
  fnbTotal: number;
  discountTotal: number;
  finalTotal: number;
  vatRate: number; // e.g. 0.08 or 0.1
  vatAmount: number;
  paymentMethod: PaymentMethod;
  status: OrderStatus;
  createdAt: string;
  buyer: BuyerInfo;
  tickets?: Ticket[];
  paymentExpiresAt?: string | null;
}

/** Ticket fields returned by the booking API. */
export interface Ticket {
  ticketNumber: string;
  qrTokenHash?: string;
  qrData?: string;
  filmTitle?: string;
  cinemaName?: string;
  roomName?: string;
  screeningStartsAt?: string;
  seatLabel?: string;
  status?: string;
}

export interface CreateOrderResponse {
  orderCode: string;
  status: string;
  subtotal: number;
  discount: number;
  vat: number;
  total: number;
  paymentExpiresAt: string | null;
  lines: Array<{
    item: string;
    screeningType: string;
    roomType: string;
    seatType: string;
    unitPrice: number;
    vat: number;
    total: number;
  }>;
  tickets: Ticket[];
}

export interface PayOrderPayload {
  provider: string;
  idempotencyKey: string;
  paymentMethod: PaymentMethod;
  successful: boolean;
}

export interface PayOrderResponse {
  orderCode: string;
  paymentStatus: string;
  orderStatus: string;
  providerTransactionReference: string | null;
  completedAt: string | null;
}
