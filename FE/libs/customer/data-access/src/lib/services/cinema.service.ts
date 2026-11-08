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
interface ScreeningApi { id: number; startsAt: string; endsAt: string; availableSeats: number; totalSeats: number; }
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

  getScreeningSeats(screeningId: number | string): Observable<Seat[]> {
    return this.http.get<ApiResponse<SeatLayoutResponse>>(
      `${this.bookingApi}/screenings/${encodeURIComponent(screeningId)}/seats`
    ).pipe(
      map((response) => response.data.seats.map((seat) => this.toSeat(seat)))
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
    const showtime = this.showtimes.find((s) => s.id === id);
    if (!showtime?.backendScreeningId) return of(showtime);
    return this.http.get<ApiResponse<ScreeningApi[]>>('/api/v1/public/screenings').pipe(
      map(({ data }) => {
        const screening = data.find((item) => item.id === showtime.backendScreeningId);
        if (!screening) return showtime;
        const start = new Date(screening.startsAt), end = new Date(screening.endsAt);
        const date = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}-${String(start.getDate()).padStart(2, '0')}`;
        const time = (value: Date) => `${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
        return { ...showtime, date, startTime: time(start), endTime: time(end), availableSeats: screening.availableSeats, totalSeats: screening.totalSeats };
      }),
      catchError(() => of(showtime))
    );
  }

  getFnbs(): Observable<FnbCombo[]> {
    // Return clone so state can be modified independently
    return of(this.fnbs.map((f) => ({ ...f })));
  }
}
