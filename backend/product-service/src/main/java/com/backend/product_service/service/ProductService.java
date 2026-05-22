package com.backend.product_service.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.ProductSimpleDTO;
import com.backend.product_service.dto.StockAdjustmentRequest;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public Product getProduct(String productID) {
        log.info("Fetching product by productID: {}", productID);
        return productRepository.findById(productID)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Get product DTO with minimal details (just product info from database)
     * Used for internal service-to-service calls.
     */
    public ProductSimpleDTO getProductDTOOnly(String productId) {
        Product product = getProduct(productId);
        return new ProductSimpleDTO(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public UpdateProductDTO updateProduct(String productId, String sellerId, UpdateProductDTO productDto) {
        Product existingProduct = checkProduct(productId, sellerId);

        if (productDto.getName() != null)
            existingProduct.setName(productDto.getName());
        if (productDto.getDescription() != null)
            existingProduct.setDescription(productDto.getDescription());
        if (productDto.getPrice() != null)
            existingProduct.setPrice(productDto.getPrice());
        if (productDto.getQuantity() != null)
            existingProduct.setQuantity(productDto.getQuantity());

        Product savedProduct = productRepository.save(existingProduct);
        return UpdateProductDTO.builder()
                .name(savedProduct.getName())
                .description(savedProduct.getDescription())
                .price(savedProduct.getPrice())
                .quantity(savedProduct.getQuantity())
                .build();
    }

    @Transactional
    public void DeleteProductsOfUser(String sellerId) {
        List<Product> products = productRepository.findAllBySellerID(sellerId);
        if (products.isEmpty())
            return;

        for (Product product : products) {
            kafkaTemplate.send("product-deleted-topic", product.getId());
        }
        productRepository.deleteAll(products);
    }

    @Transactional
    public void deleteProduct(String productId, String sellerId) {
        Product existingProduct = checkProduct(productId, sellerId);
        kafkaTemplate.send("product-deleted-topic", productId);
        productRepository.delete(existingProduct);
    }

    public void deleteProductMedia(String productId, String sellerId, String mediaId) {
        checkProduct(productId, sellerId);
        deleteMedia(mediaId);
    }

    public ProductDTO getProductWithDetail(String productId, String userId) {
        Product product = getProduct(productId);
        InfoUserDTO seller = getSellersInfo(product.getSellerID());
        List<MediaUploadResponseDTO> media = getMedia(product.getId());

        ProductDTO productDTO = new ProductDTO(product, seller, media);
        productDTO.setCreatedByMe(userId != null && product.getSellerID().equals(userId));
        return productDTO;
    }

    public List<ProductDTO> getAllProductsWithEmail(String email) {
        InfoUserDTO seller = getSellerInfoWithEmail(email);
        if (seller == null) {
            throw new CustomException("Seller not found", HttpStatus.NOT_FOUND);
        }

        List<Product> products = productRepository.findAllBySellerID(seller.getId());
        if (products.isEmpty())
            return Collections.emptyList();

        // FIX: Replaced sequential loop with Concurrent Reactive fetching
        return Flux.fromIterable(products)
                .flatMapSequential(product -> getMediaReactive(product.getId())
                        .map(media -> {
                            ProductDTO dto = new ProductDTO(product, seller, media);
                            dto.setCreatedByMe(false);
                            return dto;
                        }))
                .collectList()
                .block();
    }

    public Page<ProductCardDTO> getAllProducts(Pageable pageable, String sellerId) {
        Page<Product> productPage = productRepository.findAll(pageable);
        return convertToCardDTOPage(productPage, sellerId);
    }

    public Page<ProductCardDTO> getMyProducts(Pageable pageable, String sellerId) {
        Page<Product> productPage = productRepository.findBySellerID(sellerId, pageable);
        return convertToCardDTOPage(productPage, sellerId);
    }

    /**
     * FIX: Concurrent network fetching for limited images to prevent N+1 lag.
     */
    private Page<ProductCardDTO> convertToCardDTOPage(Page<Product> productPage, String sellerId) {
        if (productPage.isEmpty())
            return Page.empty(productPage.getPageable());

        List<ProductCardDTO> dtoList = Flux.fromIterable(productPage.getContent())
                .flatMapSequential(product -> {
                    boolean isCreator = sellerId != null && product.getSellerID().equals(sellerId);
                    return getLimitedImageUrlsReactive(product.getId(), 3)
                            .map(images -> new ProductCardDTO(
                                    product.getId(),
                                    product.getName(),
                                    product.getDescription(),
                                    product.getPrice(),
                                    product.getQuantity(),
                                    isCreator,
                                    images));
                })
                .collectList()
                .block();

        return new PageImpl<>(dtoList, productPage.getPageable(), productPage.getTotalElements());
    }

    public Product createProduct(String sellerId, CreateProductDTO productDto) {
        if (checkId(sellerId)) {
            throw new CustomException("Seller ID is null", HttpStatus.UNAUTHORIZED);
        }

        Product product = productDto.toProduct();
        product.setSellerID(sellerId);
        return productRepository.save(product);
    }

    @Transactional // FIX: Ensure atomic database writes
    public void adjustProductStock(List<StockAdjustmentRequest> adjustments) {
        if (adjustments == null || adjustments.isEmpty())
            return;

        for (StockAdjustmentRequest adjustment : adjustments) {
            Product product = getProduct(adjustment.getProductId());
            if (product.getQuantity() < adjustment.getQuantity()) {
                throw new CustomException(
                        String.format("Product '%s' is out of stock. Available: %d, Requested: %d",
                                product.getName(), product.getQuantity(), adjustment.getQuantity()),
                        HttpStatus.BAD_REQUEST);
            }
            product.setQuantity(product.getQuantity() - adjustment.getQuantity());
            productRepository.save(product);
        }
    }

    @Transactional // FIX: Ensure atomic database writes
    public void restockProducts(List<StockAdjustmentRequest> adjustments) {
        if (adjustments == null || adjustments.isEmpty())
            return;

        for (StockAdjustmentRequest adjustment : adjustments) {
            Product product = getProduct(adjustment.getProductId());
            product.setQuantity(product.getQuantity() + adjustment.getQuantity());
            productRepository.save(product);
        }
    }

    public void createImage(MultipartFile file, String productId, String sellerId, String role) {
        if (file == null || file.isEmpty())
            return;
        checkProduct(productId, sellerId);
        saveProductImage(file, productId, role);
    }

    public String saveProductImage(MultipartFile image, String productId, String role) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        try {
            ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            };
            body.add("file", fileResource);
            body.add("productId", productId);

        } catch (IOException e) {
            log.error("Failed to read image file: {}", e.getMessage());
            throw new CustomException("Failed to read image file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        MediaUploadResponseDTO mediaResponse = webClientBuilder.build().post()
                .uri("https://MEDIA-SERVICE/api/media/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("X-User-Role", role)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(MediaUploadResponseDTO.class)
                .block();

        if (mediaResponse == null || mediaResponse.getFileUrl().isBlank()) {
            throw new CustomException("Failed to upload product image.", HttpStatus.BAD_REQUEST);
        }
        return mediaResponse.getFileUrl();
    }

    // --------------------------------------------------------
    // Helper Methods & WebClient Calls
    // --------------------------------------------------------

    private InfoUserDTO getSellersInfo(String sellerId) {
        return webClientBuilder.build().get()
                .uri("https://USER-SERVICE/api/users/seller?id={sellerId}", sellerId)
                .retrieve()
                .bodyToMono(InfoUserDTO.class)
                .block();
    }

    private InfoUserDTO getSellerInfoWithEmail(String email) {
        return webClientBuilder.build().get()
                .uri("https://USER-SERVICE/api/users/email?email=" + email)
                .retrieve()
                .bodyToMono(InfoUserDTO.class)
                .block();
    }

    private List<MediaUploadResponseDTO> getMedia(String productId) {
        return getMediaReactive(productId).block();
    }

    private Mono<List<MediaUploadResponseDTO>> getMediaReactive(String productId) {
        return webClientBuilder.build().get()
                .uri("https://MEDIA-SERVICE/api/media/batch?productID={productId}", productId)
                .retrieve()
                .bodyToFlux(MediaUploadResponseDTO.class)
                .collectList()
                .onErrorResume(e -> {
                    log.error("Failed to fetch media batch for product {}: {}", productId, e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    private Mono<List<String>> getLimitedImageUrlsReactive(String productId, int limit) {
        ParameterizedTypeReference<List<String>> listType = new ParameterizedTypeReference<>() {
        };
        return webClientBuilder.build().get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("MEDIA-SERVICE")
                        .path("/api/media/product/{productId}/urls")
                        .queryParam("limit", limit)
                        .build(productId))
                .retrieve()
                .bodyToMono(listType)
                .onErrorResume(e -> {
                    log.error("Failed to fetch media URLs for product {}: {}", productId, e.getMessage());
                    return Mono.just(List.of());
                });
    }

    private String deleteMedia(String mediaId) {
        log.info("Sending delete request for media: {}", mediaId);
        return webClientBuilder.build().delete()
                .uri("https://MEDIA-SERVICE/api/media/{mediaId}", mediaId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private boolean checkId(String id) {
        return id == null || id.isBlank();
    }

    private Product checkProduct(String productId, String sellerId) {
        // FIX: The error messages here were originally swapped. They are now correct.
        if (checkId(productId)) {
            throw new CustomException("Product ID is null", HttpStatus.BAD_REQUEST);
        }
        if (checkId(sellerId)) {
            throw new CustomException("Seller ID is null", HttpStatus.UNAUTHORIZED);
        }
        Product existingProduct = getProduct(productId);
        if (!existingProduct.getSellerID().equals(sellerId)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return existingProduct;
    }
}