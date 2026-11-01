import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FigmaPosterComponent } from '@the-film/shared/ui';
import { formatVND } from '@the-film/shared/util';
import {
  Movie,
  Cinema,
  Showtime,
  CinemaService,
  BookingService,
} from '@the-film/customer/data-access';

interface DateOption {
  displayDate: string;
  date: string;
  isToday: boolean;
}

interface GroupedShowtime {
  roomName: string;
  format: string;
  roomType: string;
  slots: Showtime[];
}

@Component({
  selector: 'thefilm-movie-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FigmaPosterComponent],
  templateUrl: './movie-detail.component.html',
  styleUrl: './movie-detail.component.scss',
})
export class MovieDetailComponent implements OnInit {
  movie: Movie | null = null;
  cinemas: Cinema[] = [];
  selectedCinema: Cinema | null = null;
  selectedCinemaId = '';

  availableDates: DateOption[] = [
    { displayDate: '01/11', date: '2026-11-01', isToday: true },
    { displayDate: '02/11', date: '2026-11-02', isToday: false },
    { displayDate: '03/11', date: '2026-11-03', isToday: false },
    { displayDate: '04/11', date: '2026-11-04', isToday: false },
  ];
  selectedDate = '2026-11-01';

  selectedRoomType = 'ALL';
  roomFilters = [
    { label: 'TẤT CẢ', value: 'ALL' },
    { label: 'STANDARD', value: 'STANDARD' },
    { label: 'VIP', value: 'GOLD_CLASS' },
    { label: '3D', value: '3D' },
  ];

  allShowtimes: Showtime[] = [];
  filteredShowtimes: Showtime[] = [];
  groupedShowtimes: GroupedShowtime[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cinemaService: CinemaService,
    private bookingService: BookingService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const slug = params.get('slug') || '';
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
      if (cinemas.length > 0) {
        this.selectedCinemaId = cinemas[0].id;
        this.selectedCinema = cinemas[0];
      }

      if (this.movie) {
        this.cinemaService.getShowtimes(this.movie.id).subscribe((sts) => {
          this.allShowtimes = sts;
          this.applyShowtimeFilter();
        });
      }
    });
  }

  onCinemaChange(): void {
    this.selectedCinema = this.cinemas.find((c) => c.id === this.selectedCinemaId) || null;
    this.applyShowtimeFilter();
  }

  onDateChange(date: string): void {
    this.selectedDate = date;
    this.applyShowtimeFilter();
  }

  onRoomFilterChange(type: string): void {
    this.selectedRoomType = type;
    this.applyShowtimeFilter();
  }

  applyShowtimeFilter(): void {
    let list = this.allShowtimes.filter((s) => s.cinemaId === this.selectedCinemaId);

    if (this.selectedRoomType !== 'ALL') {
      list = list.filter((s) => s.roomType === this.selectedRoomType);
    }

    this.filteredShowtimes = list;

    // Group showtimes by Room
    const groups: { [key: string]: GroupedShowtime } = {};
    list.forEach((st) => {
      const key = `${st.roomName}_${st.format}`;
      if (!groups[key]) {
        groups[key] = {
          roomName: st.roomName,
          format: st.format,
          roomType: st.roomType,
          slots: [],
        };
      }
      groups[key].slots.push(st);
    });

    this.groupedShowtimes = Object.values(groups);
  }

  formatPrice(p: number): string {
    return formatVND(p);
  }

  selectShowtimeAndBook(showtime: Showtime): void {
    if (this.movie && this.selectedCinema) {
      this.bookingService.initShowtime(this.movie, this.selectedCinema, showtime);
      this.router.navigate(['/booking', showtime.id]);
    }
  }
}
