export type FilmAdminStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'NOW_SHOWING'
  | 'COMING_SOON'
  | 'ARCHIVED';

export type AgeRating = 'P' | 'K' | 'T13' | 'T16' | 'T18' | 'C';

export type ScreeningStatus = 'SCHEDULED' | 'ON_SALE' | 'SOLD_OUT' | 'CANCELLED' | 'COMPLETED';

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'CANCELLED'
  | 'REFUNDED'
  | 'EXPIRED';

export type PromoStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ENDED';

export type SupportStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING' | 'RESOLVED' | 'CLOSED';

export interface AdminFilm {
  id: number;
  title: string;
  originalTitle?: string;
  posterUrl: string;
  durationMin: number;
  ageRating: AgeRating;
  genres: string[];
  status: FilmAdminStatus;
  releaseDate: string;
  director: string;
  synopsis: string;
  trailerUrl?: string;
  updatedAt: string;
}

export interface AdminCinema {
  id: number;
  code: string;
  name: string;
  province: string;
  address: string;
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
  roomCount: number;
  phone?: string;
}

export interface AdminRoom {
  id: number;
  cinemaId: number;
  name: string;
  roomType: string;
  seatCount: number;
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
}

export interface AdminScreening {
  id: number;
  filmId: number;
  filmTitle: string;
  cinemaId: number;
  cinemaName: string;
  roomId: number;
  roomName: string;
  startAt: string;
  endAt: string;
  status: ScreeningStatus;
  basePrice: number;
  soldSeats: number;
  totalSeats: number;
}

export interface AdminOrder {
  id: number;
  orderCode: string;
  customerName: string;
  customerPhone: string;
  cinemaName: string;
  filmTitle: string;
  showtimeAt: string;
  seatLabels: string[];
  totalAmount: number;
  status: OrderStatus;
  createdAt: string;
}

export interface AdminPromotion {
  id: number;
  code: string;
  name: string;
  discountType: 'PERCENT' | 'FIXED';
  discountValue: number;
  startDate: string;
  endDate: string;
  status: PromoStatus;
  usageCount: number;
  usageLimit?: number;
}

export interface AdminSupportCase {
  id: number;
  ticketCode: string;
  subject: string;
  customerName: string;
  channel: 'WEB' | 'HOTLINE' | 'EMAIL' | 'CHAT';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  status: SupportStatus;
  updatedAt: string;
}

export interface DashboardStats {
  revenueToday: number;
  ticketsSoldToday: number;
  occupancyRate: number;
  activeFilms: number;
  openOrders: number;
  openSupportCases: number;
  revenueTrend: number;
  ticketTrend: number;
}

export interface CinemaOccupancy {
  cinemaName: string;
  occupancy: number;
  screenings: number;
}

export interface AdminUser {
  name: string;
  email: string;
  role: string;
  cinemaScope: string;
}
