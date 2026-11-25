import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal, computed, effect } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { Movie } from '../models/movie.model';
import { Cinema } from '../models/cinema.model';
import { Showtime } from '../models/showtime.model';
import { Seat } from '../models/seat.model';
import { FnbCombo } from '../models/combo.model';
import { BuyerInfo, CreateOrderPayload, HoldSeatsResponse } from '../models/booking.model';
import { CreateOrderResponse, Order, PaymentMethod, PayOrderPayload, PayOrderResponse, Ticket } from '../models/order.model';
import { CinemaService } from './cinema.service';

export interface ApiVoucher {
  voucherId: number;
  voucherCode: string;
  campaignCode: string;
  campaignName: string;
  discountType: string;
  discountValue: number;
  maxDiscount: number | null;
  validFrom: string | null;
  validTo: string | null;
  status: string;
}

export interface VoucherValidationResult {
  voucherCode: string;
  campaignName: string;
  discountType: string;
  discountValue: number;
  discountAmount: number;
  message: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  private readonly bookingApi = '/api/v1/public/bookings';
  private holdSalesOrderId = 0;
  private holdToken = '';
  private voucherValidationKey = '';
  private voucherValidationSequence = 0;
  get holdSession(): { salesOrderId: number; holdToken: string } {
    return { salesOrderId: this.holdSalesOrderId, holdToken: this.holdToken };
  }

  readonly currentMovie = signal<Movie | null>(null);
  readonly currentCinema = signal<Cinema | null>(null);
  readonly currentShowtime = signal<Showtime | null>(null);
  readonly availableSeats = signal<Seat[]>([]);
  readonly selectedSeats = signal<Seat[]>([]);
  readonly selectedCombos = signal<FnbCombo[]>([]);
  readonly buyerInfo = signal<BuyerInfo>({
    fullName: '',
    email: '',
    phone: '',
    needInvoice: false,
    companyName: '',
    taxCode: '',
    companyAddress: '',
  });

  readonly voucherCode = signal<string>('');
  readonly discountAmount = signal<number>(0);
  readonly voucherMessage = signal<string>('');

  readonly holdSeconds = signal<number>(600);
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  readonly orders = signal<Order[]>(this.loadOrders());

  readonly ticketTotal = computed(() =>
    this.selectedSeats().reduce((sum, s) => sum + s.price, 0)
  );

  readonly fnbTotal = computed(() =>
    this.selectedCombos().reduce((sum, c) => sum + c.price * c.quantity, 0)
  );

  readonly subtotal = computed(() => this.ticketTotal() + this.fnbTotal());

  readonly vatAmount = computed(() => {
    const taxableAmount = Math.max(0, this.subtotal() - this.discountAmount());
    return Math.round(taxableAmount * 0.08);
  });

  readonly grandTotal = computed(() =>
    Math.max(0, this.subtotal() - this.discountAmount()) + this.vatAmount()
  );

  constructor(private readonly http: HttpClient, private readonly cinemaService: CinemaService) {
    effect(() => {
      const seats = this.selectedSeats();
      if (seats.length > 0 && !this.timerInterval) {
        this.startHoldTimer();
      } else if (seats.length === 0 && this.timerInterval) {
        this.stopHoldTimer();
      }
    });
    effect(() => {
      const code = this.voucherCode();
      const subtotal = this.subtotal();
      const key = `${code}:${subtotal}`;
      if (code && subtotal > 0 && key !== this.voucherValidationKey) {
        this.validateVoucher(code, subtotal).subscribe({ error: () => undefined });
      }
    });
  }

  initShowtime(movie: Movie, cinema: Cinema, showtime: Showtime): void {
    this.currentMovie.set(movie);
    this.currentCinema.set(cinema);
    this.currentShowtime.set(showtime);
    this.selectedSeats.set([]);
    this.selectedCombos.set([]);
    this.voucherCode.set('');
    this.discountAmount.set(0);
    this.voucherMessage.set('');
    this.holdSalesOrderId = 0;
    this.holdToken = '';
    this.availableSeats.set([]);
    this.holdSeconds.set(600);
  }

