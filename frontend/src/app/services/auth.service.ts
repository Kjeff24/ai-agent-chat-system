import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

const AUTH_TOKEN_KEY = 'auth_token';
const USER_ID_KEY = 'user_id';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private loggedInSubject = new BehaviorSubject<boolean>(this.hasStoredToken());
  readonly isLoggedIn$ = this.loggedInSubject.asObservable();

  constructor(private http: HttpClient) {}

  private hasStoredToken(): boolean {
    return !!localStorage.getItem(AUTH_TOKEN_KEY);
  }

  login(email: string, password: string): Observable<{ token: string; userId: string }> {
    return this.http
      .post<{ token: string; userId: string }>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(
        tap(({ token, userId }) => {
          localStorage.setItem(AUTH_TOKEN_KEY, token);
          localStorage.setItem(USER_ID_KEY, userId);
          this.loggedInSubject.next(true);
        })
      );
  }

  register(
    name: string,
    email: string,
    password: string
  ): Observable<{ token: string; userId: string }> {
    return this.http
      .post<{ token: string; userId: string }>(`${environment.apiUrl}/auth/register`, {
        name,
        email,
        password
      })
      .pipe(
        tap(({ token, userId }) => {
          localStorage.setItem(AUTH_TOKEN_KEY, token);
          localStorage.setItem(USER_ID_KEY, userId);
          this.loggedInSubject.next(true);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(USER_ID_KEY);
    this.loggedInSubject.next(false);
  }

  get isLoggedIn(): boolean {
    return this.loggedInSubject.value;
  }
}
