import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, switchMap, throwError, timeout } from 'rxjs';
import { AdminAuthService } from './admin-auth.service';

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
  bannerUri: string | null;
  trailerUri: string | null;
  releaseDate: string | null;
  director: string | null;
  castNames: string | null;
  externalRatingScore: number | null;
  externalRatingCount: number | null;
  externalRatingSource: string | null;
  externalRatingUri: string | null;
  externalRatingUpdatedAt: string | null;
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
  bannerUri?: string;
  trailerUri?: string;
  releaseDate?: string;
  director?: string;
  castNames?: string;
  externalRatingScore?: number;
  externalRatingCount?: number;
  externalRatingSource?: string;
  externalRatingUri?: string;
  externalRatingUpdatedAt?: string;
  genreIds: number[];
}

export type FilmUpdateRequest = Omit<FilmCreateRequest, 'filmCode'>;

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

export interface FnbCategoryDto {
  categoryId: number;
  categoryCode: string;
  categoryName: string;
}

export interface FnbLocationDto {
  locationId: number;
  locationCode: string;
  locationName: string;
  locationType: string;
}

export interface FnbProductDto {
  productId: number;
  productCode: string;
  productName: string;
  categoryId: number;
  categoryCode: string;
  categoryName: string;
  productType: 'ITEM' | 'COMBO';
  unitCode: string;
  onHandQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  unitPrice: number | null;
  lowStockThreshold: number;
  trackInventory: boolean;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface FnbOverviewDto {
  categories: FnbCategoryDto[];
  locations: FnbLocationDto[];
  products: FnbProductDto[];
  recentMovements: Array<{
    transactionNumber: string;
    transactionType: string;
    productCode: string;
    productName: string;
    locationName: string;
    quantityDelta: number;
    quantityBefore: number;
    quantityAfter: number;
    reason: string;
    occurredAt: string;
  }>;
  metrics: {
    availableStock: number;
    lowStockProducts: number;
    monthlyWastePercent: number | null;
    revenueToday: number;
  };
}

export interface FnbProductCreateRequest {
  productCode: string;
  productName: string;
  categoryId: number;
  productType: 'ITEM' | 'COMBO';
  unitCode: string;
  unitPrice: number;
  lowStockThreshold: number;
  initialStock: number;
  trackInventory: boolean;
}

export interface FnbProductUpdateRequest {
  productName: string;
  categoryId: number;
  unitPrice: number;
  lowStockThreshold: number;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface FnbInventoryMovementRequest {
  locationId: number;
  transactionType: 'RECEIPT' | 'ISSUE' | 'WASTE' | 'COUNT';
  quantity: number;
  unitCost?: number;
  reason: string;
}

export interface ChainDto {
  chainId: number;
  chainCode: string;
  chainName: string;
  status: string;
}

export interface ChainRequest {
  chainCode: string;
  chainName: string;
}

export interface ChainUpdateRequest {
  chainName: string;
}

export type ActiveStatusCode = 'ACTIVE' | 'INACTIVE';
export type CinemaStatusCode = 'ACTIVE' | 'INACTIVE' | 'CLOSED';

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

export interface LegalEntityUpdateRequest {
  legalName: string;
  taxIdentifier?: string;
}

export interface CinemaUpdateRequest {
  legalEntityId: number;
  cinemaName: string;
  addressLine: string;
  provinceName?: string;
  districtName?: string;
  wardName?: string;
  phone?: string;
  email?: string;
  timeZone?: string;
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
  roomTypeName?: string;
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

export interface ScreeningSeatDto {
  showSeatId: number;
  seatCode: string;
  rowLabel: string;
  seatLabel: string;
  seatTypeCode: string;
  status: string;
  basePrice: number | null;
  holdExpiresAt?: string | null;
}

export interface CounterSaleResponse {
  order: {
    orderCode: string;
    status: string;
    subtotal: number;
    total: number;
    lines: Array<{
      item: string;
      screeningType: string | null;
      quantity: number;
      unitPrice: number;
      total: number;
    }>;
    tickets: Array<{
      ticketNumber: string;
      filmTitle: string;
      cinemaName: string;
      roomName: string;
      screeningStartsAt: string;
      seatLabel: string;
      status: string;
      qrData: string;
    }>;
  };
  payment: { paymentStatus: string; orderStatus: string } | null;
  paymentMethod: 'CASH' | 'CARD' | 'QR';
}

export interface TicketAdminDto {
  ticketId: number;
  ticketNumber: string;
  cinemaId: number;
  screeningId: number;
  orderNumber: string;
  filmTitle: string;
  cinemaName: string;
  roomName: string;
  seatLabel: string;
  screeningStartsAt: string;
  status: string;
  issuedAt: string;
  scannedAt?: string | null;
}

export interface TicketScanResult {
  result: string;
  reason: string;
  ticket: TicketAdminDto | null;
}

export interface ScreeningCreateRequest {
  cinemaId: number;
  roomId: number;
  filmVersionId: number;
  startsAt: string;
  screeningTypeCode: string;
  cleanupMinutes: number;
}

export interface ScreeningBulkCreateRequest {
  cinemaId: number;
  roomId: number;
  filmVersionId: number;
  startsAt: string[];
  screeningTypeCode: string;
  cleanupMinutes: number;
}

export interface ScreeningUpdateRequest {
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
  layoutName: string;
  rows: RoomLayoutRowRequest[];
}

export interface RoomLayoutRowRequest {
  rowLabel: string;
  seatCount: number;
  seatTypeId: number;
  alignment: 'LEFT' | 'CENTER' | 'RIGHT';
}

export interface RoomLayoutCreateRequest {
  layoutName: string;
  rows: RoomLayoutRowRequest[];
}

export interface RoomLayoutAlignmentRequest {
  layoutName: string;
  rows: Array<{
    rowLabel: string;
    seatCount: number;
    seatTypeId: number | null;
    alignment: 'LEFT' | 'CENTER' | 'RIGHT';
  }>;
}

export interface RoomLayoutDto {
  layoutId: number;
  roomId: number;
  versionNo: number;
  layoutName: string;
  status: string;
  validFrom?: string;
  validTo?: string;
  seatCount: number;
}

export interface SeatTypeCreateRequest {
  typeCode: string;
  typeName: string;
  description?: string;
}

export interface RoomSeatDto {
  roomSeatId: number;
  seatTypeId: number;
  seatTypeCode: string;
  seatCode: string;
  rowLabel: string;
  columnNo: number;
  seatLabel: string;
  sellable: boolean;
  status: string;
}

export interface RoomTypeCreateRequest {
  typeCode: string;
  typeName: string;
  description?: string;
}

// Price Rule DTOs
export interface PriceRuleDto {
  priceRuleId: number;
  ruleCode: string;
  cinemaId?: number;
  roomTypeId?: number;
  seatTypeId: number;
  filmId?: number;
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
  filmId?: number;
  screeningTypeCode?: string;
  weekdayNo?: number;
  validFrom: string;
  validTo?: string;
  basePrice: number;
  priorityNo: number;
}

export interface VoucherCampaignDto {
  campaignId: number;
  campaignCode: string;
  campaignName: string;
  discountType: 'FIXED' | 'PERCENT';
  discountValue: number;
  minimumOrderAmount: number;
  maxDiscount: number | null;
  budgetAmount: number | null;
  budgetUsed: number;
  usageLimit: number | null;
  perCustomerLimit: number | null;
  validFrom: string;
  validTo: string;
  stackable: boolean;
  status: string;
  voucherCount: number;
  redeemedCount: number;
}

export interface VoucherCampaignRequest {
  campaignName: string;
  discountType: 'FIXED' | 'PERCENT';
  discountValue: number;
  minimumOrderAmount: number;
  maxDiscount?: number | null;
  budgetAmount?: number | null;
  usageLimit?: number | null;
  perCustomerLimit?: number | null;
  validFrom: string;
  validTo: string;
  stackable: boolean;
  voucherPrefix: string;
  voucherCount: number;
}

export type VoucherCampaignUpdateRequest = Omit<VoucherCampaignRequest, 'voucherPrefix' | 'voucherCount'>;

export interface VoucherCodeDto {
  voucherId: number;
  voucherCode: string;
  status: string;
  validFrom: string | null;
  validTo: string | null;
  createdAt: string;
}

export interface VoucherCodeBatchRequest {
  voucherPrefix: string;
  quantity: number;
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

export type EmployeeStatusCode = 'PENDING' | 'ACTIVE' | 'ON_LEAVE' | 'TERMINATED';

export interface EmployeeCreateRequest {
  chainId: number;
  employeeCode: string;
  fullName: string;
  dateOfBirth?: string;
  phone?: string;
  email?: string;
  hiredOn?: string;
  accountMode?: 'NONE' | 'CREATE';
  accountEmail?: string;
  initialPassword?: string;
  roleCode?: string;
  cinemaId: number;
  departmentId: number;
  jobPositionId: number;
}

export interface HrCatalogItem { id: number; chainId: number; code: string; name: string; status: string; }
export interface DepartmentModulePermission {
  moduleCode: string;
  moduleName: string;
  description: string;
  sortOrder: number;
  enabled: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AdminOpsService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AdminAuthService);
  private readonly baseApi = '/api/v1/admin';

