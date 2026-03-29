import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-input',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="input-wrapper">
      @if (label) {
        <label class="input-label">{{ label }}</label>
      }
      <input
        class="input-field"
        [type]="type"
        [placeholder]="placeholder"
        [disabled]="disabled"
        [ngModel]="value"
        (ngModelChange)="valueChange.emit($event)"
      />
    </div>
  `,
  styles: [`
    .input-wrapper {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-1);
    }

    .input-label {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
      color: var(--color-text-secondary);
    }

    .input-field {
      padding: var(--spacing-1) var(--spacing-2);
      border: 1px solid var(--color-border-primary);
      border-radius: var(--radius-md);
      font-size: var(--font-size-base);
      color: var(--color-text-primary);
      background: var(--color-bg-primary);
      outline: none;
      transition: border-color var(--transition-fast);

      &::placeholder {
        color: var(--color-text-tertiary);
      }

      &:focus {
        border-color: var(--color-border-focus);
      }

      &:disabled {
        background: var(--color-bg-secondary);
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  `],
})
export class InputComponent {
  @Input() label = '';
  @Input() type = 'text';
  @Input() placeholder = '';
  @Input() disabled = false;
  @Input() value = '';
  @Output() valueChange = new EventEmitter<string>();
}
