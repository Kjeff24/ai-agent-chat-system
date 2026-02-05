import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * On 401 Unauthorized from a protected API (not login/register), clears the session
 * and shows the login screen. Also shows a toast so the user knows why they were logged out.
 */
export const authExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const toast = inject(ToastService);
  const url = req.url;
  const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register');

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !isAuthEndpoint && auth.isLoggedIn) {
        auth.logout();
        const msg =
          (err.error && typeof err.error === 'object' && (err.error as { error?: string }).error) ||
          (err.error && typeof err.error === 'object' && (err.error as { message?: string }).message) ||
          err.message;
        toast.error(typeof msg === 'string' ? msg : 'Session expired. Please log in again.');
      }
      return throwError(() => err);
    })
  );
};
