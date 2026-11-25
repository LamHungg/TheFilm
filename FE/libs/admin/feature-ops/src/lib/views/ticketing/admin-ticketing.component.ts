import { ChangeDetectorRef, Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import {
  AdminOpsService,
  CinemaAdminDto,
  CounterSaleResponse,
  FnbProductDto,
  ScreeningAdminDto,
  ScreeningSeatDto,
  TicketAdminDto,
  TicketScanResult,
} from '../../services/admin-ops.service';

type CounterPayMethod = 'CASH' | 'QR';

@Component({
  selector: 'thefilm-admin-ticketing',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-ticketing.component.html',
  styleUrl: './admin-ticketing.component.scss',
})
export class AdminTicketingComponent implements OnInit, OnDestroy {
  private readonly adminOps = inject(AdminOpsService);
  private readonly cdr = inject(ChangeDetectorRef);

  activeTab = 0;
  cinemas: CinemaAdminDto[] = [];
  selectedCinemaId: number | null = null;
  screenings: ScreeningAdminDto[] = [];
  seats: ScreeningSeatDto[] = [];
  selectedFilmId: number | null = null;
  selectedScreeningId: number | null = null;
  selectedSeats = new Set<number>();
  fnbProducts: FnbProductDto[] = [];
  selectedFnb = new Map<number, number>();
  loadingFnb = false;
  seatBusy = false;
  private holdOrderId: number | null = null;
  private holdToken: string | null = null;
  private seatPollTimer: ReturnType<typeof setInterval> | null = null;
  private seatVersion = 0;
  private readonly holdExpiries = new Map<number, number>();
  paymentMethod: CounterPayMethod = 'CASH';
  cashReceived = 0;
  cashReceivedText = '';
  pendingQrSale: CounterSaleResponse | null = null;
  checkingCounterPayment = false;
  counterPaymentMessage = '';
  private counterPaymentPollTimer: ReturnType<typeof setInterval> | null = null;
  loadingCinemas = true;
  loadingCatalog = false;
  loadingSeats = false;
  submittingSale = false;
  errorMessage = '';
  noticeMessage = '';
  idempotencyKey = this.newIdempotencyKey();
  completedSale: CounterSaleResponse | null = null;

  ticketCodeInput = '';
  isChecking = false;
  scanResult: TicketScanResult | null = null;
  lookupTerm = '';
  lookupLoading = false;
  lookupDone = false;
  lookupError = '';
  foundTickets: TicketAdminDto[] = [];

  ngOnInit(): void {
    this.loadCinemas();
    this.seatPollTimer = setInterval(() => this.refreshSeatStatuses(), 2000);
  }

  ngOnDestroy(): void {
    if (this.seatPollTimer) clearInterval(this.seatPollTimer);
    this.stopCounterPaymentPolling();
    this.releaseSelection();
  }

  private releaseSelection(): void {
    this.seatVersion++;
    const ids = [...this.selectedSeats];
    if (!this.pendingQrSale && ids.length && this.selectedScreeningId && this.holdOrderId && this.holdToken) {
      this.adminOps.releaseCounterSeats(this.selectedScreeningId, this.holdOrderId, this.holdToken, ids).subscribe({ error: () => {} });
    }
    this.selectedSeats.clear();
    this.holdExpiries.clear();
    this.holdOrderId = null;
    this.holdToken = null;
  }

  private refreshSeatStatuses(): void {
    const screeningId = this.selectedScreeningId;
    const version = this.seatVersion;
    if (!screeningId || this.loadingSeats || this.loadingCatalog || this.seatBusy || this.submittingSale) return;
    this.adminOps.getTicketingSeats(screeningId).subscribe({ next: (seats) => {
      if (this.selectedScreeningId !== screeningId || this.seatVersion !== version || this.seatBusy || this.submittingSale) return;
      if (this.pendingQrSale) {
        this.seats = seats;
        this.cdr.markForCheck();
        return;
      }
      for (const id of this.selectedSeats) {
        if ((this.holdExpiries.get(id) ?? 0) <= Date.now() || !seats.some((seat) => seat.showSeatId === id && seat.status === 'HELD')) {
          this.selectedSeats.delete(id);
          this.holdExpiries.delete(id);
          this.errorMessage = 'Ghế giữ đã hết hạn hoặc không còn khả dụng. Vui lòng chọn lại.';
        }
      }
      if (!this.selectedSeats.size) { this.holdOrderId = null; this.holdToken = null; }
      this.seats = seats;
      this.cdr.markForCheck();
    }, error: () => {} });
  }

  loadCinemas(): void {
    this.loadingCinemas = true;
    this.errorMessage = '';
    this.adminOps.getTicketingCinemas().subscribe({
      next: (cinemas) => {
        this.cinemas = cinemas.filter((cinema) => cinema.status === 'ACTIVE');
        this.selectedCinemaId = this.cinemas[0]?.cinemaId ?? null;
        this.loadingCinemas = false;
        this.loadCatalog();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loadingCinemas = false;
        this.cinemas = [];
        this.selectedCinemaId = null;
        this.screenings = [];
        this.seats = [];
        this.errorMessage = err?.error?.error?.message || (err?.name === 'TimeoutError'
          ? 'Tải danh sách rạp quá thời gian chờ. Hãy thử tải lại.'
          : 'Không tải được danh sách rạp được phân quyền. Hãy thử tải lại.');
        this.cdr.markForCheck();
      },
    });
  }

  refreshCatalog(): void {
    if (this.loadingCinemas || this.loadingCatalog || this.submittingSale) return;
    this.releaseSelection();
    if (!this.cinemas.length) this.loadCinemas();
    else this.loadCatalog();
  }

  get availableScreenings(): ScreeningAdminDto[] {
    const now = Date.now();
    return this.screenings.filter((screening) =>
      screening.filmId === this.selectedFilmId &&
      Date.parse(screening.startsAt) > now && screening.status === 'ON_SALE');
  }

  get filmsForSale(): Array<{ filmId: number; titleLocal: string }> {
    const available = this.screenings.filter((screening) =>
      Date.parse(screening.startsAt) > Date.now() && screening.status === 'ON_SALE');
    return [...new Map(available.map((screening) => [screening.filmId,
      { filmId: screening.filmId, titleLocal: screening.filmTitle || `Phim #${screening.filmId}` }])).values()];
  }

  get pendingPublicationCount(): number {
    return this.screenings.filter((screening) =>
      Date.parse(screening.startsAt) > Date.now() && screening.status === 'SCHEDULED').length;
  }

  get seatRows(): Array<{ label: string; seats: ScreeningSeatDto[] }> {
    const rows = new Map<string, ScreeningSeatDto[]>();
    for (const seat of this.seats) {
      const label = seat.rowLabel || seat.seatCode.match(/^[A-Z]+/i)?.[0] || '?';
      rows.set(label, [...(rows.get(label) || []), seat]);
    }
    return [...rows.entries()].map(([label, seats]) => ({
      label,
      seats: seats.sort((a, b) => a.seatCode.localeCompare(b.seatCode, undefined, { numeric: true })),
    }));
  }

  get maxSeatColumns(): number {
    return Math.max(1, ...this.seatRows.map((row) => row.seats.length));
  }

  get selectedSeatItems(): ScreeningSeatDto[] {
    return this.seats.filter((seat) => this.selectedSeats.has(seat.showSeatId));
  }

  get selectedFnbItems(): Array<{ product: FnbProductDto; quantity: number }> {
    return [...this.selectedFnb.entries()].flatMap(([productId, quantity]) => {
      const product = this.fnbProducts.find((item) => item.productId === productId);
      return product && quantity > 0 ? [{ product, quantity }] : [];
    });
  }

  get selectedFnbCount(): number {
    return this.selectedFnbItems.reduce((sum, item) => sum + item.quantity, 0);
  }

  get unpricedAvailableSeatCount(): number {
    return this.seats.filter((seat) => seat.status === 'AVAILABLE' && !(Number(seat.basePrice) > 0)).length;
  }

  get subtotalPrice(): number {
    return this.selectedSeatItems.reduce((sum, seat) => sum + (seat.basePrice || 0), 0) +
      this.selectedFnbItems.reduce((sum, item) => sum + Number(item.product.unitPrice || 0) * item.quantity, 0);
  }

  get vatAmount(): number {
    const seatVat = this.selectedSeatItems.reduce((sum, seat) => sum + Math.round(((seat.basePrice || 0) * 0.08) * 100) / 100, 0);
    const fnbVat = this.selectedFnbItems.reduce((sum, item) =>
      sum + Math.round(Number(item.product.unitPrice || 0) * item.quantity * 0.08 * 100) / 100, 0);
    return seatVat + fnbVat;
  }

  get totalPrice(): number { return this.subtotalPrice + this.vatAmount; }

  get changeDue(): number { return Math.max(0, Number(this.cashReceived || 0) - this.totalPrice); }

  addFnb(product: FnbProductDto): void {
    if (this.pendingQrSale || this.submittingSale) return;
    const quantity = this.selectedFnb.get(product.productId) || 0;
    if (product.trackInventory && quantity >= Math.floor(product.availableQuantity)) return;
    if (quantity >= 99) return;
    this.selectedFnb.set(product.productId, quantity + 1);
    this.errorMessage = '';
  }

  removeFnb(product: FnbProductDto): void {
    const quantity = this.selectedFnb.get(product.productId) || 0;
    if (quantity <= 1) this.selectedFnb.delete(product.productId);
    else this.selectedFnb.set(product.productId, quantity - 1);
  }

  fnbQuantity(productId: number): number { return this.selectedFnb.get(productId) || 0; }

  canAddFnb(product: FnbProductDto): boolean {
    return !product.trackInventory || this.fnbQuantity(product.productId) < Math.floor(product.availableQuantity);
  }

  onCashReceivedInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const cursor = input.selectionStart ?? input.value.length;
    const digitsBeforeCursor = input.value.slice(0, cursor).replace(/\D/g, '').length;
    const digits = input.value.replace(/\D/g, '');
    this.cashReceived = digits ? Number(digits) : 0;
    this.cashReceivedText = digits ? this.cashReceived.toLocaleString('vi-VN') : '';
    input.value = this.cashReceivedText;

    let nextCursor = 0;
    let countedDigits = 0;
    while (nextCursor < input.value.length && countedDigits < digitsBeforeCursor) {
      if (/\d/.test(input.value[nextCursor])) countedDigits++;
      nextCursor++;
    }
    input.setSelectionRange(nextCursor, nextCursor);
  }

  get counterPaymentReference(): string {
    return this.pendingQrSale?.order.orderCode || `TFPOS-${this.idempotencyKey.replace(/-/g, '').slice(0, 16).toUpperCase()}`;
  }

  get counterPaymentQrUrl(): string {
    if (!this.pendingQrSale?.order.total) return '';
    const params = new URLSearchParams({
      amount: String(Math.round(this.pendingQrSale.order.total)),
      addInfo: this.counterPaymentReference,
      accountName: 'DANG LAM HUNG',
    });
    return `https://img.vietqr.io/image/MB-0567846199999-qr_only.png?${params.toString()}`;
  }

  get selectedScreening(): ScreeningAdminDto | undefined {
    return this.screenings.find((screening) => screening.screeningId === this.selectedScreeningId);
  }

  loadCatalog(): void {
    if (this.loadingCinemas) return;
    if (!this.selectedCinemaId) {
      this.screenings = [];
      this.seats = [];
      this.errorMessage = 'Tài khoản chưa được phân quyền vào rạp nào đang hoạt động.';
      return;
    }
    this.loadingCatalog = true;
    this.errorMessage = '';
    this.screenings = [];
    this.seats = [];
    this.selectedFilmId = null;
    this.selectedScreeningId = null;
    this.selectedFnb.clear();
    this.loadFnbCatalog(this.selectedCinemaId);
    this.adminOps.getTicketingScreenings(this.selectedCinemaId).subscribe({
      next: (screenings) => {
        this.screenings = screenings;
        this.loadingCatalog = false;
        const first = screenings.find((screening) =>
          Date.parse(screening.startsAt) > Date.now() && screening.status === 'ON_SALE');
        this.selectedFilmId = first?.filmId ?? null;
        this.selectedScreeningId = first?.screeningId ?? null;
        if (first) this.loadSeats(first.screeningId);
        else {
          this.seats = [];
          this.errorMessage = this.pendingPublicationCount
            ? `Có ${this.pendingPublicationCount} suất chiếu đang chờ công bố. Quầy chỉ bán suất đã công bố mở bán.`
            : 'Rạp này chưa có suất chiếu bắt đầu trong 30 ngày tới đang mở bán. Hãy tạo và công bố suất tại mục Lịch chiếu trước.';
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loadingCatalog = false;
        this.errorMessage = err?.error?.error?.message || (err?.name === 'TimeoutError'
          ? 'Tải danh sách suất chiếu quá thời gian chờ. Hãy thử tải lại.'
          : 'Không tải được suất chiếu của rạp.');
        this.cdr.markForCheck();
      },
    });
  }

  private loadFnbCatalog(cinemaId: number | null): void {
    this.fnbProducts = [];
    if (!cinemaId) { this.loadingFnb = false; return; }
    this.loadingFnb = true;
    this.adminOps.getTicketingFnbProducts(cinemaId).subscribe({
      next: (products) => { this.fnbProducts = products; this.loadingFnb = false; this.cdr.markForCheck(); },
      error: () => { this.fnbProducts = []; this.loadingFnb = false; this.cdr.markForCheck(); },
    });
  }

  onCinemaChange(value: number): void {
    this.releaseSelection();
    this.selectedCinemaId = Number(value) || null;
    this.selectedFilmId = null;
    this.selectedScreeningId = null;
    this.selectedSeats.clear();
    this.seats = [];
    this.loadCatalog();
  }

  onFilmChange(value: number): void {
    this.releaseSelection();
    this.selectedFilmId = Number(value) || null;
    const next = this.availableScreenings[0];
    this.selectedScreeningId = next?.screeningId ?? null;
    this.selectedSeats.clear();
    this.seats = [];
    if (next) this.loadSeats(next.screeningId);
  }

  onScreeningChange(value: number): void {
    this.releaseSelection();
    this.selectedScreeningId = Number(value) || null;
    this.selectedSeats.clear();
    if (this.selectedScreeningId) this.loadSeats(this.selectedScreeningId);
  }

  loadSeats(screeningId: number): void {
    this.loadingSeats = true;
    this.errorMessage = '';
    this.adminOps.getTicketingSeats(screeningId).subscribe({
      next: (seats) => {
        this.seats = seats;
        this.selectedSeats.clear();
        this.loadingSeats = false;
        if (!seats.length) this.errorMessage = 'Suất chiếu chưa có sơ đồ ghế. Hãy cấu hình và kích hoạt sơ đồ trước.';
        else if (!seats.some((seat) => seat.status === 'AVAILABLE')) this.errorMessage = 'Suất chiếu hiện không còn ghế trống.';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loadingSeats = false;
        this.errorMessage = err?.error?.error?.message || 'Không tải được sơ đồ ghế của suất chiếu.';
        this.cdr.markForCheck();
      },
    });
  }

  isSeatSelected(id: number): boolean { return this.selectedSeats.has(id); }

  canSelect(seat: ScreeningSeatDto): boolean {
    return (seat.status === 'AVAILABLE' || this.selectedSeats.has(seat.showSeatId)) && seat.basePrice != null && Number(seat.basePrice) > 0;
  }

  isSeatPriced(seat: ScreeningSeatDto): boolean {
    return seat.basePrice != null && Number(seat.basePrice) > 0;
  }

  toggleSeat(seat: ScreeningSeatDto): void {
    if (!this.canSelect(seat) || this.submittingSale || this.seatBusy || !this.selectedScreeningId) return;
    const selected = this.selectedSeats.has(seat.showSeatId);
    if (!selected && this.selectedSeats.size >= 8) { this.errorMessage = 'Mỗi đơn tại quầy tối đa 8 ghế.'; return; }
    this.seatBusy = true;
    this.seatVersion++;
    const screeningId = this.selectedScreeningId;
    const request: Observable<string | { salesOrderId: number; holdToken: string; expiresAt: string }> = selected && this.holdOrderId && this.holdToken
      ? this.adminOps.releaseCounterSeats(screeningId, this.holdOrderId, this.holdToken, [seat.showSeatId])
      : this.adminOps.holdCounterSeat(screeningId, this.holdOrderId, this.holdToken, [seat.showSeatId]);
    request.subscribe({ next: (result) => {
      if (selected) {
        this.selectedSeats.delete(seat.showSeatId); this.holdExpiries.delete(seat.showSeatId); seat.status = 'AVAILABLE';
        if (!this.selectedSeats.size) { this.holdOrderId = null; this.holdToken = null; }
      } else if (typeof result !== 'string') {
        this.holdOrderId = result.salesOrderId; this.holdToken = result.holdToken;
        this.selectedSeats.add(seat.showSeatId); this.holdExpiries.set(seat.showSeatId, Date.parse(result.expiresAt)); seat.status = 'HELD';
      }
      this.seatBusy = false; this.errorMessage = ''; this.cdr.markForCheck();
    }, error: (err) => {
      this.seatBusy = false; this.errorMessage = err?.error?.error?.message || 'Không thể cập nhật giữ ghế. Vui lòng thử lại.';
      this.refreshSeatStatuses(); this.cdr.markForCheck();
    } });
  }

  checkout(): void {
    if (this.paymentMethod === 'QR' && this.pendingQrSale) {
      this.checkCounterPayment();
      return;
    }
    if (!this.selectedCinemaId || !this.selectedScreeningId || !this.selectedSeats.size) {
      this.errorMessage = 'Chọn suất chiếu và ít nhất một ghế trống trước khi thu tiền.';
      return;
    }
    if (this.paymentMethod === 'CASH' && Number(this.cashReceived) < this.totalPrice) {
      this.errorMessage = 'Tiền khách đưa chưa đủ tổng thanh toán.';
      return;
    }
    this.submittingSale = true;
    this.errorMessage = '';
    this.adminOps.createCounterSale({
      screeningId: this.selectedScreeningId,
      showSeatIds: [...this.selectedSeats],
      fnbItems: this.selectedFnbItems.map(({ product, quantity }) => ({ productId: product.productId, quantity })),
      paymentMethod: this.paymentMethod,
      idempotencyKey: this.idempotencyKey,
      salesOrderId: this.holdOrderId ?? undefined,
      holdToken: this.holdToken ?? undefined,
    }).subscribe({
      next: (sale) => {
        this.submittingSale = false;
        if (this.paymentMethod === 'QR' && sale.order.status !== 'ISSUED' && sale.order.status !== 'PAID') {
          this.pendingQrSale = sale;
          this.counterPaymentMessage = 'Đang chờ ngân hàng xác nhận khoản chuyển. Vé chỉ được phát hành sau khi nhận được tiền.';
          this.startCounterPaymentPolling(sale.order.orderCode);
        } else {
          this.finishCounterSale(sale);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.submittingSale = false;
        this.errorMessage = err?.error?.error?.message || 'Không hoàn tất được giao dịch. Kiểm tra trạng thái ghế hoặc tra cứu đơn trước khi thử lại.';
        this.cdr.markForCheck();
      },
    });
  }

  private finishCounterSale(sale: CounterSaleResponse): void {
    this.stopCounterPaymentPolling();
    this.pendingQrSale = null;
    this.checkingCounterPayment = false;
    this.counterPaymentMessage = '';
    this.completedSale = sale;
    this.selectedSeats.clear();
    this.selectedFnb.clear();
    this.cashReceived = 0;
    this.cashReceivedText = '';
    this.holdExpiries.clear();
    this.holdOrderId = null;
    this.holdToken = null;
    this.idempotencyKey = this.newIdempotencyKey();
    if (this.selectedScreeningId) this.loadSeats(this.selectedScreeningId);
    if (this.selectedCinemaId) this.loadFnbCatalog(this.selectedCinemaId);
  }

  private startCounterPaymentPolling(orderCode: string): void {
    this.stopCounterPaymentPolling();
    this.counterPaymentPollTimer = setInterval(() => {
      if (this.pendingQrSale?.order.orderCode !== orderCode || this.checkingCounterPayment) return;
      this.fetchCounterPaymentStatus(false);
    }, 2500);
  }

  private stopCounterPaymentPolling(): void {
    if (this.counterPaymentPollTimer) clearInterval(this.counterPaymentPollTimer);
    this.counterPaymentPollTimer = null;
  }

  checkCounterPayment(): void {
    if (!this.pendingQrSale || this.checkingCounterPayment) return;
    this.fetchCounterPaymentStatus(true);
  }

  private fetchCounterPaymentStatus(showPendingMessage: boolean): void {
    const pending = this.pendingQrSale;
    if (!pending) return;
    this.checkingCounterPayment = true;
    this.adminOps.getCounterSaleStatus(pending.order.orderCode).subscribe({
      next: (order) => {
        if (this.pendingQrSale?.order.orderCode !== pending.order.orderCode) return;
        if (order.status === 'ISSUED' || order.status === 'PAID') {
          this.finishCounterSale({ ...pending, order });
        } else if (order.status === 'EXPIRED' || order.status === 'CANCELLED') {
          this.stopCounterPaymentPolling();
          this.pendingQrSale = null;
          this.selectedSeats.clear();
          this.holdExpiries.clear();
          this.holdOrderId = null;
          this.holdToken = null;
          this.counterPaymentMessage = '';
          this.errorMessage = 'Đơn thanh toán đã hết hạn hoặc bị hủy. Vui lòng chọn ghế và tạo đơn mới.';
          if (this.selectedScreeningId) this.loadSeats(this.selectedScreeningId);
        } else if (showPendingMessage) {
          this.counterPaymentMessage = `Ngân hàng chưa xác nhận đã nhận tiền cho đơn ${pending.order.orderCode}. Sau khi chuyển khoản, hệ thống sẽ tự cập nhật và phát hành vé.`;
        }
        this.checkingCounterPayment = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.checkingCounterPayment = false;
        if (showPendingMessage) this.counterPaymentMessage = err?.error?.error?.message || 'Chưa kiểm tra được thanh toán. Vui lòng thử lại.';
        this.cdr.markForCheck();
      },
    });
  }

  checkTicket(): void {
    if (!this.selectedCinemaId || !this.ticketCodeInput.trim()) return;
    this.isChecking = true;
    this.scanResult = null;
    this.adminOps.scanTicket(this.ticketCodeInput.trim(), this.selectedCinemaId).subscribe({
      next: (result) => {
        this.scanResult = result;
        this.isChecking = false;
        this.ticketCodeInput = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isChecking = false;
        this.scanResult = { result: 'ERROR', reason: err?.error?.error?.message || 'Không kết nối được dịch vụ soát vé.', ticket: null };
        this.cdr.markForCheck();
      },
    });
  }

  lookupTickets(): void {
    if (!this.selectedCinemaId || !this.lookupTerm.trim()) {
      this.lookupError = 'Nhập mã vé hoặc mã đơn để tra cứu.';
      return;
    }
    const term = this.lookupTerm.trim();
    const isOrderNumber = /^TF\d{8}/i.test(term);
    this.lookupLoading = true;
    this.lookupDone = false;
    this.lookupError = '';
    this.adminOps.searchTickets({
      cinemaId: this.selectedCinemaId,
      ticketNumber: isOrderNumber ? undefined : term,
      orderNumber: isOrderNumber ? term : undefined,
    }).subscribe({
      next: (tickets) => {
        this.foundTickets = tickets;
        this.lookupLoading = false;
        this.lookupDone = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.lookupLoading = false;
        this.lookupError = err?.error?.error?.message || 'Không tra cứu được vé.';
        this.cdr.markForCheck();
      },
    });
  }

  scanAccepted(): boolean { return this.scanResult?.result === 'ACCEPTED'; }

  paymentMethodLabel(method: CounterPayMethod | 'CARD'): string {
    return method === 'CASH' ? 'Tiền mặt' : method === 'QR' ? 'QR chuyển khoản' : 'Thẻ';
  }

  setPaymentMethod(method: CounterPayMethod): void {
    if (this.pendingQrSale) return;
    this.paymentMethod = method;
    this.errorMessage = '';
  }

  onCounterQrError(event: Event): void {
    const image = event.target as HTMLImageElement;
    if (!image.src.includes('api.qrserver.com') && this.pendingQrSale?.order.total) {
      const fallbackData = `2|99|0567846199999|DANG LAM HUNG||0|0|${Math.round(this.pendingQrSale.order.total)}|${this.counterPaymentReference}|transfer_p2p`;
      image.src = `https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${encodeURIComponent(fallbackData)}`;
    }
  }

  qrCodeUrl(qrData: string): string {
    return `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(qrData)}`;
  }

  printTickets(): void { globalThis.print(); }

  onScanCinemaChange(value: number): void { this.selectedCinemaId = Number(value) || null; this.scanResult = null; }

  scanMessage(): string {
    const messages: Record<string, string> = {
      ACCEPTED: 'Vé hợp lệ, đã ghi nhận lượt vào.',
      ALREADY_USED: 'Vé đã được quét trước đó.',
      WRONG_CINEMA: 'Vé thuộc rạp khác.',
      CANCELLED: 'Vé đã bị hủy hoặc hoàn tiền.',
      EXPIRED: 'Vé đã hết hạn.',
      INVALID: 'Mã QR không hợp lệ hoặc vé chưa phát hành.',
      ERROR: this.scanResult?.reason || 'Không thể kiểm tra vé.',
    };
    return messages[this.scanResult?.result || ''] || this.scanResult?.reason || 'Không xác định được trạng thái vé.';
  }

  finishOrder(): void { this.completedSale = null; this.selectedSeats.clear(); }

  newIdempotencyKey(): string {
    return globalThis.crypto?.randomUUID?.() || `POS-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
  }
}
