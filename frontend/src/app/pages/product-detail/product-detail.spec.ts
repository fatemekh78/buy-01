import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { ProductDetail } from './product-detail';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { OrderService } from '../../services/order.service';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations'; // 🚨 Prevents Karma crash

describe('ProductDetail', () => { // 🚨 Removed 'x'
  let component: ProductDetail;
  let fixture: ComponentFixture<ProductDetail>;
  let authServiceMock: jasmine.SpyObj<AuthService>;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let orderServiceMock: jasmine.SpyObj<OrderService>; // 🚨 Added missing mock
  let routerMock: jasmine.SpyObj<Router>;
  let dialogMock: jasmine.SpyObj<MatDialog>;
  let activatedRouteMock: any;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['fetchCurrentUser']);
    // Mock the currentUser$ observable requirement
    (authServiceMock as any).currentUser$ = of({ id: 'user-1', role: 'SELLER' });
    authServiceMock.fetchCurrentUser.and.returnValue(of({ id: 'user-1', role: 'SELLER' } as any));

    productServiceMock = jasmine.createSpyObj('ProductService', ['getProductById', 'deleteProduct']);
    productServiceMock.getProductById.and.returnValue(of({
      productId: '123',
      name: 'Test Product',
      price: 99.99,
      description: 'Test description',
      quantity: 10,
      media: [{ fileUrl: '/images/test.jpg' }]
    } as any));

    // 🚨 FIX: Mocking the injected OrderService
    orderServiceMock = jasmine.createSpyObj('OrderService', ['getOrCreateCart', 'addItemToOrder']);
    orderServiceMock.getOrCreateCart.and.returnValue(of({ id: 'cart-1' } as any));
    orderServiceMock.addItemToOrder.and.returnValue(of({} as any));

    routerMock = jasmine.createSpyObj('Router', ['navigate']);
    dialogMock = jasmine.createSpyObj('MatDialog', ['open']);

    activatedRouteMock = {
      snapshot: { paramMap: { get: jasmine.createSpy('get').and.returnValue('123') } }
    };

    TestBed.overrideComponent(ProductDetail, {
      set: {
        template: `
          <div *ngIf="product">
            <h1>{{ product.name }}</h1>
            <button (click)="onEdit()">Edit</button>
            <button (click)="onDelete()">Delete</button>
            <button (click)="onAddToCart()">Add To Cart</button>
          </div>
        `,
        styles: [],
        imports: [CommonModule]
      }
    });

    await TestBed.configureTestingModule({
      imports: [
        ProductDetail,
        HttpClientTestingModule,
        CommonModule,
        MatDialogModule,
        RouterTestingModule,
        NoopAnimationsModule // 🚨 Critical for Dialogs
      ],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock },
        { provide: OrderService, useValue: orderServiceMock }, // 🚨 Injected!
        { provide: Router, useValue: routerMock },
        { provide: MatDialog, useValue: dialogMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetail);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch product on init', () => {
    fixture.detectChanges();
    expect(productServiceMock.getProductById).toHaveBeenCalledWith('123');
    expect(component.product).toBeTruthy();
    expect(component.product?.name).toBe('Test Product');
  });

  // 🚨 FIX: Nginx Proxy assertion
  it('should set first image as selected on init with relative path', () => {
    fixture.detectChanges();
    expect(component.selectedImageUrl).toBe('/images/test.jpg');
  });

  it('should set isLoading to false after successful fetch', () => {
    fixture.detectChanges();
    expect(component.isLoading).toBe(false);
  });

  // 🚨 FIX: Nginx Proxy assertion
  it('should build relative image URL correctly', () => {
    const fullUrl = component.getFullImageUrl('/media/image.png');
    expect(fullUrl).toBe('/media/image.png');
  });

  it('should prepend slash to relative image URL if missing', () => {
    const fullUrl = component.getFullImageUrl('media/image.png');
    expect(fullUrl).toBe('/media/image.png');
  });

  it('should open edit modal on onEdit', () => {
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    component.product = { productId: '123', name: 'Test' } as any;

    component.onEdit();
    expect(dialogMock.open).toHaveBeenCalled();
  });

  it('should process add to cart correctly', fakeAsync(() => {
    const dialogRefMock = { afterClosed: () => of(2) }; // User chooses qty 2
    dialogMock.open.and.returnValue(dialogRefMock as any);

    component.product = { productId: '123', name: 'Test', quantity: 10 } as any;

    component.onAddToCart();
    tick(); // Resolve Observables

    expect(dialogMock.open).toHaveBeenCalled();
    expect(orderServiceMock.getOrCreateCart).toHaveBeenCalledWith('user-1', 'Default Address');
    expect(orderServiceMock.addItemToOrder).toHaveBeenCalledWith('cart-1', { productId: '123', quantity: 2 });
  }));

  it('should open delete confirmation on onDelete and navigate on success', () => {
    const dialogRefMock = { afterClosed: () => of(true) };
    dialogMock.open.and.returnValue(dialogRefMock as any);
    productServiceMock.deleteProduct.and.returnValue(of('Deleted'));
    component.product = { productId: '123', name: 'Test' } as any;

    component.onDelete();

    expect(dialogMock.open).toHaveBeenCalled();
    expect(productServiceMock.deleteProduct).toHaveBeenCalledWith('123');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/my-products']);
  });
});