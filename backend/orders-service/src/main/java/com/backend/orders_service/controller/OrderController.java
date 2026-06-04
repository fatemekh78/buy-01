package com.backend.orders_service.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.common.dto.Role;
import com.backend.common.exception.CustomException;
import com.backend.orders_service.dto.CheckoutRequest;
import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.dto.RedoOrderResponse;
import com.backend.orders_service.dto.SellerOrderDTO;
import com.backend.orders_service.dto.UpdateOrderStatusRequest;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.service.OrderSearchService;
import com.backend.orders_service.service.OrderService;
import com.backend.orders_service.service.OrderStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders API", description = "Endpoints for managing shopping carts, checkouts, and order history")
public class OrderController {

    private final OrderService orderService;
    private final OrderStatsService orderStatsService;
    private final OrderSearchService orderSearchService;

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String ADMIN_ROLE = "ADMIN";

    @PostMapping
    @Operation(summary = "Create a new order", description = "Initializes a new order (cart) for a user.")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orderService.createOrder(req));
    }

    @PostMapping("/{orderId}/checkout")
    @Operation(summary = "Checkout order", description = "Finalizes a pending order and initiates the payment/shipping process.")
    public ResponseEntity<Order> checkout(@PathVariable String orderId,
                                          @Valid @RequestBody CheckoutRequest request,
                                          HttpServletRequest httpRequest) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order checkedOut = orderService.checkoutOrder(orderId, request);
            return ResponseEntity.ok(checkedOut);
        } catch (IllegalStateException ex) {
            log.warn("Checkout failed for order {}: {}", orderId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details", description = "Retrieves an order. Sellers only see their specific items within the order.")
    public ResponseEntity<Object> getOrderById(@PathVariable String orderId, HttpServletRequest request) {
        String userRole = request.getHeader(USER_ROLE_HEADER);
        String requestingUserId = request.getHeader(USER_ID_HEADER);
        
        log.info("Order detail request: orderId={}, requestingUserId={}, role={}", orderId, requestingUserId, userRole);

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (isSeller(userRole)) {
            SellerOrderDTO sellerOrderDetail = orderSearchService.getSellerOrderDetail(orderId, requestingUserId);
            
            if (sellerOrderDetail == null) {
                log.warn("UNAUTHORIZED: Seller {} has NO items in order {}", requestingUserId, orderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't have permission to view this order"));
            }
            return ResponseEntity.ok(sellerOrderDetail);
        }

        // Handle client/admin order detail
        if (!hasAccessToOrder(order, request)) {
            log.warn("UNAUTHORIZED: Client {} does not own order {}", requestingUserId, orderId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Search user orders", description = "Fetches paginated order history with dynamic filtering.")
    public ResponseEntity<Page<?>> getUserOrders(@PathVariable String userId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Double minPrice,
                                                 @RequestParam(required = false) Double maxPrice,
                                                 @RequestParam(required = false) String minUpdateDate,
                                                 @RequestParam(required = false) String maxUpdateDate,
                                                 @RequestParam(required = false) List<OrderStatus> statuses,
                                                 HttpServletRequest request) {

        String userRole = request.getHeader(USER_ROLE_HEADER);
        String requestingUserId = request.getHeader(USER_ID_HEADER);

        if (!isAdmin(userRole) && !requestingUserId.equals(userId)) {
            return ResponseEntity.ok(Page.empty());
        }

        if (isSeller(userRole)) {
            return ResponseEntity.ok(orderSearchService.searchAndFilterSellerOrders(
                    userId, keyword, minPrice, maxPrice, minUpdateDate, maxUpdateDate, statuses, page, size));
        }

        return ResponseEntity.ok(orderSearchService.searchAndFilterOrders(
                userId, keyword, minPrice, maxPrice, minUpdateDate, maxUpdateDate, statuses, page, size));
    }

    @GetMapping("/user/{userId}/cart")
    @Operation(summary = "Get active cart", description = "Retrieves the user's latest pending order.")
    public ResponseEntity<Order> getActiveCart(@PathVariable String userId, HttpServletRequest request) {
        String requestingUserId = request.getHeader(USER_ID_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);

        if (!isAdmin(userRole) && !userId.equals(requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Order> pendingCart = orderService.findLatestPendingOrder(userId);
        return pendingCart.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get all orders (Internal/Admin)", description = "Fetches all orders in the system for statistical calculation.")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/user/{userId}/stats")
    @Operation(summary = "Get user statistics", description = "Calculates total orders, amount spent, and top products for a user.")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String userId) {
        return ResponseEntity.ok(orderStatsService.calculateUserStats(userId));
    }

    @GetMapping("/seller/{sellerId}/stats")
    @Operation(summary = "Get seller statistics", description = "Calculates total revenue, sales, and customers for a seller.")
    public ResponseEntity<Map<String, Object>> getSellerStats(@PathVariable String sellerId) {
        return ResponseEntity.ok(orderStatsService.calculateSellerStats(sellerId));
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Transitions an order to a new state (Admin only).")
    public ResponseEntity<Order> updateStatus(@PathVariable String orderId,
                                              @Valid @RequestBody UpdateOrderStatusRequest req,
                                              HttpServletRequest request) {
        String userRole = request.getHeader(USER_ROLE_HEADER);
        if (!isAdmin(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, req.getStatus()));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Cancel order", description = "Cancels an order and attempts to restore product stock.")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't have permission to cancel this order"));
        }

        try {
            orderService.cancelOrder(orderId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/redo")
    @PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Redo order", description = "Recreates a previous order based on current stock availability.")
    public ResponseEntity<RedoOrderResponse> redo(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        RedoOrderResponse response = orderService.redoOrder(orderId);

        if (response.getOrder() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // ────────────────────────────────────────────────────────────────
    // ORDER ITEM MANAGEMENT
    // ────────────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/items")
    @Operation(summary = "Add item to cart", description = "Adds a new product to an existing pending order.")
    public ResponseEntity<Order> addItemToOrder(@PathVariable String orderId,
                                                @Valid @RequestBody OrderItem item,
                                                HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return ResponseEntity.ok(orderService.addItemToOrder(orderId, item));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{orderId}/items/{productId}")
    @Operation(summary = "Update item quantity", description = "Modifies the quantity of a product currently in the cart.")
    public ResponseEntity<Order> updateOrderItem(@PathVariable String orderId,
                                                 @PathVariable String productId,
                                                 @Valid @RequestBody OrderItem item,
                                                 HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return ResponseEntity.ok(orderService.updateOrderItem(orderId, productId, item));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{orderId}/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Deletes a specific product from a pending order.")
    public ResponseEntity<Order> removeItemFromOrder(@PathVariable String orderId,
                                                     @PathVariable String productId,
                                                     HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return ResponseEntity.ok(orderService.removeItemFromOrder(orderId, productId));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{orderId}/items")
    @Operation(summary = "Clear cart", description = "Removes all items from a pending order.")
    public ResponseEntity<Order> clearOrderItems(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return ResponseEntity.ok(orderService.clearOrderItems(orderId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{orderId}/remove")
    @PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN')")
    @Operation(summary = "Soft delete order", description = "Marks a completed/cancelled order as hidden from the user's history.")
    public ResponseEntity<Map<String, String>> removeOrder(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            orderService.removeOrder(orderId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // ────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────

    private boolean hasAccessToOrder(Order order, HttpServletRequest request) {
        String requestingUserId = request.getHeader(USER_ID_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);
        return isAdmin(userRole) || (requestingUserId != null && requestingUserId.equals(order.getUserId()));
    }

    private boolean isAdmin(String userRole) {
        return userRole != null && userRole.equalsIgnoreCase(ADMIN_ROLE);
    }

    private boolean isSeller(String userRole) {
        return userRole != null && (userRole.equalsIgnoreCase(Role.SELLER.name()) || userRole.contains(Role.SELLER.name()));
    }
}