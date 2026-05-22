package com.backend.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.StockAdjustmentRequest;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private InfoUserDTO testSeller;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("product123")
                .name("Test Product")
                .description("Test Description")
                .price(99.99)
                .quantity(10)
                .sellerID("seller123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testSeller = InfoUserDTO.builder()
                .id("seller123")
                .firstName("John")
                .lastName("Seller")
                .email("seller@example.com")
                .build();

        // Leniently configure standard web client builder behavior to prevent UnnecessaryStubbing exceptions
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
    }

    // ==========================================
    // CORE PRODUCT FETCHING
    // ==========================================

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductSuccess() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        Product found = productService.getProduct("product123");
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("product123");
        verify(productRepository).findById("product123");
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductNotFound() {
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProduct("nonexistent"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("Should get all products successfully")
    void testGetAllProductsSuccess() {
        Product p2 = Product.builder().id("product2").name("Product 2").sellerID("seller2").build();
        when(productRepository.findAll()).thenReturn(List.of(testProduct, p2));
        
        List<Product> products = productService.getAllProducts();
        
        assertThat(products).hasSize(2);
        verify(productRepository).findAll();
    }

    // ==========================================
    // VALIDATION & SECURITY (checkProduct)
    // ==========================================

    @Test
    @DisplayName("checkProduct: Should throw BAD_REQUEST when productId is null")
    void testCheckProductNullProductId() {
        assertThatThrownBy(() -> productService.updateProduct(null, "seller123", new UpdateProductDTO()))
                .isInstanceOf(CustomException.class)
                .hasMessage("Product ID is null")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("checkProduct: Should throw UNAUTHORIZED when sellerId is null")
    void testCheckProductNullSellerId() {
        assertThatThrownBy(() -> productService.updateProduct("product123", null, new UpdateProductDTO()))
                .isInstanceOf(CustomException.class)
                .hasMessage("Seller ID is null")
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("checkProduct: Should throw FORBIDDEN when seller does not own product")
    void testCheckProductForbiddenAccess() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        
        assertThatThrownBy(() -> productService.updateProduct("product123", "wrongSellerID", new UpdateProductDTO()))
                .isInstanceOf(CustomException.class)
                .hasMessage("Access Denied")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ==========================================
    // LIFECYCLE (Create, Update, Delete)
    // ==========================================

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProduct() {
        CreateProductDTO createDTO = new CreateProductDTO("New Product", "Desc", 79.99, 15);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId("newId123");
            return p;
        });

        Product result = productService.createProduct("seller123", createDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("newId123");
        assertThat(result.getSellerID()).isEqualTo("seller123");
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct() {
        UpdateProductDTO updateDTO = UpdateProductDTO.builder()
                .name("Updated Product")
                .price(149.99)
                .build();

        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        UpdateProductDTO result = productService.updateProduct("product123", "seller123", updateDTO);

        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getPrice()).isEqualTo(149.99);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product and publish Kafka event")
    void testDeleteProductSuccess() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        productService.deleteProduct("product123", "seller123");
        verify(kafkaTemplate).send("product-deleted-topic", "product123");
        verify(productRepository).delete(testProduct);
    }

    @Test
    @DisplayName("Should delete all products of seller successfully using deleteAll")
    void testDeleteProductsOfUserSuccess() {
        Product p2 = Product.builder().id("product2").sellerID("seller123").build();
        List<Product> products = List.of(testProduct, p2);

        when(productRepository.findAllBySellerID("seller123")).thenReturn(products);

        productService.DeleteProductsOfUser("seller123");

        verify(kafkaTemplate).send("product-deleted-topic", "product123");
        verify(kafkaTemplate).send("product-deleted-topic", "product2");
        verify(productRepository).deleteAll(products); // Matches the updated Service logic
    }

    // ==========================================
    // INVENTORY MANAGEMENT (Adjust & Restock)
    // ==========================================

    @Test
    @DisplayName("Should decrease stock successfully")
    void testAdjustStockSuccess() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        StockAdjustmentRequest req = new StockAdjustmentRequest("product123", 3);
        
        productService.adjustProductStock(List.of(req));
        
        assertThat(testProduct.getQuantity()).isEqualTo(7); // 10 - 3
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should throw exception if stock is insufficient")
    void testAdjustStockInsufficient() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        StockAdjustmentRequest req = new StockAdjustmentRequest("product123", 15); // Wants 15, has 10
        
        assertThatThrownBy(() -> productService.adjustProductStock(List.of(req)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("is out of stock");
    }

    @Test
    @DisplayName("Should increase stock successfully")
    void testRestockSuccess() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        StockAdjustmentRequest req = new StockAdjustmentRequest("product123", 5);
        
        productService.restockProducts(List.of(req));
        
        assertThat(testProduct.getQuantity()).isEqualTo(15); // 10 + 5
        verify(productRepository).save(testProduct);
    }

    // ==========================================
    // WEBCLIENT & MEDIA FETCHING
    // ==========================================

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should get product with detail including createdByMe flag")
    void testGetProductWithDetail() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(InfoUserDTO.class)).thenReturn(Mono.just(testSeller));
        when(responseSpec.bodyToFlux(MediaUploadResponseDTO.class)).thenReturn(Flux.empty());

        ProductDTO result = productService.getProductWithDetail("product123", "seller123");

        assertThat(result).isNotNull();
        assertThat(result.isCreatedByMe()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should get my products with pagination and concurrent image fetching")
    void testGetMyProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findBySellerID(eq("seller123"), any(Pageable.class))).thenReturn(productPage);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(List.of("http://image1.jpg")));

        Page<ProductCardDTO> result = productService.getMyProducts(pageable, "seller123");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getImageUrls()).containsExactly("http://image1.jpg");
    }

@Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should delete product media via WebClient")
    void testDeleteProductMedia() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        
        // FIX: webClient.delete() returns RequestHeadersUriSpec, NOT RequestBodyUriSpec
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        
        // Mock the rest of the chain using requestHeadersUriSpec
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Deleted"));

        // Act
        productService.deleteProductMedia("product123", "seller123", "media456");

        // Assert
        verify(productRepository).findById("product123");
        verify(webClient).delete();
    }

    @Test
    @DisplayName("Should ignore media upload if file is empty")
    void testCreateImageWithEmptyFile() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);

        productService.createImage(mockFile, "product123", "seller123", "ROLE_SELLER");

        verify(productRepository, never()).findById(anyString());
        verify(webClient, never()).post();
    }
}