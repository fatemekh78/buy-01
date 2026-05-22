package com.backend.product_service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.dto.Role;
import com.backend.product_service.model.Product;

@DisplayName("Product DTO Mapping Tests")
class ProductDtoMappingTest {

    @Test
    @DisplayName("CreateProductDTO should correctly map to Product")
    void createProductDto_ToProduct() {
        // Arrange
        CreateProductDTO dto = new CreateProductDTO("Test Item", "Test Desc", 99.99, 10);

        // Act
        Product product = dto.toProduct();

        // Assert
        assertThat(product.getName()).isEqualTo("Test Item");
        assertThat(product.getDescription()).isEqualTo("Test Desc");
        assertThat(product.getPrice()).isEqualTo(99.99);
        assertThat(product.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("UpdateProductDTO should correctly map to Product")
    void updateProductDto_ToProduct() {
        // Arrange
        UpdateProductDTO dto = UpdateProductDTO.builder()
                .name("Updated Item")
                .description("Updated Desc")
                .price(49.99)
                .quantity(5)
                .build();

        // Act
        Product product = dto.toProduct();

        // Assert
        assertThat(product.getName()).isEqualTo("Updated Item");
        assertThat(product.getDescription()).isEqualTo("Updated Desc");
        assertThat(product.getPrice()).isEqualTo(49.99);
        assertThat(product.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("ClientProductDTO should map a list of Products")
    void clientProductDto_FromProductList() {
        // Arrange
        Product p1 = Product.builder().name("P1").description("D1").price(10.0).quantity(1).build();
        Product p2 = Product.builder().name("P2").description("D2").price(20.0).quantity(2).build();
        List<Product> products = List.of(p1, p2);

        ClientProductDTO mapper = ClientProductDTO.builder().build(); // Need an instance to call the method

        // Act
        List<ClientProductDTO> result = mapper.fromProductList(products);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("P1");
        assertThat(result.get(1).getPrice()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("ProductDTO custom constructor should map all fields correctly")
    void productDto_CustomConstructor() {
        // Arrange
        Product product = Product.builder()
                .id("prod123")
                .name("Camera")
                .description("DSLR Camera")
                .price(500.0)
                .quantity(3)
                .sellerID("seller99")
                .createdAt(Instant.now())
                .build();

        InfoUserDTO seller = new InfoUserDTO();
        seller.setId("seller99");
        seller.setFirstName("John");
        seller.setLastName("Doe");
        seller.setEmail("john.doe@test.com");
        seller.setRole(Role.SELLER);

        List<MediaUploadResponseDTO> media = new ArrayList<>(); // Empty list for test

        // Act
        ProductDTO dto = new ProductDTO(product, seller, media);

        // Assert
        assertThat(dto.getProductId()).isEqualTo("prod123");
        assertThat(dto.getName()).isEqualTo("Camera");
        assertThat(dto.getPrice()).isEqualTo(500.0);
        assertThat(dto.getSellerFirstName()).isEqualTo("John");
        assertThat(dto.getSellerEmail()).isEqualTo("john.doe@test.com");
        assertThat(dto.isCreatedByMe()).isFalse(); // Defaults to false in your constructor
        assertThat(dto.getMedia()).isEmpty();
    }

    @Test
    @DisplayName("ProductSimpleDTO custom constructor should map core fields")
    void productSimpleDto_CustomConstructor() {
        // Arrange
        Product product = Product.builder()
                .id("prod456")
                .name("Lens")
                .description("50mm Lens")
                .price(150.0)
                .quantity(10)
                .sellerID("seller99")
                .build();

        // Act
        ProductSimpleDTO dto = new ProductSimpleDTO(product);

        // Assert
        assertThat(dto.getProductId()).isEqualTo("prod456");
        assertThat(dto.getName()).isEqualTo("Lens");
        assertThat(dto.getSellerID()).isEqualTo("seller99");
    }
}