  private getHeaders(json = true): HttpHeaders {
    let headers = new HttpHeaders({ 'X-Request-Id': 'req-' + Date.now() });
    const accessToken = this.auth.getAccessToken();
    if (accessToken) headers = headers.set('Authorization', `Bearer ${accessToken}`);
    if (json) headers = headers.set('Content-Type', 'application/json');
    return headers;
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
        timeout({ first: 15000 }),
        catchError((err) => throwError(() => err))
      );
  }

  getFilmLookups(): Observable<FilmLookups> {
    return this.http
      .get<ApiResponse<FilmLookups>>(`${this.baseApi}/films/lookups`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || { genres: [], ageClassifications: [] }),
        timeout({ first: 15000 }),
        catchError((err) => throwError(() => err))
      );
  }

  changeFilmStatus(filmId: number, status: string): Observable<FilmAdminDto> {
    return this.http
      .patch<ApiResponse<FilmAdminDto>>(`${this.baseApi}/films/${filmId}/status`, { status }, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => throwError(() => err))
      );
  }

  getFilmVersions(filmId: number): Observable<FilmVersionDto[]> {
    return this.http
      .get<ApiResponse<FilmVersionDto[]>>(`${this.baseApi}/films/${filmId}/versions`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        timeout({ first: 15000 }),
        catchError((err) => throwError(() => err))
      );
  }

