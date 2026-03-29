import { computed, Injectable, signal } from '@angular/core';

export interface AuthUser {
  sub: string;
  username: string;
  email: string;
  roles: string[];
}

const TOKEN_KEY = 'tc_access_token';
const REFRESH_TOKEN_KEY = 'tc_refresh_token';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly _token = signal<string | null>(
    localStorage.getItem(TOKEN_KEY),
  );
  private readonly _user = signal<AuthUser | null>(null);

  readonly token = this._token.asReadonly();
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());

  constructor() {
    const stored = this._token();
    if (stored) {
      this._user.set(this.decodeToken(stored));
    }
  }

  setTokens(accessToken: string, refreshToken?: string): void {
    localStorage.setItem(TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
    this._token.set(accessToken);
    this._user.set(this.decodeToken(accessToken));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    this._token.set(null);
    this._user.set(null);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  private decodeToken(token: string): AuthUser | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = JSON.parse(atob(parts[1]));
      return {
        sub: payload.sub ?? '',
        username: payload.preferred_username ?? '',
        email: payload.email ?? '',
        roles: payload.realm_access?.roles ?? [],
      };
    } catch {
      return null;
    }
  }
}
