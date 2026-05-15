import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

@Injectable()
export class ApiInterceptor implements HttpInterceptor {
  constructor(
    private auth: AuthService,
    private notifications: NotificationService
  ) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.getToken();
    const authReq = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        const message = this.extractMessage(error);
        this.notifications.error(message);
        return throwError(() => error);
      })
    );
  }

  private extractMessage(error: HttpErrorResponse): string {
    if (error.status === 0) return 'Cannot connect to the server. Is the backend running?';
    if (error.status === 404) return error.error?.detail ?? 'Resource not found.';
    if (error.status === 409) return error.error?.detail ?? 'Conflict: resource already exists.';
    if (error.status >= 400 && error.status < 500) return error.error?.detail ?? 'Invalid request.';
    return 'Server error. Please try again later.';
  }
}
