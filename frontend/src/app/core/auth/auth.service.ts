import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'cosmolab_token';

  login(): void {
    sessionStorage.setItem(this.tokenKey, 'eyJhbGciOiJIUzI1NiJ9.mock.signature');
  }

  logout(): void {
    sessionStorage.removeItem(this.tokenKey);
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.tokenKey);
  }
}
