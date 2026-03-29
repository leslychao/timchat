import { Component } from '@angular/core';

@Component({
  selector: 'app-right-panel',
  standalone: true,
  template: `
    <aside class="right-panel">
      <div class="right-panel__header">
        <h3 class="right-panel__title">Details</h3>
      </div>
      <div class="right-panel__content">
        <!-- Members, presence, moderation details will be rendered here in later stages -->
      </div>
    </aside>
  `,
  styles: [`
    .right-panel {
      display: flex;
      flex-direction: column;
      width: var(--right-panel-width);
      height: 100%;
      background: var(--color-bg-secondary);
      border-left: 1px solid var(--color-border-secondary);
      flex-shrink: 0;
    }

    .right-panel__header {
      display: flex;
      align-items: center;
      height: var(--top-bar-height);
      padding: 0 var(--spacing-4);
      border-bottom: 1px solid var(--color-border-secondary);
    }

    .right-panel__title {
      font-size: var(--font-size-md);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }

    .right-panel__content {
      flex: 1;
      overflow-y: auto;
      padding: var(--spacing-3);
    }
  `],
})
export class RightPanelComponent {}
