package com.backend.product_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.dto.InfoUserDTO;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.ProductSimpleDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Pageable pageable;
    private List<ProductCardDTO> productList;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        productList = new ArrayList<>();
        productList.add(new ProductCardDTO("p1", "Product 1", "Desc", 29.99, 10, false, null));
        productList.add(new ProductCardDTO("p2", "Product 2", "Desc", 39.99, 5, false, null));
    }

    // Helper: build a ProductDTO via its only public constructor using mocks.
    // ProductDTO has no no-arg constructor — @Data alone won't generate one
    // when an explicit constructor is already declared.
    private ProductDTO buildProductDTO(String productId, String name) {
        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(productId);
        when(mockProduct.getName()).thenReturn(name);
        when(mockProduct.getDescription()).thenReturn("desc");
        when(mockProduct.getPrice()).thenReturn(10.0);
        when(mockProduct.getQuantity()).thenReturn(1);

        InfoUserDTO mockSeller = mock(InfoUserDTO.class);
        when(mockSeller.getFirstName()).thenReturn("John");
        when(mockSeller.getLastName()).thenReturn("Doe");
        when(mockSeller.getEmail()).thenReturn("seller@example.com");

        return new ProductDTO(mockProduct, mockSeller, List.of());
    }

    @Test
    void testGetAllProducts_Success() {
        Page<ProductCardDTO> productPage = new PageImpl<>(productList, pageable, 2);
        when(productService.getAllProducts(any(Pageable.class), anyString())).thenReturn(productPage);

        ResponseEntity<Page<ProductCardDTO>> result = productController.getAllProducts(pageable, "seller123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getContent().size());
        verify(productService).getAllProducts(any(Pageable.class), anyString());
    }

    @Test
    @DisplayName("Should successfully adjust stock")
    void testAdjustStock_Success() {
        doNothing().when(productService).adjustProductStock(anyList());

        ResponseEntity<Void> result = productController.adjustStock(new ArrayList<>());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(productService).adjustProductStock(anyList());
    }

    @Test
    @DisplayName("Should successfully restock")
    void testRestock_Success() {
        doNothing().when(productService).restockProducts(anyList());

        ResponseEntity<Void> result = productController.restock(new ArrayList<>());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(productService).restockProducts(anyList());
    }

    @Test
    @DisplayName("Should get simple product info successfully")
    void testGetProductSimple_Success() {
        ProductSimpleDTO simpleDTO = new ProductSimpleDTO();
        simpleDTO.setProductId("p1");
        when(productService.getProductDTOOnly("p1")).thenReturn(simpleDTO);

        ResponseEntity<ProductSimpleDTO> result = productController.getProductSimple("p1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("p1", result.getBody().getProductId());
    }

    @Test
    void testCreateProduct_Success() {
        CreateProductDTO createDTO = new CreateProductDTO("New", "Desc", 49.99, 5);
        Product createdProduct = Product.builder().id("newId").build();

        when(productService.createProduct(eq("seller123"), any(CreateProductDTO.class))).thenReturn(createdProduct);

        ResponseEntity<Product> result = productController.createProduct(createDTO, "seller123");

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("newId", result.getBody().getId());
    }

    @Test
    void testDeleteProduct_Success() {
        doNothing().when(productService).deleteProduct(eq("p1"), eq("seller123"));

        ResponseEntity<String> result = productController.deleteProduct("p1", "seller123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Product deleted successfully", result.getBody());
    }

    @Test
    @DisplayName("getMyProducts – should return 200 with seller's products page")
    void testGetMyProducts_Success() {
        List<ProductCardDTO> products = List.of(
                new ProductCardDTO("p1", "Product 1", "Desc", 29.99, 10, true, null));
        Page<ProductCardDTO> page = new PageImpl<>(products, pageable, 1);

        when(productService.getMyProducts(any(Pageable.class), eq("seller123"))).thenReturn(page);

        ResponseEntity<Page<ProductCardDTO>> result = productController.getMyProducts(pageable, "seller123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getContent().size());
        assertEquals("p1", result.getBody().getContent().get(0).getId());
        verify(productService).getMyProducts(any(Pageable.class), eq("seller123"));
    }

    @Test
    @DisplayName("addImagesToProduct – should return 200 with success message")
    void testAddImagesToProduct_Success() {
        MultipartFile mockFile = mock(MultipartFile.class);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        doNothing().when(productService)
                .createImage(any(MultipartFile.class), eq("p1"), eq("seller123"), eq("ROLE_SELLER"));

        ResponseEntity<Map<String, String>> result = productController.addImagesToProduct(
                "seller123", "ROLE_SELLER", "p1", mockFile, mockRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Image created successfully", result.getBody().get("message"));
    }

    @Test
    @DisplayName("getMyProducts – should return empty page when seller has no products")
    void testGetMyProducts_EmptyPage() {
        Page<ProductCardDTO> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productService.getMyProducts(any(Pageable.class), eq("seller123"))).thenReturn(emptyPage);

        ResponseEntity<Page<ProductCardDTO>> result = productController.getMyProducts(pageable, "seller123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, result.getBody().getTotalElements());
    }

    // -------------------------------------------------------------------------
    // PUT /{productId}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateProduct – should return 200 with updated product DTO")
    void testUpdateProduct_Success() {
        UpdateProductDTO updateDTO = new UpdateProductDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setDescription("Updated Desc");
        updateDTO.setPrice(59.99);

        when(productService.updateProduct(eq("p1"), eq("seller123"), any(UpdateProductDTO.class)))
                .thenReturn(updateDTO);

        ResponseEntity<UpdateProductDTO> result = productController.updateProduct("p1", updateDTO, "seller123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Updated Name", result.getBody().getName());
        verify(productService).updateProduct(eq("p1"), eq("seller123"), any(UpdateProductDTO.class));
    }

    // -------------------------------------------------------------------------
    // DELETE /deleteMedia/{productId}/{mediaId}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteMedia – should return 200 with success message")
    void testDeleteMedia_Success() {
        doNothing().when(productService).deleteProductMedia(eq("p1"), eq("seller123"), eq("m1"));

        ResponseEntity<Map<String, String>> result = productController.deleteMedia("m1", "p1", "seller123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Media deleted successfully", result.getBody().get("message"));
        verify(productService).deleteProductMedia("p1", "seller123", "m1");
    }

    // -------------------------------------------------------------------------
    // GET /{productId} (authenticated)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProductWithId – should return 200 with full product details for authenticated user")
    void testGetProductWithId_AuthenticatedUser_Success() {
        ProductDTO productDTO = buildProductDTO("p1", "Test Product");

        when(productService.getProductWithDetail(eq("p1"), eq("user123"))).thenReturn(productDTO);

        ResponseEntity<ProductDTO> result = productController.getProductWithId("p1", "user123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("p1", result.getBody().getProductId());
        assertEquals("Test Product", result.getBody().getName());
        verify(productService).getProductWithDetail("p1", "user123");
    }

    @Test
    @DisplayName("getProductWithId – should still return 200 when X-User-ID is an unknown user")
    void testGetProductWithId_UnknownUser_Success() {
        ProductDTO productDTO = buildProductDTO("p1", "Test Product");

        when(productService.getProductWithDetail(eq("p1"), eq("unknown"))).thenReturn(productDTO);

        ResponseEntity<ProductDTO> result = productController.getProductWithId("p1", "unknown");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("p1", result.getBody().getProductId());
        verify(productService).getProductWithDetail("p1", "unknown");
    }

    // -------------------------------------------------------------------------
    // GET /public/{productId}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProductPublic – should return 200 with full product details without user context")
    void testGetProductPublic_Success() {
        ProductDTO productDTO = buildProductDTO("p1", "Public Product");

        // Public endpoint passes null as userId — key difference from authenticated
        // variant
        when(productService.getProductWithDetail(eq("p1"), eq(null))).thenReturn(productDTO);

        ResponseEntity<ProductDTO> result = productController.getProductPublic("p1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("p1", result.getBody().getProductId());
        assertEquals("Public Product", result.getBody().getName());
        verify(productService).getProductWithDetail("p1", null);
    }

    // -------------------------------------------------------------------------
    // GET /seller/{email}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllProductsByEmail – should return 200 with all products for given email")
    void testGetAllProductsByEmail_Success() {
        ProductDTO p1 = buildProductDTO("p1", "Product 1");
        ProductDTO p2 = buildProductDTO("p2", "Product 2");

        when(productService.getAllProductsWithEmail(eq("seller@example.com")))
                .thenReturn(List.of(p1, p2));

        ResponseEntity<List<ProductDTO>> result = productController.getAllProductsByEmail("seller@example.com");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertEquals("p1", result.getBody().get(0).getProductId());
        assertEquals("p2", result.getBody().get(1).getProductId());
        verify(productService).getAllProductsWithEmail("seller@example.com");
    }

    @Test
    @DisplayName("getAllProductsByEmail – should return 200 with empty list when seller has no products")
    void testGetAllProductsByEmail_EmptyList() {
        when(productService.getAllProductsWithEmail(anyString())).thenReturn(List.of());

        ResponseEntity<List<ProductDTO>> result = productController.getAllProductsByEmail("nobody@example.com");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, result.getBody().size());
    }

    // -------------------------------------------------------------------------
    // GET /all — edge case: null sellerId (unauthenticated caller)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllProducts – should return 200 when X-User-ID header is absent (null sellerId)")
    void testGetAllProducts_NullSellerId_Success() {
        Page<ProductCardDTO> page = new PageImpl<>(List.of(), pageable, 0);

        when(productService.getAllProducts(any(Pageable.class), eq(null))).thenReturn(page);

        ResponseEntity<Page<ProductCardDTO>> result = productController.getAllProducts(pageable, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(productService).getAllProducts(any(Pageable.class), eq(null));
    }
}