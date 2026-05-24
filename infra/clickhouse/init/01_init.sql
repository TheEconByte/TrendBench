CREATE TABLE IF NOT EXISTS raw_order_items (
    store_id UInt64,
    upload_id UInt64,

    order_date Date,
    order_time String,
    order_no String,
    order_channel String,
    payment_status String,

    product_name String,
    product_code String,
    product_category String,
    option_name String,

    quantity Int32,
    product_price Int64,
    option_price Int64,
    product_discount_amount Int64,
    order_discount_amount Int64,
    net_sales Int64,
    vat_amount Int64,

    created_at DateTime DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(order_date)
ORDER BY (store_id, upload_id, order_date, order_no, product_name);

CREATE TABLE IF NOT EXISTS fact_daily_sales (
    store_id UInt64,
    upload_id UInt64,
    sale_date Date,

    total_revenue Int64,
    total_quantity Int64,
    total_orders UInt64,
    avg_order_value Float64,

    created_at DateTime DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(sale_date)
ORDER BY (store_id, upload_id, sale_date);

CREATE TABLE IF NOT EXISTS menu_sales_daily (
    store_id UInt64,
    upload_id UInt64,
    sale_date Date,

    product_name String,
    product_category String,
    total_quantity Int64,
    total_sales Int64,
    sales_share Float64,

    created_at DateTime DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(sale_date)
ORDER BY (store_id, upload_id, sale_date, product_name);

CREATE TABLE IF NOT EXISTS hourly_sales (
    store_id UInt64,
    upload_id UInt64,
    sale_date Date,
    sales_hour UInt8,

    total_sales Int64,
    total_quantity Int64,
    order_count UInt64,

    created_at DateTime DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(sale_date)
ORDER BY (store_id, upload_id, sale_date, sales_hour);

CREATE TABLE IF NOT EXISTS weekday_sales (
    store_id UInt64,
    upload_id UInt64,
    weekday UInt8,
    weekday_name String,

    total_sales Int64,
    total_quantity Int64,
    order_count UInt64,
    avg_sales Float64,

    created_at DateTime DEFAULT now()
)
ENGINE = MergeTree
ORDER BY (store_id, upload_id, weekday);

CREATE TABLE IF NOT EXISTS market_statistics (
    stat_month Date,

    region_code String,
    region_name String,

    business_area_code String,
    business_area_name String,

    industry_code String,
    industry_name String,

    avg_sales Int64,
    avg_payment_count Int64,

    store_count Int32,
    open_count Int32,
    close_count Int32,

    sales_growth_rate Float64,
    store_growth_rate Float64,
    saturation_score Float64,

    status String,
    source_name String,
    loaded_at DateTime DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(stat_month)
ORDER BY (region_name, industry_name, business_area_name, stat_month);