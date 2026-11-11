import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Movie } from '@the-film/customer/data-access';

@Component({
  selector: 'thefilm-movie-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './movie-card.component.html',
  styleUrls: ['./movie-card.component.scss'],
})
export class MovieCardComponent {
  @Input({ required: true }) movie!: Movie;
  @Output() selectShowtime = new EventEmitter<Movie>();
  @Output() cardClick = new EventEmitter<Movie>();

  imageError = false;

  getMetaString(): string {
    const genre = this.movie.genres?.[0] || 'Điện ảnh';
    const duration = this.movie.duration || 120;
    return `${genre} · ${duration} phút`;
  }

  getPrimaryFormat(): string {
    return this.movie.formats?.[0] || '2D';
  }

  getSecondaryBadge(): string {
    return this.movie.formats?.[1] || 'VIETSUB';
  }

  onImageError(): void {
    this.imageError = true;
  }

  onCardClick(): void {
    this.cardClick.emit(this.movie);
  }

  onBtnClick(event: MouseEvent): void {
    event.stopPropagation();
    this.selectShowtime.emit(this.movie);
  }
}
