package com.portfolio.orderms.repository;

import com.portfolio.orderms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
