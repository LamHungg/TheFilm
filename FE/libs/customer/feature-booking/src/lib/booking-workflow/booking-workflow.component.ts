import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BadgeComponent } from '@the-film/shared/ui';
import { formatVND, formatCountdown } from '@the-film/shared/util';
import { catchError, finalize, of, switchMap } from 'rxjs';
import {
  Movie,
  Cinema,
  Showtime,
  Seat,
  FnbCombo,
  BuyerInfo,
  Order,
  PaymentMethod,
  CinemaService,
  BookingService,
  CreateOrderPayload,
  Ticket,
} from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-booking-workflow',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, BadgeComponent],
  templateUrl: './booking-workflow.component.html',
  styleUrl: './booking-workflow.component.scss',
})
export class BookingWorkflowComponent implements OnInit, OnDestroy {
  step = 1; // 1: Chọn ghế, 2: Bắp nước, 3: Thanh toán, 4: Thành công & QR

  movie: Movie | null = null;
  cinema: Cinema | null = null;
  showtime: Showtime | null = null;

  availableSeats: Seat[] = [];
  seatRows: string[] = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J'];

  fnbItems: FnbCombo[] = [];

  buyer: BuyerInfo = {
    fullName: '',
    email: '',
    phone: '',
    needInvoice: false,
    companyName: '',
    taxCode: '',
    companyAddress: '',
  };

