import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register';
import { AuthService } from '../../services/auth';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('RegisterComponent', () => { // Removed 'x' so the test runs!
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let routerMock: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['register']);
    authServiceMock.register.and.returnValue(of({}));
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.overrideComponent(RegisterComponent, {
      set: {
        template: `
          <form [(ngModel)]="registerData">
            <input [(ngModel)]="registerData.email" name="email" />
            <input [(ngModel)]="registerData.password" name="password" type="password" />
            <button (click)="onRegister()">Register</button>
          </form>
        `,
        styles: [],
        imports: [CommonModule, FormsModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,
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

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty registerData and default states', () => {
    expect(component.registerData.email).toBe('');
    expect(component.registerData.password).toBe('');
    expect(component.registerData.firstName).toBe('');
    expect(component.registerData.lastName).toBe('');
    expect(component.registerData.role).toBe('CLIENT');
    expect(component.isLoading).toBeFalse();
  });

  it('should set isLoading to true and call authService.register', () => {
    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    expect(component.isLoading).toBeTrue();
    expect(authServiceMock.register).toHaveBeenCalled();
    const callArg = authServiceMock.register.calls.mostRecent().args[0];
    expect(callArg instanceof FormData).toBeTrue();
  });

  it('should navigate to /auth/login on successful registration', () => {
    authServiceMock.register.and.returnValue(of({ message: 'Success' }));
    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    // Validating the new strict path
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('should handle registration error by resetting isLoading', () => {
    const errorResponse = { error: { error: 'Email already exists' }, status: 409 };
    authServiceMock.register.and.returnValue(throwError(() => errorResponse));

    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'existing@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    // The Global error interceptor handles the snackbar, we just need to ensure the button unlocks
    expect(component.isLoading).toBeFalse();
  });

  it('should handle file selection and show cropper', () => {
    const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file] } } as any;

    component.onFileSelected(event);

    expect(component.showCropper).toBeTrue();
    expect(component.imageChangedEvent).toBe(event);
  });

  it('should handle image blob from cropper', () => {
    const blob = new Blob(['test'], { type: 'image/png' });
    component.handleImageBlob(blob);

    expect(component.croppedBlob).toBe(blob);
    expect(component.croppedImage).toBeTruthy();
  });

  it('should include avatar file in registration for SELLER with cropped image', () => {
    const blob = new Blob(['test'], { type: 'image/png' });
    component.croppedBlob = blob;
    component.registerData = {
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      password: 'password123',
      role: 'SELLER'
    };

    component.onRegister();

    const callArg = authServiceMock.register.calls.mostRecent().args[0] as FormData;
    expect(callArg instanceof FormData).toBeTrue();
    expect(callArg.has('avatarFile')).toBeTrue();
  });

  it('should not include avatar for CLIENT role', () => {
    component.croppedBlob = new Blob(['test'], { type: 'image/png' });
    component.registerData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123',
      role: 'CLIENT'
    };

    component.onRegister();

    const callArg = authServiceMock.register.calls.mostRecent().args[0] as FormData;
    expect(callArg.has('avatarFile')).toBeFalse();
  });
});