import { Component } from '@angular/core';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  template: `
    <aside class="sidebar">
      <div class="sidebar__header">
        <h2 class="sidebar__title">Channels</h2>
      </div>
      <div class="sidebar__content">
        <!-- Channel list will be rendered here in Stage 10 -->
      </div>
    </aside>
  `,
  styles: [`
    .sidebar {
      display: flex;
      flex-direction: column;
      width: var(--sidebar-width);
      height: 100%;
      background: var(--color-bg-secondary);
      border-right: 1px solid var(--color-border-secondary);
      flex-shrink: 0;
    }

    .sidebar__header {
      display: flex;
      align-items: center;
      height: var(--top-bar-height);
      padding: 0 var(--spacing-4);
      border-bottom: 1px solid var(--color-border-secondary);
    }

    .sidebar__title {
      font-size: var(--font-size-md);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }

    .sidebar__content {
      flex: 1;
      overflow-y: auto;
      padding: var(--spacing-2);
    }
  `],
})
export class SidebarComponent {}
