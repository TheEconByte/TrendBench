CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    role VARCHAR(30) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS stores (
    store_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    store_name VARCHAR(100) NOT NULL,
    business_number VARCHAR(30),
    region_name VARCHAR(50),
    business_area_name VARCHAR(50),
    industry_name VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    price_level VARCHAR(30),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sales_uploads (
    upload_id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(store_id),
    original_file_name VARCHAR(255),
    file_type VARCHAR(20) DEFAULT 'xlsx',
    report_start_date DATE,
    report_end_date DATE,
    settlement_basis VARCHAR(50),
    aggregation_unit VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    uploaded_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diagnosis_results (
    diagnosis_id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(store_id),
    upload_id BIGINT REFERENCES sales_uploads(upload_id),
    diagnosis_type VARCHAR(50),
    summary TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);