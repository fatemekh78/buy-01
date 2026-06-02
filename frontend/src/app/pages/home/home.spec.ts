import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { PageEvent } from '@angular/material/paginator';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('Home', () => { // 🚨 Removed 'x'
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let productServiceMock: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    authServiceMock.fetchCurrentUser.and.returnValue(of({ id: '1', email: 'test@example.com', role: 'CLIENT' } as any));

    // 🚨 FIX: Added 'searchProducts' to the mocked methods so the new logic works!
    productServiceMock = jasmine.createSpyObj('ProductService', ['getAllProducts', 'searchProducts']);
    
    const mockPageData = {
      content: [
        { id: '1', name: 'Product 1', price: 100, images: [] },
        { id: '2', name: 'Product 2', price: 200, images: [] }
      ],
      totalElements: 20,
      totalPages: 2,
      number: 0
    };

    productServiceMock.getAllProducts.and.returnValue(of(mockPageData as any));
    productServiceMock.searchProducts.and.returnValue(of(mockPageData as any));

    // Simplified template for unit testing
    TestBed.overrideComponent(HomeComponent, {
      set: {
        template: `<div *ngFor="let product of products">{{ product.name }}</div>`,
        styles: [],
        imports: [CommonModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        HomeComponent, 
        HttpClientTestingModule, 
        CommonModule,
        FormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatButtonModule,
        MatCardModule,
        NoopAnimationsModule // 🚨 Prevents Karma crash with forms
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch current user on init', () => {
    fixture.detectChanges();
    expect(authServiceMock.fetchCurrentUser).toHaveBeenCalled();
    expect(component.currentUser).toEqual({ id: '1', email: 'test@example.com', role: 'CLIENT' } as any);
  });

  it('should fetch all products when no filters are applied', () => {
    fixture.detectChanges();
    expect(productServiceMock.getAllProducts).toHaveBeenCalledWith(0, 10);
    expect(component.products.length).toBe(2);
  });

  it('should call searchProducts when filters are applied', () => {
    component.searchKeyword = 'test';
    fixture.detectChanges();
    
    component.fetchProducts();
    
    expect(productServiceMock.searchProducts).toHaveBeenCalled();
  });

  it('should handle page change event', () => {
    fixture.detectChanges();
    const pageEvent: PageEvent = { pageIndex: 1, pageSize: 20, length: 40 };
    
    component.onPageChange(pageEvent);

    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(20);
    expect(productServiceMock.getAllProducts).toHaveBeenCalledWith(1, 20);
  });

  it('should prevent fetch if filters are invalid', () => {
    component.minPrice = 100;
    component.maxPrice = 50; // Invalid! Min > Max
    
    component.fetchProducts();
    
    expect(component.filterError).toBe('Min price must be less than or equal to max price.');
  });
});