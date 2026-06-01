import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError, finalize } from 'rxjs/operators';
import { User } from '../models/user.model';
import { CookieService } from 'ngx-cookie-service';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // 🚨 FIX 1: Use relative paths so Nginx can seamlessly proxy the requests!
  private authApiUrl = '/api/auth';
  private usersApiUrl = '/api/users';

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public readonly currentUser$ = this.currentUserSubject.asObservable();

  private userLoaded = false;

  constructor(
    private http: HttpClient,
    private cookieService: CookieService,
    private router: Router
  ) { }

  public get currentUserRole(): string | null {
    return this.currentUserSubject.value?.role || null;
  }

  init(): Observable<User | null> {
    if (this.userLoaded) {
      return this.currentUser$;
    }
    this.userLoaded = true;
    return this.fetchCurrentUser().pipe(
      catchError(err => {
        this.currentUserSubject.next(null);
        return throwError(() => err);
      })
    );
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.authApiUrl}/login`, credentials, {
      withCredentials: true
    }).pipe(
      tap(() => this.fetchCurrentUser().subscribe())
    );
  }

  fetchCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.usersApiUrl}/me`, { withCredentials: true }).pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401) {
          this.currentUserSubject.next(null);
        }
        return throwError(() => err);
      })
    );
  }

  logout(): Observable<any> {
    // 🚨 FIX 2: Do NOT delete the cookie before the request, otherwise the interceptor 
    // won't be able to attach the Bearer token for the logout endpoint!
    return this.http.post(`${this.authApiUrl}/logout`, {}, { withCredentials: true }).pipe(
      finalize(() => {
        // Wipe the cookie after the request resolves (success or fail)
        this.cookieService.delete('jwt', '/'); 
        this.currentUserSubject.next(null);
        this.router.navigate(['/auth/login']);
      })
    );
  }

  register(formData: FormData): Observable<any> {
    return this.http.post(`${this.authApiUrl}/register`, formData);
  }
}