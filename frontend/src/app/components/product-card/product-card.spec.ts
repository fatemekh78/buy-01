import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { ProductCard } from './product-card';
import { ProductService } from '../../services/product-service';
import { AuthService } from '../../services/auth';
import { OrderService } from '../../services/order.service';
import { ProductCardDTO } from '../../models/productCard.model';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CurrencyPipe } from '@angular/common';

describe('ProductCard', () => { // 🚨 Removed 'x'
  let component: ProductCard;
  let fixture: ComponentFixture<ProductCard>;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let authServiceMock: any;
  let orderServiceMock: jasmine.SpyObj<OrderService>;
  let dialogMock: jasmine.SpyObj<MatDialog>;
  let routerMock: jasmine.SpyObj<Router>;

  const mockProduct: ProductCardDTO = {
    id: 'prod-123',
    name: 'Test Product',
    description: 'A test product',
    price: 99.99,
    quantity: 10,
    createdByMe: true,
    imageUrls: ['/uploads/image1.jpg', '/uploads/image2.jpg', '/uploads/image3.jpg']
  };

  beforeEach(async () => {
    productServiceMock = jasmine.createSpyObj('ProductService', ['deleteProduct', 'updateProduct', 'getProductById']);
    productServiceMock.deleteProduct.and.returnValue(of('Product deleted successfully'));

    // 🚨 FIX: Mocking missing dependencies
    authServiceMock = { currentUser$: new BehaviorSubject({ id: 'user1', role: 'CLIENT' }) };
    orderServiceMock = jasmine.createSpyObj('OrderService', ['getOrCreateCart', 'addItemToOrder']);

    dialogMock = jasmine.createSpyObj('MatDialog', ['open']);
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        ProductCard,
        HttpClientTestingModule,
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        CurrencyPipe,
        MatDialogModule,
        RouterLink
      ],
      providers: [
        { provide: ProductService, useValue: productServiceMock },
        { provide: AuthService, useValue: authServiceMock },       // 🚨 Added
        { provide: OrderService, useValue: orderServiceMock },     // 🚨 Added
        { provide: MatDialog, useValue: dialogMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCard);
    component = fixture.componentInstance;
    component.product = mockProduct;
  });

  afterEach(() => {
    if (component.imageChangeInterval) {
      clearInterval(component.imageChangeInterval);
    }
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should get full image URL correctly relative for Nginx', () => {
    // 🚨 FIX: Assertion updated for relative paths
    const url = component.getImageUrl('/uploads/test.jpg');
    expect(url).toBe('/uploads/test.jpg');
  });

  it('should start image carousel on init with multiple images', fakeAsync(() => {
    component.ngOnInit();
    expect(component.currentImageIndex).toBe(0);

    tick(3000);
    expect(component.currentImageIndex).toBe(1);

    // Clear the interval so the test exits cleanly
    clearInterval(component.imageChangeInterval);
  }));

  it('should open delete confirmation dialog', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(mockEvent, 'stopPropagation');

    const dialogRefMock = { afterClosed: () => of(false) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(dialogMock.open).toHaveBeenCalled();
  });

  it('should delete product when confirmed', () => {
    const mockEvent = new MouseEvent('click');
    spyOn(component.delete, 'emit');

    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.onDelete(mockEvent);

    expect(productServiceMock.deleteProduct).toHaveBeenCalledWith('prod-123');
    expect(component.delete.emit).toHaveBeenCalled();
  });
});