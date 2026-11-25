import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { AgeRating, Movie } from '../models/movie.model';
import { Cinema } from '../models/cinema.model';
import { Showtime } from '../models/showtime.model';
import { FnbCombo } from '../models/combo.model';
import { FNB_CATALOG } from '../data/fnb-catalog';
import { Seat, SeatType } from '../models/seat.model';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

interface FilmApi {
  id: number;
  code: string;
  title: string;
  originalTitle: string | null;
  synopsis: string | null;
  runtimeMinutes: number | null;
  posterUri: string | null;
  bannerUri: string | null;
  trailerUri: string | null;
  releaseDate: string | null;
  director: string | null;
  cast: string[] | null;
  externalRatingScore: number | null;
  externalRatingCount: number | null;
  externalRatingSource: string | null;
  externalRatingUri: string | null;
  externalRatingUpdatedAt: string | null;
  ageClassification: {
    code: string;
    name: string;
    minimumAge: number;
    description: string;
  } | null;
  status: string;
  genres: Array<{ code: string; name: string }>;
}

interface CinemaApi {
  id: number;
  code: string;
  name: string;
  address: string;
  province: string;
  district: string;
  ward: string;
  phone: string | null;
  email: string | null;
  timeZone: string | null;
}

interface ScreeningApi {
  id: number;
  filmId: number;
  filmTitle: string;
  posterUri: string | null;
  cinemaId: number;
  cinemaName: string;
  roomId: number;
  roomName: string;
  roomTypeCode: string;
  roomTypeName: string;
  format: string;
  screeningType: string;
  startsAt: string;
  endsAt: string;
  availableSeats: number;
  totalSeats: number;
}

interface SeatLayoutResponse {
  screeningId: number;
  seats: Array<{
    showSeatId: number;
    seatCode: string;
    rowLabel: string;
    seatLabel: string;
    seatTypeCode: string;
    status: 'AVAILABLE' | 'HELD' | 'SOLD';
    basePrice: number;
    holdExpiresAt: string | null;
  }>;
}

const VALID_RATINGS: AgeRating[] = ['P', 'K', 'T13', 'T16', 'T18', 'C'];
const THEMES: Movie['figmaTheme'][] = ['navy', 'coral', 'purple', 'teal'];

@Injectable({
  providedIn: 'root',
})
export class CinemaService {
  private readonly publicApi = '/api/v1/public';
  private readonly bookingApi = '/api/v1/public/bookings';

  constructor(private readonly http: HttpClient) {}

  getScreeningSeats(screeningId: number | string): Observable<Seat[]> {
    return this.http
      .get<ApiResponse<SeatLayoutResponse>>(
        `${this.bookingApi}/screenings/${encodeURIComponent(screeningId)}/seats`
      )
      .pipe(map((response) => response.data.seats.map((seat) => this.toSeat(seat))));
  }

  getMovies(status?: 'NOW_SHOWING' | 'COMING_SOON'): Observable<Movie[]> {
    const params = new HttpParams()
      .set('status', status ?? 'ALL')
      .set('page', '0')
      .set('size', '100');
    return this.http
      .get<ApiResponse<PageResponse<FilmApi>>>(`${this.publicApi}/films`, { params })
      .pipe(map((res) => (res.data?.items ?? []).map((film) => this.toMovie(film))));
  }

  getMovieById(id: string): Observable<Movie | undefined> {
    const numericId = Number(id);
    if (!Number.isFinite(numericId) || numericId <= 0) {
      return of(undefined);
    }
    return this.http
      .get<ApiResponse<FilmApi>>(`${this.publicApi}/films/${numericId}`)
      .pipe(
        map((res) => (res.data ? this.toMovie(res.data) : undefined)),
        catchError(() => of(undefined))
      );
  }

  getMovieBySlug(slug: string): Observable<Movie | undefined> {
    const idPrefix = slug.match(/^(\d+)(?:-|$)/)?.[1];
    if (idPrefix) {
      return this.getMovieById(idPrefix);
    }
    return this.getMovies().pipe(
      map((movies) =>
        movies.find((m) => m.slug === slug || m.id === slug)
      )
    );
  }

  getCinemas(province?: string): Observable<Cinema[]> {
    let params = new HttpParams();
    if (province?.trim()) {
      params = params.set('province', province.trim());
    }
    return this.http
      .get<ApiResponse<CinemaApi[]>>(`${this.publicApi}/cinemas`, { params })
      .pipe(map((res) => (res.data ?? []).map((c) => this.toCinema(c))));
  }

  getCinemaById(id: string): Observable<Cinema | undefined> {
    return this.getCinemas().pipe(
      map((cinemas) => cinemas.find((c) => c.id === id))
    );
  }

  getShowtimes(movieId?: string, cinemaId?: string, date?: string): Observable<Showtime[]> {
    let params = new HttpParams();
    if (movieId && Number.isFinite(Number(movieId))) {
      params = params.set('filmId', String(Number(movieId)));
    }
    if (cinemaId && Number.isFinite(Number(cinemaId))) {
      params = params.set('cinemaId', String(Number(cinemaId)));
    }
    if (date) {
      params = params.set('date', date);
    }
    return this.http
      .get<ApiResponse<ScreeningApi[]>>(`${this.publicApi}/screenings`, { params })
      .pipe(map((res) => (res.data ?? []).map((s) => this.toShowtime(s))));
  }

