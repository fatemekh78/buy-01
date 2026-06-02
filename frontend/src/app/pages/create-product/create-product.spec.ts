import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { CreateProduct } from './create-product';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { NoopAnimationsModule } from '@angular/platform-browser/animations'; // 🚨 Prevents Karma Crash

describe('CreateProduct', () => { // 🚨 Removed 'x'
  let component: CreateProduct;
  let fixture: ComponentFixture<CreateProduct>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let routerMock: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({ id: '1', role: 'SELLER' } as any));

    productServiceMock = jasmine.createSpyObj('ProductService', ['createProduct', 'uploadProductImage']);
    productServiceMock.createProduct.and.returnValue(of({ id: 'new-product-123' }));
    productServiceMock.uploadProductImage.and.returnValue(of({ message: 'Uploaded' }));

    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    // Simplified template for unit testing business logic
    TestBed.overrideComponent(CreateProduct, {
      set: {
        template: `
          <form [formGroup]="productForm">
            <input formControlName="name" />
            <input formControlName="description" />
            <input formControlName="price" />
            <input formControlName="quantity" />
            <button (click)="onSubmit()">Create</button>
          </form>
        `,
        styles: [],
        imports: [ReactiveFormsModule, CommonModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        CreateProduct,
        HttpClientTestingModule,
        ReactiveFormsModule,
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatDividerModule,
        MatCardModule,
        MatIconModule,
        NoopAnimationsModule // 🚨 Critical for Material forms testing
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateProduct);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with required validators', () => {
    expect(component.productForm.get('name')?.hasError('required')).toBe(true);
    expect(component.productForm.get('description')?.hasError('required')).toBe(true);
  });

  it('should validate price minimum value', () => {
    const priceControl = component.productForm.get('price');
    priceControl?.setValue(0);
    expect(priceControl?.hasError('min')).toBe(true);

    priceControl?.setValue(0.01);
    expect(priceControl?.hasError('min')).toBe(false);
  });

  it('should add valid files to selectedFiles array', () => {
    const file = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file] } };

    component.onFilesSelected(event);

    expect(component.selectedFiles.length).toBe(1);
    expect(component.selectedFiles[0]).toBe(file);
  });

  it('should reject files with invalid type', () => {
    const file = new File(['content'], 'test.txt', { type: 'text/plain' });
    const event = { target: { files: [file] } };

    component.onFilesSelected(event);

    expect(component.selectedFiles.length).toBe(0);
    expect(component.fileErrors.length).toBeGreaterThan(0);
  });

  it('should reject files exceeding size limit', () => {
    const largeFile = new File(['x'.repeat(13 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [largeFile] } };

    component.onFilesSelected(event);

    expect(component.selectedFiles.length).toBe(0);
    expect(component.fileErrors.length).toBeGreaterThan(0);
  });

  it('should create product without images', () => {
    component.productForm.setValue({
      name: 'Test Product',
      description: 'Description',
      price: 99.99,
      quantity: 10
    });

    component.onSubmit();

    expect(productServiceMock.createProduct).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/my-products']);
  });
});