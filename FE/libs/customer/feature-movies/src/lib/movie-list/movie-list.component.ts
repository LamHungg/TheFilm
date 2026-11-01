import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
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

  constructor(
    private cinemaService: CinemaService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cinemaService.getMovies().subscribe((movies) => {
      this.movies = movies;

      // Extract unique genres
      const genreSet = new Set<string>();
      movies.forEach((m) => m.genres.forEach((g) => genreSet.add(g)));
      this.availableGenres = Array.from(genreSet);

      this.applyFilters();
    });

    this.cinemaService.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
    });

    this.cinemaService.getShowtimes().subscribe((showtimes) => {
      this.showtimes = showtimes;
    });
  }

  setTab(tab: 'NOW_SHOWING' | 'COMING_SOON'): void {
    this.activeTab = tab;
    this.applyFilters();
  }

  applyFilters(): void {
    let nowList = this.movies.filter((m) => m.status === 'NOW_SHOWING');
    let upList = this.movies.filter((m) => m.status === 'COMING_SOON');

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase().trim();
      const filterFn = (m: Movie) =>
        m.title.toLowerCase().includes(q) ||
        m.genres.some((g) => g.toLowerCase().includes(q)) ||
        m.cast.some((c) => c.toLowerCase().includes(q)) ||
        m.director.toLowerCase().includes(q);

      nowList = nowList.filter(filterFn);
      upList = upList.filter(filterFn);
    }

    if (this.selectedGenre) {
      nowList = nowList.filter((m) => m.genres.includes(this.selectedGenre));
      upList = upList.filter((m) => m.genres.includes(this.selectedGenre));
    }

    if (this.selectedFormat) {
      nowList = nowList.filter((m) => m.formats.includes(this.selectedFormat as any));
      upList = upList.filter((m) => m.formats.includes(this.selectedFormat as any));
    }

    if (this.activeTab === 'NOW_SHOWING') {
      this.displayedNowShowing = nowList;
      this.upcomingMovies = upList.slice(0, 2);
    } else {
      this.displayedNowShowing = upList;
      this.upcomingMovies = [];
    }
  }

  goToMovieDetail(movie: Movie): void {
    this.router.navigate(['/movies', movie.slug]);
  }
}