  voucherInput = '';
  selectedPaymentMethod: PaymentMethod = 'VNPAY_QR';
  isProcessingPayment = false;
  confirmedOrder: Order | null = null;
  holdExpiresAt: string | null = null;
  holdSecondsRemaining = 0;
  seatMessage = '';
  showHoldExpired = false;
  seatBusy = false;
  seatsOnline = false;
  private seatPollTimer: ReturnType<typeof setInterval> | null = null;
  private seatRequestVersion = 0;
  private seatLoadInFlight = false;
  private pendingOrder: Order | null = null;
  private holdTimer: ReturnType<typeof setInterval> | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private changeDetector: ChangeDetectorRef,
    private cinemaService: CinemaService,
    private bookingService: BookingService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const showtimeId = params.get('showtimeId') || '';
      this.initWorkflow(showtimeId);
    });

    this.cinemaService.getFnbs().subscribe((fnbs) => {
      this.fnbItems = fnbs;
    });
  }

  ngOnDestroy(): void {
    this.stopHoldCountdown();
    if (this.seatPollTimer) clearInterval(this.seatPollTimer);
    if (this.step < 4) this.bookingService.stopHoldTimer();
  }

  private initWorkflow(showtimeId: string): void {
    this.pendingOrder = null;
    this.seatRequestVersion++;
    this.cinemaService.getShowtimeById(showtimeId).subscribe((st) => {
      if (!st) {
        this.router.navigate(['/movies']);
        return;
      }
      this.showtime = st;

      this.cinemaService.getMovieById(st.movieId).subscribe((m) => {
        this.movie = m || null;
      });

      this.cinemaService.getCinemaById(st.cinemaId).subscribe((c) => {
        this.cinema = c || null;
      });

      if (this.movie && this.cinema) {
        this.bookingService.initShowtime(this.movie, this.cinema, st);
        this.loadSeats();
      } else {
        // Fallback if accessed directly by URL
        this.cinemaService.getMovies().subscribe((movies) => {
          this.movie = movies.find((m) => m.id === st.movieId) || movies[0];
          this.cinemaService.getCinemas().subscribe((cinemas) => {
            this.cinema = cinemas.find((c) => c.id === st.cinemaId) || cinemas[0];
            this.bookingService.initShowtime(this.movie!, this.cinema!, st);
            this.loadSeats();
          });
        });
      }
      if (this.seatPollTimer) clearInterval(this.seatPollTimer);
      this.seatPollTimer = setInterval(() => { if (!this.seatBusy && this.step < 4) this.loadSeats(); }, 2000);
      this.changeDetector.markForCheck();
    });
  }

  // Getters connected to BookingService signals
  get selectedSeats(): Seat[] {
    return this.bookingService.selectedSeats();
  }

  get selectedCombos(): FnbCombo[] {
    return this.bookingService.selectedCombos();
  }

  get ticketTotal(): number {
    return this.bookingService.ticketTotal();
  }

  get fnbTotal(): number {
    return this.bookingService.fnbTotal();
  }

  get vatAmount(): number {
    return this.bookingService.vatAmount();
  }

  get discountAmount(): number {
    return this.bookingService.discountAmount();
  }

  get grandTotal(): number {
    return this.bookingService.grandTotal();
  }

  get voucherMessage(): string {
    return this.bookingService.voucherMessage();
  }

  get holdSeconds(): number {
    return this.holdSecondsRemaining;
  }

  get formattedCountdown(): string {
    return formatCountdown(this.holdSeconds);
  }

  getSeatsByRow(row: string): Seat[] {
    return this.availableSeats.filter((s) => s.row === row);
  }

  isSeatSelected(seat: Seat): boolean {
    return this.selectedSeats.some((s) => s.id === seat.id);
  }

  onSeatClick(seat: Seat): void {
    this.seatMessage = '';
    if (this.seatBusy || !this.seatsOnline) return;
    if (seat.seatStatus === 'SOLD' || seat.status === 'BOOKED') return;
    this.seatRequestVersion++;
    const selected = this.selectedSeats;
    const isSelected = selected.some((item) => item.id === seat.id);
    if (isSelected) {
      if (seat.showSeatId == null) return;
      this.seatBusy = true;
      this.bookingService.releaseSeats([seat.showSeatId]).pipe(finalize(() => { this.seatBusy = false; this.changeDetector.markForCheck(); })).subscribe({
        next: () => {
          this.bookingService.selectedSeats.set(this.selectedSeats.filter((item) => item.id !== seat.id));
          this.startHoldCountdownFromSelection();
          this.loadSeats();
        },
        error: () => { this.seatMessage = 'Không thể bỏ giữ ghế. Vui lòng thử lại.'; this.loadSeats(); }
      });
      return;
    }
    if (seat.seatStatus === 'HELD') {
      this.seatMessage = 'Ghế này đang được giữ bởi khách hàng khác. Sơ đồ ghế đã được cập nhật.';
      this.loadSeats();
      return;
    }
    if (selected.length >= 8) {
      this.seatMessage = 'Quý khách chỉ có thể chọn tối đa 8 ghế cho mỗi lượt đặt.';
      return;
    }
    const holdSeatLocally = (holdExpiresAt: string) => {
      const heldSeat = {
        ...seat,
        seatStatus: 'HELD' as const,
        status: 'SELECTED' as const,
        holdExpiresAt,
      };
      this.bookingService.availableSeats.update((seats) => seats.map((item) => item.id === seat.id ? heldSeat : item));
      this.bookingService.selectedSeats.set([...this.selectedSeats, heldSeat]);
      this.availableSeats = this.bookingService.availableSeats();
      this.holdExpiresAt = this.selectedSeats
        .map((item) => item.holdExpiresAt)
        .filter((value): value is string => Boolean(value))
        .sort((left, right) => new Date(left).getTime() - new Date(right).getTime())[0] || holdExpiresAt;
      this.updateHoldCountdown();
      this.startHoldCountdown();
    };

    if (seat.showSeatId == null) { this.seatMessage = 'Ghế chưa được kết nối với hệ thống đặt vé.'; return; }
    this.seatBusy = true;
    this.bookingService.holdSeats([Number(seat.showSeatId)]).pipe(finalize(() => { this.seatBusy = false; this.changeDetector.markForCheck(); })).subscribe({
      next: (hold) => holdSeatLocally(hold.expiresAt),
      error: (error) => {
        this.seatMessage = error.status === 409 ? 'Ghế vừa được người khác giữ. Sơ đồ ghế đã được cập nhật.' : 'Không thể giữ ghế. Vui lòng kiểm tra kết nối và thử lại.';
        this.loadSeats();
      }
    });
  }

  getFnbQty(item: FnbCombo): number {
    const found = this.selectedCombos.find((c) => c.id === item.id);
    return found ? found.quantity : 0;
  }

  updateFnb(item: FnbCombo, delta: number): void {
    this.bookingService.updateComboQuantity(item, delta);
  }

  applyVoucher(): void {
    this.bookingService.applyVoucher(this.voucherInput);
  }

  removeVoucher(): void {
    this.voucherInput = '';
    this.bookingService.applyVoucher('');
  }

  nextStep(): void {
    if (this.step === 1 && (this.seatBusy || !this.seatsOnline)) { this.seatMessage = 'Vui lòng chờ kết nối và xác nhận giữ ghế.'; return; }
    if (this.step === 1 && this.selectedSeats.length === 0) {
      alert('Vui lòng chọn ít nhất 1 ghế để tiếp tục.');
      return;
    }
    if (this.step === 1 && (this.holdSecondsRemaining <= 0 || !this.holdExpiresAt)) {
      this.seatMessage = 'Vui lòng giữ ghế thành công trước khi tiếp tục.';
      return;
    }
    this.step++;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  prevStep(): void {
    if (this.pendingOrder) { this.paymentMessage = 'Đơn đã được tạo. Vui lòng hoàn tất thanh toán giả lập hoặc chờ đơn hết hạn.'; return; }
    if (this.step > 1) {
      this.step--;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  submitPayment(): void {
    if (!this.buyer.fullName.trim() || !this.buyer.phone.trim() || !this.buyer.email.trim()
      || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.buyer.email.trim())
      || (this.buyer.needInvoice && (!this.buyer.companyName?.trim() || !this.buyer.taxCode?.trim() || !this.buyer.companyAddress?.trim()))) {
      this.paymentMessage = 'Vui lòng kiểm tra họ tên, số điện thoại, email và thông tin hóa đơn.';
      return;
    }
    if (!this.holdExpiresAt || this.holdSecondsRemaining <= 0) {
      this.paymentMessage = 'Thời gian giữ ghế đã hết. Vui lòng chọn lại ghế.';
      this.step = 1;
      return;
    }
    this.paymentMessage = '';
    const screeningId = this.showtime?.backendScreeningId;
    if (!screeningId || !Number.isSafeInteger(screeningId) || screeningId <= 0) {
      this.paymentMessage = 'Không xác định được mã suất chiếu. Vui lòng tải lại suất chiếu.';
      return;
    }
    this.bookingService.updateBuyerInfo(this.buyer);
    this.isProcessingPayment = true;
    const payload: CreateOrderPayload = {
      screeningId,
      ...this.bookingService.holdSession,
      showSeatIds: this.selectedSeats.map((seat) => Number(seat.showSeatId)).filter(Number.isFinite),
      idempotencyKey: `booking-${this.showtime?.id}-${this.selectedSeats.map((seat) => seat.showSeatId).join('-')}-${Date.now()}`,
      voucherCode: this.bookingService.voucherCode() || undefined,
      guestName: this.buyer.fullName.trim(),
      guestEmail: this.buyer.email.trim(),
      guestPhone: this.buyer.phone.trim(),
      fnbItems: this.selectedCombos.map((combo) => ({
        productId: Number(combo.id.match(/\d+/)?.[0]),
        productName: combo.name,
        productCode: combo.id,
        quantity: combo.quantity,
        unitPrice: combo.price,
      })).filter((item) => Number.isFinite(item.productId)),
    };
    (this.pendingOrder ? of(this.pendingOrder) : this.bookingService.createOrder(payload)).pipe(
      switchMap((order) => { this.pendingOrder = order; return of(order); }),
      switchMap((order) => this.bookingService.payOrder(order.orderCode, this.selectedPaymentMethod).pipe(
        switchMap((payment) => {
          if (!['SUCCEEDED', 'SUCCESS'].includes(payment.paymentStatus.toUpperCase())) {
            this.paymentMessage = 'Thanh toán chưa thành công. Vui lòng thử lại.';
            return of(order);
          }
          return this.bookingService.getOrder(order.orderCode).pipe(switchMap((updated) => of(updated || order)));
        })
      )),
      catchError(() => {
        this.paymentMessage = 'Không thể hoàn tất thanh toán. Vui lòng kiểm tra kết nối và thử lại.';
        return of(null);
      }),
      finalize(() => { this.isProcessingPayment = false; this.changeDetector.markForCheck(); })
    ).subscribe((order) => {
      if (!order || this.paymentMessage) return;
      this.confirmedOrder = order;
      this.stopHoldCountdown();
      this.step = 4;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  paymentMessage = '';

  get tickets(): Ticket[] {
    return this.confirmedOrder?.tickets?.length ? this.confirmedOrder.tickets : [];
  }

  ticketQrSource(ticket: Ticket): string {
    const qrData = ticket.qrData || ticket.qrTokenHash || ticket.ticketNumber;
    return `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(qrData)}`;
  }

  private loadSeats(): void {
    const screeningId = this.showtime?.backendScreeningId;
    if (!screeningId) { this.seatsOnline = false; this.seatMessage = 'Suất chiếu này chưa kết nối với hệ thống đặt vé.'; return; }
    if (this.seatLoadInFlight) return;
    this.seatLoadInFlight = true;
    const version = this.seatRequestVersion;
    this.bookingService.fetchSeats(screeningId).pipe(finalize(() => { this.seatLoadInFlight = false; this.changeDetector.markForCheck(); })).subscribe({
      next: (seats) => {
        if (version !== this.seatRequestVersion) return;
        this.seatsOnline = true;
        const selectedIds = new Set(this.selectedSeats.map((seat) => seat.id));
        const stillHeld = this.selectedSeats.filter((selected) => seats.some((seat) => seat.id === selected.id && seat.seatStatus === 'HELD'));
        if (stillHeld.length !== this.selectedSeats.length) {
          this.bookingService.selectedSeats.set(stillHeld);
          this.seatMessage = 'Một ghế không còn được giữ. Vui lòng chọn lại.';
          this.startHoldCountdownFromSelection();
        }
        this.availableSeats = seats.map((seat) => selectedIds.has(seat.id) && seat.seatStatus === 'HELD' ? { ...seat, status: 'SELECTED' as const } : seat);
        this.seatRows = [...new Set(seats.map((seat) => seat.row))];
      },
      error: () => { if (version === this.seatRequestVersion) { this.seatsOnline = false; this.seatMessage = 'Không tải được sơ đồ ghế. Vui lòng kiểm tra kết nối backend.'; } }
    });
  }

  private updateHoldCountdown(): void {
    const expiresAt = this.holdExpiresAt ? new Date(this.holdExpiresAt).getTime() : 0;
    this.holdSecondsRemaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
  }

  private startHoldCountdown(): void {
    this.stopHoldCountdown();
    this.holdTimer = setInterval(() => {
      // BookingService has a legacy timer with a blocking alert; this countdown
      // uses backend holdExpiresAt and owns the expiry experience.
      this.bookingService.stopHoldTimer();
      this.updateHoldCountdown();
      if (this.holdSecondsRemaining === 0) this.onHoldExpired();
      this.changeDetector.markForCheck();
    }, 1000);
  }

  private startHoldCountdownFromSelection(): void {
    const remainingExpiry = this.selectedSeats
      .map((seat) => seat.holdExpiresAt)
      .filter((value): value is string => Boolean(value))
      .sort((left, right) => new Date(left).getTime() - new Date(right).getTime())[0] || null;
    this.holdExpiresAt = remainingExpiry;
    if (remainingExpiry) this.startHoldCountdown();
    else this.stopHoldCountdown();
  }

  private onHoldExpired(): void {
    if (!this.selectedSeats.length) return;
    this.stopHoldCountdown();
    const expiredSeatIds = new Set(this.selectedSeats.map((seat) => seat.id));
    this.bookingService.selectedSeats.set([]);
    this.bookingService.stopHoldTimer();
    this.bookingService.availableSeats.update((seats) => seats.map((seat) => expiredSeatIds.has(seat.id)
      ? { ...seat, seatStatus: 'AVAILABLE', status: 'AVAILABLE', holdExpiresAt: null }
      : seat));
    this.holdExpiresAt = null;
    this.showHoldExpired = true;
    this.loadSeats();
  }

  private stopHoldCountdown(): void {
    if (this.holdTimer) clearInterval(this.holdTimer);
    this.holdTimer = null;
  }

  getSeatCodes(seats: Seat[]): string {
    return seats.map((s) => s.code).join(', ');
  }

  getCombosSummary(combos: FnbCombo[]): string {
    return combos.map((c) => `${c.name} (x${c.quantity})`).join(', ');
  }

  formatMoney(amount: number): string {
    return formatVND(amount);
  }
}
