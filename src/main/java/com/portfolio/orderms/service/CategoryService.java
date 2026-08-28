package com.portfolio.orderms.service;

import com.portfolio.orderms.dto.CategoryResponse;
import com.portfolio.orderms.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only on purpose - Phase 3 seeds categories via Flyway (V3) and
 * doesn't expose a category-management API yet. Categories are low-churn
 * reference data; admin CRUD for them can be added later as its own small
 * phase if the project needs it, without touching Product at all.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> CategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .build())
                .toList();
    }
}
