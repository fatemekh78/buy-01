import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product-service';
import { forkJoin, Observable } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-create-product',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule
  ],
  templateUrl: './create-product.html',
  styleUrls: ['./create-product.scss'] // Updated to SCSS
})
export class CreateProduct implements OnInit {
  productForm: FormGroup;
  selectedFiles: File[] = [];
  isUploading = false;
  fileErrors: string[] = []; 
  
  private maxSizeInBytes = 12 * 1024 * 1024; // 12MB
  private allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private productService: ProductService,
    private router: Router
  ) {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      price: [null, [Validators.required, Validators.min(0.01)]],
      quantity: [null, [Validators.required, Validators.min(0)]],
    });
  }

  ngOnInit(): void {
    this.authService.fetchCurrentUser().subscribe({
      error: () => {
        // Handled silently. Navbar routing manages unauthenticated state.
      }
    });
  }

  onFilesSelected(event: any): void {
    this.fileErrors = []; 
    const files = event.target.files;

    if (files && files.length > 0) {
      for (const file of files) {
        if (!this.allowedTypes.includes(file.type)) {
          this.fileErrors.push(`File: "${file.name}" has an invalid type. Only PNG, JPG, GIF, WebP are allowed.`);
          continue; 
        }
        if (file.size > this.maxSizeInBytes) {
          this.fileErrors.push(`File: "${file.name}" is too large (Max 12MB).`);
          continue; 
        }
        this.selectedFiles.push(file);
      }
    }
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  onSubmit(): void {
    if (this.productForm.invalid || this.isUploading) return;
    
    this.isUploading = true;

    this.productService.createProduct(this.productForm.value).subscribe({
      next: (productResponse: any) => {
        const newProductId = productResponse.id;

        if (this.selectedFiles.length > 0) {
          const uploadObservables: Observable<any>[] = this.selectedFiles.map(file => {
            return this.productService.uploadProductImage(newProductId, file);
          });

          forkJoin(uploadObservables).subscribe({
            next: () => {
              this.isUploading = false;
              this.router.navigate(['/my-products']);
            },
            error: (err) => {
              console.error('One or more image uploads failed', err);
              this.isUploading = false;
              alert('Product created, but image upload failed.');
              this.router.navigate(['/my-products']);
            }
          });
        } else {
          this.isUploading = false;
          this.router.navigate(['/my-products']);
        }
      },
      error: (err) => {
        console.error('Failed to create product', err);
        this.isUploading = false;
        alert('Error creating product. Please try again.');
      }
    });
  }
}