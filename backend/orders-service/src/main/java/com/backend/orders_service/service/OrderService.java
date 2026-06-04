package com.backend.orders_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.backend.common.exception.CustomException;
import com.backend.orders_service.client.MediaClient;
import com.backend.orders_service.client.ProductInventoryClient;
import com.backend.orders_service.dto.CheckoutRequest;
import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.dto.RedoOrderResponse;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.model.PaymentMethod;
import com.backend.orders_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductInventoryClient productInventoryClient;
    private final MediaClient mediaClient;
    private final OrderStatusScheduler orderStatusScheduler;
    private final RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://product-service";
    private static final String CANNOT_MODIFY_ORDER_MSG = "Cannot modify order in status: ";

    // ────────────────────────────────────────────────────────────────
    // CORE ORDER LIFECYCLE
    // ────────────────────────────────────────────────────────────────

    public Order createOrder(CreateOrderRequest req) {
        Order order = Order.builder()
                .userId(req.getUserId())
                .shippingAddress(req.getShippingAddress())
                .items(req.getItems() != null ? new ArrayList<>(req.getItems()) : new ArrayList<>())
                .paymentMethod(req.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();
        return orderRepository.save(order);
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public Page<Order> getOrdersByUserId(String userId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt", "orderDate", "createdAt");
        Pageable p = PageRequest.of(page, size, sort);
        return orderRepository.findByUserIdAndIsRemovedFalse(userId, p);
    }

    public Optional<Order> findLatestPendingOrder(String userId) {
        return orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc(userId, OrderStatus.PENDING);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        if (order.isRemoved()) {
            throw new CustomException("Cannot cancel a removed order", HttpStatus.BAD_REQUEST);
        }

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new CustomException("Order can only be cancelled when in SHIPPING status. Current status: " + order.getStatus(), HttpStatus.BAD_REQUEST);
        }

        try {
            productInventoryClient.increaseStock(order.getItems());
            log.info("Successfully restored stock for cancelled order {}", orderId);
        } catch (Exception ex) {
            log.error("Failed to restore stock for cancelled order {}", orderId, ex);
            throw new CustomException("Failed to restore stock for order cancellation", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} has been cancelled", orderId);
    }

    public void removeOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED) {
            throw new CustomException("Order can only be removed when in DELIVERED or CANCELLED status.", HttpStatus.BAD_REQUEST);
        }

        order.setRemoved(true);
        orderRepository.save(order);
        log.info("Order {} has been marked as removed", orderId);
    }

    // ────────────────────────────────────────────────────────────────
    // CHECKOUT & RE-ORDER LOGIC
    // ────────────────────────────────────────────────────────────────

    public Order checkoutOrder(String orderId, CheckoutRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException("Only pending orders can be checked out", HttpStatus.BAD_REQUEST);
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new CustomException("Cannot checkout an empty order", HttpStatus.BAD_REQUEST);
        }

        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());

        if (request.getPaymentMethod() == PaymentMethod.CARD && !simulatePayment()) {
            throw new CustomException("Payment processing failed. Please check your card details and try again.", HttpStatus.PAYMENT_REQUIRED);
        }

        try {
            productInventoryClient.decreaseStock(order.getItems());
        } catch (WebClientResponseException e) {
            throw new CustomException(extractErrorMessage(e), HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.SHIPPING);
        order.setOrderDate(Instant.now());

        Order saved = orderRepository.save(order);
        orderStatusScheduler.schedulePostCheckoutUpdate(saved.getId());

        // Initialize a fresh cart for the user
        Order newCart = Order.builder()
                .userId(order.getUserId())
                .shippingAddress("")
                .items(new ArrayList<>())
                .paymentMethod(PaymentMethod.CARD)
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();
        orderRepository.save(newCart);

        log.info("Checkout successful. New empty cart created for user {}", order.getUserId());
        return saved;
    }

    public RedoOrderResponse redoOrder(String orderId) {
        Order existing = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        if (existing.isRemoved()) {
            throw new CustomException("Cannot reorder a removed order", HttpStatus.BAD_REQUEST);
        }

        List<String> outOfStockProducts = new ArrayList<>();
        List<String> partiallyFilledProducts = new ArrayList<>();
        List<OrderItem> adjustedItems = new ArrayList<>();

        for (OrderItem originalItem : existing.getItems()) {
            try {
                ProductInventoryClient.ProductDetail productDetail = productInventoryClient.getProductDetails(originalItem.getProductId());
                int availableQuantity = productDetail.getQuantity() != null ? productDetail.getQuantity() : 0;
                String productName = productDetail.getName() != null ? productDetail.getName() : originalItem.getProductName();

                if (availableQuantity == 0) {
                    outOfStockProducts.add(String.format("'%s' is out of stock", productName));
                } else if (availableQuantity < originalItem.getQuantity()) {
                    partiallyFilledProducts.add(String.format("'%s' has only %d available instead of %d", productName, availableQuantity, originalItem.getQuantity()));
                    
                    adjustedItems.add(OrderItem.builder()
                            .productId(originalItem.getProductId())
                            .quantity(availableQuantity)
                            .price(originalItem.getPrice())
                            .sellerId(originalItem.getSellerId())
                            .productName(productName)
                            .imageUrl(originalItem.getImageUrl())
                            .build());
                } else {
                    adjustedItems.add(OrderItem.builder()
                            .productId(originalItem.getProductId())
                            .quantity(originalItem.getQuantity())
                            .price(originalItem.getPrice())
                            .sellerId(originalItem.getSellerId())
                            .productName(productName)
                            .imageUrl(originalItem.getImageUrl())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Failed to check stock for product {}: {}", originalItem.getProductId(), e.getMessage());
                outOfStockProducts.add(String.format("'%s' could not be verified (may be unavailable)",
                        originalItem.getProductName() != null ? originalItem.getProductName() : originalItem.getProductId()));
            }
        }

        String message;
        if (adjustedItems.isEmpty()) {
            message = "No items could be added to cart. All products are out of stock.";
        } else if (outOfStockProducts.isEmpty() && partiallyFilledProducts.isEmpty()) {
            message = "All items successfully added to cart";
        } else {
            message = "Some items could not be fully added to cart";
        }

        Order order = null;
        if (!adjustedItems.isEmpty()) {
            Order pendingOrder = findLatestPendingOrder(existing.getUserId()).orElseGet(() -> 
                Order.builder()
                    .userId(existing.getUserId())
                    .shippingAddress(existing.getShippingAddress())
                    .items(new ArrayList<>())
                    .paymentMethod(existing.getPaymentMethod())
                    .status(OrderStatus.PENDING)
                    .orderDate(Instant.now())
                    .build()
            );

            for (OrderItem newItem : adjustedItems) {
                pendingOrder.getItems().stream()
                        .filter(item -> item.getProductId().equals(newItem.getProductId()))
                        .findFirst()
                        .ifPresentOrElse(
                                item -> item.setQuantity(item.getQuantity() + newItem.getQuantity()),
                                () -> pendingOrder.getItems().add(newItem)
                        );
            }
            order = orderRepository.save(pendingOrder);
        }

        return RedoOrderResponse.builder()
                .order(order)
                .message(message)
                .outOfStockProducts(outOfStockProducts)
                .partiallyFilledProducts(partiallyFilledProducts)
                .build();
    }

    // ────────────────────────────────────────────────────────────────
    // ORDER ITEM MANAGEMENT
    // ────────────────────────────────────────────────────────────────

    public Order addItemToOrder(String orderId, OrderItem item) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        if (order.isRemoved()) throw new CustomException("Cannot add items to a removed order", HttpStatus.BAD_REQUEST);
        validatePendingStatus(order);

        if (item.getPrice() == null || item.getSellerId() == null || item.getProductName() == null) {
            populateProductDetails(item);
        }

        order.getItems().stream()
                .filter(existing -> existing.getProductId().equals(item.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + item.getQuantity()),
                        () -> order.getItems().add(item)
                );

        return orderRepository.save(order);
    }

    public Order updateOrderItem(String orderId, String productId, OrderItem updatedItem) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        validatePendingStatus(order);

        // 🚨 FIX: Safely update only the quantity to avoid erasing historical price/seller details
        OrderItem targetItem = order.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CustomException("Product not found in order: " + productId, HttpStatus.NOT_FOUND));

        targetItem.setQuantity(updatedItem.getQuantity());

        return orderRepository.save(order);
    }

    public Order removeItemFromOrder(String orderId, String productId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        validatePendingStatus(order);

        boolean removed = order.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new CustomException("Product not found in order: " + productId, HttpStatus.NOT_FOUND);
        }

        return orderRepository.save(order);
    }

    public Order clearOrderItems(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));

        validatePendingStatus(order);
        order.setItems(new ArrayList<>());
        return orderRepository.save(order);
    }

    // ────────────────────────────────────────────────────────────────
    // UTILITIES & HELPERS
    // ────────────────────────────────────────────────────────────────

    private void validatePendingStatus(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(CANNOT_MODIFY_ORDER_MSG + order.getStatus(), HttpStatus.BAD_REQUEST);
        }
    }

    private void populateProductDetails(OrderItem item) {
        try {
            Map<String, Object> product = fetchProductFromService(item.getProductId());
            if (product != null) {
                applyProductDetails(item, product);
            } else {
                setDefaultProductDetails(item);
            }
        } catch (Exception e) {
            log.error("Failed to fetch product details for productId: {}", item.getProductId(), e);
            setDefaultProductDetails(item);
        }
    }

    private Map<String, Object> fetchProductFromService(String productId) {
        String productUrl = PRODUCT_SERVICE_URL + "/api/products/" + productId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ID", "system-service"); // Bypasses auth requirements for internal traffic
        
        return restTemplate.exchange(
                productUrl, 
                HttpMethod.GET,
                new HttpEntity<>(headers), 
                new ParameterizedTypeReference<Map<String, Object>>() {} // 🚨 FIX: Type-safe JSON deserialization
        ).getBody();
    }

    private void applyProductDetails(OrderItem item, Map<String, Object> product) {
        if (product.containsKey("price")) item.setPrice(new BigDecimal(product.get("price").toString()));
        if (product.containsKey("sellerId")) item.setSellerId(product.get("sellerId").toString());
        if (product.containsKey("name")) item.setProductName(product.get("name").toString());

        try {
            item.setImageUrl(mediaClient.getFirstImageUrl(item.getProductId()));
        } catch (Exception e) {
            log.warn("Failed to fetch image for productId: {}", item.getProductId(), e);
        }
    }

    private void setDefaultProductDetails(OrderItem item) {
        if (item.getPrice() == null) item.setPrice(BigDecimal.ZERO);
        if (item.getProductName() == null) item.setProductName("Unknown Product");
        if (item.getSellerId() == null) item.setSellerId("Unknown Seller");
    }

    @SuppressWarnings("java:S2245") // ThreadLocalRandom is appropriate here for simulation purposes
    private boolean simulatePayment() {
        return ThreadLocalRandom.current().nextInt(100) < 80; // 80% Success Rate
    }

    private String extractErrorMessage(WebClientResponseException e) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(e.getResponseBodyAsString());
            if (jsonNode.has("message")) return jsonNode.get("message").asText();
            if (jsonNode.has("error")) return jsonNode.get("error").asText();
            return e.getResponseBodyAsString();
        } catch (Exception ex) {
            return e.getMessage();
        }
    }
}