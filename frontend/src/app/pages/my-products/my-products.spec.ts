import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { MyProducts } from './my-products';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { PageEvent } from '@angular/material/paginator';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

describe('MyProducts', () => { // 🚨 Removed 'x'
  let component: MyProducts;
  let fixture: ComponentFixture<MyProducts>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let productServiceMock: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({ id: '1', email: 'test@example.com' } as any));

    productServiceMock = jasmine.createSpyObj('ProductService', ['getMyProducts']);
    productServiceMock.getMyProducts.and.returnValue(of({
      content: [
        { id: '1', name: 'Product 1', price: 100, images: [] },
        { id: '2', name: 'Product 2', price: 200, images: [] }
      ],
      totalElements: 20,
      totalPages: 2,
      number: 0
    } as any));

    TestBed.overrideComponent(MyProducts, {
      set: {
        template: `
          <div *ngFor="let product of products">
            {{ product.name }} - {{ product.price }}
          </div>
        `,
        styles: [],
        imports: [CommonModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        MyProducts,
        HttpClientTestingModule,
        CommonModule,
        RouterTestingModule, // 🚨 Required for routerLink in template
        MatIconModule,
        MatButtonModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MyProducts);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch products on init', () => {
    fixture.detectChanges();
    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(0, 10);
    expect(component.products.length).toBe(2);
    expect(component.totalElements).toBe(20);
  });

  it('should fetch current user on init', () => {
    fixture.detectChanges();
    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
  });

  it('should handle page change event', () => {
    fixture.detectChanges();
    const pageEvent: PageEvent = { pageIndex: 1, pageSize: 20, length: 40 };

    component.onPageChange(pageEvent);

    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(20);
    expect(productServiceMock.getMyProducts).toHaveBeenCalledWith(1, 20);
  });

  // 🚨 FIX: Nginx relative routing tests
  it('should build relative image URL', () => {
    const url = component.getImageUrl('/media/image.jpg');
    expect(url).toBe('/media/image.jpg'); // Validates it doesn't prepend localhost:8443
  });

  it('should ensure root slash on image URL if missing', () => {
    const url = component.getImageUrl('media/image.jpg');
    expect(url).toBe('/media/image.jpg');
  });

  it('should handle empty image URL', () => {
    const url = component.getImageUrl('');
    expect(url).toBe('');
  });

  it('should refresh products after product deleted', () => {
    fixture.detectChanges();
    const initialCallCount = productServiceMock.getMyProducts.calls.count();

    component.onProductDeleted();

    expect(productServiceMock.getMyProducts.calls.count()).toBe(initialCallCount + 1);
  });
});