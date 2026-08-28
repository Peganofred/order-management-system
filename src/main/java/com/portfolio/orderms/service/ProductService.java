package com.portfolio.orderms.service;

import com.portfolio.orderms.dto.ProductRequest;
import com.portfolio.orderms.dto.ProductResponse;
import com.portfolio.orderms.entity.Category;
import com.portfolio.orderms.entity.Inventory;
import com.portfolio.orderms.entity.Product;
import com.portfolio.orderms.exception.CategoryNotFoundException;
import com.portfolio.orderms.exception.DuplicateSkuException;
import com.portfolio.orderms.exception.ProductNotFoundException;
import com.portfolio.orderms.repository.CategoryRepository;
import com.portfolio.orderms.repository.ProductRepository;
import com.portfolio.orderms.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku())
                .category(category)
                .active(true)
                .build();

        // Wire the association before save so @MapsId on Inventory has a
        // Product to copy the id from once Product's id is assigned.
        // cascade = PERSIST on Product.inventory means saving the product
        // here also inserts this Inventory row - one repository call,
        // one transaction, no partially-created product without stock.
        Inventory inventory = Inventory.builder()
                .product(product)
                .stockQuantity(request.getStockQuantity())
                .build();
        product.setInventory(inventory);

        // saveAndFlush, not save: @CreationTimestamp/@UpdateTimestamp are
        // populated by Hibernate at actual INSERT time (flush), not at
        // persist() time. Our id doesn't need a flush to exist (@UuidGenerator
        // assigns it in the JVM, unlike an IDENTITY/sequence strategy), so a
        // plain save() would return before the INSERT runs, and this method
        // would build a response with createdAt/updatedAt still null. Flushing
        // here forces the INSERT now, inside this transaction, so the
        // returned entity has real DB-generated timestamp values to map into
        // ProductResponse.
        Product saved = productRepository.saveAndFlush(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new DuplicateSkuException(request.getSku());
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setCategory(category);
        // Inventory is lazy - touching it here inside the open transaction
        // triggers the (single, expected) extra SELECT, then JPA dirty
        // checking handles the UPDATE on flush. No explicit save() call
        // needed for the inventory row itself.
        product.getInventory().setStockQuantity(request.getStockQuantity());

        // Explicit flush before mapping the response - same reasoning as
        // createProduct's saveAndFlush. `product` is a managed entity, so
        // Hibernate WOULD write these changes automatically at transaction
        // commit via dirty checking - but @UpdateTimestamp's new value is
        // only computed at that flush. Without forcing it here, toResponse()
        // would read the stale pre-update `updatedAt` still sitting on the
        // in-memory entity, not the value this update actually produces.
        productRepository.flush();
        return toResponse(product);
    }

    /**
     * Soft delete: flips active=false instead of removing the row.
     * A hard DELETE would either cascade-orphan or foreign-key-fail against
     * order line items once the Order module exists, and would destroy the
     * SKU/name/price an old order needs to keep displaying correctly.
     * "Deleted" for a product means "no longer sold", not "never existed".
     */
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setActive(false);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        // Returned regardless of active status (unlike the list endpoint):
        // an admin needs to be able to fetch a deactivated product to view
        // or reactivate it, and a direct link to a since-discontinued
        // product should show "discontinued", not a generic 404.
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(String category, BigDecimal minPrice,
                                               BigDecimal maxPrice, String search,
                                               Pageable pageable) {
        return productRepository
                .findAll(ProductSpecification.build(category, minPrice, maxPrice, search), pageable)
                .map(this::toResponse);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .sku(product.getSku())
                .categoryName(product.getCategory().getName())
                .active(product.isActive())
                .stockQuantity(product.getInventory().getStockQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}