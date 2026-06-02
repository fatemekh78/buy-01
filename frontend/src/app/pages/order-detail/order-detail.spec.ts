import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError, Subject, BehaviorSubject } from 'rxjs';
import { OrderDetail } from './order-detail';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product-service';
import { Order, OrderStatus, OrderItem, PaymentMethod, RedoOrderResponse } from '../../models/order.model';
import { ProductDetailDTO } from '../../models/product.model';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

describe('OrderDetail', () => {
    let component: OrderDetail;
    let fixture: ComponentFixture<OrderDetail>;
    let orderServiceSpy: jasmine.SpyObj<OrderService>;
    let productServiceSpy: jasmine.SpyObj<ProductService>;
    let router: Router;
    let paramMapSubject: Subject<any>;

    const mockOrder: Order = {
        id: 'order-123',
        userId: 'user-456',
        shippingAddress: '123 Main St, City, Country',
        status: OrderStatus.PROCESSING,
        items: [
            { productId: 'prod-1', quantity: 2 },
            { productId: 'prod-2', quantity: 1 }
        ],
        paymentMethod: PaymentMethod.CARD,
        orderDate: '2026-02-06T10:00:00Z',
        createdAt: '2026-02-06T10:00:00Z',
        updatedAt: '2026-02-06T10:00:00Z',
        isRemoved: false
    };

    const mockProductDetail: ProductDetailDTO = {
        productId: 'prod-1', name: 'Test Product', description: 'A test product', price: 49.99, quantity: 10,
        sellerId: 'seller-1', sellerFirstName: 'John', sellerLastName: 'Doe', sellerEmail: 'john@example.com', createdByMe: false,
        media: [{ fileId: 'media-1', fileUrl: '/uploads/image.jpg', productId: 'prod-1' }]
    };

    const mockProductDetail2: ProductDetailDTO = {
        ...mockProductDetail, productId: 'prod-2', name: 'Another Product', price: 29.99, media: []
    };

    beforeEach(async () => {
        paramMapSubject = new Subject();
        orderServiceSpy = jasmine.createSpyObj('OrderService', ['getOrderById', 'redoOrder', 'cancelShippingOrder', 'removeOrder'], {
            cartSubject: new BehaviorSubject<Order | null>(null)
        });
        productServiceSpy = jasmine.createSpyObj('ProductService', ['getProductById']);

        await TestBed.configureTestingModule({
            imports: [
                OrderDetail,
                HttpClientTestingModule,
                NoopAnimationsModule, // 🚨 Critical for Material tests
                RouterTestingModule.withRoutes([
                    { path: 'my-orders', component: OrderDetail },
                    { path: 'cart', component: OrderDetail }
                ]),
                MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatDividerModule, MatProgressSpinnerModule
            ],
            providers: [
                { provide: OrderService, useValue: orderServiceSpy },
                { provide: ProductService, useValue: productServiceSpy },
                { provide: ActivatedRoute, useValue: { paramMap: paramMapSubject.asObservable() } }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(OrderDetail);
        component = fixture.componentInstance;
        router = TestBed.inject(Router);
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load order when id param is present', fakeAsync(() => {
        orderServiceSpy.getOrderById.and.returnValue(of(mockOrder));
        productServiceSpy.getProductById.and.returnValue(of(mockProductDetail));

        fixture.detectChanges();
        paramMapSubject.next({ get: () => 'order-123' });
        tick();

        expect(component.orderId).toBe('order-123');
        expect(orderServiceSpy.getOrderById).toHaveBeenCalledWith('order-123');
    }));

    // 🚨 FIX: Nginx URL Mock Tests
    describe('getImageUrl()', () => {
        it('should return relative imageUrl from OrderItem when available', () => {
            const item: OrderItem = { productId: 'prod-1', quantity: 1, imageUrl: '/api/media/files/image.jpg' };
            expect(component.getImageUrl(item)).toBe('/api/media/files/image.jpg');
        });

        it('should return relative media URL from product details when imageUrl not available', () => {
            component.productDetails = { 'prod-1': mockProductDetail };
            const item: OrderItem = { productId: 'prod-1', quantity: 1 };
            expect(component.getImageUrl(item)).toBe('/uploads/image.jpg');
        });

        it('should return relative placeholder when no media and no imageUrl', () => {
            component.productDetails = { 'prod-2': mockProductDetail2 };
            const item: OrderItem = { productId: 'prod-2', quantity: 1 };
            expect(component.getImageUrl(item)).toBe('/assets/placeholder.jpg');
        });
    });

    it('should calculate total correctly', () => {
        component.order = mockOrder;
        component.productDetails = {
            'prod-1': { ...mockProductDetail, price: 10 },
            'prod-2': { ...mockProductDetail2, price: 20 }
        };
        // prod-1: 10 * 2 = 20, prod-2: 20 * 1 = 20 -> Total = 40
        expect(component.getTotal()).toBe(40);
    });
});