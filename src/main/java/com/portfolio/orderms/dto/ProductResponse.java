package com.portfolio.orderms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flattens category down to its name and stock down to a plain int -
 * clients don't need to know Category or Inventory are separate tables
 * internally. Never expose the Product/Category/Inventory entities
 * directly from a controller.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private String categoryName;
    private boolean active;
    private int stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
