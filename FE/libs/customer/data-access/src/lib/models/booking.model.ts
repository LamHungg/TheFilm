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
  holdExpiresAt: number | null; // timestamp
}
