import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MovieCardComponent } from '@the-film/shared/ui';
import { Movie, Cinema, Showtime, CinemaService } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-movie-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MovieCardComponent],
  templateUrl: './movie-list.component.html',
  styleUrl: './movie-list.component.scss',
})
export class MovieListComponent implements OnInit {
  movies: Movie[] = [];
  cinemas: Cinema[] = [];
  showtimes: Showtime[] = [];

  displayedNowShowing: Movie[] = [];
  upcomingMovies: Movie[] = [];

  activeTab: 'NOW_SHOWING' | 'COMING_SOON' = 'NOW_SHOWING';
  searchQuery = '';
  selectedGenre = '';
  selectedFormat = '';
  selectedCinemaFilter = '';
  selectedDateFilter = '';

  availableGenres: string[] = [];
  readonly todayIso: string;
  readonly tomorrowIso: string;
  readonly todayLabel: string;
  readonly tomorrowLabel: string;

  constructor(
    private cinemaService: CinemaService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {
    const today = new Date();
    const tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
    this.todayIso = this.cinemaService.formatLocalDate(today);
    this.tomorrowIso = this.cinemaService.formatLocalDate(tomorrow);
    this.todayLabel = `${String(today.getDate()).padStart(2, '0')}/${String(today.getMonth() + 1).padStart(2, '0')}`;
    this.tomorrowLabel = `${String(tomorrow.getDate()).padStart(2, '0')}/${String(tomorrow.getMonth() + 1).padStart(2, '0')}`;
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['cinemaId']) this.selectedCinemaFilter = params['cinemaId'];
      if (params['q']) this.searchQuery = params['q'];
      this.reloadShowtimes();
    });

    this.cinemaService.getMovies().subscribe((movies) => {
      this.movies = movies;
      const genreSet = new Set<string>();
      movies.forEach((m) => m.genres.forEach((g) => genreSet.add(g)));
      this.availableGenres = Array.from(genreSet);
      this.applyFilters();
      this.cdr.markForCheck();
    });

    this.cinemaService.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      this.cdr.markForCheck();
    });
  }

  onScheduleFilterChange(): void {
    this.reloadShowtimes();
  }

  private reloadShowtimes(): void {
    const targetDate = this.selectedDateFilter === 'tomorrow' ? this.tomorrowIso : this.todayIso;
    this.cinemaService
      .getShowtimes(undefined, this.selectedCinemaFilter || undefined, targetDate)
      .subscribe((showtimes) => {
        this.showtimes = showtimes;
        this.applyFilters();
        this.cdr.markForCheck();
      });
  }

  setTab(tab: 'NOW_SHOWING' | 'COMING_SOON'): void {
    this.activeTab = tab;
    this.applyFilters();
  }

  applyFilters(): void {
    let nowList = this.movies.filter((m) => m.status === 'NOW_SHOWING');
    let upList = this.movies.filter((m) => m.status !== 'NOW_SHOWING');

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase().trim();
      const filterFn = (m: Movie) =>
        m.title.toLowerCase().includes(q) ||
        (m.originalTitle?.toLowerCase().includes(q) ?? false) ||
        m.genres.some((g) => g.toLowerCase().includes(q));

      nowList = nowList.filter(filterFn);
      upList = upList.filter(filterFn);
    }

    if (this.selectedGenre) {
      nowList = nowList.filter((m) => m.genres.includes(this.selectedGenre));
      upList = upList.filter((m) => m.genres.includes(this.selectedGenre));
    }

    if (this.selectedFormat) {
      const matchFormat = (m: Movie) =>
        this.showtimes.some((s) => s.movieId === m.id && s.format === this.selectedFormat) ||
        m.formats.includes(this.selectedFormat as Movie['formats'][number]);
      nowList = nowList.filter(matchFormat);
      upList = upList.filter(matchFormat);
    }

    if (this.selectedCinemaFilter || this.selectedDateFilter) {
      const movieIdsWithShowtimes = new Set(this.showtimes.map((s) => s.movieId));
      nowList = nowList.filter((m) => movieIdsWithShowtimes.has(m.id));
    }

    if (this.activeTab === 'NOW_SHOWING') {
      this.displayedNowShowing = nowList;
      this.upcomingMovies = upList;
    } else {
      this.displayedNowShowing = upList;
      this.upcomingMovies = [];
    }
  }

  resetFilters(): void {
    this.searchQuery = '';
    this.selectedGenre = '';
    this.selectedDateFilter = '';
    this.selectedFormat = '';
    this.selectedCinemaFilter = '';
    this.reloadShowtimes();
  }

  goToMovieDetail(movie: Movie): void {
    this.router.navigate(['/movies', movie.slug]);
  }
}
