import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { ProductDetailDTO, MediaUploadResponseDTO } from '../../models/product.model';
import { ProductService } from '../../services/product-service'; 
import { forkJoin, Observable } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';

@Component({
  selector: 'app-edit-product-modal',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatProgressSpinnerModule,
    MatDividerModule, MatTabsModule
  ],
  templateUrl: './edit-product-modal.html',
  styleUrls: ['./edit-product-modal.scss'] // Updated to SCSS
})
export class EditProductModal implements OnInit {
  editForm: FormGroup;
  product: ProductDetailDTO;

  currentMedia: MediaUploadResponseDTO[] = [];
  filesToUpload: File[] = [];
  fileErrors: string[] = [];
  isLoading = false;
  mediaToDelete: string[] = [];

  private maxSizeInBytes = 2 * 1024 * 1024; // 2MB
  private allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    public dialogRef: MatDialogRef<EditProductModal>,
    @Inject(MAT_DIALOG_DATA) public data: { product: ProductDetailDTO }
  ) {
    this.dialogRef.updateSize('600px'); // Give the tabs enough room to breathe
    
    this.product = data.product;
    this.currentMedia = JSON.parse(JSON.stringify(this.product.media || []));

    this.editForm = this.fb.group({
      name: [this.product.name, Validators.required],
      description: [this.product.description, Validators.required],
      price: [this.product.price, [Validators.required, Validators.min(0.01)]],
      quantity: [this.product.quantity, [Validators.required, Validators.min(0)]],
    });
  }

  ngOnInit(): void { }

  onFilesSelected(event: any): void {
    this.fileErrors = [];
    const files = event.target.files;

    if (files && files.length > 0) {
      for (const file of files) {
        if (!this.allowedTypes.includes(file.type)) {
          this.fileErrors.push(`File: "${file.name}" has an invalid type.`);
          continue;
        }
        if (file.size > this.maxSizeInBytes) {
          this.fileErrors.push(`File: "${file.name}" is too large (Max 2MB).`);
          continue;
        }
        this.filesToUpload.push(file);
      }
    }
  }

  removeNewFile(index: number): void {
    this.filesToUpload.splice(index, 1);
  }

  deleteExistingImage(mediaId: string, index: number): void {
    if (!this.mediaToDelete.includes(mediaId)) {
      this.mediaToDelete.push(mediaId);
    }
    this.currentMedia.splice(index, 1);
  }

  onSave(): void {
    if (this.editForm.invalid) return;
    this.isLoading = true;

    const tasks: Observable<any>[] = [];

    if (this.editForm.dirty) {
      tasks.push(this.productService.updateProduct(this.product.productId, this.editForm.value));
    }

    const uploadTasks = this.filesToUpload.map(file =>
      this.productService.uploadProductImage(this.product.productId, file)
    );
    tasks.push(...uploadTasks);

    const deleteTasks = this.mediaToDelete.map(mediaId =>
      this.productService.deleteProductImage(this.product.productId, mediaId)
    );
    tasks.push(...deleteTasks);

    if (tasks.length === 0) {
      this.isLoading = false;
      this.dialogRef.close(false); 
      return;
    }

    forkJoin(tasks).subscribe({
      next: () => {
        this.isLoading = false;
        this.dialogRef.close(true); 
      },
      error: (err) => {
        this.isLoading = false;
        // Letting the global error interceptor catch it is usually better than alert()
        console.error(err);
      }
    });
  }

  onCancel(): void {
    this.mediaToDelete = [];
    this.filesToUpload = [];
    this.dialogRef.close(false);
  }

  get hasChanges(): boolean {
    return this.editForm.dirty || this.filesToUpload.length > 0 || this.mediaToDelete.length > 0;
  }

  // 🚨 FIX: Allow Nginx to handle the proxying by using a relative path!
  getImageUrl(imagePath: string): string {
    if (!imagePath) return '';
    return imagePath.startsWith('/') ? imagePath : `/${imagePath}`;
  }
}