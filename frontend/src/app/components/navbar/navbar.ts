import { Component, EventEmitter, Output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { User } from '../../models/user.model';
import { AuthService } from '../../services/auth';
import { OrderService } from '../../services/order.service'; // Note: Ensure the file name matches your actual file

// Import Angular Material modules
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatBadgeModule,
    MatDividerModule
  ],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss'] // Updated to SCSS
})
export class Navbar implements OnInit, OnDestroy {
  @Output() toggleSidenav = new EventEmitter<void>();

  public currentUser$: Observable<User | null>;
  public cartItemCount$: Observable<number>;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private router: Router,
    private orderService: OrderService
  ) {
    this.currentUser$ = this.authService.currentUser$;
    this.cartItemCount$ = this.orderService.cartItemCount$;
  }

  ngOnInit() {
    this.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      if (user && user.id && user.role === 'CLIENT') {
        this.orderService.loadCart(user.id).pipe(
          takeUntil(this.destroy$)
        ).subscribe({
          next: () => console.log('[Navbar] Cart synchronized.'),
          error: (error) => console.error('[Navbar] Error loading cart:', error)
        });
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // 🚨 FIX: Removed hardcoded localhost. Use relative paths so Nginx can proxy!
  getAvatarUrl(avatarPath: string): string {
    if (!avatarPath) return '';
    return avatarPath.startsWith('/') ? avatarPath : `/${avatarPath}`;
  }

  onLogout() {
    this.authService.logout().subscribe(() => {
      // 🚨 FIX: Strict routing to match app.routes.ts
      this.router.navigate(['/auth/login']);
    });
  }
}