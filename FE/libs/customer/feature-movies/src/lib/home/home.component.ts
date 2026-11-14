import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CinemaService, Movie, Cinema } from '@the-film/customer/data-access';
import {
  MovieCardComponent,
  QuickBookingComponent,
  QuickBookingSearchParams,
  SelectComponent,
  SelectOptionComponent,
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
    SelectComponent,
    SelectOptionComponent,
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

  constructor(
    private cinemaService: CinemaService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cinemaService.getMovies().subscribe((movies) => {
      this.movies = movies;
      this.nowShowingMovies = movies.filter((m) => m.status === 'NOW_SHOWING').slice(0, 4);
      this.upcomingMovies = movies.filter((m) => m.status === 'COMING_SOON');
    });

    this.cinemaService.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
    });
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
    this.router.navigate(['/movies']);
  }

  goToMovie(movie: Movie): void {
    this.router.navigate(['/movies', movie.slug]);
  }
}
