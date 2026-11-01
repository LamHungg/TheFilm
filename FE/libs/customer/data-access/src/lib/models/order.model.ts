import { Seat } from './seat.model';
import { FnbCombo } from './combo.model';
import { BuyerInfo } from './booking.model';

export type PaymentMethod = 'VNPAY_QR' | 'DOMESTIC_CARD' | 'INTERNATIONAL_CARD' | 'E_WALLET';
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'USED';

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
}
