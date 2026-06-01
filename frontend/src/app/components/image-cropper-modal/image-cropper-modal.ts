import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ImageCropperComponent, ImageCroppedEvent } from 'ngx-image-cropper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-image-cropper-modal',
  standalone: true,
  imports: [CommonModule, ImageCropperComponent, MatButtonModule, MatIconModule],
  templateUrl: './image-cropper-modal.html',
  styleUrls: ['./image-cropper-modal.scss'] // Updated to SCSS
})
export class ImageCropperModal {

  @Input() imageChangedEvent: any = '';

  @Output() croppedImageBlob = new EventEmitter<Blob>();
  @Output() modalClosed = new EventEmitter<void>();

  croppedImage: any = '';
  croppedBlob: Blob | null = null;

  constructor() { }

  imageCropped(event: ImageCroppedEvent) {
    this.croppedBlob = event.blob ?? null;
    this.croppedImage = event.base64; 
  }

  loadImageFailed() {
    console.error('Image failed to load in cropper');
    this.modalClosed.emit();
  }

  saveCrop() {
    if (this.croppedBlob) {
      this.croppedImageBlob.emit(this.croppedBlob);
    }
    this.modalClosed.emit();
  }

  cancelCrop() {
    this.modalClosed.emit();
  }
}