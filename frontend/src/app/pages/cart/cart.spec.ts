import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Cart } from './cart';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { Router } from '@angular/router';
import { of, BehaviorSubject } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('Cart', () => {
  let component: Cart;
  let fixture: ComponentFixture<Cart>;
  let orderServiceMock: jasmine.SpyObj<OrderService>;

  beforeEach(async () => {
    orderServiceMock = jasmine.createSpyObj('OrderService', ['loadCart', 'removeItemFromOrder', 'clearCartItems', 'updateOrderItem']);
    orderServiceMock.cart$ = new BehaviorSubject(null);
    
    const authServiceMock = jasmine.createSpyObj('AuthService', [], { currentUser$: of({ id: '1', role: 'CLIENT' }) });
    const routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [Cart, HttpClientTestingModule],
      providers: [
        { provide: OrderService, useValue: orderServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProductService, useValue: jasmine.createSpyObj('ProductService', ['getProductById']) },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Cart);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load cart on init', () => {
    expect(orderServiceMock.loadCart).toHaveBeenCalled();
  });
});