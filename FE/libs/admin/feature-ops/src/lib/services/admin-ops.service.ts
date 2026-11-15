import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
  };
  timestamp?: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

// Film DTOs
export interface FilmAdminDto {
  filmId: number;
  filmCode: string;
  titleLocal: string;
  titleOriginal: string | null;
  synopsis: string | null;
  runtimeMinutes: number;
  ageClassificationId: number | null;
  posterUri: string | null;
  trailerUri: string | null;
  approvalStatus: string;
  genreIds: number[];
}

export interface FilmLookups {
  genres: Array<{ id: number; code: string; name: string }>;
  ageClassifications: Array<{ id: number; code: string; name: string }>;
}

export interface FilmVersionDto {
  filmVersionId: number;
  filmId: number;
  versionCode: string;
  languageCode?: string;
  subtitleCode?: string;
  formatCode: string;
  runtimeMinutes: number;
  status: string;
}

export interface FilmCreateRequest {
  filmCode: string;
  titleLocal: string;
  titleOriginal?: string;
  synopsis?: string;
  runtimeMinutes: number;
  ageClassificationId?: number;
  posterUri?: string;
  trailerUri?: string;
  genreIds: number[];
}

// Cinema & Org DTOs
export interface CinemaAdminDto {
  cinemaId: number;
  chainId: number;
  legalEntityId: number;
  cinemaCode: string;
  cinemaName: string;
  addressLine: string;
  provinceName?: string;
  districtName?: string;
  wardName?: string;
  phone?: string;
  email?: string;
  timeZone: string;
  status: string;
}

export interface ChainDto {
  chainId: number;
  chainCode: string;
  chainName: string;
  status: string;
}

export interface LegalEntityDto {
  legalEntityId: number;
  chainId: number;
  entityCode: string;
  legalName: string;
  taxIdentifier?: string;
  status: string;
}

export interface LegalEntityRequest {
  chainId: number;
  entityCode: string;
  legalName: string;
  taxIdentifier?: string;
}

export interface CinemaCreateRequest {
  chainId: number;
  legalEntityId: number;
  cinemaCode: string;
  cinemaName: string;
  addressLine: string;
  provinceName?: string;
  districtName?: string;
  wardName?: string;
  phone?: string;
  email?: string;
  timeZone?: string;
}

// Screening DTOs
export interface ScreeningAdminDto {
  screeningId: number;
  cinemaId: number;
  roomId: number;
  roomName?: string;
  roomTypeCode?: string;
  filmId: number;
  filmTitle?: string;
  filmVersionId: number;
  formatCode: string;
  screeningTypeCode: string;
  startsAt: string;
  endsAt: string;
  cleanupMinutes: number;
  status: string;
}

export interface ScreeningCreateRequest {
  cinemaId: number;
  roomId: number;
  filmVersionId: number;
  startsAt: string;
  screeningTypeCode: string;
  cleanupMinutes: number;
}

// Room & Seat DTOs
export interface RoomTypeDto {
  id: number;
  typeCode: string;
  typeName: string;
  description?: string;
  status: string;
}

export interface SeatTypeDto {
  id: number;
  typeCode: string;
  typeName: string;
  description?: string;
  status: string;
}

export interface RoomAdminDto {
  roomId: number;
  cinemaId: number;
  roomTypeId: number;
  roomTypeName?: string;
  roomCode: string;
  roomName: string;
  status: string;
  activeLayoutId?: number;
}

export interface RoomCreateRequest {
  roomTypeId: number;
  roomCode: string;
  roomName: string;
}

// Price Rule DTOs
export interface PriceRuleDto {
  priceRuleId: number;
  ruleCode: string;
  cinemaId?: number;
  roomTypeId?: number;
  seatTypeId: number;
  screeningTypeCode?: string;
  weekdayNo?: number;
  validFrom: string;
  validTo?: string;
  basePrice: number;
  priorityNo: number;
  approvalStatus: string;
}

export interface PriceRuleRequest {
  ruleCode: string;
  cinemaId?: number;
  roomTypeId?: number;
  seatTypeId: number;
  screeningTypeCode?: string;
  weekdayNo?: number;
  validFrom: string;
  validTo?: string;
  basePrice: number;
  priorityNo: number;
}

// Employee DTOs
export interface EmployeeDto {
  employeeId: number;
  chainId: number;
  appUserId?: number;
  employeeCode: string;
  fullName: string;
  phone?: string;
  email?: string;
  status: string;
  hiredOn?: string;
}

