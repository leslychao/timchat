import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'w',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/layout/app-layout.component').then(
        m => m.AppLayoutComponent,
      ),
    children: [
      {
        path: ':workspaceId/c/:channelId',
        loadComponent: () =>
          import('./features/chat/chat-placeholder.component').then(
            m => m.ChatPlaceholderComponent,
          ),
      },
      {
        path: ':workspaceId',
        loadComponent: () =>
          import('./features/workspace/workspace-placeholder.component').then(
            m => m.WorkspacePlaceholderComponent,
          ),
      },
      {
        path: '',
        loadComponent: () =>
          import('./features/workspace/workspace-placeholder.component').then(
            m => m.WorkspacePlaceholderComponent,
          ),
      },
    ],
  },
  { path: '', redirectTo: 'w', pathMatch: 'full' },
  { path: '**', redirectTo: 'w' },
];
