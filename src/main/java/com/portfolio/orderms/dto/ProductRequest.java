package com.portfolio.orderms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Used for BOTH create and update - same shape either way, and
 * ProductService.updateProduct treats every field as a full replace (PUT
 * semantics), not a partial patch. A separate PATCH-style "only send what
 * changed" DTO is a reasonable future addition but out of scope here.
 *
 * stockQuantity is part of this DTO (not a separate endpoint) because
 * Phase 3 only needs "set the count" when an admin creates/edits a
 * product. The concurrency-sensitive "decrement stock when an order is
 * placed" operation is a different, narrower endpoint that belongs to the
 * Order module - see Inventory's javadoc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
    private BigDecimal price;

    @NotBlank(message = "SKU is required")
    @Size(max = 64)
    private String sku;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must not be negative")
    private Integer stockQuantity;
}