export interface EmployeeCreateRequest {
  chainId: number;
  employeeCode: string;
  fullName: string;
  dateOfBirth?: string;
  phone?: string;
  email?: string;
  hiredOn?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AdminOpsService {
  private readonly http = inject(HttpClient);
  private readonly baseApi = '/api/v1/admin';

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      Authorization: 'Bearer mock-admin-token',
      'Content-Type': 'application/json',
      'X-Request-Id': 'req-' + Date.now(),
    });
  }

  // ---- FILMS ----
  getFilms(keyword = '', page = 0, size = 50): Observable<FilmAdminDto[]> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (keyword) {
      params = params.set('keyword', keyword);
    }
    return this.http
      .get<ApiResponse<PageResponse<FilmAdminDto>>>(`${this.baseApi}/films`, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((res) => res.data?.items || []),
        catchError(() => of([]))
      );
  }

  getFilmLookups(): Observable<FilmLookups> {
    return this.http
      .get<ApiResponse<FilmLookups>>(`${this.baseApi}/films/lookups`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || { genres: [], ageClassifications: [] }),
        catchError(() => of({ genres: [], ageClassifications: [] }))
      );
  }

  getFilmVersions(filmId: number): Observable<FilmVersionDto[]> {
    return this.http
      .get<ApiResponse<FilmVersionDto[]>>(`${this.baseApi}/films/${filmId}/versions`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  createFilm(request: FilmCreateRequest): Observable<FilmAdminDto | null> {
    return this.http
      .post<ApiResponse<FilmAdminDto>>(`${this.baseApi}/films`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        catchError((err) => {
          console.error('Error creating film:', err);
          return of(null);
        })
      );
  }

  // ---- CINEMAS & ORGS ----
  getCinemas(): Observable<CinemaAdminDto[]> {
    return this.http
      .get<ApiResponse<CinemaAdminDto[]>>(`${this.baseApi}/cinemas`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  getChains(): Observable<ChainDto[]> {
    return this.http
      .get<ApiResponse<ChainDto[]>>(`${this.baseApi}/chains`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  getLegalEntities(): Observable<LegalEntityDto[]> {
    return this.http
      .get<ApiResponse<LegalEntityDto[]>>(`${this.baseApi}/legal-entities`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  createLegalEntity(request: LegalEntityRequest): Observable<LegalEntityDto | null> {
    return this.http
      .post<ApiResponse<LegalEntityDto>>(`${this.baseApi}/legal-entities`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        catchError((err) => {
          console.error('Error creating legal entity:', err);
          return of(null);
        })
      );
  }

  createCinema(request: CinemaCreateRequest): Observable<CinemaAdminDto | null> {
    return this.http
      .post<ApiResponse<CinemaAdminDto>>(`${this.baseApi}/cinemas`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        catchError((err) => {
          console.error('Error creating cinema:', err);
          return of(null);
        })
      );
  }

  // ---- ROOMS & SEATS ----
  getRooms(cinemaId = 1): Observable<RoomAdminDto[]> {
    return this.http
      .get<ApiResponse<RoomAdminDto[]>>(`${this.baseApi}/cinemas/${cinemaId}/rooms`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  getRoomTypes(): Observable<RoomTypeDto[]> {
    return this.http
      .get<ApiResponse<RoomTypeDto[]>>(`${this.baseApi}/room-types`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  getSeatTypes(): Observable<SeatTypeDto[]> {
    return this.http
      .get<ApiResponse<SeatTypeDto[]>>(`${this.baseApi}/seat-types`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  createRoom(cinemaId: number, request: RoomCreateRequest): Observable<RoomAdminDto | null> {
    return this.http
      .post<ApiResponse<RoomAdminDto>>(`${this.baseApi}/cinemas/${cinemaId}/rooms`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        catchError((err) => {
          console.error('Error creating room:', err);
          return of(null);
        })
      );
  }

  // ---- SCREENINGS ----
  getScreenings(cinemaId = 1, from?: string, to?: string): Observable<ScreeningAdminDto[]> {
    const fromTime = from || new Date().toISOString();
    const toTime =
      to || new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
    const params = new HttpParams()
      .set('cinemaId', cinemaId.toString())
      .set('from', fromTime)
      .set('to', toTime);

    return this.http
      .get<ApiResponse<ScreeningAdminDto[]>>(`${this.baseApi}/screenings`, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  createScreening(request: ScreeningCreateRequest): Observable<ScreeningAdminDto | null> {
    return this.http
      .post<ApiResponse<ScreeningAdminDto>>(`${this.baseApi}/screenings`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        catchError((err) => {
          console.error('Error creating screening:', err);
          return of(null);
        })
      );
  }

  // ---- PRICE RULES ----
  getPriceRules(cinemaId?: number): Observable<PriceRuleDto[]> {
    let params = new HttpParams();
    if (cinemaId) {
      params = params.set('cinemaId', cinemaId.toString());
    }
    return this.http
      .get<ApiResponse<PriceRuleDto[]>>(`${this.baseApi}/price-rules`, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  createPriceRule(request: PriceRuleRequest): Observable<PriceRuleDto | null> {
    return this.http
      .post<ApiResponse<PriceRuleDto>>(`${this.baseApi}/price-rules`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        catchError((err) => {
          console.error('Error creating price rule:', err);
          return of(null);
        })
      );
  }

  // ---- EMPLOYEES & WORKFORCE ----
  getEmployees(page = 0, size = 50): Observable<EmployeeDto[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http
      .get<ApiResponse<PageResponse<EmployeeDto>>>(`${this.baseApi}/employees/page`, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((res) => res.data?.items || []),
        catchError(() => of([]))
      );
  }

  createEmployee(request: EmployeeCreateRequest): Observable<EmployeeDto | null> {
    return this.http
      .post<ApiResponse<EmployeeDto>>(`${this.baseApi}/employees`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        catchError((err) => {
          console.error('Error creating employee:', err);
          return of(null);
        })
      );
  }

  // ---- TICKETS / SCANNING ----
  validateTicket(qrHash: string, cinemaId = 1): Observable<{ valid: boolean; message: string }> {
    return this.http
      .post<ApiResponse<{ result: string; message: string }>>(`${this.baseApi}/tickets/validate`, {
        cinemaId,
        scannedQrHash: qrHash,
      }, { headers: this.getHeaders() })
      .pipe(
        map((res) => ({
          valid: res.data?.result === 'VALID' || res.success,
          message: res.data?.message || 'Vé hợp lệ. Cho phép vào phòng chiếu.',
        })),
        catchError((err) => of({ valid: false, message: 'Mã vé không tồn tại hoặc đã sử dụng.' }))
      );
  }
}

