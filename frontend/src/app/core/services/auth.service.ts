import { Injectable } from '@angular/core';

const TOKEN_KEY = 'token';
const MOCK_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.mock.signature';

@Injectable({ providedIn: 'root' })
export class AuthService {
  login(): void {
    sessionStorage.setItem(TOKEN_KEY, MOCK_TOKEN);
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
  }

  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