  getShowtimeById(id: string): Observable<Showtime | undefined> {
    const numericId = Number(id);
    if (!Number.isFinite(numericId) || numericId <= 0) {
      return of(undefined);
    }
    return this.getShowtimes(undefined, undefined, this.formatLocalDate(new Date())).pipe(
      switchMap((todayList) => {
        const matchToday = todayList.find((item) => item.backendScreeningId === numericId);
        if (matchToday) return of(matchToday);
        const upcomingDates = this.getUpcomingDates(6, 1).map((d) =>
          this.getShowtimes(undefined, undefined, d).pipe(catchError(() => of([] as Showtime[])))
        );
        return forkJoin(upcomingDates).pipe(
          map((lists) => lists.flat().find((item) => item.backendScreeningId === numericId))
        );
      }),
      catchError(() => of(undefined))
    );
  }

  getFnbs(): Observable<FnbCombo[]> {
    return of(FNB_CATALOG.map((f) => ({ ...f })));
  }

  formatLocalDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  getUpcomingDates(count: number, startOffsetDays = 0): string[] {
    const dates: string[] = [];
    const base = new Date();
    for (let i = 0; i < count; i++) {
      const next = new Date(base.getFullYear(), base.getMonth(), base.getDate() + startOffsetDays + i);
      dates.push(this.formatLocalDate(next));
    }
    return dates;
  }

  private toSeat(seat: SeatLayoutResponse['seats'][number]): Seat {
    const seatStatus = seat.status;
    const status = seatStatus === 'HELD' ? 'SELECTED' : seatStatus === 'SOLD' ? 'BOOKED' : 'AVAILABLE';
    const seatType: SeatType = ['VIP', 'COUPLE'].includes(seat.seatTypeCode)
      ? (seat.seatTypeCode as SeatType)
      : 'STANDARD';
    const col = Number(seat.seatLabel.match(/\d+/)?.[0] ?? 0);
    return {
      showSeatId: seat.showSeatId,
      seatStatus,
      holdExpiresAt: seat.holdExpiresAt,
      id: String(seat.showSeatId),
      row: seat.rowLabel,
      col,
      code: seat.seatCode,
      type: seatType,
      status,
      price: Number(seat.basePrice),
    };
  }

  private toMovie(film: FilmApi): Movie {
    const rawRating = (film.ageClassification?.code || 'P').toUpperCase() as AgeRating;
    const rating: AgeRating = VALID_RATINGS.includes(rawRating) ? rawRating : 'P';
    const status: Movie['status'] = film.status === 'NOW_SHOWING' ? 'NOW_SHOWING' : 'COMING_SOON';
    const posterUrl = film.posterUri || '';
    return {
      id: String(film.id),
      title: film.title,
      originalTitle: film.originalTitle || undefined,
      slug: `${film.id}-${this.slugify(film.title)}`,
      posterUrl,
      bannerUrl: film.bannerUri || posterUrl,
      rating,
      ratingDesc: film.ageClassification?.name || film.ageClassification?.description || '',
      duration: film.runtimeMinutes ?? 120,
      releaseDate: film.releaseDate || '',
      status,
      genres: (film.genres ?? []).map((g) => g.name),
      formats: ['2D'],
      languages: ['Phụ đề Việt'],
      director: film.director || '',
      cast: film.cast || [],
      synopsis: film.synopsis || '',
      trailerUrl: film.trailerUri || '',
      score: Number(film.externalRatingScore ?? 0),
      ratingCount: film.externalRatingCount ?? 0,
      ratingSource: film.externalRatingSource || '',
      ratingSourceUrl: film.externalRatingUri || '',
      ratingUpdatedAt: film.externalRatingUpdatedAt || '',
      figmaTheme: THEMES[(film.id - 1) % THEMES.length] || 'navy',
      category: film.genres?.[0]?.name,
    };
  }

  private toCinema(cinema: CinemaApi): Cinema {
    const fullAddress = [cinema.address, cinema.ward, cinema.district, cinema.province]
      .filter((part) => Boolean(part && part.trim()))
      .join(', ');
    return {
      id: String(cinema.id),
      name: cinema.name,
      brand: 'THEFILM',
      city: cinema.province || '',
      address: fullAddress,
      hotline: cinema.phone || '',
      rooms: [],
    };
  }

  private toShowtime(screening: ScreeningApi): Showtime {
    const start = new Date(screening.startsAt);
    const end = new Date(screening.endsAt);
    const formatTime = (d: Date) =>
      `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    const rawRoomType = (screening.roomTypeCode || 'STANDARD').toUpperCase();
    const roomType: Showtime['roomType'] =
      rawRoomType === 'VIP' || rawRoomType === 'IMAX' || rawRoomType === 'GOLD_CLASS' || rawRoomType === '4DX'
        ? rawRoomType
        : 'STANDARD';
    const rawFormat = (screening.format || '2D').toUpperCase();
    const format: Showtime['format'] =
      rawFormat === '3D' || rawFormat === 'IMAX' ? rawFormat : '2D';

    return {
      id: String(screening.id),
      backendScreeningId: screening.id,
      movieId: String(screening.filmId),
      cinemaId: String(screening.cinemaId),
      roomName: screening.roomName,
      roomType,
      format,
      language: 'Phụ đề Việt',
      date: this.formatLocalDate(start),
      startTime: formatTime(start),
      endTime: formatTime(end),
      priceStandard: 0,
      priceVip: 0,
      priceCouple: 0,
      availableSeats: screening.availableSeats,
      totalSeats: screening.totalSeats,
    };
  }

  private slugify(text: string): string {
    return text
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'd')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }
}
