import { HttpClient } from '@angular/common/http';
import { Injectable, signal, computed, effect } from '@angular/core';
import { Observable, map, throwError } from 'rxjs';
import { Movie } from '../models/movie.model';
import { Cinema } from '../models/cinema.model';
import { Showtime } from '../models/showtime.model';
import { Seat } from '../models/seat.model';
import { FnbCombo } from '../models/combo.model';
import { BuyerInfo, CreateOrderPayload, HoldSeatsResponse } from '../models/booking.model';
import { CreateOrderResponse, Order, PaymentMethod, PayOrderPayload, PayOrderResponse } from '../models/order.model';
import { CinemaService } from './cinema.service';

const MOCK_INITIAL_ORDERS: Order[] = [
  {
    id: 'ord-demo-01',
    orderCode: 'TF2611019842',
    bookingCode: 'TFM-842-990',
    qrCodeUrl: 'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=THEFILM-TFM-842-990-CONFIRMED',
    movieTitle: 'Tầng Mây Cuối Cùng',
    movieRating: 'T13',
    cinemaName: 'THEFILM Nguyễn Trãi',
    cinemaAddress: '136 Nguyễn Trãi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh',
    roomName: 'Phòng 02 (IMAX)',
    format: 'IMAX Laser',
    showDate: '2026-11-01',
    showTime: '19:30 - 21:28',
    seats: [
      { id: 'seat-F-07', row: 'F', col: 7, code: 'F07', type: 'VIP', status: 'BOOKED', price: 160000 },
      { id: 'seat-F-08', row: 'F', col: 8, code: 'F08', type: 'VIP', status: 'BOOKED', price: 160000 },
    ],
    combos: [
      { id: 'fnb-02', name: 'Combo Couple Cinema', description: '1 Bắp lớn + 2 Nước ngọt', price: 129000, imageUrl: '', category: 'COMBO', quantity: 1 },
    ],
    ticketTotal: 320000,
    fnbTotal: 129000,
    discountTotal: 40000,
    finalTotal: 409000,
    vatRate: 0.08,
    vatAmount: 30296,
    paymentMethod: 'VNPAY_QR',
    status: 'CONFIRMED',
    createdAt: '2026-11-01 10:15:20',
    buyer: {
      fullName: 'Nguyễn Văn An',
      email: 'an.nguyen@example.com',
      phone: '0909123456',
      needInvoice: false,
    },
  },
];

interface ApiResponse<T> { success: boolean; data: T; }

@Injectable({
  providedIn: 'root',
})
export class BookingService {
  private readonly bookingApi = '/api/v1/public/bookings';
  private holdSalesOrderId = 0;
  private holdToken = '';
  get holdSession(): { salesOrderId: number; holdToken: string } {
    return { salesOrderId: this.holdSalesOrderId, holdToken: this.holdToken };
  }
  // Booking state signals
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

  // Hold timer countdown (seconds)
  readonly holdSeconds = signal<number>(600); // 10 minutes
  private timerInterval: any = null;

  // Saved Orders in memory & localStorage
  readonly orders = signal<Order[]>(this.loadOrders());

  // Computed totals
  readonly ticketTotal = computed(() => {
    return this.selectedSeats().reduce((sum, s) => sum + s.price, 0);
  });

  readonly fnbTotal = computed(() => {
    return this.selectedCombos().reduce((sum, c) => sum + c.price * c.quantity, 0);
  });

  readonly subtotal = computed(() => {
    return this.ticketTotal() + this.fnbTotal();
  });

  readonly vatAmount = computed(() => {
    // Thuế giả lập tính trên giá trước thuế, tương ứng với backend.
    const taxableAmount = Math.max(0, this.subtotal() - this.discountAmount());
    return Math.round(taxableAmount * 0.08);
  });

  readonly grandTotal = computed(() => {
    return Math.max(0, this.subtotal() - this.discountAmount()) + this.vatAmount();
  });

