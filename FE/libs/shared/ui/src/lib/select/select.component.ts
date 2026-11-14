import {
  Component,
  ContentChildren,
  ElementRef,
  Input,
  QueryList,
  forwardRef,
  inject,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** One choice of <thefilm-select>; its text content is the visible label. */
@Component({
  selector: 'thefilm-option',
  standalone: true,
  template: '<ng-content></ng-content>',
  host: { hidden: '' },
})
export class SelectOptionComponent {
  @Input() value: unknown = '';
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  get label(): string {
    return (this.host.nativeElement.textContent ?? '').trim();
  }
}

let nextId = 0;

/**
 * Styled replacement for a native <select> (WAI-ARIA select-only combobox).
 * Works with [(ngModel)]; option values are emitted as-is (no string coercion).
 */
@Component({
  selector: 'thefilm-select',
  standalone: true,
  templateUrl: './select.component.html',
  styleUrls: ['./select.component.scss'],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectComponent), multi: true },
  ],
  host: {
    '[class.chip]': "variant === 'chip'",
    '[class.open]': 'open()',
    // Blur alone is not enough: Safari does not focus buttons on click.
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
  },
})
export class SelectComponent implements ControlValueAccessor {
  /** Id for the trigger button, so an external <label for> can target it. */
  @Input() inputId = `thefilm-select-${nextId++}`;
  @Input() ariaLabel: string | null = null;
  @Input() variant: 'field' | 'chip' = 'field';

  @ContentChildren(SelectOptionComponent) optionList?: QueryList<SelectOptionComponent>;

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly open = signal(false);
  readonly activeIndex = signal(-1);
  readonly value = signal<unknown>(undefined);
  disabled = false;

  private onChange: (value: unknown) => void = () => undefined;
  private onTouched: () => void = () => undefined;
  private typeahead = '';
  private typeaheadTimer?: ReturnType<typeof setTimeout>;

  get listId(): string {
    return `${this.inputId}-list`;
  }

  get options(): SelectOptionComponent[] {
    return this.optionList?.toArray() ?? [];
  }

  get selectedIndex(): number {
    return this.options.findIndex((o) => o.value === this.value());
  }

  /** Like a native select, show the first option when the value matches none. */
  get selectedLabel(): string {
    const options = this.options;
    return (options[this.selectedIndex] ?? options[0])?.label ?? '';
  }

  /** Empty value ('' / null) = the "Chọn …" / "Tất cả …" option; shown muted like a placeholder. */
  get isEmpty(): boolean {
    const value = this.value();
    return value === '' || value === null || value === undefined;
  }

  writeValue(value: unknown): void {
    this.value.set(value);
  }

  registerOnChange(fn: (value: unknown) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
    if (disabled) this.close();
  }

  toggle(): void {
    if (this.open()) {
      this.close();
    } else {
      this.openList();
    }
  }

  choose(index: number): void {
    const option = this.options[index];
    if (!option) return;
    if (option.value !== this.value()) {
      this.value.set(option.value);
      this.onChange(option.value);
    }
    this.close();
  }

  onDocumentPointerDown(event: PointerEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  onBlur(): void {
    this.close();
    this.onTouched();
  }

  onKeydown(event: KeyboardEvent): void {
    const last = this.options.length - 1;
    if (last < 0) return;
    const isOpen = this.open();

    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        event.preventDefault();
        if (!isOpen) {
          this.openList();
        } else {
          const step = event.key === 'ArrowDown' ? 1 : -1;
          this.moveTo(Math.min(last, Math.max(0, this.activeIndex() + step)));
        }
        return;
      case 'Home':
      case 'End':
        event.preventDefault();
        if (!isOpen) this.openList();
        this.moveTo(event.key === 'Home' ? 0 : last);
        return;
      case 'Enter':
      case ' ':
        if (event.key === ' ' && this.typeahead) break; // space inside a type-ahead word
        event.preventDefault();
        if (isOpen) {
          this.choose(this.activeIndex());
        } else {
          this.openList();
        }
        return;
      case 'Escape':
        if (isOpen) {
          event.preventDefault();
          this.close();
        }
        return;
      case 'Tab':
        this.close();
        return;
    }

    if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      this.typeAhead(event.key, isOpen);
    }
  }

  private openList(): void {
    if (this.disabled) return;
    this.open.set(true);
    this.moveTo(Math.max(0, this.selectedIndex));
  }

  private close(): void {
    this.open.set(false);
    this.activeIndex.set(-1);
  }

  private moveTo(index: number): void {
    this.activeIndex.set(index);
    // Wait for the list to render before scrolling the active option into view.
    setTimeout(() =>
      document.getElementById(`${this.listId}-${index}`)?.scrollIntoView({ block: 'nearest' })
    );
  }

  /** Jump to the next option starting with the typed text; selects directly when closed, like native. */
  private typeAhead(char: string, isOpen: boolean): void {
    clearTimeout(this.typeaheadTimer);
    this.typeahead += char.toLocaleLowerCase('vi');
    this.typeaheadTimer = setTimeout(() => (this.typeahead = ''), 600);

    const options = this.options;
    const current = isOpen ? this.activeIndex() : this.selectedIndex;
    // One key cycles to the next match; a longer word keeps the current option if it still matches.
    const from = this.typeahead.length > 1 ? Math.max(current, 0) : current + 1;
    for (let i = 0; i < options.length; i++) {
      const candidate = (from + i) % options.length;
      if (options[candidate].label.toLocaleLowerCase('vi').startsWith(this.typeahead)) {
        if (isOpen) {
          this.moveTo(candidate);
        } else {
          this.choose(candidate);
        }
        return;
      }
    }
  }
}
