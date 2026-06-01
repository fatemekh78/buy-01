import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('LoginComponent', () => { // Removed 'x' so the test runs!
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let routerMock: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['login', 'fetchCurrentUser']);
    authServiceMock.login.and.returnValue(of({}));
    authServiceMock.fetchCurrentUser.and.returnValue(of({ role: 'CLIENT', email: 'test@example.com' } as any));
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    // Override component to use a simple inline template for testing logic
    TestBed.overrideComponent(LoginComponent, {
      set: {
        template: `
          <form [(ngModel)]="loginData">
            <input [(ngModel)]="loginData.email" name="email" />
            <input [(ngModel)]="loginData.password" name="password" type="password" />
            <button (click)="onLogin()">Login</button>
          </form>
        `,
        styles: [],
        imports: [CommonModule, FormsModule] // Ensure standalone imports are present
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        LoginComponent,
        HttpClientTestingModule,
        FormsModule,
        CommonModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty loginData and default states', () => {
    expect(component.loginData.email).toBe('');
    expect(component.loginData.password).toBe('');
    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBe('');
  });

  it('should set isLoading to true and call authService.login', () => {
    component.loginData = { email: 'test@example.com', password: 'password123' };
    component.onLogin();

    expect(component.isLoading).toBeTrue();
    expect(authServiceMock.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'password123'
    });
  });

  it('should navigate to /home on successful login and user fetch', () => {
    component.loginData = { email: 'client@example.com', password: 'password' };
    component.onLogin();

    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should navigate to /home even if fetchCurrentUser fails', () => {
    authServiceMock.fetchCurrentUser.and.returnValue(throwError(() => ({ status: 500 })));
    component.loginData = { email: 'test@example.com', password: 'password' };
    component.onLogin();

    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should handle login error by resetting isLoading and setting errorMessage', () => {
    const errorResponse = { error: { error: 'Invalid credentials' }, status: 401 };
    authServiceMock.login.and.returnValue(throwError(() => errorResponse));

    component.loginData = { email: 'wrong@example.com', password: 'wrongpass' };
    component.onLogin();

    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBe('Invalid email or password. Please try again.');
  });
});