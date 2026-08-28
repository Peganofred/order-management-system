package com.portfolio.orderms.repository;

import com.portfolio.orderms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * JpaSpecificationExecutor gives us findAll(Specification, Pageable) - lets
 * the service compose category/price/search filters into one dynamic query
 * instead of writing a combinatorial explosion of @Query methods
 * (byCategory, byPriceRange, byCategoryAndPriceRange, byNameAndCategory...).
 * See ProductSpecification for the actual predicate-building.
 */
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);
}
