package com.backend.orders_service.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;
// Import this at the top:
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Order> searchAndFilterOrdersByUser(
            String userId, String keyword, Double minPrice, Double maxPrice,
            Instant minUpdateDate, Instant maxUpdateDate,
            List<OrderStatus> statuses, Pageable pageable) {

        log.info("Executing Aggregation search for userId: {} - keyword: {}, price: {}-{}, dates: {} to {}, statuses: {}",
                userId, keyword, minPrice, maxPrice, minUpdateDate, maxUpdateDate, statuses);

        // 1. Build Initial Match Criteria (User, Dates, Keyword, Status)
        List<Criteria> matchCriteria = new ArrayList<>();
        matchCriteria.add(Criteria.where("userId").is(userId));
        matchCriteria.add(Criteria.where("isRemoved").is(false));
        applyCommonFilters(matchCriteria, keyword, minUpdateDate, maxUpdateDate, statuses);

        // 2. Initialize Aggregation Pipeline Operations
        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(Aggregation.match(new Criteria().andOperator(matchCriteria.toArray(new Criteria[0]))));

        // 3. 🚀 THE MAGIC: Calculate total price dynamically in the DB
        // This injects raw BSON to map over the items array and sum (price * quantity)
        operations.add(context -> new Document("$addFields", 
            new Document("calculatedTotalPrice", 
                new Document("$sum", 
                    new Document("$map", 
                        new Document("input", "$items")
                        .append("as", "item")
                        .append("in", new Document("$multiply", List.of(
                            new Document("$toDecimal", new Document("$ifNull", List.of("$$item.price", 0))),
                            new Document("$ifNull", List.of("$$item.quantity", 0))
                        )))
                    )
                )
            )
        ));
        // 4. Apply Price Filtering (Now that the DB knows the calculatedTotalPrice)
        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = Criteria.where("calculatedTotalPrice");
            if (minPrice != null) priceCriteria.gte(minPrice);
            if (maxPrice != null) priceCriteria.lte(maxPrice);
            operations.add(Aggregation.match(priceCriteria));
        }

        // 5. Execute Count (BEFORE pagination is applied)
        List<AggregationOperation> countOps = new ArrayList<>(operations);
        countOps.add(Aggregation.count().as("total"));
        Document countDoc = mongoTemplate.aggregate(Aggregation.newAggregation(countOps), Order.class, Document.class).getUniqueMappedResult();
        long total = countDoc != null ? countDoc.getInteger("total", 0) : 0;

        // 6. Apply Sorting and Pagination limits
        if (pageable.getSort().isSorted()) {
            operations.add(Aggregation.sort(pageable.getSort()));
        } else {
            operations.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"))); // Default sort
        }
        operations.add(Aggregation.skip((long) pageable.getOffset()));
        operations.add(Aggregation.limit(pageable.getPageSize()));

        // 7. Execute Final Query
        Aggregation mainAgg = Aggregation.newAggregation(operations);
        List<Order> results = mongoTemplate.aggregate(mainAgg, Order.class, Order.class).getMappedResults();

        log.info("Returned {} perfectly paginated orders from DB.", results.size());
        return new PageImpl<>(results, pageable, total);
    }

    // =======================================================================
    // Seller methods remain unchanged (Using standard Queries)
    // =======================================================================

    @Override
    public List<Order> findNonPendingOrdersWithFilters(Instant minDate, Instant maxDate, List<OrderStatus> statuses) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("status").ne(OrderStatus.PENDING));

        applyDateFilters(criteria, minDate, maxDate);
        applyStatusFilters(criteria, statuses);

        Query query = buildQuery(criteria);
        return mongoTemplate.find(query, Order.class);
    }

    @Override
    public Page<Order> searchAndFilterOrdersForSeller(
            String keyword, Double minPrice, Double maxPrice,
            Instant minUpdateDate, Instant maxUpdateDate,
            List<OrderStatus> statuses, Pageable pageable) {

        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("status").ne(OrderStatus.PENDING));

        applyCommonFilters(criteria, keyword, minUpdateDate, maxUpdateDate, statuses);

        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, Order.class);
        
        query.with(pageable);
        List<Order> results = mongoTemplate.find(query, Order.class);
        
        // Seller Price filtering occurs in the service layer due to mixed-seller cart complexities
        return new PageImpl<>(results, pageable, total);
    }

    // --- Helper Methods ---

    private void applyCommonFilters(List<Criteria> criteria, String keyword, Instant minDate, Instant maxDate, List<OrderStatus> statuses) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String sanitized = keyword.trim();
            Pattern pattern = Pattern.compile(Pattern.quote(sanitized), Pattern.CASE_INSENSITIVE);
            criteria.add(new Criteria().orOperator(
                    Criteria.where("id").regex(pattern),
                    Criteria.where("items.productName").regex(pattern)
            ));
        }
        applyDateFilters(criteria, minDate, maxDate);
        applyStatusFilters(criteria, statuses);
    }

    private void applyDateFilters(List<Criteria> criteria, Instant minDate, Instant maxDate) {
        if (minDate != null && maxDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minDate).lte(maxDate));
        } else if (minDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minDate));
        } else if (maxDate != null) {
            criteria.add(Criteria.where("updatedAt").lte(maxDate));
        }
    }

    private void applyStatusFilters(List<Criteria> criteria, List<OrderStatus> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            criteria.add(Criteria.where("status").in(statuses));
        }
    }

    private Query buildQuery(List<Criteria> criteria) {
        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        return query;
    }
}