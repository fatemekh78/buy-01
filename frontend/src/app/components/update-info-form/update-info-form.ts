import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { User } from '../../models/user.model';
import { UpdateUserDTO } from '../../models/update-user.dto';
import { UserService } from '../../services/user';

// Import Material Modules
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-update-info-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatIconModule
  ],
  templateUrl: './update-info-form.html',
  styleUrls: ['./update-info-form.scss'] // Updated to SCSS
})
export class UpdateInfoForm implements OnInit {
  @Input() currentUser!: User; 
  @Output() close = new EventEmitter<boolean>(); 

  updateForm: FormGroup;
  isLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService
  ) {
    this.updateForm = this.fb.group({
      firstName: ['', Validators.minLength(2)],
      lastName: ['', Validators.minLength(2)],
      email: ['', Validators.email],
      currentPassword: [''], 
      newPassword: ['', Validators.minLength(5)]
    });
  }

  ngOnInit(): void {
    if (this.currentUser) {
      this.updateForm.patchValue({
        firstName: this.currentUser.firstName,
        lastName: this.currentUser.lastName,
        email: this.currentUser.email
      });
    }
  }

  onSubmit(): void {
    if (this.updateForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const formValues = this.updateForm.value;
    const dto: UpdateUserDTO = {};

    if (formValues.firstName !== this.currentUser.firstName) dto.firstName = formValues.firstName;
    if (formValues.lastName !== this.currentUser.lastName) dto.lastName = formValues.lastName;
    if (formValues.email !== this.currentUser.email) dto.email = formValues.email;
    if (formValues.newPassword) dto.newPassword = formValues.newPassword;

    if (dto.email || dto.newPassword) {
      if (!formValues.currentPassword) {
        this.updateForm.controls['currentPassword'].setErrors({ required: true });
        this.errorMessage = 'Current Password is required to change Email or New Password.';
        this.isLoading = false;
        return;
      }
      dto.currentPassword = formValues.currentPassword;
    }

    this.userService.updateUser(dto).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Profile updated successfully!';
        setTimeout(() => this.close.emit(true), 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'An unknown error occurred.';
      }
    });
  }

  onCancel(): void {
    this.close.emit(false); 
  }
}