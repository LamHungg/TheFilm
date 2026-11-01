export interface Showtime {
  id: string;
  movieId: string;
  cinemaId: string;
  roomName: string;
  roomType: 'STANDARD' | 'IMAX' | 'GOLD_CLASS' | '4DX';
  format: '2D' | '3D' | 'IMAX';
  language: string; // 'Phụ đề VI' | 'Lồng tiếng'
  date: string; // '2026-11-01'
  startTime: string; // '09:30'
  endTime: string; // '11:30'
  priceStandard: number;
  priceVip: number;
  priceCouple: number;
  availableSeats: number;
  totalSeats: number;
}
