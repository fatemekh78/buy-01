import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BehaviorSubject, of } from 'rxjs';
import { Navbar } from './navbar';
import { AuthService } from '../../services/auth';
import { OrderService } from '../../services/order.service'; // Adjust path!
import { User } from '../../models/user.model';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';

describe('Navbar', () => { // Removed 'x'
  let component: Navbar;
  let fixture: ComponentFixture<Navbar>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let orderServiceMock: jasmine.SpyObj<OrderService>;
  let routerMock: jasmine.SpyObj<Router>;
  let currentUserSubject: BehaviorSubject<User | null>;
  let cartItemCountSubject: BehaviorSubject<number>;

  beforeEach(async () => {
    currentUserSubject = new BehaviorSubject<User | null>(null);
    cartItemCountSubject = new BehaviorSubject<number>(0);

    authServiceMock = jasmine.createSpyObj('AuthService', ['logout']);
    authServiceMock.logout.and.returnValue(of(void 0));
    Object.defineProperty(authServiceMock, 'currentUser$', { get: () => currentUserSubject.asObservable() });

    // 🚨 ADDED: Missing OrderService Mock
    orderServiceMock = jasmine.createSpyObj('OrderService', ['loadCart']);
    orderServiceMock.loadCart.and.returnValue(of(null));
    Object.defineProperty(orderServiceMock, 'cartItemCount$', { get: () => cartItemCountSubject.asObservable() });

    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [Navbar, HttpClientTestingModule, CommonModule, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: OrderService, useValue: orderServiceMock }, // 🚨 Injected!
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Navbar);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call loadCart on init if user is CLIENT', () => {
    const mockUser: User = { id: '1', role: 'CLIENT' } as User;

    // Trigger ngOnInit subscription
    fixture.detectChanges();
    currentUserSubject.next(mockUser);

    expect(orderServiceMock.loadCart).toHaveBeenCalledWith('1');
  });

  it('should NOT call loadCart on init if user is SELLER', () => {
    const mockUser: User = { id: '2', role: 'SELLER' } as User;

    fixture.detectChanges();
    currentUserSubject.next(mockUser);

    expect(orderServiceMock.loadCart).not.toHaveBeenCalled();
  });

  it('should build relative avatar URL for Nginx routing', () => {
    // 🚨 FIX: Verifying relative paths
    const url1 = component.getAvatarUrl('/uploads/avatar123.jpg');
    expect(url1).toBe('/uploads/avatar123.jpg');

    const url2 = component.getAvatarUrl('uploads/avatar.jpg');
    expect(url2).toBe('/uploads/avatar.jpg');
  });

  it('should call logout and navigate to /auth/login on onLogout', () => {
    component.onLogout();

    expect(authServiceMock.logout).toHaveBeenCalled();
    // 🚨 FIX: Match new route layout
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});