  constructor(private readonly http: HttpClient, private readonly cinemaService: CinemaService) {
    // Watch for seat selection changes to start/stop timer
    effect(() => {
      const seats = this.selectedSeats();
      if (seats.length > 0 && !this.timerInterval) {
        this.startHoldTimer();
      } else if (seats.length === 0 && this.timerInterval) {
        this.stopHoldTimer();
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

  toggleSeat(seat: Seat): boolean {
    if (seat.status === 'BOOKED' || seat.seatStatus === 'SOLD') return false;

    const currentSelected = this.selectedSeats();
    const index = currentSelected.findIndex((s) => s.id === seat.id);

    if (index >= 0) {
      // Unselect
      this.selectedSeats.set(currentSelected.filter((s) => s.id !== seat.id));
      this.updateSeatStatusInMatrix(seat.id, 'AVAILABLE');
      return true;
    } else {
      // Max 8 seats per booking
      if (currentSelected.length >= 8) {
        alert('Quý khách chỉ có thể chọn tối đa 8 ghế cho mỗi lượt đặt.');
        return false;
      }
      this.selectedSeats.set([...currentSelected, { ...seat, status: 'SELECTED' }]);
      this.updateSeatStatusInMatrix(seat.id, 'SELECTED');
      if (seat.showSeatId != null && this.currentShowtime()?.backendScreeningId) {
        this.holdSeats([seat.showSeatId]).subscribe();
      }
      return true;
    }
  }

  private updateSeatStatusInMatrix(seatId: string, status: 'AVAILABLE' | 'SELECTED'): void {
    this.availableSeats.update((seats) =>
      seats.map((s) => (s.id === seatId ? {
        ...s,
        status,
        seatStatus: status === 'SELECTED' ? 'HELD' : 'AVAILABLE',
      } : s))
    );
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

  applyVoucher(code: string): { success: boolean; message: string } {
    const clean = code.trim().toUpperCase();
    if (!clean) {
      this.voucherCode.set('');
      this.discountAmount.set(0);
      this.voucherMessage.set('');
      return { success: false, message: 'Vui lòng nhập mã giảm giá' };
    }

    this.voucherCode.set('');
    this.discountAmount.set(0);
    this.voucherMessage.set('Mã ưu đãi chưa được kết nối với hệ thống thanh toán giả lập.');
    return { success: false, message: this.voucherMessage() };
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
          alert('Hết thời gian giữ ghế. Các ghế đang chọn sẽ được giải phóng.');
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
    const screeningId = this.currentShowtime()?.backendScreeningId;
    if (!screeningId) return throwError(() => new Error('Suất chiếu chưa được kết nối.'));
    const payload = { salesOrderId: this.holdSalesOrderId, holdToken: this.holdToken || null, showSeatIds: seatIds };
    return this.http.post<ApiResponse<HoldSeatsResponse>>(
      `${this.bookingApi}/hold?screeningId=${encodeURIComponent(screeningId)}`, payload
    ).pipe(
      map(({ data }) => {
        this.holdSalesOrderId = data.salesOrderId;
        this.holdToken = data.holdToken;
        this.availableSeats.update((seats) => seats.map((seat) => seatIds.includes(Number(seat.showSeatId))
          ? { ...seat, seatStatus: 'HELD', status: 'SELECTED', holdExpiresAt: data.expiresAt }
          : seat));
        return data;
      })
    );
  }

  releaseSeats(seatIds: number[]): Observable<string> {
    return this.http.post<ApiResponse<string>>(`${this.bookingApi}/release`, {
      salesOrderId: this.holdSalesOrderId, holdToken: this.holdToken, showSeatIds: seatIds,
    }).pipe(map(({ data }) => data));
  }

  payOrder(orderCode: string, method: PaymentMethod): Observable<PayOrderResponse> {
    const payload: PayOrderPayload = {
      provider: method,
      paymentMethod: method,
      idempotencyKey: `pay-${orderCode}`,
      successful: true,
    };
    return this.http.post<ApiResponse<PayOrderResponse>>(
      `${this.bookingApi}/orders/${encodeURIComponent(orderCode)}/pay`, payload
    ).pipe(
      map(({ data }) => data)
    );
  }

  getOrder(orderCode: string): Observable<Order | undefined> {
    return this.http.get<ApiResponse<CreateOrderResponse>>(
      `${this.bookingApi}/orders/${encodeURIComponent(orderCode)}`
    ).pipe(
      map(({ data }) => this.fromApiOrder(data))
    );
  }

  private createMockOrder(paymentMethod: PaymentMethod): Order {
    const movie = this.currentMovie();
    const cinema = this.currentCinema();
    const showtime = this.currentShowtime();
    const seats = this.selectedSeats();
    const combos = this.selectedCombos();
    const buyer = this.buyerInfo();

    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const orderCode = `TF261101${randomSuffix}`;
    const bookingCode = `TFM-${randomSuffix}-OK`;
    const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=THEFILM-${bookingCode}`;

    const newOrder: Order = {
      id: `ord-${Date.now()}`,
      orderCode,
      bookingCode,
      qrCodeUrl,
      movieTitle: movie?.title || 'Phim rạp TheFilm',
      movieRating: movie?.rating || 'T13',
      cinemaName: cinema?.name || 'THEFILM Cinema',
      cinemaAddress: cinema?.address || 'TP. Hồ Chí Minh',
      roomName: showtime?.roomName || 'Phòng chiếu',
      format: showtime?.format || '2D',
      showDate: showtime?.date || '2026-11-01',
      showTime: `${showtime?.startTime || '19:30'} - ${showtime?.endTime || '21:30'}`,
      seats: [...seats],
      combos: [...combos],
      ticketTotal: this.ticketTotal(),
      fnbTotal: this.fnbTotal(),
      discountTotal: this.discountAmount(),
      finalTotal: this.grandTotal(),
      vatRate: 0.08,
      vatAmount: this.vatAmount(),
      paymentMethod,
      status: 'CONFIRMED',
      createdAt: new Date().toLocaleString('vi-VN'),
      buyer: { ...buyer },
    };

    // Save order
    const updatedOrders = [newOrder, ...this.orders()];
    this.orders.set(updatedOrders);
    this.saveOrders(updatedOrders);

    // Stop timer & reset booking state
    this.stopHoldTimer();
    return newOrder;
  }

  private fromApiOrder(data: CreateOrderResponse): Order {
    const movie = this.currentMovie();
    const cinema = this.currentCinema();
    const showtime = this.currentShowtime();
    const fallback: Order = {
      id: data.orderCode,
      orderCode: data.orderCode,
      bookingCode: data.orderCode,
      qrCodeUrl: '',
      movieTitle: movie?.title || 'Phim rạp TheFilm',
      movieRating: movie?.rating || 'T13',
      cinemaName: cinema?.name || 'THEFILM Cinema',
      cinemaAddress: cinema?.address || 'TP. Hồ Chí Minh',
      roomName: showtime?.roomName || 'Phòng chiếu',
      format: showtime?.format || '2D',
      showDate: showtime?.date || '',
      showTime: `${showtime?.startTime || ''} - ${showtime?.endTime || ''}`,
      seats: [...this.selectedSeats()],
      combos: [...this.selectedCombos()],
      ticketTotal: 0,
      fnbTotal: this.fnbTotal(),
      discountTotal: 0,
      finalTotal: 0,
      vatRate: 0.08,
      vatAmount: 0,
      paymentMethod: 'VNPAY_QR',
      status: 'PENDING',
      createdAt: new Date().toISOString(),
      buyer: { ...this.buyerInfo() },
    };
    const order: Order = {
      ...fallback,
      id: data.orderCode,
      orderCode: data.orderCode,
      bookingCode: data.orderCode,
      qrCodeUrl: '',
      ticketTotal: Number(data.subtotal),
      discountTotal: Number(data.discount),
      vatAmount: Number(data.vat),
      finalTotal: Number(data.total),
      paymentExpiresAt: data.paymentExpiresAt,
      status: data.status as Order['status'],
      tickets: (data.tickets ?? []).map((ticket) => ({
        ticketNumber: ticket.ticketNumber,
        qrData: ticket.qrData,
        filmTitle: ticket.filmTitle,
        cinemaName: ticket.cinemaName,
        roomName: ticket.roomName,
        screeningStartsAt: ticket.screeningStartsAt,
        seatLabel: ticket.seatLabel,
        status: ticket.status,
      })),
    };
    const updatedOrders = [order, ...this.orders().filter((existing) => existing.orderCode !== order.orderCode)];
    this.orders.set(updatedOrders);
    this.saveOrders(updatedOrders);
    this.stopHoldTimer();
    return order;
  }

  getOrderById(id: string): Order | undefined {
    return this.orders().find((o) => o.id === id || o.orderCode === id || o.bookingCode === id);
  }

  private loadOrders(): Order[] {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const stored = localStorage.getItem('thefilm_orders');
        if (stored) {
          return JSON.parse(stored);
        }
      }
    } catch (e) {
      console.warn('Cannot load orders from localStorage', e);
    }
    return [...MOCK_INITIAL_ORDERS];
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
