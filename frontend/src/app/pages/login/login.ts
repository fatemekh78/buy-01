import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth'; // Adjusted path

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.scss'] // Updated to SCSS
})
export class LoginComponent {
  loginData = {
    email: '',
    password: ''
  };
  
  isLoading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) { }

  onLogin() {
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginData).subscribe({
      next: (response) => {
        this.authService.fetchCurrentUser().subscribe({
          next: () => this.router.navigate(['/home']),
          error: () => this.router.navigate(['/home'])
        });
      },
      error: (err) => {
        this.isLoading = false;
        // The error.interceptor handles the snackbar, but we can also show it locally
        this.errorMessage = 'Invalid email or password. Please try again.';
      }
    });
  }
}