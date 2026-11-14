import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'thefilm-poster',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="poster-frame" [ngClass]="variant">
      <img
        *ngIf="imageUrl"
        class="poster-photo"
        [src]="imageUrl"
        [alt]="title || 'Movie poster'"
        loading="lazy"
      />
      <div class="poster-scrim"></div>
      <div class="poster-caption" *ngIf="title">
        <span class="poster-kicker">THEFILM SELECT</span>
        <h3 class="poster-heading">{{ title }}</h3>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }
    .poster-frame {
      position: relative;
      width: 100%;
      aspect-ratio: 2 / 3;
      border-radius: 22px;
      overflow: hidden;
      background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%);
      border: 1px solid #E2E8F0;
      box-shadow: 0 20px 44px -12px rgba(15, 23, 42, 0.18);

      &.banner {
        aspect-ratio: 16 / 9;
      }
    }
    .poster-photo {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .poster-frame:hover .poster-photo {
      transform: scale(1.04);
    }
    .poster-scrim {
      position: absolute;
      inset: 0;
      background: linear-gradient(
        to top,
        rgba(15, 23, 42, 0.85) 0%,
        rgba(15, 23, 42, 0.2) 45%,
        transparent 100%
      );
      pointer-events: none;
    }
    .poster-caption {
      position: absolute;
      left: 24px;
      right: 24px;
      bottom: 22px;
      z-index: 2;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .poster-kicker {
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.14em;
      color: #FBBF24;
      text-transform: uppercase;
    }
    .poster-heading {
      font-family: var(--font-display, 'Be Vietnam Pro', sans-serif);
      font-size: clamp(18px, 2.2vw, 24px);
      font-weight: 800;
      color: #FFFFFF;
      margin: 0;
      line-height: 1.25;
      text-shadow: 0 4px 16px rgba(0, 0, 0, 0.45);
    }
  `]
})
export class FigmaPosterComponent {
  @Input() title = '';
  @Input() theme = 'navy';
  @Input() variant: 'card' | 'banner' = 'card';
  @Input() imageUrl?: string;
}
