import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { MyInfo } from './my-info';
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user';
import { Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { NoopAnimationsModule } from '@angular/platform-browser/animations'; // 🚨 Prevents Karma Crash

describe('MyInfo', () => { // 🚨 Removed 'x'
  let component: MyInfo;
  let fixture: ComponentFixture<MyInfo>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let userServiceMock: jasmine.SpyObj<UserService>;
  let routerMock: jasmine.SpyObj<Router>;
  let dialogMock: jasmine.SpyObj<MatDialog>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser', 'logout']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      role: 'CLIENT',
      avatarUrl: '/uploads/avatar.jpg'
    } as any));
    authServiceMock.logout.and.returnValue(of({}));

    userServiceMock = jasmine.createSpyObj('UserService', ['updateUser', 'updateAvatar', 'deleteAvatar', 'deleteUser']);
    userServiceMock.updateAvatar.and.returnValue(of({} as any));
    userServiceMock.deleteAvatar.and.returnValue(of('Avatar deleted'));
    userServiceMock.deleteUser.and.returnValue(of({ message: 'User deleted' }));

    routerMock = jasmine.createSpyObj('Router', ['navigate']);
    dialogMock = jasmine.createSpyObj('MatDialog', ['open']);

    // Override component to use inline template for simpler testing
    TestBed.overrideComponent(MyInfo, {
      set: {
        template: `
          <div *ngIf="currentUser">
            <h1>{{ currentUser.firstName }} {{ currentUser.lastName }}</h1>
            <p>{{ currentUser.email }}</p>
            <button (click)="onDeleteAvatar()">Delete Avatar</button>
            <button (click)="onDeleteMe()">Delete Account</button>
          </div>
        `,
        styles: [],
        imports: [CommonModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        MyInfo, 
        HttpClientTestingModule, 
        CommonModule, 
        MatDialogModule,
        NoopAnimationsModule // 🚨 Critical for Material Dialog testing
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: MatDialog, useValue: dialogMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyInfo);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch current user on init', () => {
    fixture.detectChanges();
    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
    expect(component.currentUser).toBeTruthy();
    expect(component.currentUser?.email).toBe('john@example.com');
  });

  it('should upload avatar when blob is provided', () => {
    component.currentUser = { id: '1', email: 'test@example.com' } as any;
    const blob = new Blob(['test'], { type: 'image/png' });

    component.handleAvatarBlob(blob);

    expect(userServiceMock.updateAvatar).toHaveBeenCalled();
    const callArg = userServiceMock.updateAvatar.calls.mostRecent().args[0];
    expect(callArg instanceof File).toBe(true);
  });

  it('should get full relative avatar URL for Nginx proxy', () => {
    // 🚨 FIX: Nginx expects a relative route
    const url = component.getAvatarUrl('/uploads/avatar.jpg');
    expect(url).toBe('/uploads/avatar.jpg');
  });

  it('should delete user and logout when password provided', () => {
    const dialogRefMock = { afterClosed: () => of('password123') };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDeleteMe();

    expect(userServiceMock.deleteUser).toHaveBeenCalledWith('password123');
    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/register']);
  });

  it('should refresh user data when form closed successfully', () => {
    component.isEditingInfo = true;
    spyOn(component, 'ngOnInit');

    component.onFormClosed(true);

    expect(component.isEditingInfo).toBe(false);
    expect(component.ngOnInit).toHaveBeenCalled();
  });
});