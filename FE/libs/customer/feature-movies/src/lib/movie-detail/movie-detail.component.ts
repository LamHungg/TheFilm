import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
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

  availableDates: DateOption[] = [
    { shortDate: '01/11', weekday: 'CN', fullLabel: 'Chủ Nhật, 01 tháng 11 năm 2026', date: '2026-11-01' },
    { shortDate: '02/11', weekday: 'T2', fullLabel: 'Thứ Hai, 02 tháng 11 năm 2026', date: '2026-11-02' },
    { shortDate: '03/11', weekday: 'T3', fullLabel: 'Thứ Ba, 03 tháng 11 năm 2026', date: '2026-11-03' },
    { shortDate: '04/11', weekday: 'T4', fullLabel: 'Thứ Tư, 04 tháng 11 năm 2026', date: '2026-11-04' },
    { shortDate: '05/11', weekday: 'T5', fullLabel: 'Thứ Năm, 05 tháng 11 năm 2026', date: '2026-11-05' },
  ];
  selectedDate = '2026-11-01';

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
    private bookingService: BookingService
  ) {}

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
      });
    });
  }

  private loadCinemasAndShowtimes(): void {
    this.cinemaService.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      if (this.movie) {
        this.cinemaService.getShowtimes(this.movie.id).subscribe((sts) => {
          this.allShowtimes = sts;
          this.applyShowtimeFilter();
        });
      }
    });
  }

  get availableCities(): string[] {
    return Array.from(new Set(this.cinemas.map((c) => c.city)));
  }

  get selectedDateLabel(): string {
    return (
      this.availableDates.find((d) => d.date === this.selectedDate)?.fullLabel ||
      'Chủ Nhật, 01 tháng 11 năm 2026'
    );
  }

  onDateChange(date: string): void {
    this.selectedDate = date;
    this.applyShowtimeFilter();
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
      if (keyword && !c.name.toLowerCase().includes(keyword) && !c.address.toLowerCase().includes(keyword)) {
        return false;
      }
      return true;
    });

    const groups: CinemaScheduleGroup[] = [];

    for (const cinema of matchingCinemas) {
      let slots = this.allShowtimes.filter((s) => s.cinemaId === cinema.id);

      // Filter by exact date if this movie has date-specific entries for the selected date;
      // otherwise keep existing mock schedule so all date pills remain previewable.
      const hasExactDate = slots.some((s) => s.date === this.selectedDate);
      if (hasExactDate) {
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
        const existing = roomMap.get(key);
        if (!existing) {
          roomMap.set(key, {
            roomName: st.roomName,
            format: st.format,
            roomType: st.roomType,
            minPrice: st.priceStandard,
            maxPrice: st.priceVip,
            slots: [st],
          });
        } else {
          existing.slots.push(st);
          existing.minPrice = Math.min(existing.minPrice, st.priceStandard);
          existing.maxPrice = Math.max(existing.maxPrice, st.priceVip);
        }
      }

      const brandVariant: 'red' | 'gold' | 'navy' =
        cinema.brand.includes('PREMIUM')
          ? 'gold'
          : cinema.id === 'cin-01'
            ? 'red'
            : 'navy';

      const badgeLabel =
        cinema.brand.includes('PREMIUM') ? 'PREMIUM' : cinema.rooms.some((r) => r.type === 'IMAX') ? 'IMAX' : 'CINEMA';

      groups.push({
        cinema,
        badgeLabel,
        badgeVariant: brandVariant,
        rooms: Array.from(roomMap.values()),
      });
    }

    this.cinemaGroups = groups;

    // Keep current selection if still visible; otherwise auto-select the first available slot
    const stillVisible =
      this.selectedShowtime &&
      groups.some((g) => g.rooms.some((r) => r.slots.some((s) => s.id === this.selectedShowtime?.id)));

    if (!stillVisible) {
      if (groups.length > 0 && groups[0].rooms.length > 0 && groups[0].rooms[0].slots.length > 0) {
        const firstGroup = groups[0];
        const firstRoom = firstGroup.rooms[0];
        this.selectSlot(firstGroup.cinema, firstRoom, firstRoom.slots[0]);
      } else {
        this.selectedShowtime = null;
        this.selectedSlotCinema = null;
        this.selectedSlotRoomLabel = '';
      }
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
