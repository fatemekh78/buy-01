import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth';
import { ImageCropperModal } from '../../components/image-cropper-modal/image-cropper-modal';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ImageCropperModal],
  templateUrl: './register.html',
  styleUrls: ['./register.scss'] // Updated to SCSS
})
export class RegisterComponent {
  registerData = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'CLIENT'
  };

  imageChangedEvent: any = '';
  showCropper = false;
  croppedImage: any = '';
  croppedBlob: Blob | null = null;
  isLoading = false;

  constructor(private authService: AuthService, private router: Router) { }

  onFileSelected(event: any): void {
    this.imageChangedEvent = event;
    this.showCropper = true;
  }

  handleImageBlob(blob: Blob) {
    this.croppedBlob = blob;
    this.croppedImage = URL.createObjectURL(blob);
  }

  handleModalClose() {
    this.showCropper = false;
    const fileInput = document.getElementById('avatar') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  onRegister(): void {
    this.isLoading = true;
    const formData = new FormData();

    // Exact match to your Spring Boot @RequestPart
    formData.append('userDto', new Blob([JSON.stringify(this.registerData)], {
      type: 'application/json'
    }));

    if (this.registerData.role === 'SELLER' && this.croppedBlob) {
      const avatarFile = new File([this.croppedBlob], 'avatar.png', { type: 'image/png' });
      formData.append('avatarFile', avatarFile);
    }

    this.authService.register(formData).subscribe({
      next: () => {
        this.router.navigate(['/auth/login']); // Fixed path to match routing
      },
      error: () => {
        this.isLoading = false; // Error interceptor handles the snackbar message
      }
    });
  }
}