import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { environment } from '../../../environments/environment';

interface TokenResponse {
  access_token: string;
  refresh_token?: string;
  token_type: string;
  expires_in: number;
}

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <div class="login">
      <div class="login__card">
        <div class="login__logo">TC</div>
        <h1 class="login__title">TimChat</h1>
        <p class="login__subtitle">Sign in to your workspace</p>
        @if (error) {
          <p class="login__error">{{ error }}</p>
        }
        @if (loading) {
          <p class="login__loading">Authenticating...</p>
        } @else {
          <button class="login__button" (click)="login()">
            Sign in with Keycloak
          </button>
        }
      </div>
    </div>
  `,
  styles: [`
    .login {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background: var(--color-bg-secondary);
    }

    .login__card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: var(--spacing-8);
      background: var(--color-bg-primary);
      border: 1px solid var(--color-border-primary);
      border-radius: var(--radius-lg);
      min-width: 320px;
    }

    .login__logo {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      background: var(--color-accent);
      color: var(--color-text-inverse);
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-semibold);
      margin-bottom: var(--spacing-4);
    }

    .login__title {
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-semibold);
      margin-bottom: var(--spacing-1);
    }

    .login__subtitle {
      font-size: var(--font-size-md);
      color: var(--color-text-secondary);
      margin-bottom: var(--spacing-6);
    }

    .login__error {
      font-size: var(--font-size-sm);
      color: var(--color-danger);
      margin-bottom: var(--spacing-4);
    }

    .login__loading {
      font-size: var(--font-size-md);
      color: var(--color-text-secondary);
    }

    .login__button {
      width: 100%;
      padding: var(--spacing-2) var(--spacing-4);
      background: var(--color-accent);
      color: var(--color-text-inverse);
      border: none;
      border-radius: var(--radius-md);
      font-size: var(--font-size-base);
      font-weight: var(--font-weight-medium);
      transition: background var(--transition-fast);

      &:hover {
        background: var(--color-accent-hover);
      }
    }
  `],
})
export class LoginComponent implements OnInit {
  private readonly authStore = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  loading = false;
  error = '';

  ngOnInit(): void {
    if (this.authStore.isAuthenticated()) {
      this.router.navigate(['/w']);
      return;
    }

    const code = this.route.snapshot.queryParamMap.get('code');
    if (code) {
      this.handleCallback(code);
    }
  }

  async login(): Promise<void> {
    const verifier = this.generateVerifier();
    sessionStorage.setItem('pkce_verifier', verifier);

    const challenge = await this.computeChallenge(verifier);

    const params = new URLSearchParams({
      client_id: environment.keycloak.clientId,
      response_type: 'code',
      scope: 'openid profile email',
      redirect_uri: environment.keycloak.redirectUri + '/login',
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });

    window.location.href =
      `${environment.keycloak.issuer}/protocol/openid-connect/auth?${params}`;
  }

  private async handleCallback(code: string): Promise<void> {
    this.loading = true;
    this.error = '';

    const verifier = sessionStorage.getItem('pkce_verifier');
    if (!verifier) {
      this.error = 'PKCE verifier not found. Please try again.';
      this.loading = false;
      return;
    }

    try {
      const body = new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: environment.keycloak.clientId,
        code,
        redirect_uri: environment.keycloak.redirectUri + '/login',
        code_verifier: verifier,
      });

      const response = await fetch(
        `${environment.keycloak.issuer}/protocol/openid-connect/token`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: body.toString(),
        },
      );

      if (!response.ok) {
        throw new Error(`Token exchange failed: ${response.status}`);
      }

      const data: TokenResponse = await response.json();
      this.authStore.setTokens(data.access_token, data.refresh_token);
      sessionStorage.removeItem('pkce_verifier');
      this.router.navigate(['/w']);
    } catch (e) {
      this.error = e instanceof Error ? e.message : 'Authentication failed';
      this.loading = false;
    }
  }

  private generateVerifier(): string {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return this.base64UrlEncode(array);
  }

  private async computeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const hash = await crypto.subtle.digest('SHA-256', data);
    return this.base64UrlEncode(new Uint8Array(hash));
  }

  private base64UrlEncode(buffer: Uint8Array): string {
    let str = '';
    for (const byte of buffer) {
      str += String.fromCharCode(byte);
    }
    return btoa(str)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }
}
