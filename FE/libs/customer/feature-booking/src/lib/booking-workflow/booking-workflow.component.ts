import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BadgeComponent } from '@the-film/shared/ui';
import { formatVND, formatCountdown } from '@the-film/shared/util';
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
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
    if (this.step < 4) {
      this.bookingService.stopHoldTimer();
    }
  }

  private initWorkflow(showtimeId: string): void {
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
        this.availableSeats = this.bookingService.availableSeats();
      } else {
        // Fallback if accessed directly by URL
        this.cinemaService.getMovies().subscribe((movies) => {
          this.movie = movies.find((m) => m.id === st.movieId) || movies[0];
          this.cinemaService.getCinemas().subscribe((cinemas) => {
            this.cinema = cinemas.find((c) => c.id === st.cinemaId) || cinemas[0];
            this.bookingService.initShowtime(this.movie!, this.cinema!, st);
            this.availableSeats = this.bookingService.availableSeats();
          });
        });
      }
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
    return this.bookingService.holdSeconds();
  }

  get formattedCountdown(): string {
    return formatCountdown(this.holdSeconds);
  }

  getSeatsByRow(row: string): Seat[] {
    return this.bookingService.availableSeats().filter((s) => s.row === row);
  }

  isSeatSelected(seat: Seat): boolean {
    return this.selectedSeats.some((s) => s.id === seat.id);
  }

  onSeatClick(seat: Seat): void {
    this.bookingService.toggleSeat(seat);
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
    if (this.step === 1 && this.selectedSeats.length === 0) {
      alert('Vui lòng chọn ít nhất 1 ghế để tiếp tục.');
      return;
    }
    this.step++;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  prevStep(): void {
    if (this.step > 1) {
      this.step--;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  submitPayment(): void {
    if (!this.buyer.fullName.trim() || !this.buyer.phone.trim() || !this.buyer.email.trim()) {
      alert('Vui lòng điền đầy đủ Họ tên, Số điện thoại và Email để nhận vé điện tử.');
      return;
    }

    this.bookingService.updateBuyerInfo(this.buyer);
    this.isProcessingPayment = true;

    // Simulate safe API payment verification
    setTimeout(() => {
      this.confirmedOrder = this.bookingService.createOrder(this.selectedPaymentMethod);
      this.isProcessingPayment = false;
      this.step = 4;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }, 1500);
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
