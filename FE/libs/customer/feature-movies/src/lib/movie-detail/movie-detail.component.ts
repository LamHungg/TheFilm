import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { formatVND } from '@the-film/shared/util';
import {
  Movie,
  Cinema,
  Showtime,
  CinemaService,
  BookingService,
} from '@the-film/customer/data-access';

interface DateOption {
  shortDate: string;
  weekday: string;
  fullLabel: string;
  date: string;
}

interface GroupedRoomShowtime {
  roomName: string;
  format: string;
  roomType: string;
  minPrice: number;
  maxPrice: number;
  slots: Showtime[];
}

interface CinemaScheduleGroup {
  cinema: Cinema;
  badgeLabel: string;
  badgeVariant: 'red' | 'gold' | 'navy';
  rooms: GroupedRoomShowtime[];
}

const WEEKDAY_SHORT = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
const WEEKDAY_FULL = [
  'Chủ Nhật',
  'Thứ Hai',
  'Thứ Ba',
  'Thứ Tư',
  'Thứ Năm',
  'Thứ Sáu',
  'Thứ Bảy',
];

@Component({
  selector: 'thefilm-movie-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './movie-detail.component.html',
  styleUrl: './movie-detail.component.scss',
})
export class MovieDetailComponent implements OnInit {
  movie: Movie | null = null;
  cinemas: Cinema[] = [];

  availableDates: DateOption[] = [];
  selectedDate = '';

  selectedCity = '';
  cinemaSearch = '';
  selectedRoomType = 'ALL';
  selectedCinemaId = '';
  sortBy: 'time' | 'price' = 'time';

  allShowtimes: Showtime[] = [];
  cinemaGroups: CinemaScheduleGroup[] = [];

