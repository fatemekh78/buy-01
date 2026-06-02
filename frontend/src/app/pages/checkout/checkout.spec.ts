import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Checkout } from './checkout';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product-service';
import { AuthService } from '../../services/auth';
import { Router } from '@angular/router';
import { Order, PaymentMethod } from '../../models/order.model';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

describe('Checkout', () => {
  let component: Checkout;
  let fixture: ComponentFixture<Checkout>;
  let orderServiceMock: jasmine.SpyObj<OrderService>;
  let authServiceMock: any;
  let productServiceMock: jasmine.SpyObj<ProductService>;
  let routerMock: jasmine.SpyObj<Router>;
  
  const mockCart: Order = {
    id: 'cart-1',
    userId: 'user-1',
    shippingAddress: '123 Test St, City, ST 12345',
    status: 'PENDING' as any,
    items: [{ productId: 'prod-1', quantity: 2 }],
    paymentMethod: PaymentMethod.CARD,
    orderDate: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    isRemoved: false
  };

  beforeEach(async () => {
    orderServiceMock = jasmine.createSpyObj('OrderService', ['loadCart', 'checkoutOrder', 'clearCart']);
    orderServiceMock.cart$ = new BehaviorSubject<Order | null>(mockCart);
    orderServiceMock.checkoutOrder.and.returnValue(of(mockCart));

    authServiceMock = { currentUser$: new BehaviorSubject({ id: 'user-1', role: 'CLIENT' }) };
    
    productServiceMock = jasmine.createSpyObj('ProductService', ['getProductById']);
    productServiceMock.getProductById.and.returnValue(of({ price: 50, name: 'Test Product' } as any));
    
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        Checkout,
        ReactiveFormsModule,
        RouterTestingModule,
        NoopAnimationsModule, // 🚨 Prevents Karma crash with forms
        MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
        MatButtonModule, MatIconModule, MatListModule, MatDividerModule, MatProgressSpinnerModule
      ],
      providers: [
        { provide: OrderService, useValue: orderServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: productServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Checkout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with cart data', () => {
    expect(component.checkoutForm.value.shippingAddress).toBe('123 Test St, City, ST 12345');
    expect(component.checkoutForm.value.paymentMethod).toBe(PaymentMethod.CARD);
  });

  it('should calculate cart total', () => {
    // 2 items * 50 price = 100
    expect(component.cartTotal).toBe(100);
  });

  it('should require minimum length for shipping address', () => {
    component.checkoutForm.patchValue({ shippingAddress: 'Too short' });
    expect(component.checkoutForm.valid).toBeFalse();
  });

  it('should process successful checkout and route to orders', fakeAsync(() => {
    component.onSubmit();
    
    expect(component.isProcessing).toBeTrue();
    expect(orderServiceMock.checkoutOrder).toHaveBeenCalledWith('cart-1', jasmine.any(Object));
    
    // Simulate time passing for the 1.5s timeout
    tick(1500); 
    
    expect(orderServiceMock.clearCart).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/my-orders']);
    expect(component.isProcessing).toBeFalse();
  }));

  it('should handle checkout error', () => {
    orderServiceMock.checkoutOrder.and.returnValue(throwError(() => ({ status: 400, error: { message: 'Card declined' } })));
    
    component.onSubmit();
    
    expect(component.errorMessage).toBe('Card declined');
    expect(component.isProcessing).toBeFalse();
  });
});