import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class LoggedInGuard implements CanActivate {

  constructor(private cookieService: CookieService, private router: Router) { }

  canActivate(): boolean {
    const isLoggedIn = this.cookieService.check('jwt');

    if (isLoggedIn) {
      return true;
    }

    // 🚨 FIX 3: Updated to match your exact route definitions in app.routes.ts
    this.router.navigate(['/auth/login']);
    return false;
  }
}