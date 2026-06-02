import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../services/product-service'; 
import { ProductCardDTO } from '../../models/productCard.model'; 
import { ProductCard } from '../../components/product-card/product-card';
import { AuthService } from '../../services/auth';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; 
}

@Component({
  selector: 'app-my-products',
  standalone: true,
  imports: [
    CommonModule, 
    MatCardModule, 
    MatButtonModule, 
    MatPaginatorModule, 
    MatIconModule,
    RouterLink,
    ProductCard
  ],
  templateUrl: './my-products.html',
  styleUrls: ['./my-products.scss'] // Updated to SCSS
})
export class MyProducts implements OnInit {
  products: ProductCardDTO[] = [];

  totalElements: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;

  constructor(
    private productService: ProductService, 
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.fetchMyProducts();
    this.authService.fetchCurrentUser().subscribe();
  }

  fetchMyProducts(): void {
    this.productService.getMyProducts(this.pageIndex, this.pageSize).subscribe({
      next: (page: Page<ProductCardDTO>) => {
        this.products = page.content;
        this.totalElements = page.totalElements;
      },
      error: (err) => console.error('Failed to fetch products', err)
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchMyProducts();
  }

  onEdit(productId: string): void {
    console.log('Edit product:', productId);
  }

  onProductDeleted(): void {
    this.fetchMyProducts();
  }

  onProductUpdated(): void {
    this.fetchMyProducts();
  }

  // 🚨 FIX: Strict relative pathing for Nginx Gateway
  getImageUrl(imagePath: string): string {
    if (!imagePath) return '';
    return imagePath.startsWith('/') ? imagePath : `/${imagePath}`;
  }
}