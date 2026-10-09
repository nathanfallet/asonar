-- The schema as `SchemaUtils.create` left it, before Flyway. A database from that era already has
-- these tables: Flyway records it at version 1 (baselineOnMigrate) and never runs this file there.
-- A database whose Apps table predates `role` needs, once, before the first boot on Flyway:
--   ALTER TABLE Apps ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'OWNED';
CREATE TABLE IF NOT EXISTS AppReviews (id BIGINT AUTO_INCREMENT PRIMARY KEY, store VARCHAR(20) NOT NULL, store_app_id VARCHAR(255) NOT NULL, country VARCHAR(2) NOT NULL, external_id VARCHAR(255) NOT NULL, author text NULL, title text NULL, content text NOT NULL, rating INT NULL, version VARCHAR(64) NULL, posted_at DATETIME(6) NOT NULL, fetched_at DATETIME(6) NOT NULL);

ALTER TABLE AppReviews ADD CONSTRAINT AppReviews_store_store_app_id_country_external_id_unique UNIQUE (store, store_app_id, country, external_id);

CREATE INDEX AppReviews_store_store_app_id_country_posted_at ON AppReviews (store, store_app_id, country, posted_at);

CREATE TABLE IF NOT EXISTS RankSnapshots (id BIGINT AUTO_INCREMENT PRIMARY KEY, keyword_id BIGINT NOT NULL, app_id BIGINT NOT NULL, `rank` INT NULL, total_results INT NULL, captured_at DATETIME(6) NOT NULL);

CREATE INDEX RankSnapshots_keyword_id_app_id_captured_at ON RankSnapshots (keyword_id, app_id, captured_at);

CREATE TABLE IF NOT EXISTS AppRatingSnapshots (id BIGINT AUTO_INCREMENT PRIMARY KEY, store VARCHAR(20) NOT NULL, store_app_id VARCHAR(255) NOT NULL, country VARCHAR(2) NOT NULL, `name` text NOT NULL, rating_count INT NULL, average_rating DOUBLE PRECISION NULL, captured_at DATETIME(6) NOT NULL);

CREATE INDEX AppRatingSnapshots_store_store_app_id_country_captured_at ON AppRatingSnapshots (store, store_app_id, country, captured_at);

CREATE TABLE IF NOT EXISTS PopularitySnapshots (id BIGINT AUTO_INCREMENT PRIMARY KEY, keyword_id BIGINT NOT NULL, popularity INT NOT NULL, captured_at DATETIME(6) NOT NULL);

CREATE INDEX PopularitySnapshots_keyword_id_captured_at ON PopularitySnapshots (keyword_id, captured_at);

CREATE TABLE IF NOT EXISTS Keywords (id BIGINT AUTO_INCREMENT PRIMARY KEY, term VARCHAR(255) NOT NULL, store VARCHAR(20) NOT NULL, country VARCHAR(2) NOT NULL, created_at DATETIME(6) NOT NULL);

ALTER TABLE Keywords ADD CONSTRAINT Keywords_term_store_country_unique UNIQUE (term, store, country);

CREATE TABLE IF NOT EXISTS TopAppSnapshots (id BIGINT AUTO_INCREMENT PRIMARY KEY, keyword_id BIGINT NOT NULL, `position` INT NOT NULL, store_app_id VARCHAR(255) NOT NULL, app_name text NOT NULL, subtitle text NULL, rating_count INT NULL, average_rating DOUBLE PRECISION NULL, captured_at DATETIME(6) NOT NULL);

CREATE INDEX TopAppSnapshots_keyword_id_captured_at ON TopAppSnapshots (keyword_id, captured_at);

CREATE TABLE IF NOT EXISTS KeywordSignalSnapshots (id BIGINT AUTO_INCREMENT PRIMARY KEY, keyword_id BIGINT NOT NULL, competitors text NOT NULL, total_results INT NULL, captured_at DATETIME(6) NOT NULL);

CREATE INDEX KeywordSignalSnapshots_keyword_id_captured_at ON KeywordSignalSnapshots (keyword_id, captured_at);

CREATE TABLE IF NOT EXISTS Apps (id BIGINT AUTO_INCREMENT PRIMARY KEY, store VARCHAR(20) NOT NULL, store_app_id VARCHAR(255) NOT NULL, `name` text NOT NULL, `role` VARCHAR(20) DEFAULT 'OWNED' NOT NULL, created_at DATETIME(6) NOT NULL);

ALTER TABLE Apps ADD CONSTRAINT Apps_store_store_app_id_unique UNIQUE (store, store_app_id);

CREATE TABLE IF NOT EXISTS KeywordCandidates (id BIGINT AUTO_INCREMENT PRIMARY KEY, app_id BIGINT NOT NULL, term VARCHAR(255) NOT NULL, country VARCHAR(2) NOT NULL, sources VARCHAR(255) NOT NULL, detail text NULL, popularity INT NULL, status VARCHAR(20) NOT NULL, discovered_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL);

ALTER TABLE KeywordCandidates ADD CONSTRAINT KeywordCandidates_app_id_term_country_unique UNIQUE (app_id, term, country);

CREATE INDEX KeywordCandidates_app_id_status ON KeywordCandidates (app_id, status);
