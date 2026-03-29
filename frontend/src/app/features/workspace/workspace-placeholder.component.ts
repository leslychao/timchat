import { Component } from '@angular/core';

@Component({
  selector: 'app-workspace-placeholder',
  standalone: true,
  template: `
    <div class="placeholder">
      <p class="placeholder__text">Select a channel to start chatting</p>
    </div>
  `,
  styles: [`
    .placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      padding: var(--spacing-8);
    }

    .placeholder__text {
      color: var(--color-text-tertiary);
      font-size: var(--font-size-base);
    }
  `],
})
export class WorkspacePlaceholderComponent {}
