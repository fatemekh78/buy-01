import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';

export interface PasswordDialogData {
  title: string;
  message: string;
  confirmText?: string;      // E.g., 'Verify', 'Delete Account'
  cancelText?: string;       
  isDestructive?: boolean;  // Controls whether the button is Green (safe) or Red (danger)
}

@Component({
  selector: 'app-password-confirm-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule
  ],
  templateUrl: './password-confirm-dialog.html',
  styleUrls: ['./password-confirm-dialog.scss'] // Updated to SCSS
})
export class PasswordConfirmDialog {
  password = '';

  constructor(
    public dialogRef: MatDialogRef<PasswordConfirmDialog>,
    @Inject(MAT_DIALOG_DATA) public data: PasswordDialogData
  ) {
    this.dialogRef.updateSize('420px'); // Standardized width
  }

  onCancel(): void {
    this.dialogRef.close(); 
  }

  onConfirm(): void {
    this.dialogRef.close(this.password); 
  }
}