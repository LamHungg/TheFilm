export interface CinemaRoom {
  id: string;
  name: string;
  type: 'STANDARD' | 'IMAX' | 'GOLD_CLASS' | '4DX';
  totalSeats: number;
}

export interface Cinema {
  id: string;
  name: string;
  brand: string;
  city: string;
  address: string;
  hotline: string;
  rooms: CinemaRoom[];
}
