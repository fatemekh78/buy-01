import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth';
import { UserService } from '../../services/user';
import { User } from '../../models/user.model';
import { Router } from '@angular/router';
import { UpdateInfoForm } from '../../components/update-info-form/update-info-form';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PasswordConfirmDialog } from '../../components/password-confirm-dialog/password-confirm-dialog';
import { ConfirmDialog } from '../../components/confirm-dialog/confirm-dialog';
import { ImageCropperModal } from '../../components/image-cropper-modal/image-cropper-modal';

@Component({
  selector: 'app-my-info',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatDividerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    ImageCropperModal,
    UpdateInfoForm
  ],
  templateUrl: './my-info.html',
  styleUrls: ['./my-info.scss'] // Updated to SCSS
})
export class MyInfo implements OnInit {
  currentUser: User | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  imageChangedEvent: any = '';
  showCropper = false;
  isEditingInfo = false;

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    public dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.authService.fetchCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Could not load user data.';
        this.isLoading = false;
      }
    });
  }

  onFileSelect(event: any): void {
    this.imageChangedEvent = event;
    this.showCropper = true; 
  }

  handleAvatarBlob(blob: Blob) {
    if (!this.currentUser) return;
    const avatarFile = new File([blob], 'avatar.png', { type: 'image/png' });

    this.userService.updateAvatar(avatarFile).subscribe({
      next: () => {
        this.ngOnInit();
        this.authService.fetchCurrentUser().subscribe(); 
      },
      error: (err) => console.error('Failed to update avatar', err)
    });
  }

  handleModalClose() {
    this.showCropper = false;
    const fileInput = document.getElementById('avatar-upload-input') as HTMLInputElement;
    if (fileInput) fileInput.value = '';
  }

  // 🚨 FIX: Strict relative pathing for Nginx
  getAvatarUrl(avatarPath: string): string {
    if (!avatarPath) return '';
    return avatarPath.startsWith('/') ? avatarPath : `/${avatarPath}`;
  }

  onDeleteAvatar(): void {
    if (!this.currentUser) return;

    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Delete Avatar',
        message: 'Are you sure you want to delete your avatar? This action cannot be undone.',
        confirmText: 'Delete',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true && this.currentUser) {
        this.userService.deleteAvatar().subscribe({
          next: () => this.ngOnInit(),
          error: (err) => console.error('Failed to delete avatar', err)
        });
      }
    });
  }

  onDeleteMe(): void {
    const dialogRef = this.dialog.open(PasswordConfirmDialog, {
      data: {
        title: 'Delete Account',
        message: 'This action is permanent. To confirm, please enter your password.',
        confirmText: 'Delete Account',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(password => {
      if (password) {
        this.userService.deleteUser(password).subscribe({
          next: (response) => {
            console.log('User deleted:', response.message);
            this.authService.logout().subscribe(() => {
              this.router.navigate(['/register']);
            });
          },
          error: (err) => {
            console.error('Failed to delete user:', err);
            alert(`Error: ${err.error?.message || 'Wrong password or server error.'}`);
          }
        });
      }
    });
  }

  onUpdateInfo(): void {
    this.isEditingInfo = true;
  }

  onFormClosed(isSuccess: boolean): void {
    this.isEditingInfo = false;
    if (isSuccess) this.ngOnInit();
  }
}