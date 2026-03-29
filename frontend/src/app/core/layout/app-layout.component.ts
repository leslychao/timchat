import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavigationRailComponent } from './navigation-rail.component';
import { SidebarComponent } from './sidebar.component';
import { TopBarComponent } from './top-bar.component';
import { RightPanelComponent } from './right-panel.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    NavigationRailComponent,
    SidebarComponent,
    TopBarComponent,
    RightPanelComponent,
  ],
  template: `
    <div class="app-layout">
      <app-navigation-rail />
      <app-sidebar />
      <div class="app-layout__main-wrapper">
        <app-top-bar />
        <main class="app-layout__main">
          <router-outlet />
        </main>
      </div>
      <app-right-panel />
    </div>
  `,
  styles: [`
    .app-layout {
      display: flex;
      height: 100vh;
      width: 100vw;
      overflow: hidden;
    }

    .app-layout__main-wrapper {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-width: 0;
    }

    .app-layout__main {
      flex: 1;
      overflow-y: auto;
      background: var(--color-bg-primary);
    }
  `],
})
export class AppLayoutComponent {}
