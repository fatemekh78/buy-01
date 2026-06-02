import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { Order, OrderItem, OrderStatus } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';

@Component({
    selector: 'app-order-detail',
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatChipsModule,
        MatDividerModule,
        MatProgressSpinnerModule
    ],
    templateUrl: './order-detail.html',
    styleUrls: ['./order-detail.scss'] // Updated to SCSS
})
export class OrderDetail implements OnInit {
    order: Order | null = null;
    isLoading = true;
    orderId: string | null = null;
    productDetails: Record<string, ProductDetailDTO> = {};
    isSeller = false;

    constructor(
        private orderService: OrderService,
        private productService: ProductService,
        private activatedRoute: ActivatedRoute,
        private router: Router
    ) {
        const userRole = sessionStorage.getItem('userRole') || localStorage.getItem('userRole') || '';
        this.isSeller = userRole.includes('SELLER');
    }

    ngOnInit(): void {
        this.activatedRoute.paramMap.subscribe(params => {
            this.orderId = params.get('id');
            if (this.orderId) {
                this.loadOrderDetail();
            }
        });
    }

    loadOrderDetail(): void {
        if (!this.orderId) return;

        this.isLoading = true;
        this.orderService.getOrderById(this.orderId).subscribe({
            next: (order) => {
                this.order = order;
                if (order && order.items) {
                    this.populateProductDetails(order.items);
                }
                this.isLoading = false;
            },
            error: (err) => {
                console.error('[OrderDetail] ERROR - Failed to load order detail', err);
                this.isLoading = false;

                if (err.status === 403) {
                    alert('Access Denied: You don\'t have permission to view this order.');
                } else if (err.status === 404) {
                    alert('Order not found.');
                } else {
                    alert('Failed to load order details. Please try again.');
                }
            }
        });
    }

    goBack(): void {
        this.router.navigate(['/my-orders']);
    }

    reorder(orderId: string): void {
        this.orderService.redoOrder(orderId).subscribe({
            next: (response) => {
                let alertMessage = response.message;
                if (response.outOfStockProducts?.length) alertMessage += '\n\nOut of stock:\n• ' + response.outOfStockProducts.join('\n• ');
                if (response.partiallyFilledProducts?.length) alertMessage += '\n\nReduced quantities:\n• ' + response.partiallyFilledProducts.join('\n• ');
                
                alert(alertMessage);

                if (response.order?.items?.length) {
                    this.orderService.cartSubject.next(response.order);
                    this.router.navigate(['/cart']);
                }
            },
            error: (err) => {
                if (err.error?.message) {
                    let alertMessage = err.error.message;
                    if (err.error.outOfStockProducts?.length) alertMessage += '\n\nOut of stock:\n• ' + err.error.outOfStockProducts.join('\n• ');
                    alert(alertMessage);
                } else {
                    alert('Failed to recreate order');
                }
            }
        });
    }

    cancelOrder(orderId: string): void {
        if (!confirm('Are you sure you want to cancel this order?')) return;

        this.orderService.cancelShippingOrder(orderId).subscribe({
            next: (response) => {
                if (response.error) alert('Cannot cancel order: ' + response.error);
                else {
                    alert('Order cancelled successfully. Stock has been restored.');
                    this.loadOrderDetail();
                }
            },
            error: () => alert('Failed to cancel order. Please try again.')
        });
    }

    removeOrder(orderId: string): void {
        if (!confirm('Are you sure you want to remove this order from history?')) return;

        this.orderService.removeOrder(orderId).subscribe({
            next: (response) => {
                if (response.error) alert('Cannot remove order: ' + response.error);
                else {
                    alert('Order removed from history successfully.');
                    this.router.navigate(['/my-orders']);
                }
            },
            error: () => alert('Failed to remove order. Please try again.')
        });
    }

    getTotal(): number {
        return this.order ? this.order.items.reduce((total, item) => total + this.getItemSubtotal(item), 0) : 0;
    }

    // 🚨 FIX: Replaced hardcoded localhost with Nginx-friendly relative routes
    getImageUrl(item: OrderItem): string {
        if (item.imageUrl) {
            return item.imageUrl.startsWith('/') ? item.imageUrl : `/${item.imageUrl}`;
        }
        const detail = this.productDetails[item.productId];
        if (detail?.media?.length) {
            return detail.media[0].fileUrl.startsWith('/') ? detail.media[0].fileUrl : `/${detail.media[0].fileUrl}`;
        }
        return '/assets/placeholder.jpg';
    }

    getProductName(productId: string): string { return this.productDetails[productId]?.name || 'Loading...'; }
    getProductPrice(productId: string): number { return this.productDetails[productId]?.price || 0; }
    getItemSubtotal(item: OrderItem): number { return this.getProductPrice(item.productId) * item.quantity; }

    private populateProductDetails(items: OrderItem[]): void {
        const idsToFetch = Array.from(new Set(items.map(item => item.productId))).filter(id => !this.productDetails[id]);
        if (idsToFetch.length === 0) return;

        forkJoin(idsToFetch.map(id => this.productService.getProductById(id))).subscribe({
            next: (products) => {
                products.forEach(p => {
                    if (p.productId || p.id) this.productDetails[p.productId || p.id!] = p;
                });
            },
            error: (err) => console.error('Failed to fetch product details:', err)
        });
    }
}