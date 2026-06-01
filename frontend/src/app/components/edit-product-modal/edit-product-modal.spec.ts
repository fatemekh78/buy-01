import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { EditProductModal } from './edit-product-modal';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations'; // 🚨 Critical for MatTabsModule

describe('EditProductModal', () => { // 🚨 Removed 'x'
  let component: EditProductModal;
  let fixture: ComponentFixture<EditProductModal>;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let dialogRefMock: jasmine.SpyObj<MatDialogRef<EditProductModal>>;

  const mockProduct: ProductDetailDTO = {
    productId: 'test-id',
    name: 'Test Product',
    description: 'Test Description',
    price: 99.99,
    quantity: 10,
    sellerId: 'seller-123',
    sellerFirstName: 'John',
    sellerLastName: 'Doe',
    sellerEmail: 'john@example.com',
    createdByMe: true,
    media: [
      { fileId: 'media-1', fileUrl: '/uploads/image1.jpg', productId: 'test-id' },
      { fileId: 'media-2', fileUrl: '/uploads/image2.jpg', productId: 'test-id' }
    ]
  };

  beforeEach(async () => {
    productServiceMock = jasmine.createSpyObj('ProductService', [
      'updateProduct',
      'uploadProductImage',
      'deleteProductImage'
    ]);
    productServiceMock.updateProduct.and.returnValue(of(mockProduct));
    productServiceMock.uploadProductImage.and.returnValue(of({ fileId: 'new-media', fileUrl: '/uploads/new.jpg' }));
    productServiceMock.deleteProductImage.and.returnValue(of({ message: 'Image deleted' }));

    dialogRefMock = jasmine.createSpyObj('MatDialogRef', ['close', 'updateSize']);

    // Simplified template for unit testing the logic
    TestBed.overrideComponent(EditProductModal, {
      set: {
        template: `
          <form [formGroup]="editForm">
            <input formControlName="name" />
            <input formControlName="description" />
            <input formControlName="price" />
            <input formControlName="quantity" />
            <button (click)="onSave()">Save</button>
            <button (click)="onCancel()">Cancel</button>
          </form>
        `,
        styles: [],
        imports: [ReactiveFormsModule, CommonModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        EditProductModal,
        HttpClientTestingModule,
        ReactiveFormsModule,
        CommonModule,
        CurrencyPipe,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatDividerModule,
        MatTabsModule,
        NoopAnimationsModule // 🚨 Prevents Karma Crash
      ],
      providers: [
        { provide: ProductService, useValue: productServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: { product: mockProduct } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EditProductModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and set dialog size', () => {
    expect(component).toBeTruthy();
    expect(dialogRefMock.updateSize).toHaveBeenCalledWith('600px');
  });

  it('should get full image URL relative for Nginx', () => {
    // 🚨 FIX: Assertion updated for relative paths
    const url = component.getImageUrl('/uploads/test.jpg');
    expect(url).toBe('/uploads/test.jpg');
  });

  it('should copy media array to currentMedia', () => {
    expect(component.currentMedia.length).toBe(2);
    expect(component.currentMedia[0].fileId).toBe('media-1');
  });

  it('should validate required fields', () => {
    component.editForm.patchValue({ name: '', description: '', price: null, quantity: null });
    expect(component.editForm.valid).toBe(false);
  });

  it('should add valid files to upload queue', () => {
    const file = new File(['image content'], 'test.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file] } };

    component.onFilesSelected(event);

    expect(component.filesToUpload.length).toBe(1);
    expect(component.fileErrors.length).toBe(0);
  });

  it('should reject files with invalid type', () => {
    const file = new File(['content'], 'test.txt', { type: 'text/plain' });
    const event = { target: { files: [file] } };

    component.onFilesSelected(event);

    expect(component.filesToUpload.length).toBe(0);
    expect(component.fileErrors.length).toBe(1);
    expect(component.fileErrors[0]).toContain('invalid type');
  });

  it('should reject files larger than 2MB', () => {
    const largeFile = new File([new ArrayBuffer(3 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [largeFile] } };

    component.onFilesSelected(event);

    expect(component.filesToUpload.length).toBe(0);
    expect(component.fileErrors.length).toBe(1);
    expect(component.fileErrors[0]).toContain('too large');
  });

  it('should update product on save with dirty form', () => {
    component.editForm.markAsDirty();
    component.editForm.patchValue({ name: 'Updated Product' });

    component.onSave();

    expect(component.isLoading).toBe(true);
    expect(productServiceMock.updateProduct).toHaveBeenCalledWith('test-id', jasmine.objectContaining({
      name: 'Updated Product'
    }));
  });

  it('should close with false if no changes on save', () => {
    component.onSave();

    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
    expect(productServiceMock.updateProduct).not.toHaveBeenCalled();
  });
});