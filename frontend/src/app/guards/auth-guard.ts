import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private cookieService: CookieService, private router: Router) {}

  canActivate(): boolean {
    const isLoggedIn = this.cookieService.check('jwt');

    if (isLoggedIn) {
      this.router.navigate(['/home']); 
      return false; 
    }

    return true; 
  }
}