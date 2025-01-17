ALTER TABLE transform
    ADD COLUMN predicate_type VARCHAR(255),
    ADD COLUMN predicate_config JSONB,
    ADD COLUMN predicate_negate BOOLEAN;