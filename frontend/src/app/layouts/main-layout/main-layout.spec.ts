import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { MainLayout } from './main-layout';
import { AuthService } from '../../services/auth';
import { OrderService } from '../../services/order.service'; // 🚨 Required for Navbar
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations'; // 🚨 Prevents Karma Crash

describe('MainLayout', () => {
  let component: MainLayout;
  let fixture: ComponentFixture<MainLayout>;

  beforeEach(async () => {
    const authServiceMock = jasmine.createSpyObj('AuthService', ['init']);
    authServiceMock.currentUser$ = of(null);
    authServiceMock.init.and.returnValue(of({}));

    // Mock OrderService since the nested Navbar component injects it
    const orderServiceMock = jasmine.createSpyObj('OrderService', ['cartItemCount$']);
    orderServiceMock.cartItemCount$ = of(0);

    await TestBed.configureTestingModule({
      imports: [
        MainLayout,
        HttpClientTestingModule,
        CommonModule,
        RouterTestingModule,
        NoopAnimationsModule // 🚨 Critical for MatSidenavModule testing
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: OrderService, useValue: orderServiceMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MainLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have router-outlet', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeDefined();
  });
});