# 07_Development_Environment_Setup.md

# TrendBench 개발 환경 설정 문서

이 문서는 TrendBench 프로젝트를 실제로 구현하기 위한 개발 환경 설정 문서다.  
팀원이 로컬 환경에서 동일한 방식으로 PostgreSQL, ClickHouse, Spring Boot, React를 실행할 수 있도록 기준을 정리한다.

---

## 1. 문서 목적

이 문서는 다음 상황에서 사용한다.

- 새 팀원이 TrendBench 개발 환경을 처음 설정할 때
- PostgreSQL과 ClickHouse를 로컬에서 실행할 때
- Spring Boot 백엔드 프로젝트를 생성할 때
- React 프론트엔드 프로젝트를 생성할 때
- 공공데이터 팀과 POS 데이터 팀이 각자 개발을 시작할 때
- AI에게 구현 환경 관련 질문을 할 때

이 문서는 프로젝트 방향을 설명하는 문서가 아니라, **개발자가 실제로 실행할 수 있는 환경 설정 가이드**다.

---

## 2. 전체 개발 환경 기준

TrendBench는 다음 기술 스택을 기준으로 개발한다.

| 영역 | 기술 |
|---|---|
| Frontend | React, TypeScript, Vite |
| Backend | Java 21, Spring Boot 3.x |
| OLTP Database | PostgreSQL |
| OLAP Database | ClickHouse |
| XLSX Parsing | Apache POI |
| Chart | Recharts 또는 Chart.js |
| API Test | Postman 또는 IntelliJ HTTP Client |
| DB Client | DBeaver |
| Local Infra | Docker Compose |
| Version Control | Git, GitHub |

---

## 3. 팀별 개발 범위

TrendBench는 두 팀으로 나누어 병렬 구현한다.

### 3.1 공공데이터 팀

공공데이터 팀은 다음을 담당한다.

- ClickHouse `market_statistics` 테이블 생성
- 강남구·한식 샘플 데이터 적재
- 업종·상권 통계 API 구현
- 월별 통계 상세 API 구현
- 업종·상권 통계 화면 연결
- 통계 상세 조회 화면 연결

### 3.2 POS 데이터 팀

POS 데이터 팀은 다음을 담당한다.

- POS 매출리포트 XLSX 업로드 API 구현
- Apache POI 기반 XLSX 파싱
- `데이터 기준` 시트 파싱
- `결제 합계` 시트 파싱
- `상품 주문 상세내역` 시트 파싱
- ClickHouse `raw_order_items` 적재
- 일별/메뉴별/시간대별/요일별 집계 생성
- 내 가게 분석 API 구현

---

## 4. 필수 설치 프로그램

팀원은 로컬 개발 전에 다음 프로그램을 설치한다.

| 프로그램 | 용도 |
|---|---|
| Git | 소스 코드 버전 관리 |
| Docker Desktop | PostgreSQL, ClickHouse 실행 |
| Java 21 | Spring Boot 실행 |
| IntelliJ IDEA | 백엔드 개발 |
| Node.js LTS | React 개발 |
| VS Code | 프론트엔드 또는 문서 편집 |
| DBeaver | PostgreSQL, ClickHouse 접속 |
| Postman | API 테스트 |

### 4.1 버전 기준

| 항목 | 권장 버전 |
|---|---|
| Java | 21 |
| Spring Boot | 3.x |
| Node.js | LTS |
| PostgreSQL | 15 |
| ClickHouse | 24.8 |
| Gradle | Spring Initializr 기본값 |

---

## 5. 권장 프로젝트 폴더 구조

루트 폴더는 `trendbench`로 둔다.

```text
trendbench/
├── backend/
│   └── trendbench-api/
├── frontend/
│   └── trendbench-web/
├── infra/
│   ├── docker-compose.yml
│   ├── postgres/
│   │   └── init/
│   │       └── 01_init.sql
│   └── clickhouse/
│       └── init/
│           └── 01_init.sql
├── docs/
│   ├── 00_TrendBench_Project_Context.md
│   ├── 01_PublicData_Team_Context.md
│   ├── 02_POS_Team_Context.md
│   ├── 03_Interface_Contract.md
│   └── 07_Development_Environment_Setup.md
├── .gitignore
├── .env.example
└── README.md
```

---

## 6. Docker Compose 설정

가장 먼저 PostgreSQL과 ClickHouse를 Docker Compose로 실행한다.

파일 위치:

```text
trendbench/infra/docker-compose.yml
```

내용:

```yaml
services:
  postgres:
    image: postgres:15
    container_name: trendbench-postgres
    environment:
      POSTGRES_USER: trendbench
      POSTGRES_PASSWORD: trendbench
      POSTGRES_DB: trendbench
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./postgres/init:/docker-entrypoint-initdb.d

  clickhouse:
    image: clickhouse/clickhouse-server:24.8
    container_name: trendbench-clickhouse
    environment:
      CLICKHOUSE_DB: trendbench
      CLICKHOUSE_USER: trendbench
      CLICKHOUSE_PASSWORD: trendbench
      CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT: 1
    ports:
      - "8123:8123"
      - "9000:9000"
    volumes:
      - clickhouse_data:/var/lib/clickhouse
      - ./clickhouse/init:/docker-entrypoint-initdb.d
    ulimits:
      nofile:
        soft: 262144
        hard: 262144

volumes:
  postgres_data:
  clickhouse_data:
```

