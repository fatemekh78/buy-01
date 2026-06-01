import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { SidenavComponent } from './sidenav';
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';
import { RouterTestingModule } from '@angular/router/testing';
import { routes } from '../../app.routes';

describe('SidenavComponent', () => { // 🚨 FIX: Removed 'x'
  let component: SidenavComponent;
  let fixture: ComponentFixture<SidenavComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let router: Router;
  let currentUserSubject: BehaviorSubject<User | null>;

  beforeEach(async () => {
    currentUserSubject = new BehaviorSubject<User | null>(null);

    authServiceMock = jasmine.createSpyObj('AuthService', ['logout']);
    authServiceMock.logout.and.returnValue(of(void 0));
    Object.defineProperty(authServiceMock, 'currentUser$', {
      get: () => currentUserSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [
        SidenavComponent,
        RouterTestingModule.withRoutes(routes),
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SidenavComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate'); 
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call isSeller and return role from authService', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'SELLER',
      configurable: true
    });
    expect(component.isSeller()).toBeTrue();
  });

  it('should return false from isSeller when not SELLER', () => {
    Object.defineProperty(authServiceMock, 'currentUserRole', {
      get: () => 'CLIENT',
      configurable: true
    });
    expect(component.isSeller()).toBeFalse();
  });

  it('should call logout, emit closeSidenav, and navigate to /auth/login', () => {
    spyOn(component.closeSidenav, 'emit');

    component.logout();

    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(component.closeSidenav.emit).toHaveBeenCalled();
    // 🚨 FIX: Validating the new strict route path
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']); 
  });

  it('should handle logout error gracefully and still route out', () => {
    authServiceMock.logout.and.returnValue(throwError(() => ({ status: 500 })));
    spyOn(component.closeSidenav, 'emit');
    
    component.logout();
    
    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(component.closeSidenav.emit).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']); 
  });
});