export type SeatType = 'STANDARD' | 'VIP' | 'COUPLE';
export type SeatStatus = 'AVAILABLE' | 'SELECTED' | 'BOOKED';

export interface Seat {
  id: string;
  row: string; // 'A', 'B', 'C', ...
  col: number; // 1, 2, 3, ...
  code: string; // 'A01', 'F08'
  type: SeatType;
  status: SeatStatus;
  price: number;
}
