import { Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [NgClass],
  template: `
    <button
      [ngClass]="['btn', 'btn--' + variant, 'btn--' + size]"
      [disabled]="disabled"
      [type]="type"
    >
      <ng-content />
    </button>
  `,
  styles: [`
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: var(--spacing-1);
      border: 1px solid transparent;
      border-radius: var(--radius-md);
      font-weight: var(--font-weight-medium);
      transition: all var(--transition-fast);
      white-space: nowrap;
      cursor: pointer;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }

    .btn--sm {
      padding: 2px var(--spacing-2);
      font-size: var(--font-size-sm);
    }

    .btn--md {
      padding: var(--spacing-1) var(--spacing-3);
      font-size: var(--font-size-md);
    }

    .btn--lg {
      padding: var(--spacing-2) var(--spacing-4);
      font-size: var(--font-size-base);
    }

    .btn--primary {
      background: var(--color-accent);
      color: var(--color-text-inverse);
      &:hover:not(:disabled) { background: var(--color-accent-hover); }
    }

    .btn--secondary {
      background: var(--color-bg-primary);
      color: var(--color-text-primary);
      border-color: var(--color-border-primary);
      &:hover:not(:disabled) { background: var(--color-bg-hover); }
    }

    .btn--ghost {
      background: transparent;
      color: var(--color-text-secondary);
      &:hover:not(:disabled) {
        background: var(--color-bg-hover);
        color: var(--color-text-primary);
      }
    }

    .btn--danger {
      background: var(--color-danger);
      color: var(--color-text-inverse);
      &:hover:not(:disabled) { background: var(--color-danger-hover); }
    }
  `],
})
export class ButtonComponent {
  @Input() variant: 'primary' | 'secondary' | 'ghost' | 'danger' = 'primary';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() disabled = false;
  @Input() type: 'button' | 'submit' = 'button';
}
