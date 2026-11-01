import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Cinema, CinemaService } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-cinemas',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './cinemas.component.html',
  styleUrl: './cinemas.component.scss',
})
export class CinemasComponent implements OnInit {
  cinemas: Cinema[] = [];

  constructor(
    private cinemaService: CinemaService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cinemaService.getCinemas().subscribe((cinemas) => {
      this.cinemas = cinemas;
    });
  }

  viewShowtimes(cinemaId: string): void {
    this.router.navigate(['/movies']);
  }
}
