import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CinemaService, Movie, Cinema } from '@the-film/customer/data-access';
import {
  MovieCardComponent,
  QuickBookingComponent,
  QuickBookingSearchParams,
} from '@the-film/shared/ui';

@Component({
  selector: 'thefilm-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MovieCardComponent,
    QuickBookingComponent,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  movies: Movie[] = [];
  nowShowingMovies: Movie[] = [];
  upcomingMovies: Movie[] = [];
  cinemas: Cinema[] = [];
  selectedArea = '';

  heroIndex = 0;
  spotlightImageErrors = new Set<string>();

  constructor(
    private cinemaService: CinemaService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cinemaService.getMovies().subscribe((movies) => {
      this.movies = movies;
      this.nowShowingMovies = movies.filter((m) => m.status === 'NOW_SHOWING').slice(0, 4);
      this.upcomingMovies = movies.filter((m) => m.status !== 'NOW_SHOWING');
      this.cdr.markForCheck();
    });

    this.cinemaService.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
      this.cdr.markForCheck();
    });
  }

  get spotlightMovies(): Movie[] {
    if (this.nowShowingMovies.length <= 2) return this.nowShowingMovies;
    const first = this.nowShowingMovies[this.heroIndex % this.nowShowingMovies.length];
    const second = this.nowShowingMovies[(this.heroIndex + 1) % this.nowShowingMovies.length];
    return [first, second];
  }

  prevSpotlight(): void {
    if (this.nowShowingMovies.length <= 1) return;
    this.heroIndex =
      (this.heroIndex - 1 + this.nowShowingMovies.length) % this.nowShowingMovies.length;
  }

  nextSpotlight(): void {
    if (this.nowShowingMovies.length <= 1) return;
    this.heroIndex = (this.heroIndex + 1) % this.nowShowingMovies.length;
  }

  onSpotlightImgError(movieId: string): void {
    this.spotlightImageErrors.add(movieId);
  }

  handleQuickSearch(params: QuickBookingSearchParams): void {
    if (params.movieSearchTerm) {
      const match = this.movies.find((m) =>
        m.title.toLowerCase().includes(params.movieSearchTerm.toLowerCase())
      );
      if (match) {
        this.router.navigate(['/movies', match.slug]);
        return;
      }
    }
    this.router.navigate(['/movies'], {
      queryParams: {
        cinemaId: params.cinemaId || undefined,
        q: params.movieSearchTerm || undefined,
      },
    });
  }

  goToMovie(movie: Movie): void {
    this.router.navigate(['/movies', movie.slug]);
  }
}
