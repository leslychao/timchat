import { Component } from '@angular/core';

@Component({
  selector: 'app-top-bar',
  standalone: true,
  template: `
    <header class="top-bar">
      <div class="top-bar__context">
        <span class="top-bar__channel-name"># general</span>
      </div>
      <div class="top-bar__actions">
        <!-- Search, settings, user menu will be added in later stages -->
      </div>
    </header>
  `,
  styles: [`
    .top-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: var(--top-bar-height);
      padding: 0 var(--spacing-4);
      background: var(--color-bg-primary);
      border-bottom: 1px solid var(--color-border-primary);
    }

    .top-bar__context {
      display: flex;
      align-items: center;
      gap: var(--spacing-2);
    }

    .top-bar__channel-name {
      font-size: var(--font-size-base);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }

    .top-bar__actions {
      display: flex;
      align-items: center;
      gap: var(--spacing-2);
    }
  `],
})
export class TopBarComponent {}
