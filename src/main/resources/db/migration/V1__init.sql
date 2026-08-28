-- V1__init.sql
-- Baseline migration for Phase 1.
-- No domain tables yet — User (Phase 2), Product/Inventory (Phase 3),
-- Cart (Phase 4), Order (Phase 5) etc. will each get their own versioned
-- migration file (V2__..., V3__...) as we build those modules.
--
-- Keeping this file here proves Flyway + Postgres are wired correctly
-- before any real domain logic exists.

SELECT 1;
