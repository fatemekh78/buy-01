import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { UpdateInfoForm } from './update-info-form';
import { UserService } from '../../services/user';
import { User } from '../../models/user.model';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { NoopAnimationsModule } from '@angular/platform-browser/animations'; // 🚨 Fixes Material Inputs

describe('UpdateInfoForm', () => { // 🚨 Removed 'x'
  let component: UpdateInfoForm;
  let fixture: ComponentFixture<UpdateInfoForm>;
  let userServiceMock: jasmine.SpyObj<UserService>;

  const mockUser: User = {
    id: 'user-123',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    role: 'CLIENT',
    avatarUrl: undefined
  };

  beforeEach(async () => {
    userServiceMock = jasmine.createSpyObj('UserService', ['updateUser']);
    userServiceMock.updateUser.and.returnValue(of({} as any));

    await TestBed.configureTestingModule({
      imports: [
        UpdateInfoForm,
        HttpClientTestingModule,
        ReactiveFormsModule,
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatDividerModule,
        MatIconModule,
        NoopAnimationsModule // 🚨 Prevents Karma Crash
      ],
      providers: [
        { provide: UserService, useValue: userServiceMock },
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UpdateInfoForm);
    component = fixture.componentInstance;
    component.currentUser = mockUser;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with current user data', () => {
    expect(component.updateForm.value.firstName).toBe('John');
    expect(component.updateForm.value.lastName).toBe('Doe');
    expect(component.updateForm.value.email).toBe('john@example.com');
  });

  it('should not submit invalid form', () => {
    component.updateForm.patchValue({ firstName: '' });
    component.updateForm.get('firstName')?.markAsTouched();

    component.onSubmit();

    expect(userServiceMock.updateUser).not.toHaveBeenCalled();
  });

  it('should require currentPassword when changing email', () => {
    component.updateForm.patchValue({ email: 'newemail@example.com', currentPassword: '' });

    component.onSubmit();

    expect(component.errorMessage).toContain('Current Password is required');
    expect(userServiceMock.updateUser).not.toHaveBeenCalled();
  });

  // 🚨 Using fakeAsync to handle the setTimeout!
  it('should emit close event with true on success after 2 seconds', fakeAsync(() => {
    spyOn(component.close, 'emit');
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();
    
    expect(component.successMessage).toBe('Profile updated successfully!');
    expect(component.close.emit).not.toHaveBeenCalled(); // Not called yet!
    
    tick(2000); // Fast forward 2 seconds
    
    expect(component.close.emit).toHaveBeenCalledWith(true);
  }));

  it('should handle update error synchronously', () => {
    userServiceMock.updateUser.and.returnValue(throwError(() => ({ error: { message: 'Update failed' } })));
    component.updateForm.patchValue({ firstName: 'Jane' });

    component.onSubmit();

    expect(component.errorMessage).toBe('Update failed');
    expect(component.isLoading).toBe(false);
  });

  it('should emit close event with false on cancel', () => {
    spyOn(component.close, 'emit');
    component.onCancel();
    expect(component.close.emit).toHaveBeenCalledWith(false);
  });

  // 🚨 Using fakeAsync to clear the timer
  it('should include all changed fields in DTO', fakeAsync(() => {
    component.updateForm.patchValue({
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      currentPassword: 'oldpass'
    });

    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith({
      firstName: 'Jane',
      lastName: 'Smith',
      email: 'jane@example.com',
      currentPassword: 'oldpass'
    });
    
    tick(2000); // Flush the timer so Karma doesn't complain
  }));
});