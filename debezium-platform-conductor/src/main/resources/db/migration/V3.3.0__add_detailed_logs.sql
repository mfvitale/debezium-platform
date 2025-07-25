ALTER TABLE pipeline RENAME COLUMN logLevel TO default_log_level;

CREATE TABLE pipeline_log_levels (
    pipeline_id BIGINT NOT NULL,
    category VARCHAR(255) NOT NULL,
    level VARCHAR(255),
    PRIMARY KEY (pipeline_id, category),
    CONSTRAINT fk_pipeline_log_levels_pipeline
        FOREIGN KEY (pipeline_id) REFERENCES pipeline (id)
        ON DELETE CASCADE
);