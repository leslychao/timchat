import { Component } from '@angular/core';

@Component({
  selector: 'app-navigation-rail',
  standalone: true,
  template: `
    <nav class="nav-rail">
      <div class="nav-rail__logo">
        <span class="nav-rail__logo-text">TC</span>
      </div>
      <div class="nav-rail__items">
        <!-- Workspace icons will be rendered here in Stage 10 -->
      </div>
    </nav>
  `,
  styles: [`
    .nav-rail {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: var(--nav-rail-width);
      height: 100%;
      background: var(--color-bg-tertiary);
      border-right: 1px solid var(--color-border-primary);
      padding: var(--spacing-2) 0;
      flex-shrink: 0;
    }

    .nav-rail__logo {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: var(--radius-md);
      background: var(--color-accent);
      color: var(--color-text-inverse);
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      margin-bottom: var(--spacing-3);
      user-select: none;
    }

    .nav-rail__items {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--spacing-1);
      flex: 1;
      overflow-y: auto;
    }
  `],
})
export class NavigationRailComponent {}
