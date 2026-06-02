import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { Order, OrderItem, OrderStatus } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { Page } from '../../services/product-service';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';
import { MatDividerModule } from '@angular/material/divider';
@Component({
    selector: 'app-my-orders',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatPaginatorModule,
        MatChipsModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatCheckboxModule,
        MatProgressSpinnerModule,
        MatDividerModule
    ],
    templateUrl: './my-orders.html',
    styleUrls: ['./my-orders.scss'] // Updated to SCSS
})
export class MyOrders implements OnInit {
    orders: Order[] = [];
    pageIndex = 0;
    pageSize = 10;
    totalElements = 0;
    isLoading = true;
    userId: string | null = null;
    userRole: string | null = null;
    productDetails: Record<string, ProductDetailDTO> = {};

    searchKeyword: string = '';
    minOrderPrice: number | null = null;
    maxOrderPrice: number | null = null;
    minUpdateDate: string = '';
    maxUpdateDate: string = '';
    selectedStatuses: OrderStatus[] = [];
    filterError: string | null = null;

    availableStatuses: OrderStatus[] = [
        OrderStatus.SHIPPING,
        OrderStatus.CANCELLED,
        OrderStatus.DELIVERED
    ];

    constructor(
        private orderService: OrderService,
        private authService: AuthService,
        private productService: ProductService
    ) { }

    ngOnInit(): void {
        this.authService.currentUser$.subscribe(user => {
            if (user?.id) {
                this.userId = user.id;
                this.userRole = user.role || null;
                this.fetchOrders();
            }
        });
    }

    fetchOrders(): void {
        if (!this.userId || !this.isFilterValid()) return;

        this.isLoading = true;
        const statusStrings = this.selectedStatuses.map(status => status.toString());

        this.orderService.getUserOrders(
            this.userId,
            this.pageIndex,
            this.pageSize,
            this.searchKeyword || undefined,
            this.minOrderPrice || undefined,
            this.maxOrderPrice || undefined,
            this.minUpdateDate || undefined,
            this.maxUpdateDate || undefined,
            statusStrings.length > 0 ? statusStrings : undefined
        ).subscribe({
            next: (page: Page<Order>) => {
                this.orders = page.content.filter(order => order.status !== OrderStatus.PENDING);
                this.totalElements = page.totalElements;
                this.populateProductDetails(this.orders);
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to fetch orders:', err);
                this.isLoading = false;
            }
        });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.fetchOrders();
    }

    getStatusColor(status: OrderStatus): string {
        switch (status) {
            case OrderStatus.PENDING: return 'accent';
            case OrderStatus.PROCESSING: return 'primary';
            case OrderStatus.SHIPPING: return 'primary';
            case OrderStatus.SHIPPED: return 'primary';
            case OrderStatus.DELIVERED: return 'success';
            case OrderStatus.CANCELLED: return 'error';
            default: return '';
        }
    }

    getTotalAmount(order: Order): number {
        return order.items.reduce((total, item) => total + this.getItemSubtotal(item), 0);
    }

    reorder(orderId: string): void {
        this.orderService.redoOrder(orderId).subscribe({
            next: (response) => {
                let alertMessage = response.message;
                if (response.outOfStockProducts?.length) alertMessage += '\n\nOut of stock:\n• ' + response.outOfStockProducts.join('\n• ');
                if (response.partiallyFilledProducts?.length) alertMessage += '\n\nReduced quantities:\n• ' + response.partiallyFilledProducts.join('\n• ');
                alert(alertMessage);
                if (response.order?.items?.length) this.orderService.cartSubject.next(response.order);
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
        if (!confirm('Are you sure you want to cancel this order? Stock will be restored.')) return;
        this.orderService.cancelShippingOrder(orderId).subscribe({
            next: (response) => {
                if (response.error) alert('Cannot cancel order: ' + response.error);
                else {
                    alert('Order cancelled successfully. Stock has been restored.');
                    this.fetchOrders();
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
                    this.fetchOrders();
                }
            },
            error: () => alert('Failed to remove order. Please try again.')
        });
    }

    getProductName(productId: string): string {
        return this.productDetails[productId]?.name || 'Loading...';
    }

    getProductPrice(productId: string): number {
        return this.productDetails[productId]?.price || 0;
    }

    getItemSubtotal(item: OrderItem): number {
        return this.getProductPrice(item.productId) * item.quantity;
    }

    // 🚨 FIX: Strict relative pathing for Nginx Gateway
    getProductImage(item: OrderItem): string {
        if (item.imageUrl) {
            return item.imageUrl.startsWith('/') ? item.imageUrl : `/${item.imageUrl}`;
        }
        const detail = this.productDetails[item.productId];
        if (detail?.media?.length) {
            return detail.media[0].fileUrl.startsWith('/') ? detail.media[0].fileUrl : `/${detail.media[0].fileUrl}`;
        }
        return '/assets/placeholder.jpg';
    }

    private populateProductDetails(orders: Order[]): void {
        const uniqueIds = new Set<string>();
        orders.forEach(order => order.items.forEach(item => uniqueIds.add(item.productId)));
        const idsToFetch = Array.from(uniqueIds).filter(id => !this.productDetails[id]);

        if (idsToFetch.length === 0) return;

        forkJoin(idsToFetch.map(id => this.productService.getProductById(id))).subscribe({
            next: (products) => products.forEach(p => this.productDetails[p.productId || p.id!] = p),
            error: (err) => console.error('Failed to fetch product details for orders:', err)
        });
    }

    canPerformActions(): boolean {
        return this.userRole === 'CLIENT' || this.userRole === 'ADMIN';
    }

    getEmptyStateMessage(): string {
        return this.userRole === 'SELLER' ? "Nobody has ordered your products yet." : "You haven't placed any orders yet.";
    }

    onSearch(): void {
        this.pageIndex = 0;
        this.fetchOrders();
    }

    clearFilters(): void {
        this.searchKeyword = '';
        this.minOrderPrice = null;
        this.maxOrderPrice = null;
        this.minUpdateDate = '';
        this.maxUpdateDate = '';
        this.selectedStatuses = [];
        this.filterError = null;
        this.pageIndex = 0;
        this.fetchOrders();
    }

    toggleStatusFilter(status: OrderStatus): void {
        const index = this.selectedStatuses.indexOf(status);
        if (index > -1) this.selectedStatuses.splice(index, 1);
        else this.selectedStatuses.push(status);
    }

    isStatusSelected(status: OrderStatus): boolean {
        return this.selectedStatuses.includes(status);
    }

    private isFilterValid(): boolean {
        this.filterError = null;
        if ((this.minOrderPrice ?? 0) < 0 || (this.maxOrderPrice ?? 0) < 0) {
            this.filterError = 'Price cannot be negative.';
            return false;
        }
        if (this.minOrderPrice != null && this.maxOrderPrice != null && this.minOrderPrice > this.maxOrderPrice) {
            this.filterError = 'Min price must be less than or equal to max price.';
            return false;
        }
        if (this.minUpdateDate && this.maxUpdateDate) {
            if (new Date(this.minUpdateDate) > new Date(this.maxUpdateDate)) {
                this.filterError = 'Start date must be before end date.';
                return false;
            }
        }
        return true;
    }
}