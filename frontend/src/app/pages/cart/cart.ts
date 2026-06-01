import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { forkJoin } from 'rxjs';
import { Order, OrderItem } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth';
import { ProductService } from '../../services/product-service';
import { ProductDetailDTO } from '../../models/product.model';

@Component({
    selector: 'app-cart',
    standalone: true,
    imports: [
        CommonModule, FormsModule, RouterLink, MatCardModule, 
        MatButtonModule, MatIconModule, MatInputModule, MatFormFieldModule
    ],
    templateUrl: './cart.html',
    styleUrls: ['./cart.scss']
})
export class Cart implements OnInit {
    cart: Order | null = null;
    isLoading = true;
    userId: string | null = null;
    productDetails: Record<string, ProductDetailDTO> = {};

    constructor(
        private orderService: OrderService,
        private authService: AuthService,
        private productService: ProductService,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.authService.currentUser$.subscribe(user => {
            if (user?.id && user.role === 'CLIENT') {
                this.userId = user.id;
                this.loadCart();
            }
        });

        this.orderService.cart$.subscribe(cart => {
            this.cart = cart;
            if (cart && cart.items.length > 0) this.populateProductDetails(cart.items);
        });
    }

    loadCart(): void {
        if (!this.userId) return;
        this.isLoading = true;
        this.orderService.loadCart(this.userId).subscribe({
            next: () => this.isLoading = false,
            error: () => this.isLoading = false
        });
    }

    updateQuantity(item: OrderItem, newQuantity: number): void {
        const quantity = Number(newQuantity);
        if (!this.cart || !Number.isFinite(quantity) || quantity < 1) return;

        this.productService.getProductById(item.productId).subscribe({
            next: (product) => {
                if (quantity > product.quantity) {
                    alert(`Only ${product.quantity} items available in stock`);
                    return;
                }
                this.orderService.updateOrderItem(this.cart!.id, item.productId, { productId: item.productId, quantity }).subscribe();
            }
        });
    }

    removeItem(productId: string): void {
        if (!this.cart) return;
        if (confirm('Remove this item from cart?')) {
            this.orderService.removeItemFromOrder(this.cart.id, productId).subscribe();
        }
    }

    clearCartItems(): void {
        if (this.cart && confirm('Remove all items from cart?')) {
            this.orderService.clearCartItems(this.cart.id).subscribe();
        }
    }

    getTotal(): number {
        return this.cart ? this.cart.items.reduce((total, item) => total + this.getItemSubtotal(item), 0) : 0;
    }

    // 🚨 FIX: Relative path for Nginx
    getImageUrl(productId: string): string {
        const detail = this.productDetails[productId];
        return detail?.media?.[0]?.fileUrl ? detail.media[0].fileUrl : '/assets/placeholder.jpg';
    }

    proceedToCheckout(): void {
        if (!this.cart?.items.length) {
            alert('Add items to your cart before checking out.');
            return;
        }
        this.router.navigate(['/checkout']);
    }

    getProductName(productId: string): string { return this.productDetails[productId]?.name || 'Loading...'; }
    getProductPrice(productId: string): number { return this.productDetails[productId]?.price || 0; }
    getItemSubtotal(item: OrderItem): number { return this.getProductPrice(item.productId) * item.quantity; }

    private populateProductDetails(items: OrderItem[]): void {
        const idsToFetch = Array.from(new Set(items.map(i => i.productId))).filter(id => !this.productDetails[id]);
        if (idsToFetch.length === 0) return;
        forkJoin(idsToFetch.map(id => this.productService.getProductById(id))).subscribe(products => {
            products.forEach(p => this.productDetails[p.productId || p.id!] = p);
        });
    }
}