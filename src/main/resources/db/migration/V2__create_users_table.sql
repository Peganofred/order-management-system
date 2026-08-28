-- V2__create_users_table.sql
-- Phase 2: authentication & role-based access control.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- id is a UUID generated in the JVM (Hibernate's @UuidGenerator on the entity),
-- not via a Postgres default like gen_random_uuid(). That keeps ID generation
-- visible in Java and avoids taking on the pgcrypto extension just for this.

-- role is a plain VARCHAR, not a native Postgres ENUM type: adding a role later
-- (e.g. SUPPORT) is then a simple ALTER/insert, not a Postgres type migration.
COMMENT ON COLUMN users.role IS 'CUSTOMER or ADMIN — see com.portfolio.orderms.entity.Role';
