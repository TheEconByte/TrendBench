USE trendbench;

INSERT INTO market_statistics
(stat_month, region_code, region_name, business_area_code, business_area_name,
 industry_code, industry_name, avg_sales, avg_payment_count, store_count,
 open_count, close_count, sales_growth_rate, store_growth_rate,
 saturation_score, status, source_name)
VALUES
('2026-01-01', '11680', '강남구', 'ALL', '전체', 'KOR_FOOD', '한식',
 32000000, 6200, 1248, 37, 22, 0.000, 0.000, 0.62, 'STABLE', 'sample'),
('2026-02-01', '11680', '강남구', 'ALL', '전체', 'KOR_FOOD', '한식',
 30500000, 5900, 1265, 42, 25, -0.047, 0.014, 0.68, 'DECLINING', 'sample'),
('2026-03-01', '11680', '강남구', 'ALL', '전체', 'KOR_FOOD', '한식',
 28900000, 5600, 1281, 39, 31, -0.052, 0.013, 0.74, 'DECLINING', 'sample');