실행 명령어:

```bash
cd trendbench/infra
docker compose up -d
```

컨테이너 확인:

```bash
docker ps
```

종료:

```bash
docker compose down
```

볼륨까지 삭제하고 완전 초기화:

```bash
docker compose down -v
```

주의: `down -v`를 실행하면 DB 데이터가 삭제된다.

---

## 7. PostgreSQL 접속 정보

| 항목 | 값 |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | trendbench |
| User | trendbench |
| Password | trendbench |

DBeaver에서 PostgreSQL 연결을 추가하고 위 정보를 입력한다.

---

## 8. ClickHouse 접속 정보

| 항목 | 값 |
|---|---|
| Host | localhost |
| HTTP Port | 8123 |
| Native Port | 9000 |
| Database | trendbench |
| User | trendbench |
| Password | trendbench |

DBeaver에서 ClickHouse 드라이버를 사용해 연결한다.

Spring Boot에서는 HTTP 포트인 `8123`을 우선 사용한다.

---

## 9. PostgreSQL 초기화 SQL

파일 위치:

```text
trendbench/infra/postgres/init/01_init.sql
```

내용:

```sql
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
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
```

---

## 10. ClickHouse 초기화 SQL

파일 위치:

```text
trendbench/infra/clickhouse/init/01_init.sql
```

내용:

```sql
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
```

---

## 11. Spring Boot 프로젝트 생성

Spring Initializr 기준 설정:

| 항목 | 값 |
|---|---|
| Project | Gradle |
| Language | Java |
| Spring Boot | 3.x |
| Java | 21 |
| Group | com.trendbench |
| Artifact | trendbench-api |

### 11.1 기본 의존성

Spring Initializr에서 선택한다.

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Lombok

### 11.2 추가 의존성

`build.gradle`에 추가한다.

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    runtimeOnly 'org.postgresql:postgresql'

    implementation 'com.clickhouse:clickhouse-jdbc:0.6.0'
    implementation 'org.apache.poi:poi-ooxml:5.2.5'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

---

## 12. Spring Boot 설정 파일

파일 위치:

```text
backend/trendbench-api/src/main/resources/application.yml
```

내용:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trendbench
    username: trendbench
    password: trendbench
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
    show-sql: true

clickhouse:
  jdbc-url: jdbc:clickhouse://localhost:8123/trendbench
  username: trendbench
  password: trendbench

server:
  port: 8080
```

주의:

- PostgreSQL은 JPA로 관리한다.
- ClickHouse는 JPA로 관리하지 않는다.
- ClickHouse는 `JdbcTemplate` 또는 별도 DAO 클래스로 접근한다.
- ClickHouse는 분석 저장소이므로 row 단위 update/delete 중심으로 사용하지 않는다.

---

## 13. Backend 패키지 구조

```text
com.trendbench
├── TrendbenchApiApplication.java
├── global
│   ├── config
│   ├── error
│   └── response
├── user
├── store
├── upload
│   ├── controller
│   ├── service
│   ├── parser
│   ├── validator
│   └── dto
├── pos
│   ├── clickhouse
│   ├── aggregation
│   └── dto
├── market
│   ├── controller
│   ├── service
│   ├── clickhouse
│   └── dto
└── diagnosis
    ├── controller
    ├── service
    └── rule
