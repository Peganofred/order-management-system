package com.portfolio.orderms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Separate table from Product on purpose, even though today it's just one
 * int column. Reasoning (this is the part worth defending in an interview):
 *
 * 1. Different write patterns / different hot path. Product rows change
 *    rarely (an admin edits a description once in a while). Inventory rows
 *    change on EVERY order placed - decrementing stock is one of the
 *    hottest write paths in the whole system. Keeping them in one table
 *    means every stock decrement takes a lock on a row that also holds
 *    name/description/price, and a slow product-catalog read can end up
 *    contending with a stock write for no reason. Splitting them means the
 *    catalog (read-heavy) and stock (write-heavy, contention-sensitive)
 *    scale independently.
 *
 * 2. Locking strategy differs. Catalog reads want no locking at all.
 *    Stock decrements need one of: a pessimistic row lock
 *    (SELECT ... FOR UPDATE) to serialize concurrent decrements on the same
 *    product, or an optimistic lock (@Version below) that retries on
 *    conflict instead of blocking. Isolating that concern to its own
 *    narrow table makes it possible to reason about (and later benchmark)
 *    that locking strategy without it being entangled with unrelated
 *    Product columns.
 *
 * 3. Room to grow without touching Product. A stock ledger / movement log
 *    (reserve on checkout, release on cancel, restock, warehouse location,
 *    multi-warehouse quantities) all hang off Inventory naturally later;
 *    none of it belongs on the Product entity.
 *
 * NOT implementing the actual decrement-with-locking logic in this phase —
 * there's no order flow yet to decrement stock from. @Version is added now
 * so the optimistic-locking column exists in the schema from day one
 * (adding it later would need its own migration + backfill). When the
 * Order module lands, the actual choice will be:
 *   - Optimistic (@Version, what's wired up here): cheap, no blocking,
 *     but the caller must handle OptimisticLockException with a retry -
 *     good when stock contention on any single SKU is expected to be rare.
 *   - Pessimistic (SELECT ... FOR UPDATE via @Lock(PESSIMISTIC_WRITE)):
 *     guarantees no lost updates without any retry logic, at the cost of
 *     serializing every checkout that touches the same product - better
 *     for a small number of very hot SKUs (flash sales).
 * Either way, a DB-level CHECK (stock_quantity >= 0) is the last line of
 * defense against overselling if application logic has a bug - see the V3
 * migration.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    // No @GeneratedValue: this ID is never generated independently, it's
    // copied from the associated Product's id by @MapsId below. That's what
    // makes this a true shared-primary-key 1:1 - the DB enforces "at most
    // one inventory row per product" for free, with no extra unique
    // constraint needed, and no separate surrogate key to keep in sync.
    @Id
    @Column(name = "product_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    // Optimistic locking column - see class javadoc. Hibernate bumps this
    // and checks it in the WHERE clause on every UPDATE, so a lost update
    // (two concurrent decrements both reading qty=1 and both writing qty=0
    // instead of correctly reaching -1/rejecting one) throws
    // OptimisticLockException instead of silently corrupting stock.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