  selectedShowtime: Showtime | null = null;
  selectedSlotCinema: Cinema | null = null;
  selectedSlotRoomLabel = '';
  posterError = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cinemaService: CinemaService,
    private bookingService: BookingService,
    private cdr: ChangeDetectorRef
  ) {
    this.availableDates = this.buildUpcomingDates(5);
    this.selectedDate = this.availableDates[0]?.date || '';
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const slug = params.get('slug') || '';
      this.posterError = false;
      this.cinemaService.getMovieBySlug(slug).subscribe((m) => {
        if (m) {
          this.movie = m;
          this.loadCinemasAndShowtimes();
        } else {
          this.router.navigate(['/movies']);
        }
        this.cdr.markForCheck();
      });
    });
  }

  private buildUpcomingDates(count: number): DateOption[] {
    const today = new Date();
    const list: DateOption[] = [];
    for (let i = 0; i < count; i++) {
      const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i);
      const day = String(d.getDate()).padStart(2, '0');
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const year = d.getFullYear();
      const wdIdx = d.getDay();
      list.push({
        shortDate: `${day}/${month}`,
        weekday: WEEKDAY_SHORT[wdIdx],
        fullLabel: `${WEEKDAY_FULL[wdIdx]}, ${day} tháng ${month} năm ${year}`,
        date: `${year}-${month}-${day}`,
      });
    }
    return list;
  }

  private loadCinemasAndShowtimes(): void {
    this.cinemaService.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      this.fetchShowtimesForDate();
    });
  }

  private fetchShowtimesForDate(): void {
    if (!this.movie) return;
    this.cinemaService
      .getShowtimes(this.movie.id, undefined, this.selectedDate)
      .subscribe((sts) => {
        this.allShowtimes = sts;
        this.applyShowtimeFilter();
        this.loadSeatPricesForShowtimes(sts);
        this.cdr.markForCheck();
      });
  }

  private loadSeatPricesForShowtimes(showtimes: Showtime[]): void {
    if (showtimes.length === 0) return;
    const priceCalls = showtimes.map((st) => this.cinemaService.getScreeningSeats(st.id));
    forkJoin(priceCalls).subscribe({
      next: (seatLists) => {
        this.allShowtimes = this.allShowtimes.map((st, idx) => {
          const seats = seatLists[idx] ?? [];
          const positivePrices = seats.map((s) => s.price).filter((p) => p > 0);
          const minPrice = positivePrices.length ? Math.min(...positivePrices) : 0;
          const maxPrice = positivePrices.length ? Math.max(...positivePrices) : 0;
          return {
            ...st,
            priceStandard: seats.find((s) => s.type === 'STANDARD')?.price ?? minPrice,
            priceVip: seats.find((s) => s.type === 'VIP')?.price ?? maxPrice,
            priceCouple: seats.find((s) => s.type === 'COUPLE')?.price ?? maxPrice,
          };
        });
        this.applyShowtimeFilter();
        this.cdr.markForCheck();
      },
      error: () => {
        // Keep schedule visible even if a room price preview request fails.
      },
    });
  }

  get availableCities(): string[] {
    return Array.from(new Set(this.cinemas.map((c) => c.city).filter(Boolean)));
  }

  get selectedDateLabel(): string {
    return (
      this.availableDates.find((d) => d.date === this.selectedDate)?.fullLabel || ''
    );
  }

  onDateChange(date: string): void {
    this.selectedDate = date;
    this.fetchShowtimesForDate();
  }

  shiftDate(direction: -1 | 1): void {
    const idx = this.availableDates.findIndex((d) => d.date === this.selectedDate);
    if (idx === -1) return;
    const nextIdx = Math.max(0, Math.min(this.availableDates.length - 1, idx + direction));
    if (nextIdx !== idx) {
      this.onDateChange(this.availableDates[nextIdx].date);
    }
  }

  applyShowtimeFilter(): void {
    const keyword = this.cinemaSearch.trim().toLowerCase();

    const matchingCinemas = this.cinemas.filter((c) => {
      if (this.selectedCity && c.city !== this.selectedCity) return false;
      if (this.selectedCinemaId && c.id !== this.selectedCinemaId) return false;
      if (
        keyword &&
        !c.name.toLowerCase().includes(keyword) &&
        !c.address.toLowerCase().includes(keyword)
      ) {
        return false;
      }
      return true;
    });

    const groups: CinemaScheduleGroup[] = [];

    for (const cinema of matchingCinemas) {
      let slots = this.allShowtimes.filter((s) => s.cinemaId === cinema.id);

      if (this.selectedDate) {
        slots = slots.filter((s) => s.date === this.selectedDate);
      }

      if (this.selectedRoomType !== 'ALL') {
        slots = slots.filter(
          (s) => s.roomType === this.selectedRoomType || s.format === this.selectedRoomType
        );
      }

      if (slots.length === 0) continue;

      const sortedSlots = [...slots].sort((a, b) =>
        this.sortBy === 'price'
          ? a.priceStandard - b.priceStandard
          : a.startTime.localeCompare(b.startTime)
      );

      const roomMap = new Map<string, GroupedRoomShowtime>();
      for (const st of sortedSlots) {
        const key = `${st.format}_${st.roomName}`;
        const slotMax = Math.max(st.priceStandard, st.priceVip, st.priceCouple);
        const existing = roomMap.get(key);
        if (!existing) {
          roomMap.set(key, {
            roomName: st.roomName,
            format: st.format,
            roomType: st.roomType,
            minPrice: st.priceStandard,
            maxPrice: slotMax,
            slots: [st],
          });
        } else {
          existing.slots.push(st);
          existing.minPrice = Math.min(existing.minPrice, st.priceStandard);
          existing.maxPrice = Math.max(existing.maxPrice, slotMax);
        }
      }

      const hasGold = sortedSlots.some((s) => s.roomType === 'GOLD_CLASS');
      const brandVariant: 'red' | 'gold' | 'navy' = hasGold
        ? 'gold'
        : cinema.id === '1'
          ? 'red'
          : 'navy';

      const badgeLabel = hasGold ? 'GOLD CLASS' : 'CINEMA';

      groups.push({
        cinema,
        badgeLabel,
        badgeVariant: brandVariant,
        rooms: Array.from(roomMap.values()),
      });
    }

    this.cinemaGroups = groups;

    const currentStillVisible = this.selectedShowtime
      ? groups
          .flatMap((g) => g.rooms)
          .flatMap((r) => r.slots)
          .find((s) => s.id === this.selectedShowtime?.id)
      : undefined;

    if (currentStillVisible) {
      this.selectedShowtime = currentStillVisible;
    } else if (groups.length > 0 && groups[0].rooms.length > 0 && groups[0].rooms[0].slots.length > 0) {
      const firstGroup = groups[0];
      const firstRoom = firstGroup.rooms[0];
      this.selectSlot(firstGroup.cinema, firstRoom, firstRoom.slots[0]);
    } else {
      this.selectedShowtime = null;
      this.selectedSlotCinema = null;
      this.selectedSlotRoomLabel = '';
    }
  }

  selectSlot(cinema: Cinema, room: GroupedRoomShowtime, showtime: Showtime): void {
    this.selectedShowtime = showtime;
    this.selectedSlotCinema = cinema;
    this.selectedSlotRoomLabel = `${room.format} · ${room.roomName}`;
  }

  bookSelectedShowtime(): void {
    if (this.movie && this.selectedSlotCinema && this.selectedShowtime) {
      this.bookingService.initShowtime(this.movie, this.selectedSlotCinema, this.selectedShowtime);
      this.router.navigate(['/booking', this.selectedShowtime.id]);
    }
  }

  formatDuration(minutes: number): string {
    const hrs = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return hrs > 0 ? `${hrs} giờ ${mins} phút` : `${mins} phút`;
  }

  formatPrice(p: number): string {
    return formatVND(p);
  }

  onPosterError(): void {
    this.posterError = true;
  }
}
