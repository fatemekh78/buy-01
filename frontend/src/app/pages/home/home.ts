import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

import { ProductCard } from '../../components/product-card/product-card';
import { ProductService, Page } from '../../services/product-service';
import { ProductCardDTO } from '../../models/productCard.model';
import { AuthService } from '../../services/auth';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    ProductCard
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.scss'] // Updated to SCSS
})
export class HomeComponent implements OnInit {
  currentUser: User | null = null;
  errorMessage: string | null = null;
  filterError: string | null = null;
  products: ProductCardDTO[] = [];

  totalElements: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;

  searchKeyword: string = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  minQuantity: number | null = null;
  maxQuantity: number | null = null;
  startDate: string = '';
  endDate: string = '';

  constructor(
    private authService: AuthService,
    private productService: ProductService
  ) { }

  ngOnInit(): void {
    this.authService.fetchCurrentUser().subscribe({
      next: (user) => this.currentUser = user,
      error: (err) => {
        console.error('Failed to fetch current user:', err);
        this.errorMessage = 'Could not load user data.';
      }
    });

    this.fetchProducts();
  }

  fetchProducts(): void {
    if (!this.isFilterValid()) return;

    const hasFilters = (this.searchKeyword && this.searchKeyword.trim()) ||
      this.minPrice != null ||
      this.maxPrice != null ||
      this.minQuantity != null ||
      this.maxQuantity != null ||
      this.startDate ||
      this.endDate;

    if (hasFilters) {
      this.productService.searchProducts(
        this.searchKeyword || undefined,
        this.minPrice || undefined,
        this.maxPrice || undefined,
        this.minQuantity || undefined,
        this.maxQuantity || undefined,
        this.startDate || undefined,
        this.endDate || undefined,
        this.pageIndex,
        this.pageSize
      ).subscribe((page: Page<ProductCardDTO>) => {
        this.products = page.content;
        this.totalElements = page.totalElements;
      });
    } else {
      this.productService.getAllProducts(this.pageIndex, this.pageSize)
        .subscribe((page: Page<ProductCardDTO>) => {
          this.products = page.content;
          this.totalElements = page.totalElements;
        });
    }
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchProducts();
  }

  onSearch(): void {
    this.pageIndex = 0;
    this.fetchProducts();
  }

  clearFilters(): void {
    this.searchKeyword = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.minQuantity = null;
    this.maxQuantity = null;
    this.startDate = '';
    this.endDate = '';
    this.filterError = null;
    this.pageIndex = 0;
    this.fetchProducts();
  }

  onEdit(productId: string): void {
    console.log('Edit (from home):', productId);
  }

  onProductDeleted(): void {
    this.fetchProducts();
  }

  onProductUpdated(): void {
    this.fetchProducts();
  }

  private isFilterValid(): boolean {
    this.filterError = null;

    if ((this.minPrice ?? 0) < 0 || (this.maxPrice ?? 0) < 0) {
      this.filterError = 'Price cannot be negative.';
      return false;
    }
    if ((this.minQuantity ?? 0) < 0 || (this.maxQuantity ?? 0) < 0) {
      this.filterError = 'Quantity cannot be negative.';
      return false;
    }
    if (this.minPrice != null && this.maxPrice != null && this.minPrice > this.maxPrice) {
      this.filterError = 'Min price must be less than or equal to max price.';
      return false;
    }
    if (this.minQuantity != null && this.maxQuantity != null && this.minQuantity > this.maxQuantity) {
      this.filterError = 'Min quantity must be less than or equal to max quantity.';
      return false;
    }
    if (this.startDate && this.endDate) {
      const start = new Date(this.startDate);
      const end = new Date(this.endDate);
      if (start > end) {
        this.filterError = 'Start date must be before end date.';
        return false;
      }
    }
    return true;
  }
}