package com.portfolio.orderms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Deliberately a real entity/table, NOT a Java enum, unlike Role.
 *
 * Role (CUSTOMER/ADMIN) is a fixed, code-level concept — adding a new role
 * means writing new @PreAuthorize checks anyway, so a Java enum is fine.
 *
 * Category is admin-managed product taxonomy: new categories should be
 * addable as a data change (an INSERT), not a Java enum change + redeploy.
 * It also needs its own identity for FK integrity from products, and
 * naturally grows attributes later (description, parent category for a
 * tree, an image/icon, display order) that a Java enum can't hold.
 *
 * Uses a plain auto-increment Long id (not UUID like User/Product). This is
 * a small, internal reference table seeded by Flyway — it's never a
 * business identifier handed to another service, so there's no reason to
 * pay for a UUID's randomness/index-locality cost here. Product and User
 * use UUID because those ARE identifiers that may need to be globally
 * unique/unguessable (exposed in URLs, referenced across future services).
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    // URL/slug-friendly identifier (e.g. "home-kitchen"). Kept distinct from
    // name so the display label can change (casing, wording) without
    // breaking any links or filters already built around the slug.
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
