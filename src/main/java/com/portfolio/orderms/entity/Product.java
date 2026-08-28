package com.portfolio.orderms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Product catalog data ONLY — name, description, price, category, whether
 * it's sellable. Stock quantity deliberately lives on Inventory, not here.
 * See Inventory's javadoc for why they're split.
 *
 * "active" is used for soft delete (see ProductService.deleteProduct) so
 * that a discontinued product doesn't vanish out from under historical
 * order line items that will reference it once the Order module exists.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // BigDecimal, never double/float, for money - avoids binary
    // floating-point rounding errors on prices.
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "sku", nullable = false, unique = true, length = 64)
    private String sku;

    // LAZY: listing 20 products shouldn't trigger 20 extra category queries
    // unless the caller actually asks for category details. ProductResponse
    // mapping reads category.getName() while still inside the transaction
    // (see ProductService), so this stays a single query per product fetch,
    // not N+1 - and the Specification-based list query joins categories
    // explicitly when filtering by category anyway.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // Owning side is Inventory (see Inventory.java for the @MapsId shared
    // primary key). Cascade PERSIST/MERGE only, deliberately no REMOVE:
    // products are soft-deleted (active=false), never hard-deleted through
    // normal app flow, so cascading deletes was never a case we need, and
    // leaving REMOVE off is one less way to accidentally wipe stock history
    // if a hard delete is ever added later for data cleanup.
    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Inventory inventory;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
