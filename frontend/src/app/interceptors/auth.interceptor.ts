import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const cookieService = inject(CookieService);

  // 1. EXTRACT AND ATTACH THE TOKEN
  const token = cookieService.get('jwt');
  let authReq = req;
  
  // Only attach the Bearer token if it exists and we aren't trying to log in/register
  const isAuthEndpoint = req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register');
  
  if (token && !isAuthEndpoint) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // 2. PROCESS THE REQUEST AND CATCH ERRORS
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Check if the error is a 401 Unauthorized from the backend
      if (error.status === 401 && !isAuthEndpoint) {
        console.log('Session expired or token is invalid. Redirecting to login.');
        
        // Wipe the dead cookie
        cookieService.delete('jwt', '/'); 
        
        // Boot the user back to the login screen
        router.navigate(['/auth/login']); // Adjusted to match your app.routes.ts path
      }
      
      // Pass the error down the chain to the error.interceptor
      return throwError(() => error);
    })
  );
};