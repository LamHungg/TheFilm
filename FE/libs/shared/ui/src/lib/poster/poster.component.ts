import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'thefilm-poster',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="figma-poster" [ngClass]="['theme-' + theme, variant]">
      <div class="poster-circle"></div>
      <div class="poster-strip" *ngIf="showTitle">
        <span>{{ title }}</span>
      </div>
    </div>
  `,
  styles: [`
    .figma-poster {
      position: relative;
      width: 100%;
      aspect-ratio: 1 / 1;
      border-radius: 14px;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);

      &.banner {
        aspect-ratio: 16 / 9;
        max-height: 220px;
        border-radius: 12px;
      }

      .poster-circle {
        width: 48%;
        aspect-ratio: 1 / 1;
        background-color: #f3a738;
        border-radius: 50%;
        box-shadow: 0 4px 12px rgba(243, 167, 56, 0.3);
      }

      .poster-strip {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        background-color: #0f172a;
        color: #ffffff;
        font-weight: 800;
        font-size: 0.68rem;
        letter-spacing: 0.06em;
        text-align: center;
        padding: 0.45rem 0.5rem;
        text-transform: uppercase;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      // Themes from Figma
      &.theme-navy {
        background-color: #1a2844;
      }

      &.theme-coral {
        background-color: #e04d3f;
      }

      &.theme-purple {
        background-color: #2b1e40;
      }

      &.theme-teal {
        background-color: #459a88;
      }
    }
  `]
})
export class FigmaPosterComponent {
  @Input() theme: 'navy' | 'coral' | 'purple' | 'teal' = 'navy';
  @Input() title: string = '';
  @Input() variant: 'square' | 'banner' = 'square';
  @Input() showTitle: boolean = true;
}
