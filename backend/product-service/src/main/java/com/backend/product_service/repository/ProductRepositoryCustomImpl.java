package com.backend.product_service.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.backend.product_service.model.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Product> searchAndFilterProducts(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity,
            Integer maxQuantity,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {

        List<Criteria> criteria = new ArrayList<>();

        // 1. Sanitize the keyword to prevent Regex Injection attacks
        if (keyword != null && !keyword.trim().isEmpty()) {
            String sanitizedKeyword = Pattern.quote(keyword.trim());
            Pattern pattern = Pattern.compile(".*" + sanitizedKeyword + ".*", Pattern.CASE_INSENSITIVE);
            criteria.add(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern),
                    Criteria.where("description").regex(pattern)
            ));
        }

        // 2. Price Filters
        if (minPrice != null) {
            criteria.add(Criteria.where("price").gte(minPrice));
        }
        if (maxPrice != null) {
            criteria.add(Criteria.where("price").lte(maxPrice));
        }

        // 3. Quantity Filters
        if (minQuantity != null) {
            criteria.add(Criteria.where("quantity").gte(minQuantity));
        }
        if (maxQuantity != null) {
            criteria.add(Criteria.where("quantity").lte(maxQuantity));
        }

        // 4. Date range filters
        if (startDate != null && endDate != null) {
            criteria.add(Criteria.where("createdAt").gte(startDate).lte(endDate));
        } else if (startDate != null) {
            criteria.add(Criteria.where("createdAt").gte(startDate));
        } else if (endDate != null) {
            criteria.add(Criteria.where("createdAt").lte(endDate));
        }

        // 5. Safely combine criteria
        Query query = new Query();
        if (!criteria.isEmpty()) {
            if (criteria.size() == 1) {
                // Prevent crash: if only 1 filter exists, just add it normally
                query.addCriteria(criteria.get(0));
            } else {
                // If multiple exist, safely use AND operator
                query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
            }
        }

        // Apply sorting and pagination
        query.with(pageable);

        // Execute queries
        long total = mongoTemplate.count(query, Product.class);
        List<Product> results = mongoTemplate.find(query, Product.class);

        log.info("Found {} products matching search criteria", total);
        return new PageImpl<>(results, pageable, total);
    }
}