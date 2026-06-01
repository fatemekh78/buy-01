package com.backend.orders_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;

/**
 * Custom repository interface for complex, dynamic Order queries.
 */
public interface OrderRepositoryCustom {

    Page<Order> searchAndFilterOrdersByUser(
            String userId,
            String keyword,
            Double minPrice,
            Double maxPrice,
            Instant minUpdateDate,
            Instant maxUpdateDate,
            List<OrderStatus> statuses,
            Pageable pageable);

    List<Order> findNonPendingOrdersWithFilters(
            Instant minDate,
            Instant maxDate,
            List<OrderStatus> statuses);

    Page<Order> searchAndFilterOrdersForSeller(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Instant minUpdateDate,
            Instant maxUpdateDate,
            List<OrderStatus> statuses,
            Pageable pageable);
}