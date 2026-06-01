import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

describe('ConfirmDialog', () => {
  let component: ConfirmDialog;
  let fixture: ComponentFixture<ConfirmDialog>;
  let dialogRefMock: jasmine.SpyObj<MatDialogRef<ConfirmDialog>>;
  
  const mockData: ConfirmDialogData = {
    title: 'Confirm Action',
    message: 'Are you sure you want to proceed?',
    confirmText: 'Yes, Proceed',
    cancelText: 'Nevermind'
  };

  beforeEach(async () => {
    dialogRefMock = jasmine.createSpyObj('MatDialogRef', ['close', 'updateSize']);

    // Overriding template with a lightweight version for unit testing logic
    TestBed.overrideComponent(ConfirmDialog, {
      set: {
        template: `
          <h2>{{ data.title }}</h2>
          <p>{{ data.message }}</p>
          <button class="confirm-btn" (click)="onConfirm()">{{ data.confirmText || 'Confirm' }}</button>
          <button class="cancel-btn" (click)="onCancel()">{{ data.cancelText || 'Cancel' }}</button>
        `,
        styles: [],
        imports: [CommonModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [ConfirmDialog, CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: mockData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and set dialog size on init', () => {
    expect(component).toBeTruthy();
    expect(dialogRefMock.updateSize).toHaveBeenCalledWith('400px');
  });

  it('should display provided title and message', () => {
    expect(component.data.title).toBe('Confirm Action');
    expect(component.data.message).toBe('Are you sure you want to proceed?');
  });

  it('should close dialog with false on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it('should close dialog with true on confirm', () => {
    component.onConfirm();
    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });
});