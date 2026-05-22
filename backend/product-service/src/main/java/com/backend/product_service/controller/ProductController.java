package com.backend.product_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.ProductSimpleDTO;
import com.backend.product_service.dto.StockAdjustmentRequest;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management API", description = "Endpoints for managing products, inventory, and product media")
public class ProductController {
    
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/all")
    @Operation(summary = "Get all products", description = "Retrieves a paginated list of all products available in the catalog.")
    public ResponseEntity<Page<ProductCardDTO>> getAllProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Optional seller ID to mark products as 'createdByMe'") 
            @RequestHeader(value = "X-User-ID", required = false) String sellerId) {

        Page<ProductCardDTO> page = productService.getAllProducts(pageable, sellerId);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/my-products")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get my products", description = "Retrieves a paginated list of products created by the currently authenticated seller.")
    public ResponseEntity<Page<ProductCardDTO>> getMyProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-User-ID") String sellerId) { 

        Page<ProductCardDTO> page = productService.getMyProducts(pageable, sellerId);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create a product", description = "Creates a new product listing under the authenticated seller's account.")
    public ResponseEntity<Product> createProduct(
            @RequestBody @NotNull(message = "This request needs a body") CreateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId) {
            
        Product newProduct = productService.createProduct(sellerId, productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);
    }

    @PostMapping("/adjust-stock")
    @Operation(summary = "Adjust product stock", description = "Decrements stock for a list of products. Typically called internally by the orders-service during checkout.")
    public ResponseEntity<Void> adjustStock(@RequestBody List<StockAdjustmentRequest> adjustments) {
        productService.adjustProductStock(adjustments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restock")
    @Operation(summary = "Restock products", description = "Increments stock for a list of products. Typically called internally upon order cancellation.")
    public ResponseEntity<Void> restock(@RequestBody List<StockAdjustmentRequest> adjustments) {
        productService.restockProducts(adjustments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create/images")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Upload product image", description = "Uploads a single image file for a specific product. Acts as a proxy to the media-service.")
    public ResponseEntity<Map<String, String>> addImagesToProduct(
            @RequestHeader("X-User-ID") String sellerId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam("productId") String productId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) { 
            
        productService.createImage(file, productId, sellerId, role);
        return ResponseEntity.ok(Map.of("message", "Image created successfully"));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update a product", description = "Updates details of an existing product. The requester must be the owner of the product.")
    public ResponseEntity<UpdateProductDTO> updateProduct(
            @PathVariable String productId,
            @RequestBody @NotNull(message = "This request needs a body") UpdateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId) {
            
        UpdateProductDTO savedProduct = productService.updateProduct(productId, sellerId, productDto);
        return ResponseEntity.ok(savedProduct);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete a product", description = "Permanently deletes a product and publishes an event to delete its associated media.")
    public ResponseEntity<String> deleteProduct(
            @PathVariable("productId") String productId,
            @RequestHeader("X-User-ID") String sellerId) {
            
        productService.deleteProduct(productId, sellerId);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @DeleteMapping("/deleteMedia/{productId}/{mediaId}")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Delete product media", description = "Deletes a specific media file associated with a product.")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @PathVariable("mediaId") String mediaId,
            @PathVariable("productId") String productId,
            @RequestHeader("X-User-ID") String sellerId) {
            
        productService.deleteProductMedia(productId, sellerId, mediaId);
        return ResponseEntity.ok(Map.of("message", "Media deleted successfully"));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get detailed product (Authenticated)", description = "Retrieves full product details including media and boolean flags for ownership.")
    public ResponseEntity<ProductDTO> getProductWithId(
            @PathVariable String productId,
            @RequestHeader("X-User-ID") String userId) {
            
        ProductDTO product = productService.getProductWithDetail(productId, userId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/public/{productId}")
    @Operation(summary = "Get detailed product (Public)", description = "Public endpoint to fetch full product details without requiring a user context.")
    public ResponseEntity<ProductDTO> getProductPublic(@PathVariable String productId) {
        ProductDTO product = productService.getProductWithDetail(productId, null);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/simple/{productId}")
    @Operation(summary = "Get simple product info", description = "Lightweight internal endpoint for fetching basic product info. Used primarily by orders-service.")
    public ResponseEntity<ProductSimpleDTO> getProductSimple(@PathVariable String productId) {
        ProductSimpleDTO product = productService.getProductDTOOnly(productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/seller/{email}")
    @Operation(summary = "Get products by seller email", description = "Retrieves a list of all products associated with a specific seller's email address.")
    public ResponseEntity<List<ProductDTO>> getAllProductsByEmail(@PathVariable String email) {
        List<ProductDTO> products = productService.getAllProductsWithEmail(email);
        return ResponseEntity.ok(products);
    }
}