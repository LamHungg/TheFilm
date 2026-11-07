export type SeatType = 'STANDARD' | 'VIP' | 'COUPLE';
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'SOLD';
export type SeatSelectionStatus = 'AVAILABLE' | 'SELECTED' | 'BOOKED';

export interface Seat {
  /** Database identifier for a seat in a specific screening. Null for mock seats. */
  showSeatId?: number | null;
  seatStatus?: SeatStatus;
  holdExpiresAt?: string | null;
  /** UI identity retained for existing booking screens and mock data. */
  id: string;
  row: string; // 'A', 'B', 'C', ...
  col: number; // 1, 2, 3, ...
  code: string; // 'A01', 'F08'
  type: SeatType;
  /** UI status alias: HELD -> SELECTED and SOLD -> BOOKED. */
  status: SeatSelectionStatus;
  price: number;
}
