import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BadgeComponent } from '@the-film/shared/ui';
import { formatVND, formatCountdown } from '@the-film/shared/util';
import { catchError, finalize, forkJoin, of, switchMap } from 'rxjs';
import {
  AuthService,
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
  showPaymentModal = false;
  showTimePicker = false;
  showExitConfirm = false;
  availableShowtimesForMovie: Showtime[] = [];
  private seatPollTimer: ReturnType<typeof setInterval> | null = null;
  private seatRequestVersion = 0;
  private seatLoadInFlight = false;
  pendingOrder: Order | null = null;
  showQrPaymentModal = false;
  isConfirmingPayment = false;
  copiedField = '';
  private copyTimer: ReturnType<typeof setTimeout> | null = null;
  private holdTimer: ReturnType<typeof setInterval> | null = null;
  private paymentPollTimer: ReturnType<typeof setInterval> | null = null;

  readonly bankName = 'MBBank - Ngân hàng TMCP Quân Đội';
  readonly bankCode = 'MB';
  readonly bankAccountNo = '0567846199999';
  readonly bankAccountName = 'DANG LAM HUNG';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private changeDetector: ChangeDetectorRef,
    private cinemaService: CinemaService,
    private bookingService: BookingService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user) {
      if (!this.buyer.fullName && user.displayName) this.buyer.fullName = user.displayName;
      if (!this.buyer.email && user.email) this.buyer.email = user.email;
      if (!this.buyer.phone && user.phone) this.buyer.phone = user.phone;
    }

    this.route.paramMap.subscribe((params) => {
      const showtimeId = params.get('showtimeId') || '';
      this.initWorkflow(showtimeId);
    });

    this.cinemaService.getFnbs().subscribe((fnbs) => {
      this.fnbItems = fnbs;
      this.changeDetector.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.stopHoldCountdown();
    this.stopPaymentPolling();
    if (this.seatPollTimer) clearInterval(this.seatPollTimer);
    if (this.step < 4) this.bookingService.stopHoldTimer();
  }

  private initWorkflow(showtimeId: string): void {
    this.pendingOrder = null;
    this.availableSeats = [];
    this.seatMessage = '';
    this.seatRequestVersion++;
    this.cinemaService.getShowtimeById(showtimeId).subscribe((st) => {
      if (!st) {
        this.router.navigate(['/movies']);
        return;
      }
      this.showtime = st;
      this.showTimePicker = false;
      this.showPaymentModal = false;
      this.showExitConfirm = false;

      this.cinemaService.getShowtimes(st.movieId, st.cinemaId, st.date).subscribe((list) => {
        this.availableShowtimesForMovie = list;
        this.changeDetector.markForCheck();
      });

      forkJoin({
        movie: this.cinemaService.getMovieById(st.movieId),
        cinema: this.cinemaService.getCinemaById(st.cinemaId),
      }).subscribe(({ movie, cinema }) => {
        this.movie = movie || null;
        this.cinema = cinema || null;
        if (!this.movie || !this.cinema) {
          this.router.navigate(['/movies']);
          return;
        }
        this.bookingService.initShowtime(this.movie, this.cinema, st);
        this.loadSeats();

        if (this.seatPollTimer) clearInterval(this.seatPollTimer);
        this.seatPollTimer = setInterval(() => {
          if (!this.seatBusy && this.step < 4) this.loadSeats();
        }, 2000);
        this.changeDetector.markForCheck();
      });
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

    if (seat.showSeatId == null) {
      this.seatMessage = 'Dữ liệu ghế chưa đồng bộ với máy chủ.';
      return;
    }

    if (isSelected) {
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

    this.seatBusy = true;
    this.bookingService.holdSeats([Number(seat.showSeatId)]).pipe(finalize(() => { this.seatBusy = false; this.changeDetector.markForCheck(); })).subscribe({
      next: (hold) => holdSeatLocally(hold.expiresAt),
      error: (error) => {
        this.seatMessage =
          error.status === 409
            ? 'Ghế vừa được người khác giữ. Sơ đồ ghế đã được cập nhật.'
            : error?.error?.error?.message ||
              error?.error?.message ||
              'Không thể giữ ghế. Vui lòng kiểm tra kết nối và thử lại.';
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
    if (this.showQrPaymentModal) {
      this.showQrPaymentModal = false;
      return;
    }
    if (this.step > 1) {
      this.step--;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  get vietQrUrl(): string {
    if (!this.pendingOrder) return '';
    const amount = this.pendingOrder.finalTotal || this.grandTotal;
    const memo = this.pendingOrder.orderCode;
    return `https://img.vietqr.io/image/MB-0567846199999-qr_only.png?amount=${amount}&addInfo=${encodeURIComponent(memo)}&accountName=DANG%20LAM%20HUNG`;
  }

  onQrImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (this.pendingOrder && !img.src.includes('api.qrserver.com')) {
      const amount = this.pendingOrder.finalTotal || this.grandTotal;
      const memo = this.pendingOrder.orderCode;
      const fallbackData = `2|99|0567846199999|DANG LAM HUNG||0|0|${amount}|${memo}|transfer_p2p`;
      img.src = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(fallbackData)}`;
    }
  }

  copyPaymentField(text: string, field: string): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(text);
    }
    this.copiedField = field;
    if (this.copyTimer) clearTimeout(this.copyTimer);
    this.copyTimer = setTimeout(() => {
      this.copiedField = '';
      this.changeDetector.markForCheck();
    }, 2500);
  }

  proceedToPayment(): void {
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
    const screeningId = this.showtime?.backendScreeningId ?? Number(this.showtime?.id);
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

    const orderReq$ = this.pendingOrder ? of(this.pendingOrder) : this.bookingService.createOrder(payload);

    orderReq$.pipe(
      finalize(() => {
        this.isProcessingPayment = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: (order) => {
        this.pendingOrder = order;
        this.showQrPaymentModal = true;
        this.startPaymentPolling();
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.paymentMessage =
          err?.error?.error?.message ||
          err?.error?.message ||
          'Không thể khởi tạo đơn hàng. Vui lòng kiểm tra kết nối và thử lại.';
      }
    });
  }

  submitPayment(): void {
    this.proceedToPayment();
  }

  startPaymentPolling(): void {
    this.stopPaymentPolling();
    this.paymentPollTimer = setInterval(() => {
      if (!this.pendingOrder || !this.showQrPaymentModal) {
        this.stopPaymentPolling();
        return;
      }
      this.bookingService.getOrder(this.pendingOrder.orderCode).subscribe({
        next: (order) => {
          if (order && (order.status === 'ISSUED' || (order.status as string) === 'PAID')) {
            this.handlePaymentSuccessful(order);
          }
        },
        error: () => {
          // ignore transient poll error
        }
      });
    }, 2500);
  }

  stopPaymentPolling(): void {
    if (this.paymentPollTimer) {
      clearInterval(this.paymentPollTimer);
      this.paymentPollTimer = null;
    }
  }

  private handlePaymentSuccessful(order: Order): void {
    this.stopPaymentPolling();
    this.confirmedOrder = order;
    this.showQrPaymentModal = false;
    this.pendingOrder = null;
    this.stopHoldCountdown();
    this.step = 4;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.changeDetector.markForCheck();
  }

  confirmQrPayment(): void {
    if (!this.pendingOrder) return;
    this.isConfirmingPayment = true;
    this.paymentMessage = '';
    this.bookingService.getOrder(this.pendingOrder.orderCode).pipe(
      finalize(() => {
        this.isConfirmingPayment = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: (order) => {
        if (order && (order.status === 'ISSUED' || (order.status as string) === 'PAID')) {
          this.handlePaymentSuccessful(order);
        } else {
          this.paymentMessage = 'Hệ thống chưa nhận được tiền từ ngân hàng cho đơn ' + this.pendingOrder!.orderCode + '. Nếu bạn vừa chuyển khoản, vui lòng đợi 5-10 giây để ngân hàng gửi tín hiệu Webhook.';
        }
      },
      error: () => {
        this.paymentMessage = 'Chưa kiểm tra được thanh toán. Vui lòng thử lại sau giây lát.';
      }
    });
  }

  simulateTransfer(): void {
    if (!this.pendingOrder) return;
    this.isConfirmingPayment = true;
    this.paymentMessage = 'Đang kích hoạt giả lập nhận tiền Webhook...';
    this.bookingService.simulateWebhookPayment(this.pendingOrder.orderCode).pipe(
      finalize(() => {
        this.isConfirmingPayment = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.bookingService.getOrder(this.pendingOrder!.orderCode).subscribe((ord) => {
          if (ord) this.handlePaymentSuccessful(ord);
        });
      },
      error: (err) => {
        this.paymentMessage = 'Lỗi giả lập: ' + (err?.error?.message || err?.message || 'Không thành công');
      }
    });
  }

  closeQrModal(): void {
    this.stopPaymentPolling();
    this.showQrPaymentModal = false;
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
    const screeningId = this.showtime?.backendScreeningId ?? Number(this.showtime?.id);
    if (!screeningId || !Number.isSafeInteger(screeningId) || screeningId <= 0) {
      this.seatsOnline = false;
      this.seatMessage = 'Suất chiếu không hợp lệ.';
      return;
    }
    if (this.seatLoadInFlight) return;
    this.seatLoadInFlight = true;
    const version = this.seatRequestVersion;
    this.bookingService.fetchSeats(screeningId).pipe(finalize(() => { this.seatLoadInFlight = false; this.changeDetector.markForCheck(); })).subscribe({
      next: (seats) => {
        if (version !== this.seatRequestVersion) return;
        this.seatsOnline = true;
        if (this.showtime && seats.length > 0) {
          const priceStandard = seats.find((s) => s.type === 'STANDARD')?.price ?? 0;
          const priceVip = seats.find((s) => s.type === 'VIP')?.price ?? priceStandard;
          const priceCouple = seats.find((s) => s.type === 'COUPLE')?.price ?? priceVip;
          this.showtime = { ...this.showtime, priceStandard, priceVip, priceCouple };
          this.bookingService.currentShowtime.set(this.showtime);
        }
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
    if (this.pendingOrder && this.showQrPaymentModal) {
      this.paymentMessage = 'Thời gian giữ ghế đã hết. Nếu bạn đã chuyển khoản, đừng chuyển lần nữa; hệ thống vẫn đang kiểm tra mã đơn ' + this.pendingOrder.orderCode + '.';
      this.showHoldExpired = false;
    } else {
      this.showHoldExpired = true;
    }
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

  get selectedPaymentLabel(): string {
    switch (this.selectedPaymentMethod) {
      case 'VNPAY_QR':
        return 'VNPAY-QR';
      case 'E_WALLET':
        return 'Ví điện tử (MoMo / ZaloPay)';
      case 'DOMESTIC_CARD':
        return 'Thẻ ATM / Napas nội địa';
      case 'INTERNATIONAL_CARD':
        return 'Thẻ quốc tế (Visa / MasterCard)';
      default:
        return 'VNPAY-QR';
    }
  }

  selectPaymentFromModal(method: PaymentMethod): void {
    this.selectedPaymentMethod = method;
    this.showPaymentModal = false;
  }

  switchShowtime(st: Showtime): void {
    this.showTimePicker = false;
    if (st.id !== this.showtime?.id) {
      this.router.navigate(['/booking', st.id]);
    }
  }

  onStep1Back(): void {
    if (this.selectedSeats.length > 0 && this.step === 1) {
      this.showExitConfirm = true;
      return;
    }
    this.navigateBackToMovie();
  }

  confirmExitBooking(): void {
    this.showExitConfirm = false;
    this.navigateBackToMovie();
  }

  private navigateBackToMovie(): void {
    const slug = this.movie?.slug;
    if (slug) {
      this.router.navigate(['/movies', slug]);
    } else {
      this.router.navigate(['/movies']);
    }
  }

  isAisleGap(rowName: string, index: number): boolean {
    const rowSeats = this.getSeatsByRow(rowName);
    return rowSeats.length >= 6 && index === Math.ceil(rowSeats.length / 2);
  }

  formatMoney(amount: number): string {
    return formatVND(amount);
  }
}
