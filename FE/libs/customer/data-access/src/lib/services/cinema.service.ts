import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { Movie } from '../models/movie.model';
import { Cinema } from '../models/cinema.model';
import { Showtime } from '../models/showtime.model';
import { FnbCombo } from '../models/combo.model';
import { MOCK_MOVIES, MOCK_CINEMAS, MOCK_SHOWTIMES, MOCK_FNBS } from '../data/mock-data';
import { Seat, SeatType } from '../models/seat.model';

interface ApiResponse<T> { success: boolean; data: T; }
interface SeatLayoutResponse { screeningId: number; seats: Array<{
  showSeatId: number; seatCode: string; rowLabel: string; seatLabel: string;
  seatTypeCode: string; status: 'AVAILABLE' | 'HELD' | 'SOLD'; basePrice: number; holdExpiresAt: string | null;
}>; }

@Injectable({
  providedIn: 'root',
})
export class CinemaService {
  private readonly bookingApi = '/api/v1/public/bookings';
  private movies: Movie[] = [...MOCK_MOVIES];
  private cinemas: Cinema[] = [...MOCK_CINEMAS];
  private showtimes: Showtime[] = [...MOCK_SHOWTIMES];
  private fnbs: FnbCombo[] = [...MOCK_FNBS];

  constructor(private readonly http: HttpClient) {}

  getScreeningSeats(screeningId: number | string, fallback: Seat[]): Observable<Seat[]> {
    return this.http.get<ApiResponse<SeatLayoutResponse>>(
      `${this.bookingApi}/screenings/${encodeURIComponent(screeningId)}/seats`
    ).pipe(
      map((response) => response.data.seats.map((seat) => this.toSeat(seat))),
      catchError(() => of(fallback.map((seat) => ({ ...seat }))))
    );
  }

  private toSeat(seat: SeatLayoutResponse['seats'][number]): Seat {
    const seatStatus = seat.status;
    const status = seatStatus === 'HELD' ? 'SELECTED' : seatStatus === 'SOLD' ? 'BOOKED' : 'AVAILABLE';
    const seatType: SeatType = ['VIP', 'COUPLE'].includes(seat.seatTypeCode) ? seat.seatTypeCode as SeatType : 'STANDARD';
    const col = Number(seat.seatLabel.match(/\d+/)?.[0] ?? 0);
    return {
      showSeatId: seat.showSeatId, seatStatus, holdExpiresAt: seat.holdExpiresAt,
      id: String(seat.showSeatId), row: seat.rowLabel, col, code: seat.seatCode,
      type: seatType, status, price: Number(seat.basePrice),
    };
  }

  getMovies(status?: 'NOW_SHOWING' | 'COMING_SOON'): Observable<Movie[]> {
    if (!status) return of(this.movies);
    return of(this.movies.filter((m) => m.status === status));
  }

  getMovieById(id: string): Observable<Movie | undefined> {
    return of(this.movies.find((m) => m.id === id));
  }

  getMovieBySlug(slug: string): Observable<Movie | undefined> {
    return of(this.movies.find((m) => m.slug === slug || m.id === slug));
  }

  getCinemas(): Observable<Cinema[]> {
    return of(this.cinemas);
  }

  getCinemaById(id: string): Observable<Cinema | undefined> {
    return of(this.cinemas.find((c) => c.id === id));
  }

  getShowtimes(movieId?: string, cinemaId?: string, date?: string): Observable<Showtime[]> {
    let result = [...this.showtimes];
    if (movieId) {
      result = result.filter((s) => s.movieId === movieId);
    }
    if (cinemaId) {
      result = result.filter((s) => s.cinemaId === cinemaId);
    }
    if (date) {
      result = result.filter((s) => s.date === date);
    }
    return of(result);
  }

  getShowtimeById(id: string): Observable<Showtime | undefined> {
    return of(this.showtimes.find((s) => s.id === id));
  }

  getFnbs(): Observable<FnbCombo[]> {
    // Return clone so state can be modified independently
    return of(this.fnbs.map((f) => ({ ...f })));
  }
}
