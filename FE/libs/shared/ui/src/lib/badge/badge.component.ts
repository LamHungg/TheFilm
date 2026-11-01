import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'thefilm-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [ngClass]="['badge-' + type.toLowerCase(), 'variant-' + variant]">
      <ng-content></ng-content>
    </span>
  `,
  styles: [`
    .badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 0.72rem;
      border-radius: 4px;
      padding: 0.15rem 0.45rem;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      line-height: 1.2;
    }

    .badge-t13 {
      background-color: #fef3c7;
      color: #b45309;
      border: 1px solid #fde68a;
    }

    .badge-t16 {
      background-color: #ffedd5;
      color: #c2410c;
      border: 1px solid #fed7aa;
    }

    .badge-t18 {
      background-color: #fee2e2;
      color: #c90e29;
      border: 1px solid #fecaca;
    }

    .badge-p {
      background-color: #d1fae5;
      color: #047857;
      border: 1px solid #a7f3d0;
    }

    .badge-format {
      background-color: #f1f5f9;
      color: #334155;
      border: 1px solid #cbd5e1;
    }

    .badge-imax {
      background-color: #0f172a;
      color: #38bdf8;
      border: 1px solid #0284c7;
    }

    .variant-solid.badge-t13 { background-color: #f59e0b; color: #fff; border: none; }
    .variant-solid.badge-t16 { background-color: #ea580c; color: #fff; border: none; }
    .variant-solid.badge-t18 { background-color: #c90e29; color: #fff; border: none; }
    .variant-solid.badge-p { background-color: #10b981; color: #fff; border: none; }
  `]
})
export class BadgeComponent {
  @Input() type: string = 'format';
  @Input() variant: 'subtle' | 'solid' = 'subtle';
}
