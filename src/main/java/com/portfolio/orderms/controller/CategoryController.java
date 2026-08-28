package com.portfolio.orderms.controller;

import com.portfolio.orderms.dto.CategoryResponse;
import com.portfolio.orderms.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public + read-only: a client (or an admin building a "create product"
 * form) needs to know which category IDs exist before calling
 * POST /api/v1/products.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.listCategories();
    }
}
