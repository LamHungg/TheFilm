import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Movie } from '../models/movie.model';
import { Cinema } from '../models/cinema.model';
import { Showtime } from '../models/showtime.model';
import { FnbCombo } from '../models/combo.model';
import { MOCK_MOVIES, MOCK_CINEMAS, MOCK_SHOWTIMES, MOCK_FNBS } from '../data/mock-data';

@Injectable({
  providedIn: 'root',
})
export class CinemaService {
  private movies: Movie[] = [...MOCK_MOVIES];
  private cinemas: Cinema[] = [...MOCK_CINEMAS];
  private showtimes: Showtime[] = [...MOCK_SHOWTIMES];
  private fnbs: FnbCombo[] = [...MOCK_FNBS];

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
