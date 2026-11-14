import { Injectable, signal } from '@angular/core';
import {
  MOCK_ADMIN_CINEMAS,
  MOCK_ADMIN_FILMS,
  MOCK_ADMIN_ROOMS,
  MOCK_ADMIN_SCREENINGS,
} from '../data/admin-mock';
import {
  AdminCinema,
  AdminFilm,
  AdminRoom,
  AdminScreening,
  FilmAdminStatus,
  ScreeningStatus,
} from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminCatalogService {
  private readonly filmsState = signal<AdminFilm[]>([...MOCK_ADMIN_FILMS]);
  private readonly cinemasState = signal<AdminCinema[]>([...MOCK_ADMIN_CINEMAS]);
  private readonly roomsState = signal<AdminRoom[]>([...MOCK_ADMIN_ROOMS]);
  private readonly screeningsState = signal<AdminScreening[]>([
    ...MOCK_ADMIN_SCREENINGS,
  ]);
  private nextFilmId = Math.max(...MOCK_ADMIN_FILMS.map((f) => f.id)) + 1;
  private nextScreeningId =
    Math.max(...MOCK_ADMIN_SCREENINGS.map((s) => s.id)) + 1;

  readonly films = this.filmsState.asReadonly();
  readonly cinemas = this.cinemasState.asReadonly();
  readonly rooms = this.roomsState.asReadonly();
  readonly screenings = this.screeningsState.asReadonly();

  upsertFilm(
    payload: Omit<AdminFilm, 'id' | 'updatedAt'> & { id?: number }
  ): AdminFilm {
    const now = new Date().toISOString();
    if (payload.id) {
      const updated: AdminFilm = {
        ...(this.filmsState().find((f) => f.id === payload.id) as AdminFilm),
        ...payload,
        id: payload.id,
        updatedAt: now,
      };
      this.filmsState.update((list) =>
        list.map((f) => (f.id === updated.id ? updated : f))
      );
      return updated;
    }

    const created: AdminFilm = {
      ...payload,
      id: this.nextFilmId++,
      updatedAt: now,
    };
    this.filmsState.update((list) => [created, ...list]);
    return created;
  }

  setFilmStatus(id: number, status: FilmAdminStatus): void {
    this.filmsState.update((list) =>
      list.map((f) =>
        f.id === id
          ? { ...f, status, updatedAt: new Date().toISOString() }
          : f
      )
    );
  }

  deleteFilm(id: number): void {
    this.filmsState.update((list) => list.filter((f) => f.id !== id));
  }

  roomsByCinema(cinemaId: number): AdminRoom[] {
    return this.roomsState().filter((r) => r.cinemaId === cinemaId);
  }

  upsertScreening(
    payload: Omit<AdminScreening, 'id'> & { id?: number }
  ): AdminScreening {
    if (payload.id) {
      const updated = { ...payload, id: payload.id } as AdminScreening;
      this.screeningsState.update((list) =>
        list.map((s) => (s.id === updated.id ? updated : s))
      );
      return updated;
    }
    const created: AdminScreening = {
      ...payload,
      id: this.nextScreeningId++,
    };
    this.screeningsState.update((list) => [created, ...list]);
    return created;
  }

  setScreeningStatus(id: number, status: ScreeningStatus): void {
    this.screeningsState.update((list) =>
      list.map((s) => (s.id === id ? { ...s, status } : s))
    );
  }

  deleteScreening(id: number): void {
    this.screeningsState.update((list) => list.filter((s) => s.id !== id));
  }

  upsertCinema(payload: Omit<AdminCinema, 'id' | 'roomCount'> & { id?: number | null }): void {
    if (payload.id) {
      this.cinemasState.update((list) => list.map((c) => (c.id === payload.id ? { ...c, ...payload, id: c.id } : c)));
      return;
    }
    const id = Math.max(0, ...this.cinemasState().map((c) => c.id)) + 1;
    this.cinemasState.update((list) => [...list, { ...payload, id, roomCount: 0 }]);
  }

  upsertRoom(payload: Omit<AdminRoom, 'id'> & { id?: number | null }): void {
    if (payload.id) {
      this.roomsState.update((list) => list.map((r) => (r.id === payload.id ? { ...r, ...payload, id: r.id } : r)));
      return;
    }
    const id = Math.max(0, ...this.roomsState().map((r) => r.id)) + 1;
    this.roomsState.update((list) => [...list, { ...payload, id }]);
    this.cinemasState.update((list) =>
      list.map((c) => (c.id === payload.cinemaId ? { ...c, roomCount: c.roomCount + 1 } : c))
    );
  }
}
