package com.portfolio.orderms.repository;

import com.portfolio.orderms.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Not used much yet in Phase 3 (Product/Inventory are saved together via
 * cascade from ProductRepository). This exists now because the future
 * stock-decrement logic (Order module) will want to load and lock an
 * Inventory row directly by product id, without loading/locking the whole
 * Product - e.g. inventoryRepository.findById(productId) with an
 * @Lock(PESSIMISTIC_WRITE) variant added at that point.
 */
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
}
