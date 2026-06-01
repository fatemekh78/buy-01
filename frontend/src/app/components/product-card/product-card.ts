import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { throwError } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';

import { ProductCardDTO } from '../../models/productCard.model';
import { ProductDetailDTO } from '../../models/product.model';
import { ConfirmDialog } from '../confirm-dialog/confirm-dialog';
import { EditProductModal } from '../edit-product-modal/edit-product-modal';
import { AddToCartDialog } from '../add-to-cart-dialog/add-to-cart-dialog';
import { ProductService } from '../../services/product-service';
import { AuthService } from '../../services/auth';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    CurrencyPipe,
    MatDialogModule,
    EditProductModal
  ],
  templateUrl: './product-card.html',
  styleUrls: ['./product-card.scss'] // Updated to SCSS
})
export class ProductCard implements OnInit, OnDestroy {
  @Input() product: ProductCardDTO | null = null;
  @Output() edit = new EventEmitter<void>();
  @Output() delete = new EventEmitter<void>();

  currentImageIndex = 0;
  imageChangeInterval: any = null;
  public currentUserRole: string | null = null;

  constructor(
    private router: Router,
    private productService: ProductService,
    public dialog: MatDialog,
    private authService: AuthService,
    private orderService: OrderService
  ) {
    this.authService.currentUser$.subscribe(user => {
      this.currentUserRole = user?.role || null;
    });
  }

  ngOnInit(): void {
    this.startImageCarousel();
  }

  ngOnDestroy(): void {
    if (this.imageChangeInterval) {
      clearInterval(this.imageChangeInterval);
    }
  }

  startImageCarousel(): void {
    if (this.product && this.product.imageUrls && this.product.imageUrls.length > 1) {
      this.imageChangeInterval = setInterval(() => {
        if (this.product && this.product.imageUrls) {
          this.currentImageIndex = (this.currentImageIndex + 1) % this.product.imageUrls.length;
        }
      }, 3000);
    }
  }

  @HostListener('mouseenter')
  onMouseEnter(): void {
    if (this.imageChangeInterval) clearInterval(this.imageChangeInterval);
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.startImageCarousel();
  }

  onAddToCart(event: MouseEvent): void {
    event.stopPropagation();
    if (!this.product) return;

    this.productService.getProductById(this.product.id).subscribe({
      next: (fullProduct) => {
        const dialogRef = this.dialog.open(AddToCartDialog, {
          data: {
            productId: this.product!.id,
            productName: this.product!.name,
            price: this.product!.price,
            availableStock: fullProduct.quantity,
            sellerId: fullProduct.sellerId
          }
        });

        dialogRef.afterClosed().subscribe((result) => {
          const quantityValue = typeof result === 'number' ? result : result?.quantity;
          const quantity = Number(quantityValue);

          if (!result || !Number.isFinite(quantity) || quantity < 1) return;

          this.authService.currentUser$.pipe(
            take(1),
            switchMap(user => {
              if (!user) return throwError(() => new Error('User not authenticated'));
              return this.orderService.getOrCreateCart(user.id, 'Default Address');
            }),
            switchMap(cart => this.orderService.addItemToOrder(cart.id, {
              productId: this.product!.id,
              quantity
            }))
          ).subscribe({
            next: (updatedCart) => this.orderService.cartSubject.next(updatedCart),
            error: (err) => console.error('Failed to add item to cart', err)
          });
        });
      },
      error: (err) => console.error('Failed to fetch product details', err)
    });
  }

  // 🚨 FIX: Strict relative pathing for Nginx
  getImageUrl(imagePath: string): string {
    if (!imagePath) return '';
    return imagePath.startsWith('/') ? imagePath : `/${imagePath}`;
  }

  onCardClick(): void {
    if (this.product) this.router.navigate(['/product', this.product.id]);
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    if (!this.product) return;

    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Delete Product',
        message: `Are you sure you want to delete "${this.product.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
        isDestructive: true // Turns the confirm button Red!
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true && this.product) {
        this.productService.deleteProduct(this.product.id).subscribe({
          next: () => this.delete.emit(),
          error: (err) => console.error('Failed to delete product', err)
        });
      }
    });
  }

  onEdit(event: MouseEvent): void {
    event.stopPropagation();
    if (!this.product) return;

    this.productService.getProductById(this.product.id).subscribe({
      next: (fullProduct: ProductDetailDTO) => {
        const dialogRef = this.dialog.open(EditProductModal, {
          data: { product: fullProduct }
        });
        dialogRef.afterClosed().subscribe(wasSuccessful => {
          if (wasSuccessful) this.edit.emit();
        });
      },
      error: (err) => {
        console.error('Failed to fetch product details for editing', err);
      }
    });
  }
}