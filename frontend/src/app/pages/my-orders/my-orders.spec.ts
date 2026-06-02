import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MyOrders } from './my-orders';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { Order, OrderStatus, PaymentMethod, RedoOrderResponse } from '../../models/order.model';
import { Page } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';

describe('MyOrders', () => {
    let component: MyOrders;
    let fixture: ComponentFixture<MyOrders>;
    let orderServiceSpy: jasmine.SpyObj<OrderService>;
    let authServiceSpy: jasmine.SpyObj<AuthService>;
    let productServiceSpy: jasmine.SpyObj<ProductService>;

    const mockOrder: Order = {
        id: 'order-123', userId: 'user-123', shippingAddress: '123 Main St',
        status: OrderStatus.PROCESSING, items: [{ productId: 'prod-1', quantity: 2 }],
        paymentMethod: PaymentMethod.CARD, orderDate: '2026-02-06T10:00:00Z',
        createdAt: '2026-02-06T10:00:00Z', updatedAt: '2026-02-06T10:00:00Z', isRemoved: false
    };

    const mockOrdersPage: Page<Order> = { content: [mockOrder], totalElements: 1, totalPages: 1, number: 0 };
    const mockProductDetail: ProductDetailDTO = { productId: 'prod-1', name: 'Test', description: 'desc', price: 29.99, quantity: 10, sellerId: 'seller-1', sellerFirstName: 'John', sellerLastName: 'Doe', sellerEmail: 'john@example.com', createdByMe: false, media: [] };

    beforeEach(async () => {
        orderServiceSpy = jasmine.createSpyObj('OrderService', ['getUserOrders', 'redoOrder', 'cancelShippingOrder', 'removeOrder'], { cartSubject: new BehaviorSubject<Order | null>(null) });
        authServiceSpy = jasmine.createSpyObj('AuthService', [], { currentUser$: of({ id: 'user-123', email: 'test@example.com' }) });
        productServiceSpy = jasmine.createSpyObj('ProductService', ['getProductById']);

        await TestBed.configureTestingModule({
            imports: [
                MyOrders, HttpClientTestingModule, RouterTestingModule, NoopAnimationsModule, FormsModule,
                MatCardModule, MatButtonModule, MatPaginatorModule, MatChipsModule, MatIconModule,
                MatFormFieldModule, MatInputModule, MatCheckboxModule, MatProgressSpinnerModule
            ],
            providers: [
                { provide: OrderService, useValue: orderServiceSpy },
                { provide: AuthService, useValue: authServiceSpy },
                { provide: ProductService, useValue: productServiceSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(MyOrders);
        component = fixture.componentInstance;
    });

    it('should create and fetch orders on init', fakeAsync(() => {
        orderServiceSpy.getUserOrders.and.returnValue(of(mockOrdersPage));
        productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));
        fixture.detectChanges();
        tick();

        expect(component).toBeTruthy();
        expect(component.userId).toBe('user-123');
        expect(orderServiceSpy.getUserOrders).toHaveBeenCalled();
    }));

    it('should calculate total amount correctly', () => {
        component.productDetails = { 'prod-1': { ...mockProductDetail, price: 10 } };
        expect(component.getTotalAmount(mockOrder)).toBe(20);
    });
});