  updateComboQuantity(combo: FnbCombo, delta: number): void {
    const combos = [...this.selectedCombos()];
    const existingIndex = combos.findIndex((c) => c.id === combo.id);

    if (existingIndex >= 0) {
      const newQty = combos[existingIndex].quantity + delta;
      if (newQty <= 0) {
        combos.splice(existingIndex, 1);
      } else {
        combos[existingIndex] = { ...combos[existingIndex], quantity: newQty };
      }
    } else if (delta > 0) {
      combos.push({ ...combo, quantity: delta });
    }

    this.selectedCombos.set(combos);
  }

  validateVoucher(code: string, subtotal = this.subtotal()): Observable<VoucherValidationResult> {
    const clean = code.trim().toUpperCase();
    if (!clean) {
      this.clearVoucher();
      return throwError(() => new Error('Vui lòng nhập mã giảm giá.'));
    }
    const validationKey = `${clean}:${subtotal}`;
    this.voucherValidationKey = validationKey;
    const sequence = ++this.voucherValidationSequence;
    return this.http.post<ApiResponse<VoucherValidationResult>>(`${this.bookingApi}/vouchers/validate`, {
      voucherCode: clean,
      subtotal,
    }).pipe(
      map(({ data }) => {
        if (sequence === this.voucherValidationSequence) {
          this.voucherCode.set(data.voucherCode);
          this.discountAmount.set(Number(data.discountAmount) || 0);
          this.voucherMessage.set(`Áp dụng ${data.voucherCode}: giảm ${this.discountAmount().toLocaleString('vi-VN')} ₫`);
        }
        return data;
      }),
      catchError((error) => {
        if (sequence === this.voucherValidationSequence) {
          this.clearVoucher();
          const response = error as { error?: { message?: string; error?: { message?: string } } };
          this.voucherMessage.set(response?.error?.error?.message || response?.error?.message || 'Mã voucher không hợp lệ.');
        }
        return throwError(() => error);
      })
    );
  }

  clearVoucher(): void {
    this.voucherValidationSequence++;
    this.voucherValidationKey = '';
    this.voucherCode.set('');
    this.discountAmount.set(0);
    this.voucherMessage.set('');
  }

  updateBuyerInfo(info: BuyerInfo): void {
    this.buyerInfo.set({ ...info });
  }

