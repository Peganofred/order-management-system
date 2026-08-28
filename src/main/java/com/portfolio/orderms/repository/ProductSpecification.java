package com.portfolio.orderms.repository;

import com.portfolio.orderms.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Each method returns a Specification<Product> that ProductService combines
 * with .and(...) - only the filters actually present in the request get
 * added to the WHERE clause, and null/blank inputs are simply skipped
 * (return null -> Specification.where(...) treats a null predicate as
 * "no-op", so callers don't need if/else chains to build the query).
 */
public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> hasCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        // Explicit join (not root.get("category").get("name")) so this
        // filter and a future "select category too" both reuse one join
        // instead of Hibernate silently adding a second one.
        return (root, query, cb) -> cb.equal(
                cb.lower(root.join("category").get("name")),
                categoryName.toLowerCase());
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> nameContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("name")),
                "%" + search.toLowerCase() + "%");
    }

    /**
     * Combines every filter, skipping the ones that are null. Starting
     * point is always isActive() - the public catalog listing never shows
     * soft-deleted products, filters or not.
     */
    @SuppressWarnings("unchecked")
    public static Specification<Product> build(String categoryName, BigDecimal minPrice,
                                                 BigDecimal maxPrice, String search) {
        Specification<Product> spec = Specification.where(isActive());
        Specification<Product>[] filters = new Specification[]{
                hasCategoryName(categoryName),
                priceGreaterThanOrEqual(minPrice),
                priceLessThanOrEqual(maxPrice),
                nameContains(search)
        };
        for (Specification<Product> filter : filters) {
            if (filter != null) {
                spec = spec.and(filter);
            }
        }
        return spec;
    }
}