  createFilmVersion(filmId: number, request: Omit<FilmVersionDto, 'filmVersionId' | 'filmId' | 'status'>): Observable<FilmVersionDto> {
    return this.http
      .post<ApiResponse<FilmVersionDto>>(`${this.baseApi}/films/${filmId}/versions`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => throwError(() => err))
      );
  }

  createFilm(request: FilmCreateRequest): Observable<FilmAdminDto | null> {
    return this.http
      .post<ApiResponse<FilmAdminDto>>(`${this.baseApi}/films`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => {
          console.error('Error creating film:', err);
          return throwError(() => err);
        })
      );
  }

  createFilmWithPoster(request: FilmCreateRequest, posterFile?: File): Observable<FilmAdminDto | null> {
    const create = (posterUri?: string) =>
      this.http.post<ApiResponse<FilmAdminDto>>(
        `${this.baseApi}/films`,
        { ...request, posterUri },
        { headers: this.getHeaders() }
      );

    if (!posterFile) {
      return create(request.posterUri).pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => {
          console.error('Error creating film:', err);
          return throwError(() => err);
        })
      );
    }

    const form = new FormData();
    form.append('file', posterFile, posterFile.name);
    const headers = this.getHeaders(false);
    return this.http
      .post<ApiResponse<string>>(`${this.baseApi}/films/posters`, form, { headers })
      .pipe(
        switchMap((upload) => create(upload.data)),
        map((res) => res.data),
        timeout({ first: 120000 }),
        catchError((err) => {
          console.error('Error creating film or saving poster:', err);
          return of(null);
        })
      );
  }

  updateFilmWithPoster(filmId: number, request: FilmUpdateRequest, posterFile?: File): Observable<FilmAdminDto | null> {
    const update = (posterUri?: string) =>
      this.http.put<ApiResponse<FilmAdminDto>>(
        `${this.baseApi}/films/${filmId}`,
        { ...request, posterUri },
        { headers: this.getHeaders() }
      );
    if (!posterFile) {
      return update(request.posterUri).pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => {
          console.error('Error updating film:', err);
          return throwError(() => err);
        })
      );
    }
    const form = new FormData();
    form.append('file', posterFile, posterFile.name);
    const headers = this.getHeaders(false);
    return this.http.post<ApiResponse<string>>(`${this.baseApi}/films/posters`, form, { headers }).pipe(
      switchMap((upload) => update(upload.data)),
      map((res) => res.data),
      timeout({ first: 120000 }),
      catchError((err) => {
        console.error('Error updating film or saving poster:', err);
        return throwError(() => err);
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

  getTicketingCinemas(): Observable<CinemaAdminDto[]> {
    return this.http.get<ApiResponse<CinemaAdminDto[]>>(`${this.baseApi}/tickets/cinemas`, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data || []), timeout({ first: 15000 }));
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

  createChain(request: ChainRequest): Observable<ChainDto | null> {
    return this.http
      .post<ApiResponse<ChainDto>>(`${this.baseApi}/chains`, request, { headers: this.getHeaders() })
      .pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
        console.error('Error creating chain:', err);
        return of(null);
      }));
  }

  updateChain(id: number, request: ChainUpdateRequest): Observable<ChainDto | null> {
    return this.http.put<ApiResponse<ChainDto>>(`${this.baseApi}/chains/${id}`, request, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
      console.error('Error updating chain:', err);
      return throwError(() => err);
    }));
  }

  changeChainStatus(id: number, status: ActiveStatusCode): Observable<ChainDto | null> {
    return this.http.patch<ApiResponse<ChainDto>>(`${this.baseApi}/chains/${id}/status`, { status }, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
      console.error('Error changing chain status:', err);
      return of(null);
    }));
  }

  getHrCatalog(kind: 'departments' | 'job-positions', chainId: number): Observable<HrCatalogItem[]> {
    return this.http.get<ApiResponse<HrCatalogItem[]>>(`${this.baseApi}/hr/${kind}`, {
      headers: this.getHeaders(),
      params: new HttpParams().set('chainId', String(chainId)),
    }).pipe(map((res) => res.data || []), catchError(() => of([])));
  }

  createHrCatalog(kind: 'departments' | 'job-positions', request: {
    chainId: number;
    code: string;
    name: string;
    status: 'ACTIVE';
  }): Observable<HrCatalogItem | null> {
    return this.http.post<ApiResponse<HrCatalogItem>>(`${this.baseApi}/hr/${kind}`, request, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
      console.error(`Error creating ${kind}:`, err);
      return of(null);
    }));
  }

  getDepartmentPermissions(departmentId: number): Observable<DepartmentModulePermission[]> {
    return this.http.get<ApiResponse<DepartmentModulePermission[]>>(
      `${this.baseApi}/permissions/departments/${departmentId}`, { headers: this.getHeaders() })
      .pipe(map((res) => res.data || []));
  }

  saveDepartmentPermissions(departmentId: number, moduleCodes: string[]): Observable<void> {
    return this.http.put<ApiResponse<void>>(`${this.baseApi}/permissions/departments/${departmentId}`, {
      moduleCodes,
    }, { headers: this.getHeaders() }).pipe(map((res) => res.data), timeout({ first: 30000 }));
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
        timeout({ first: 30000 }),
        catchError((err) => {
          console.error('Error creating legal entity:', err);
          return of(null);
        })
      );
  }

  updateLegalEntity(id: number, request: LegalEntityUpdateRequest): Observable<LegalEntityDto | null> {
    return this.http.put<ApiResponse<LegalEntityDto>>(`${this.baseApi}/legal-entities/${id}`, request, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
      console.error('Error updating legal entity:', err);
      return of(null);
    }));
  }

  changeLegalEntityStatus(id: number, status: ActiveStatusCode): Observable<LegalEntityDto | null> {
    return this.http.patch<ApiResponse<LegalEntityDto>>(`${this.baseApi}/legal-entities/${id}/status`, { status }, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
      console.error('Error changing legal entity status:', err);
      return of(null);
    }));
  }

  createCinema(request: CinemaCreateRequest): Observable<CinemaAdminDto | null> {
    return this.http
      .post<ApiResponse<CinemaAdminDto>>(`${this.baseApi}/cinemas`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => {
          console.error('Error creating cinema:', err);
          return of(null);
        })
      );
  }

  updateCinema(id: number, request: CinemaUpdateRequest): Observable<CinemaAdminDto | null> {
    return this.http.put<ApiResponse<CinemaAdminDto>>(`${this.baseApi}/cinemas/${id}`, request, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
      console.error('Error updating cinema:', err);
      return of(null);
    }));
  }

  changeCinemaStatus(id: number, status: CinemaStatusCode): Observable<CinemaAdminDto | null> {
    return this.http.patch<ApiResponse<CinemaAdminDto>>(`${this.baseApi}/cinemas/${id}/status`, { status }, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }), catchError((err) => {
      console.error('Error changing cinema status:', err);
      return of(null);
    }));
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

  getRoomTypes(cinemaId: number): Observable<RoomTypeDto[]> {
    return this.http
      .get<ApiResponse<RoomTypeDto[]>>(`${this.baseApi}/cinemas/${cinemaId}/room-types`, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data || []),
        catchError(() => of([]))
      );
  }

  createRoomType(cinemaId: number, request: RoomTypeCreateRequest): Observable<RoomTypeDto> {
    return this.http
      .post<ApiResponse<RoomTypeDto>>(`${this.baseApi}/cinemas/${cinemaId}/room-types`, request, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  changeRoomTypeStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Observable<RoomTypeDto> {
    return this.http
      .patch<ApiResponse<RoomTypeDto>>(`${this.baseApi}/room-types/${id}/status`, { status }, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
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

  createSeatType(request: SeatTypeCreateRequest): Observable<SeatTypeDto> {
    return this.http
      .post<ApiResponse<SeatTypeDto>>(`${this.baseApi}/seat-types`, request, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  changeSeatTypeStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Observable<SeatTypeDto> {
    return this.http
      .patch<ApiResponse<SeatTypeDto>>(`${this.baseApi}/seat-types/${id}/status`, { status }, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  createRoom(cinemaId: number, request: RoomCreateRequest): Observable<RoomAdminDto> {
    return this.http
      .post<ApiResponse<RoomAdminDto>>(`${this.baseApi}/cinemas/${cinemaId}/rooms`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => {
          console.error('Error creating room:', err);
          return throwError(() => err);
        })
      );
  }

  getLayoutSeats(layoutId: number): Observable<RoomSeatDto[]> {
    return this.http
      .get<ApiResponse<RoomSeatDto[]>>(`${this.baseApi}/layouts/${layoutId}/seats`, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data || []), timeout({ first: 30000 }));
  }

  createAndActivateLayout(roomId: number, request: RoomLayoutCreateRequest): Observable<RoomLayoutDto> {
    return this.http
      .post<ApiResponse<RoomLayoutDto>>(`${this.baseApi}/rooms/${roomId}/layouts/activate`, request, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  createAlignedRevision(roomId: number, request: RoomLayoutAlignmentRequest): Observable<RoomLayoutDto> {
    return this.http
      .post<ApiResponse<RoomLayoutDto>>(`${this.baseApi}/rooms/${roomId}/layouts/alignment`, request, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  // ---- SCREENINGS ----
  getScreenings(cinemaId = 1, from?: string, to?: string): Observable<ScreeningAdminDto[]> {
    let params = new HttpParams().set('cinemaId', cinemaId.toString());
    if (from && to) params = params.set('from', from).set('to', to);

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

  getTicketingScreenings(cinemaId: number): Observable<ScreeningAdminDto[]> {
    const now = new Date();
    const startOfLocalDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const params = new HttpParams()
      .set('cinemaId', String(cinemaId))
      .set('from', startOfLocalDay.toISOString())
      .set('to', new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString());
    return this.http.get<ApiResponse<ScreeningAdminDto[]>>(`${this.baseApi}/tickets/screenings`, {
      headers: this.getHeaders(), params,
    }).pipe(map((res) => res.data || []), timeout({ first: 15000 }));
  }

  getTicketingFnbProducts(cinemaId: number): Observable<FnbProductDto[]> {
    return this.http.get<ApiResponse<FnbProductDto[]>>(`${this.baseApi}/tickets/fnb-products`, {
      headers: this.getHeaders(), params: new HttpParams().set('cinemaId', String(cinemaId)),
    }).pipe(map((res) => res.data || []), timeout({ first: 15000 }));
  }

  getTicketingSeats(screeningId: number): Observable<ScreeningSeatDto[]> {
    return this.http.get<ApiResponse<{ screeningId: number; seats: ScreeningSeatDto[] }>>(
      `${this.baseApi}/tickets/screenings/${screeningId}/seats`, { headers: this.getHeaders() })
      .pipe(map((res) => res.data?.seats || []), timeout({ first: 30000 }));
  }

  holdCounterSeat(screeningId: number, salesOrderId: number | null, holdToken: string | null, showSeatIds: number[]): Observable<{ salesOrderId: number; holdToken: string; expiresAt: string }> {
    return this.http.post<ApiResponse<{ salesOrderId: number; holdToken: string; expiresAt: string }>>(
      `${this.baseApi}/tickets/screenings/${screeningId}/hold`, { salesOrderId, holdToken, showSeatIds }, { headers: this.getHeaders() }
    ).pipe(map((res) => res.data), timeout({ first: 15000 }));
  }

  releaseCounterSeats(screeningId: number, salesOrderId: number, holdToken: string, showSeatIds: number[]): Observable<string> {
    return this.http.post<ApiResponse<string>>(`${this.baseApi}/tickets/screenings/${screeningId}/release`,
      { salesOrderId, holdToken, showSeatIds }, { headers: this.getHeaders() }).pipe(map((res) => res.data), timeout({ first: 15000 }));
  }

  createCounterSale(request: {
    screeningId: number;
    showSeatIds: number[];
    paymentMethod: 'CASH' | 'CARD' | 'QR';
    idempotencyKey: string;
    salesOrderId?: number;
    holdToken?: string;
    fnbItems?: Array<{ productId: number; quantity: number }>;
  }): Observable<CounterSaleResponse> {
    return this.http.post<ApiResponse<CounterSaleResponse>>(`${this.baseApi}/tickets/counter-sales`, request,
      { headers: this.getHeaders() }).pipe(
        map((res) => res.data), timeout({ first: 45000 }),
        catchError((err) => throwError(() => err))
      );
  }

  getCounterSaleStatus(orderCode: string): Observable<CounterSaleResponse['order']> {
    return this.http.get<ApiResponse<CounterSaleResponse['order']>>(
      `${this.baseApi}/tickets/counter-sales/${encodeURIComponent(orderCode)}`, { headers: this.getHeaders() }
    ).pipe(map((res) => res.data), timeout({ first: 15000 }));
  }

  searchTickets(query: { cinemaId?: number; ticketNumber?: string; orderNumber?: string }): Observable<TicketAdminDto[]> {
    let params = new HttpParams().set('page', '0').set('size', '20');
    if (query.cinemaId) params = params.set('cinemaId', String(query.cinemaId));
    if (query.ticketNumber?.trim()) params = params.set('ticketNumber', query.ticketNumber.trim());
    if (query.orderNumber?.trim()) params = params.set('orderNumber', query.orderNumber.trim());
    return this.http.get<ApiResponse<PageResponse<TicketAdminDto>>>(`${this.baseApi}/tickets`, {
      headers: this.getHeaders(), params,
    }).pipe(map((res) => res.data?.items || []), timeout({ first: 30000 }));
  }

  createScreening(request: ScreeningCreateRequest): Observable<ScreeningAdminDto> {
    return this.http
      .post<ApiResponse<ScreeningAdminDto>>(`${this.baseApi}/screenings`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => throwError(() => err))
      );
  }

  createScreeningsBulk(request: ScreeningBulkCreateRequest): Observable<ScreeningAdminDto[]> {
    return this.http
      .post<ApiResponse<ScreeningAdminDto[]>>(`${this.baseApi}/screenings/bulk`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 60000 }),
        catchError((err) => throwError(() => err))
      );
  }

  updateScreening(screeningId: number, request: ScreeningUpdateRequest): Observable<ScreeningAdminDto> {
    return this.http
      .put<ApiResponse<ScreeningAdminDto>>(`${this.baseApi}/screenings/${screeningId}`, request, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => throwError(() => err))
      );
  }

  publishScreening(screeningId: number): Observable<ScreeningAdminDto> {
    return this.http
      .post<ApiResponse<ScreeningAdminDto>>(`${this.baseApi}/screenings/${screeningId}/publish`, {}, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => throwError(() => err))
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
        timeout({ first: 30000 }),
        catchError((err) => throwError(() => err))
      );
  }

  changePriceRuleStatus(priceRuleId: number, status: string): Observable<PriceRuleDto> {
    return this.http
      .patch<ApiResponse<PriceRuleDto>>(`${this.baseApi}/price-rules/${priceRuleId}/status`, { status }, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((res) => res.data),
        timeout({ first: 30000 }),
        catchError((err) => throwError(() => err))
      );
  }

  getVoucherCampaigns(status?: string, keyword?: string): Observable<VoucherCampaignDto[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (keyword?.trim()) params = params.set('keyword', keyword.trim());
    return this.http.get<ApiResponse<VoucherCampaignDto[]>>(`${this.baseApi}/promotions`, {
      headers: this.getHeaders(), params,
    }).pipe(map((res) => res.data || []), timeout({ first: 30000 }));
  }

  createVoucherCampaign(request: VoucherCampaignRequest): Observable<VoucherCampaignDto> {
    return this.http.post<ApiResponse<VoucherCampaignDto>>(`${this.baseApi}/promotions`, request, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  updateVoucherCampaign(id: number, request: VoucherCampaignUpdateRequest): Observable<VoucherCampaignDto> {
    return this.http.put<ApiResponse<VoucherCampaignDto>>(`${this.baseApi}/promotions/${id}`, request, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  changeVoucherCampaignStatus(id: number, status: string): Observable<VoucherCampaignDto> {
    return this.http.patch<ApiResponse<VoucherCampaignDto>>(`${this.baseApi}/promotions/${id}/status`, { status }, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  getVoucherCodes(campaignId: number): Observable<VoucherCodeDto[]> {
    return this.http.get<ApiResponse<VoucherCodeDto[]>>(`${this.baseApi}/promotions/${campaignId}/vouchers`, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data || []), timeout({ first: 30000 }));
  }

  addVoucherCodes(campaignId: number, request: VoucherCodeBatchRequest): Observable<VoucherCodeDto[]> {
    return this.http.post<ApiResponse<VoucherCodeDto[]>>(`${this.baseApi}/promotions/${campaignId}/vouchers`, request, {
      headers: this.getHeaders(),
    }).pipe(map((res) => res.data || []), timeout({ first: 30000 }));
  }

  cancelVoucherCode(voucherId: number): Observable<VoucherCodeDto> {
    return this.http.patch<ApiResponse<VoucherCodeDto>>(`${this.baseApi}/promotions/vouchers/${voucherId}/status`,
      { status: 'CANCELLED' }, { headers: this.getHeaders() })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
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
        timeout({ first: 30000 }),
        catchError((err) => {
          console.error('Error creating employee:', err);
          return throwError(() => err);
        })
      );
  }

  updateEmployeeStatus(employeeId: number, status: EmployeeStatusCode, reason: string): Observable<EmployeeDto> {
    return this.http
      .patch<ApiResponse<EmployeeDto>>(`${this.baseApi}/employees/${employeeId}/status`, { status, reason }, {
        headers: this.getHeaders(),
      })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  // ---- F&B / INVENTORY ----
  getFnbCinemas(): Observable<CinemaAdminDto[]> {
    return this.http
      .get<ApiResponse<CinemaAdminDto[]>>(`${this.baseApi}/fnb/cinemas`, { headers: this.getHeaders() })
      .pipe(map((res) => res.data || []), timeout({ first: 15000 }));
  }

  getFnbOverview(cinemaId: number): Observable<FnbOverviewDto> {
    const params = new HttpParams().set('cinemaId', cinemaId.toString());
    return this.http
      .get<ApiResponse<FnbOverviewDto>>(`${this.baseApi}/fnb/overview`, { headers: this.getHeaders(), params })
      .pipe(map((res) => res.data), timeout({ first: 20000 }));
  }

  createFnbProduct(cinemaId: number, request: FnbProductCreateRequest): Observable<FnbProductDto> {
    const params = new HttpParams().set('cinemaId', cinemaId.toString());
    return this.http
      .post<ApiResponse<FnbProductDto>>(`${this.baseApi}/fnb/products`, request, { headers: this.getHeaders(), params })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  updateFnbProduct(cinemaId: number, productId: number, request: FnbProductUpdateRequest): Observable<FnbProductDto> {
    const params = new HttpParams().set('cinemaId', cinemaId.toString());
    return this.http
      .put<ApiResponse<FnbProductDto>>(`${this.baseApi}/fnb/products/${productId}`, request, { headers: this.getHeaders(), params })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  postFnbInventoryMovement(cinemaId: number, productId: number,
    request: FnbInventoryMovementRequest): Observable<unknown> {
    const params = new HttpParams().set('cinemaId', cinemaId.toString());
    return this.http
      .post<ApiResponse<unknown>>(`${this.baseApi}/fnb/products/${productId}/inventory`, request,
        { headers: this.getHeaders(), params })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }

  // ---- TICKETS / SCANNING ----
  scanTicket(qrData: string, cinemaId: number): Observable<TicketScanResult> {
    return this.http
      .post<ApiResponse<TicketScanResult>>(`${this.baseApi}/tickets/scan`, {
        cinemaId,
        qrData,
        deviceReference: 'ADMIN_TICKET_COUNTER',
      }, { headers: this.getHeaders() })
      .pipe(map((res) => res.data), timeout({ first: 30000 }));
  }
}