  startHoldTimer(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.holdSeconds.set(600);
    this.timerInterval = setInterval(() => {
      this.holdSeconds.update((s) => {
        if (s <= 1) {
          this.stopHoldTimer();
          this.selectedSeats.set([]);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
  }

  stopHoldTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  createOrder(input: CreateOrderPayload): Observable<Order> {
    return this.http.post<ApiResponse<CreateOrderResponse>>(`${this.bookingApi}/orders`, input).pipe(
      map(({ data }) => this.fromApiOrder(data))
    );
  }

  fetchSeats(screeningId: number | string): Observable<Seat[]> {
    return this.cinemaService.getScreeningSeats(screeningId).pipe(
      map((seats) => {
        this.availableSeats.set(seats);
        return seats;
      })
    );
  }

  holdSeats(seatIds: number[]): Observable<HoldSeatsResponse> {
    const showtime = this.currentShowtime();
    const screeningId = showtime?.backendScreeningId ?? Number(showtime?.id);
    if (!screeningId || !Number.isFinite(screeningId)) {
      return throwError(() => new Error('Suất chiếu chưa được kết nối.'));
    }
    const payload = {
      salesOrderId: this.holdSalesOrderId,
      holdToken: this.holdToken || null,
      showSeatIds: seatIds,
    };
    return this.http
      .post<ApiResponse<HoldSeatsResponse>>(
        `${this.bookingApi}/hold?screeningId=${encodeURIComponent(screeningId)}`,
        payload
      )
      .pipe(
        map(({ data }) => {
          this.holdSalesOrderId = data.salesOrderId;
          this.holdToken = data.holdToken;
          this.availableSeats.update((seats) =>
            seats.map((seat) =>
              seatIds.includes(Number(seat.showSeatId))
                ? { ...seat, seatStatus: 'HELD', status: 'SELECTED', holdExpiresAt: data.expiresAt }
                : seat
            )
          );
          return data;
        })
      );
  }

  releaseSeats(seatIds: number[]): Observable<string> {
    return this.http
      .post<ApiResponse<string>>(`${this.bookingApi}/release`, {
        salesOrderId: this.holdSalesOrderId,
        holdToken: this.holdToken,
        showSeatIds: seatIds,
      })
      .pipe(map(({ data }) => data));
  }

  payOrder(orderCode: string, method: PaymentMethod): Observable<PayOrderResponse> {
    const payload: PayOrderPayload = {
      provider: method,
      paymentMethod: method,
      idempotencyKey: `pay-${orderCode}`,
      successful: true,
    };
    return this.http
      .post<ApiResponse<PayOrderResponse>>(
        `${this.bookingApi}/orders/${encodeURIComponent(orderCode)}/pay`,
        payload
      )
      .pipe(map(({ data }) => data));
  }

  getOrder(orderCode: string): Observable<Order | undefined> {
    return this.http
      .get<ApiResponse<CreateOrderResponse>>(`${this.bookingApi}/orders/${encodeURIComponent(orderCode)}`)
      .pipe(map(({ data }) => this.fromApiOrder(data)));
  }

  simulateWebhookPayment(orderCode: string): Observable<any> {
    return this.http.post<any>(`/api/v1/public/payments/webhook/simulate?orderCode=${encodeURIComponent(orderCode)}`, {});
  }

  getMyOrders(): Observable<Order[]> {
    return this.http
      .get<ApiResponse<CreateOrderResponse[]>>('/api/v1/customer/orders')
      .pipe(
        map(({ data }) => {
          const list = (data || []).map((item) => this.fromApiOrder(item));
          return list;
        }),
        catchError(() => {
          return this.http
            .get<ApiResponse<CreateOrderResponse[]>>(`${this.bookingApi}/my-orders`)
            .pipe(
              map(({ data }) => (data || []).map((item) => this.fromApiOrder(item))),
              catchError(() => of([]))
            );
        })
      );
  }

  getVouchers(): Observable<ApiVoucher[]> {
    return this.http
      .get<ApiResponse<ApiVoucher[]>>('/api/v1/customer/vouchers')
      .pipe(
        map(({ data }) => data || []),
        catchError((error: HttpErrorResponse) => {
          if (error.status !== 401 && error.status !== 403) return throwError(() => error);
          return this.http
            .get<ApiResponse<ApiVoucher[]>>(`${this.bookingApi}/vouchers`)
            .pipe(map(({ data }) => data || []));
        })
      );
  }

  removeOrder(orderCode: string): void {
    const updated = this.orders().filter(
      (item) => item.orderCode !== orderCode && item.id !== orderCode
    );
    this.orders.set(updated);
    this.saveOrders(updated);
  }

  private fromApiOrder(data: CreateOrderResponse): Order {
    const existing = this.orders().find((o) => o.orderCode === data.orderCode);
    const movie = this.currentMovie();
    const cinema = this.currentCinema();
    const showtime = this.currentShowtime();

    const tickets: Ticket[] = (data.tickets ?? []).map((ticket) => ({
      ticketNumber: ticket.ticketNumber,
      qrTokenHash: ticket.qrTokenHash,
      qrData: ticket.qrData,
      filmTitle: ticket.filmTitle,
      cinemaName: ticket.cinemaName,
      roomName: ticket.roomName,
      screeningStartsAt: ticket.screeningStartsAt,
      seatLabel: ticket.seatLabel,
      status: ticket.status,
    }));

    const firstTicket = tickets[0];
    const startDateObj = firstTicket?.screeningStartsAt ? new Date(firstTicket.screeningStartsAt) : null;
    const derivedDate = startDateObj
      ? `${startDateObj.getFullYear()}-${String(startDateObj.getMonth() + 1).padStart(2, '0')}-${String(startDateObj.getDate()).padStart(2, '0')}`
      : '';
    const derivedTime = startDateObj
      ? `${String(startDateObj.getHours()).padStart(2, '0')}:${String(startDateObj.getMinutes()).padStart(2, '0')}`
      : '';

    const ticketSeats: Seat[] =
      existing?.seats?.length
        ? existing.seats
        : this.selectedSeats().length
          ? [...this.selectedSeats()]
          : tickets.map((t, idx) => ({
              id: `tkt-${idx}-${t.ticketNumber}`,
              row: (t.seatLabel || '').slice(0, 1),
              col: Number((t.seatLabel || '').match(/\d+/)?.[0] ?? 0),
              code: t.seatLabel || t.ticketNumber,
              type: 'STANDARD',
              status: 'BOOKED',
              price: Number(data.lines?.[idx]?.unitPrice ?? 0),
            }));

    const fnbLineTotal = (data.lines ?? [])
      .filter((line) => !line.seatType)
      .reduce((sum, line) => sum + Number(line.unitPrice ?? 0), 0);

    const qrPayload =
      firstTicket?.qrData ||
      firstTicket?.qrTokenHash ||
      firstTicket?.ticketNumber ||
      data.orderCode;
    const qrCodeUrl = qrPayload
      ? `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(qrPayload)}`
      : '';

    const order: Order = {
      id: data.orderCode,
      orderCode: data.orderCode,
      bookingCode: data.orderCode,
      qrCodeUrl,
      movieTitle: existing?.movieTitle || firstTicket?.filmTitle || movie?.title || 'Phim rạp THEFILM',
      movieRating: existing?.movieRating || movie?.rating || 'T13',
      cinemaName: existing?.cinemaName || firstTicket?.cinemaName || cinema?.name || 'THEFILM Cinema',
      cinemaAddress: existing?.cinemaAddress || cinema?.address || '',
      roomName: existing?.roomName || firstTicket?.roomName || showtime?.roomName || 'Phòng chiếu',
      format: existing?.format || showtime?.format || '2D',
      showDate: existing?.showDate || showtime?.date || derivedDate,
      showTime:
        existing?.showTime ||
        (showtime ? `${showtime.startTime} - ${showtime.endTime}` : derivedTime),
      seats: ticketSeats,
      combos: existing?.combos?.length ? existing.combos : [...this.selectedCombos()],
      ticketTotal: Number(data.subtotal),
      fnbTotal: existing?.fnbTotal ?? (this.selectedCombos().length ? this.fnbTotal() : fnbLineTotal),
      discountTotal: Number(data.discount),
      finalTotal: Number(data.total),
      vatRate: 0.08,
      vatAmount: Number(data.vat),
      paymentMethod: existing?.paymentMethod ?? 'VNPAY_QR',
      paymentExpiresAt: data.paymentExpiresAt,
      status: data.status as Order['status'],
      createdAt: existing?.createdAt || new Date().toISOString(),
      buyer: existing?.buyer || { ...this.buyerInfo() },
      tickets,
    };

    const updatedOrders = [
      order,
      ...this.orders().filter((item) => item.orderCode !== order.orderCode),
    ];
    this.orders.set(updatedOrders);
    this.saveOrders(updatedOrders);
    this.stopHoldTimer();
    return order;
  }

  getOrderById(id: string): Order | undefined {
    return this.orders().find((o) => o.id === id || o.orderCode === id || o.bookingCode === id);
  }

  private getLoggedInEmail(): string | null {
    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        const raw = sessionStorage.getItem('thefilm.auth.session');
        if (raw) {
          const parsed = JSON.parse(raw);
          return (parsed?.user?.email || '').trim().toLowerCase() || null;
        }
      }
    } catch {}
    return null;
  }

  private loadOrders(): Order[] {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const stored = localStorage.getItem('thefilm_orders');
        if (stored) {
          const parsed = JSON.parse(stored) as Order[];
          // Keep only real backend-issued orders (strip legacy mock entries).
          const valid = parsed.filter(
            (o) => Boolean(o?.orderCode) && o.id !== 'ord-demo-01' && !o.bookingCode?.startsWith('TFM-')
          );
          const loggedEmail = this.getLoggedInEmail();
          if (loggedEmail) {
            return valid.filter((o) => {
              const email = (o.buyer?.email || '').trim().toLowerCase();
              return !email || email === loggedEmail;
            });
          }
          return valid;
        }
      }
    } catch (e) {
      console.warn('Cannot load orders from localStorage', e);
    }
    return [];
  }

  private saveOrders(orders: Order[]): void {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.setItem('thefilm_orders', JSON.stringify(orders));
      }
    } catch (e) {
      console.warn('Cannot save orders to localStorage', e);
    }
  }
}