```

팀별 담당 패키지:

| 팀 | 패키지 |
|---|---|
| 공공데이터 팀 | `market` |
| POS 데이터 팀 | `upload`, `pos` |
| 통합 | `diagnosis` |
| 공통 | `global`, `store`, `user` |

---

## 14. React 프로젝트 생성

React는 Vite + TypeScript 기준으로 생성한다.

```bash
cd trendbench/frontend
npm create vite@latest trendbench-web -- --template react-ts
cd trendbench-web
npm install
npm install axios recharts react-router-dom
npm run dev
```

---

## 15. Frontend 폴더 구조

```text
frontend/trendbench-web/
├── src/
│   ├── api/
│   │   ├── marketApi.ts
│   │   ├── posApi.ts
│   │   └── diagnosisApi.ts
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── MarketOverviewPage.tsx
│   │   ├── MarketDetailPage.tsx
│   │   ├── UploadPage.tsx
│   │   └── StoreDiagnosisPage.tsx
│   ├── components/
│   ├── types/
│   └── App.tsx
```

---

## 16. 기본 API 우선순위

처음부터 모든 API를 만들지 않는다.  
아래 순서대로 구현한다.

### 16.1 1차 API

```http
GET /api/health
```

```http
GET /api/markets/statistics?region=강남구&industry=한식
```

```http
GET /api/markets/statistics/monthly?region=강남구&industry=한식
```

```http
POST /api/pos/uploads
```

```http
GET /api/pos/uploads/{uploadId}
```

### 16.2 2차 API

```http
GET /api/stores/{storeId}/sales/daily?uploadId={uploadId}
```

```http
GET /api/stores/{storeId}/analysis/menu?uploadId={uploadId}
```

```http
GET /api/stores/{storeId}/analysis/hourly?uploadId={uploadId}
```

```http
GET /api/stores/{storeId}/analysis/weekday?uploadId={uploadId}
```

### 16.3 3차 API

```http
GET /api/stores/{storeId}/diagnosis?uploadId={uploadId}
```

---

## 17. 공공데이터 팀 시작 순서

공공데이터 팀은 다음 순서로 시작한다.

```text
1. ClickHouse market_statistics 테이블 생성 확인
2. 강남구·한식 샘플 데이터 insert
3. GET /api/markets/statistics 구현
4. GET /api/markets/statistics/monthly 구현
5. React 업종·상권 통계 화면 연결
6. React 통계 상세 조회 화면 연결
7. 실제 공공데이터 적재 방식 검토
```

공공데이터 팀은 처음부터 Airflow를 구현하지 않는다.  
MVP에서는 샘플 데이터 또는 수동 정제 데이터를 먼저 사용한다.

---

## 18. POS 데이터 팀 시작 순서

POS 데이터 팀은 다음 순서로 시작한다.

```text
1. PostgreSQL sales_uploads 테이블 확인
2. POST /api/pos/uploads 구현
3. XLSX 파일 업로드 받기
4. Apache POI로 시트 목록 출력
5. 데이터 기준 시트 파싱
6. 결제 합계 시트 파싱
7. 상품 주문 상세내역 시트 파싱
8. raw_order_items ClickHouse batch insert
9. fact_daily_sales 집계
10. menu_sales_daily 집계
11. hourly_sales 집계
12. weekday_sales 집계
13. 내 가게 분석 API 구현
```

POS 데이터 팀은 CSV 업로드 기준으로 구현하지 않는다.  
반드시 POS 매출리포트 XLSX 기준으로 구현한다.

---

## 19. `.gitignore`

루트에 `.gitignore`를 만든다.

```gitignore
# Java / Spring
/build/
/.gradle/
*.class
*.jar

# IntelliJ
.idea/
*.iml

# Node
node_modules/
dist/

# Env
.env
.env.local

# OS
.DS_Store

# Upload files
uploads/
*.xlsx
```

주의:

- 실제 POS XLSX 파일은 개인정보와 매출 정보가 포함될 수 있으므로 GitHub에 올리지 않는다.
- 샘플 파일이 필요한 경우 개인정보가 없는 테스트 파일만 사용한다.

---

## 20. `.env.example`

루트에 `.env.example`을 만든다.

```env
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=trendbench
POSTGRES_USER=trendbench
POSTGRES_PASSWORD=trendbench

CLICKHOUSE_HOST=localhost
CLICKHOUSE_HTTP_PORT=8123
CLICKHOUSE_DB=trendbench
CLICKHOUSE_USER=trendbench
CLICKHOUSE_PASSWORD=trendbench

BACKEND_PORT=8080
FRONTEND_PORT=5173
```

---

## 21. 첫날 작업 체크리스트

개발 첫날에는 다음 작업만 완료하면 된다.

```text
1. GitHub repository 생성
2. trendbench 폴더 구조 생성
3. infra/docker-compose.yml 작성
4. PostgreSQL, ClickHouse 실행
5. DBeaver로 두 DB 접속 확인
6. Spring Boot 프로젝트 생성
7. GET /api/health API 생성
8. React 프로젝트 생성
9. React에서 백엔드 health API 호출
10. 공공데이터 팀은 market_statistics 샘플 데이터 insert
11. POS 데이터 팀은 XLSX 업로드 API 뼈대 생성
```

---

## 22. 구현 시작 시 피해야 할 것

MVP 초기에 다음을 피한다.

```text
- Kafka부터 붙이기
- Airflow부터 붙이기
- LLM 리포트부터 만들기
- 전국 단위 공공데이터 자동 수집부터 시작하기
- CSV 업로드 기준으로 구현하기
- ClickHouse를 PostgreSQL처럼 row update/delete 중심으로 사용하기
- 실제 POS 파일을 GitHub에 올리기
```

---

## 23. 개발 환경 최종 요약

TrendBench의 로컬 개발 환경은 다음을 기준으로 한다.

```text
Docker Compose
+ PostgreSQL
+ ClickHouse
+ Spring Boot
+ React
+ Apache POI
```

공공데이터 팀은 `market_statistics` API부터 구현한다.  
POS 데이터 팀은 POS 매출리포트 XLSX 업로드와 `raw_order_items` 적재부터 구현한다.  
두 팀의 결과는 최종적으로 내 가게 분석·진단 화면에서 결합된다.
