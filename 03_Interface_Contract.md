# 03_Interface_Contract.md

# TrendBench Interface Contract

이 문서는 TrendBench 프로젝트에서 **공공데이터 팀**과 **POS 데이터 팀**이 병렬 구현할 때 반드시 맞춰야 하는 인터페이스 계약 문서다.

목적은 다음과 같다.

1. 두 팀이 서로 다른 모듈을 개발해도 최종 통합 화면에서 데이터가 충돌하지 않게 한다.
2. 공통 식별자, 필드명, API 응답 구조, DB 역할을 통일한다.
3. 내 가게 분석 결과와 업종·상권 통계가 최종 진단 화면에서 결합될 수 있게 한다.
4. AI에게 작업을 요청할 때 프로젝트 구조가 흔들리지 않도록 기준을 제공한다.

---

## 1. 프로젝트 전제

TrendBench는 **Give-to-Get 기반 소상공인 의사결정 지원 시스템**이다.

- 사용자는 POS 매출리포트 XLSX 파일을 업로드한다.
- 시스템은 XLSX 파일에서 매출·메뉴·시간대·요일 패턴을 분석한다.
- 사용자의 데이터는 익명 집계되어 업종·상권 benchmark 데이터 풀에 기여한다.
- 사용자는 데이터를 제공한 대가로 내 가게 분석 결과와 익명 peer benchmark 지표를 제공받는다.
- 진단은 AI가 임의로 판단하지 않고, 계산된 지표를 기반으로 한 규칙 기반 로직으로 처리한다.

---

## 2. 팀별 책임 경계

### 2.1 공공데이터 팀 책임

공공데이터 팀은 업종·상권 통계 데이터를 담당한다.

주요 책임:

1. `market_statistics` 테이블 설계 및 적재
2. 강남구·한식 기본 통계 데이터 제공
3. 업종·상권 통계 요약 API 제공
4. 월별 통계 상세 API 제공
5. 상권 포화도, 점포 수, 신규 개업 수, 폐업 수, 업종 평균 매출 변화율 제공
6. 통계 화면과 통계 상세 화면에 필요한 데이터 제공

### 2.2 POS 데이터 팀 책임

POS 데이터 팀은 사용자의 POS 매출리포트 XLSX 분석을 담당한다.

주요 책임:

1. POS 매출리포트 XLSX 업로드 API 구현
2. `데이터 기준`, `결제 합계`, `상품 주문 상세내역` 시트 파싱
3. `raw_order_items` ClickHouse 적재
4. `fact_daily_sales`, `menu_sales_daily`, `hourly_sales`, `weekday_sales` 집계 생성
5. 내 가게 분석 API 제공
6. 진단 화면에 필요한 매출·메뉴·시간대·요일 지표 제공

### 2.3 통합 책임

최종 통합 지점은 **내 가게 분석·진단 화면**이다.

통합 화면에서는 다음 데이터를 결합한다.

- POS 데이터 팀 제공: 내 가게 매출, 주문 수, 객단가, 메뉴, 시간대, 요일 패턴
- 공공데이터 팀 제공: 업종 평균, 상권 평균, 점포 수, 신규 개업 수, 폐업 수, 포화도
- 최종 결과: 진단 유형, 진단 요약, 판단 근거, 대응 방향

---

## 3. 저장소 역할 계약

### 3.1 PostgreSQL

PostgreSQL은 서비스 운영에 필요한 트랜잭션성 데이터를 저장한다.

사용 대상:

- 사용자 계정
- 매장 정보
- 업로드 이력
- 업로드 상태
- 진단 결과
- 팀 간 연결에 필요한 메타데이터

주요 테이블:

- `users`
- `stores`
- `sales_uploads`
- `diagnosis_results`

### 3.2 ClickHouse

ClickHouse는 분석용 데이터를 저장한다.

공공데이터 팀 사용 테이블:

- `market_statistics`

POS 데이터 팀 사용 테이블:

- `raw_order_items`
- `fact_daily_sales`
- `menu_sales_daily`
- `hourly_sales`
- `weekday_sales`

---

## 4. 공통 식별자 계약

두 팀은 다음 공통 식별자를 반드시 일관되게 사용한다.

| 식별자 | 타입 | 생성 주체 | 사용 위치 | 설명 |
|---|---|---|---|---|
| `user_id` | Long | PostgreSQL | `users`, `stores` | 사용자 식별자 |
| `store_id` | Long | PostgreSQL | 모든 POS/진단 데이터 | 매장 식별자 |
| `upload_id` | Long | PostgreSQL | `sales_uploads`, POS ClickHouse 테이블 | 업로드 단위 식별자 |
| `region_code` | String | 공공데이터 팀 | `market_statistics`, API | 지역 코드 |
| `region_name` | String | 공공데이터 팀 | `market_statistics`, API | 지역명 |
| `business_area_code` | String | 공공데이터 팀 | `market_statistics`, API | 상권 코드 |
| `business_area_name` | String | 공공데이터 팀 | `market_statistics`, API | 상권명 |
| `industry_code` | String | 공공데이터 팀 | `market_statistics`, stores | 업종 코드 |
| `industry_name` | String | 공공데이터 팀 | `market_statistics`, stores | 업종명 |

---

## 5. 기본값 계약

초기 화면과 테스트 데이터는 다음 기본값을 사용한다.

| 항목 | 기본값 |
|---|---|
| 지역 | `강남구` |
| 업종 | `한식` |
| 상권 | `전체` 또는 `ALL` |
| 기간 단위 | 월별 |
| 업로드 파일 형식 | `.xlsx` |
| POS 핵심 시트 | `데이터 기준`, `결제 합계`, `상품 주문 상세내역` |

---

## 6. 명명 규칙

### 6.1 API 응답 필드

API 응답 JSON은 camelCase를 사용한다.

예:

```json
{
  "storeId": 1,
  "uploadId": 10,
  "totalRevenue": 3200000,
  "avgOrderValue": 14500
}
```

### 6.2 DB 컬럼

DB 컬럼은 snake_case를 사용한다.

예:

```sql
store_id
upload_id
total_revenue
avg_order_value
```

### 6.3 날짜 형식

API 응답의 날짜는 ISO-8601 문자열을 사용한다.

| 항목 | 형식 |
|---|---|
| 일자 | `YYYY-MM-DD` |
| 월 | `YYYY-MM` |
| 일시 | `YYYY-MM-DDTHH:mm:ss` |
| 시간 | `HH:mm:ss` |

---

## 7. 공공데이터 팀 데이터 계약

### 7.1 `market_statistics` 테이블

ClickHouse 테이블이다.

필드:

| 필드명 | 타입 예시 | 설명 |
|---|---|---|
| `stat_month` | Date | 통계 기준 월. 해당 월의 1일로 저장 |
| `region_code` | String | 지역 코드 |
| `region_name` | String | 지역명 |
| `business_area_code` | String | 상권 코드. 없으면 `ALL` |
| `business_area_name` | String | 상권명. 없으면 `전체` |
| `industry_code` | String | 업종 코드 |
| `industry_name` | String | 업종명 |
| `avg_sales` | Int64 | 평균 매출 |
| `avg_payment_count` | Int64 | 평균 결제건수 |
| `store_count` | Int32 | 점포 수 |
| `open_count` | Int32 | 신규 개업 수 |
| `close_count` | Int32 | 폐업 수 |
| `saturation_score` | Float64 | 상권 포화도 |
| `source_name` | String | 데이터 출처 |
| `loaded_at` | DateTime | 적재 시각 |

### 7.2 공공데이터 요약 API

Endpoint:

```http
GET /api/markets/statistics?region=강남구&industry=한식
```

응답 필드 계약:

```json
{
  "region": "강남구",
  "industry": "한식",
  "businessArea": "전체",
  "period": "2026-03",
  "avgSales": 28900000,
  "avgPaymentCount": 5600,
  "storeCount": 1281,
  "openCount": 39,
  "closeCount": 31,
  "saturationScore": 0.74,
  "salesGrowthRate": -0.052,
  "storeCountGrowthRate": 0.018,
  "status": "DECLINING"
}
```

### 7.3 공공데이터 월별 상세 API

Endpoint:

```http
GET /api/markets/statistics/monthly?region=강남구&industry=한식
```

응답 필드 계약:

```json
{
  "region": "강남구",
  "industry": "한식",
  "businessArea": "전체",
  "items": [
    {
      "period": "2026-01",
      "avgSales": 32000000,
      "avgPaymentCount": 6200,
      "storeCount": 1248,
      "openCount": 37,
      "closeCount": 22,
      "saturationScore": 0.62
    },
    {
      "period": "2026-02",
      "avgSales": 30500000,
      "avgPaymentCount": 5900,
      "storeCount": 1265,
      "openCount": 42,
      "closeCount": 25,
      "saturationScore": 0.68
    }
  ]
}
```

### 7.4 공공데이터 상태값

`status`는 다음 중 하나를 사용한다.

| 값 | 의미 |
|---|---|
| `GROWING` | 성장 |
| `STABLE` | 보합 |
| `DECLINING` | 하락 |
| `SATURATED` | 포화 |
| `UNKNOWN` | 판단 불가 |

---

## 8. POS 데이터 팀 데이터 계약

### 8.1 `sales_uploads` 테이블

PostgreSQL 테이블이다.

필드:

| 필드명 | 설명 |
|---|---|
| `upload_id` | 업로드 식별자 |
| `store_id` | 매장 식별자 |
| `original_file_name` | 원본 파일명 |
| `file_type` | 파일 형식. 기본값 `xlsx` |
| `report_start_date` | 리포트 시작일 |
| `report_end_date` | 리포트 종료일 |
| `settlement_basis` | 매출 정산 기준 |
| `aggregation_unit` | 집계 단위 |
| `status` | 업로드 처리 상태 |
| `error_message` | 오류 메시지 |
| `uploaded_at` | 업로드 시각 |
| `processed_at` | 처리 완료 시각 |

상태값:

| 값 | 의미 |
|---|---|
| `PENDING` | 업로드 접수 |
| `PARSING` | 파싱 중 |
| `SUCCESS` | 처리 성공 |
| `FAILED` | 처리 실패 |
| `SUPERSEDED` | 동일 기간 재업로드로 대체됨 |

### 8.2 `raw_order_items` 테이블

ClickHouse 테이블이다.

필드:

| 필드명 | 설명 |
|---|---|
| `store_id` | 매장 식별자 |
| `upload_id` | 업로드 식별자 |
| `order_date` | 주문일 |
| `order_time` | 주문시각 |
| `order_no` | 주문번호 |
| `order_channel` | 주문채널 |
| `payment_status` | 결제상태 |
| `product_name` | 상품명 |
| `product_code` | 상품코드 |
| `product_category` | 상품 카테고리 |
| `option_name` | 옵션명 |
| `quantity` | 수량 |
| `product_price` | 상품가격 |
| `option_price` | 옵션가격 |
| `product_discount_amount` | 상품할인 금액 |
| `order_discount_amount` | 주문할인 금액 |
| `net_sales` | 실판매금액 |
| `vat_amount` | 부가세 |
| `created_at` | 적재 시각 |

### 8.3 집계 테이블

POS 데이터 팀은 다음 집계 테이블을 제공한다.

- `fact_daily_sales`
- `menu_sales_daily`
- `hourly_sales`
- `weekday_sales`

---

## 9. POS 업로드 API 계약

### 9.1 XLSX 업로드 API

Endpoint:

```http
POST /api/pos/uploads
```

요청:

- multipart/form-data
- field: `file`
- field: `storeId`

응답 예시:

```json
{
  "uploadId": 15,
  "storeId": 1,
  "fileName": "매출리포트-260517154717.xlsx",
  "status": "PENDING",
  "message": "업로드가 접수되었습니다."
}
```

### 9.2 업로드 상태 조회 API

Endpoint:

```http
GET /api/pos/uploads/{uploadId}
```

응답 예시:

```json
{
  "uploadId": 15,
  "storeId": 1,
  "status": "SUCCESS",
  "reportStartDate": "2026-05-01",
  "reportEndDate": "2026-05-31",
  "processedAt": "2026-05-17T15:52:00",
  "errorMessage": null
}
```

실패 응답 예시:

```json
{
  "uploadId": 15,
  "storeId": 1,
  "status": "FAILED",
  "errorMessage": "필수 시트 '상품 주문 상세내역'이 존재하지 않습니다."
}
```

---

## 10. POS 분석 API 계약

### 10.1 일별 매출 API

Endpoint:

```http
GET /api/stores/{storeId}/sales/daily?uploadId={uploadId}
```

응답 예시:

```json
{
  "storeId": 1,
  "uploadId": 15,
  "items": [
    {
      "saleDate": "2026-05-17",
      "totalRevenue": 13500,
      "totalQuantity": 3,
      "totalOrders": 2,
      "avgOrderValue": 6750
    }
  ]
}
```

### 10.2 메뉴 분석 API

Endpoint:

```http
GET /api/stores/{storeId}/analysis/menu?uploadId={uploadId}
```

응답 예시:

```json
{
  "storeId": 1,
  "uploadId": 15,
  "items": [
    {
      "productName": "김치찌개",
      "productCategory": "메뉴",
      "totalSales": 420000,
      "totalQuantity": 84,
      "salesShare": 0.28
    }
  ],
  "topMenu": "김치찌개"
}
```

### 10.3 시간대 분석 API

Endpoint:

```http
GET /api/stores/{storeId}/analysis/hourly?uploadId={uploadId}
```

응답 예시:

```json
{
  "storeId": 1,
  "uploadId": 15,
  "items": [
    {
      "salesHour": 12,
      "totalSales": 320000,
      "totalQuantity": 64,
      "orderCount": 52
    }
  ],
  "peakHour": 12,
  "weakHourRange": "15:00-17:00"
}
```

### 10.4 요일 분석 API

Endpoint:

```http
GET /api/stores/{storeId}/analysis/weekday?uploadId={uploadId}
```

응답 예시:

```json
{
  "storeId": 1,
  "uploadId": 15,
  "items": [
    {
      "weekday": 6,
      "weekdayName": "토요일",
      "totalSales": 950000,
      "totalQuantity": 180,
      "orderCount": 142,
      "avgSales": 475000
    }
  ],
  "strongWeekday": "토요일",
  "weakWeekday": "월요일"
}
```

---

## 11. 통합 진단 API 계약

통합 진단 API는 POS 데이터 팀 지표와 공공데이터 팀 지표를 결합한다.

Endpoint:

```http
GET /api/stores/{storeId}/diagnosis?uploadId={uploadId}
```

응답 예시:

```json
{
  "storeId": 1,
  "uploadId": 15,
  "region": "강남구",
  "industry": "한식",
  "diagnosisType": "COMPLEX",
  "summary": "최근 매출 감소는 업종 평균 하락과 점심 시간대 주문 감소가 함께 영향을 준 것으로 보입니다.",
  "evidence": {
    "storeGrowthRate": -0.18,
    "industryGrowthRate": -0.12,
    "marketGrowthRate": -0.08,
    "paymentCountGrowthRate": -0.21,
    "avgTicketGrowthRate": 0.02,
    "saturationScore": 0.74,
    "decliningMenu": "김치찌개",
    "weakHourRange": "11:00-14:00",
    "weakWeekday": "월요일"
  },
  "causes": [
    {
      "type": "INDUSTRY_DECLINE",
      "label": "업종 전체 하락",
      "severity": "MEDIUM",
      "reason": "내 매출과 업종 평균 매출이 함께 하락했습니다."
    },
    {
      "type": "WEAK_HOUR",
      "label": "특정 시간대 부진",
      "severity": "HIGH",
      "reason": "점심 시간대 매출 감소폭이 큽니다."
    }
  ],
  "recommendations": [
    "점심 시간대 메뉴 구성을 점검하세요.",
    "업종 전체 하락 추세가 있으므로 단기 광고비 확대는 신중하게 판단하세요."
  ]
}
```

---

## 12. 진단 유형 코드 계약

진단 유형은 다음 코드를 사용한다.

| 코드 | 의미 |
|---|---|
| `STORE_UNDERPERFORMANCE` | 개별 가게 부진 |
| `INDUSTRY_DECLINE` | 업종 전체 하락 |
| `MARKET_DECLINE` | 상권 침체 |
| `MARKET_SATURATION` | 상권 포화 |
| `MENU_DECLINE` | 주요 메뉴 부진 |
| `WEAK_HOUR` | 특정 시간대 부진 |
| `WEAK_WEEKDAY` | 요일 패턴 문제 |
| `ORDER_COUNT_DECLINE` | 주문 수 감소 |
| `AVG_TICKET_DECLINE` | 객단가 하락 |
| `COMPLEX` | 복합 원인 |
| `UNKNOWN` | 판단 불가 |

---

## 13. 심각도 코드 계약

진단 원인의 심각도는 다음 코드를 사용한다.

| 코드 | 의미 |
|---|---|
| `LOW` | 낮음 |
| `MEDIUM` | 보통 |
| `HIGH` | 높음 |

---

## 14. 팀 간 결합 필드

최종 진단 화면에서 두 팀 데이터는 다음 필드를 기준으로 결합된다.

### POS 데이터 팀 제공 필드

| 필드 | 설명 |
|---|---|
| `storeGrowthRate` | 내 가게 매출 변화율 |
| `paymentCountGrowthRate` | 결제건수 변화율 |
| `avgTicketGrowthRate` | 객단가 변화율 |
| `topMenu` | 매출 1위 메뉴 |
| `topMenuSalesShare` | 1위 메뉴 매출 비중 |
| `decliningMenu` | 하락 메뉴 |
| `decliningMenuGrowthRate` | 하락 메뉴 매출 변화율 |
| `peakHour` | 피크 시간대 |
| `weakHourRange` | 취약 시간대 |
| `weakHourGrowthRate` | 취약 시간대 변화율 |
| `strongWeekday` | 강한 요일 |
| `weakWeekday` | 약한 요일 |
| `weekdayVariance` | 요일별 변동성 |

### 공공데이터 팀 제공 필드

| 필드 | 설명 |
|---|---|
| `industryGrowthRate` | 업종 평균 매출 변화율 |
| `marketGrowthRate` | 상권 평균 매출 변화율 |
| `storeCount` | 점포 수 |
| `openCount` | 신규 개업 수 |
| `closeCount` | 폐업 수 |
| `saturationScore` | 상권 포화도 |
| `marketStatus` | 상권 상태 |
| `industryStatus` | 업종 상태 |

---

## 15. 오류 응답 계약

모든 API 오류 응답은 가능한 한 다음 형식을 따른다.

```json
{
  "errorCode": "MISSING_REQUIRED_SHEET",
  "message": "필수 시트 '상품 주문 상세내역'이 존재하지 않습니다.",
  "details": {
    "sheetName": "상품 주문 상세내역"
  }
}
```

주요 오류 코드:

| 코드 | 의미 |
|---|---|
| `INVALID_FILE_TYPE` | XLSX가 아닌 파일 |
| `MISSING_REQUIRED_SHEET` | 필수 시트 누락 |
| `MISSING_REQUIRED_COLUMN` | 필수 컬럼 누락 |
| `INVALID_DATE_FORMAT` | 날짜 형식 오류 |
| `INVALID_NUMERIC_VALUE` | 금액 또는 수량 오류 |
| `OUT_OF_REPORT_PERIOD` | 리포트 기간 밖 데이터 |
| `DUPLICATE_UPLOAD` | 중복 업로드 |
| `UPLOAD_NOT_FOUND` | 업로드 없음 |
| `MARKET_STATISTICS_NOT_FOUND` | 공공 통계 없음 |
| `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 16. 중복 업로드 계약

MVP에서는 중복 업로드를 다음 방식으로 처리한다.

1. 동일 `store_id + report_start_date + report_end_date` 조합을 확인한다.
2. 기존 성공 업로드가 있으면 사용자에게 덮어쓰기 여부를 확인하거나 기존 업로드를 `SUPERSEDED` 처리한다.
3. 새 파일은 새로운 `upload_id`를 가진다.
4. ClickHouse에는 새 `upload_id` 기준으로 데이터를 insert한다.
5. 기본 조회는 최신 `SUCCESS` 업로드를 사용한다.

---

## 17. 통합 화면 계약

내 가게 분석·진단 화면은 다음 탭으로 구성한다.

1. 요약
2. 매출 추이
3. 메뉴 분석
4. 시간대 분석
5. 요일 패턴
6. 업종·상권 비교
7. 벤치마킹 결과

각 탭의 데이터 출처는 다음과 같다.

| 탭 | 데이터 출처 |
|---|---|
| 요약 | POS 데이터 + 공공데이터 |
| 매출 추이 | POS 데이터 |
| 메뉴 분석 | POS 데이터 |
| 시간대 분석 | POS 데이터 |
| 요일 패턴 | POS 데이터 |
| 업종·상권 비교 | 공공데이터 + POS 데이터 |
| 벤치마킹 결과 | 익명 peer benchmark + 공공데이터 |

---

## 18. 구현 우선순위 계약

### 공공데이터 팀

1. `market_statistics` 테이블 생성
2. 강남구·한식 샘플 데이터 적재
3. 통계 요약 API 구현
4. 월별 상세 API 구현
5. 통계 화면 연결
6. 실제 공공데이터 적재로 교체
7. Airflow 자동화는 후순위

### POS 데이터 팀

1. `sales_uploads` 테이블 생성
2. XLSX 업로드 API 구현
3. 시트 목록 검증
4. `데이터 기준` 파싱
5. `결제 합계` 파싱
6. `상품 주문 상세내역` 파싱
7. `raw_order_items` ClickHouse batch insert
8. 집계 테이블 생성
9. 분석 API 구현
10. 진단 화면 연결

### 통합

1. 두 팀 API 응답 필드명 확인
2. `store_id`, `region`, `industry`, `upload_id` 연결 확인
3. 진단 API에서 POS 지표와 공공 지표 결합
4. 진단 요약 문구 생성
5. UI에서 최종 화면 표시

---

## 19. 반드시 지켜야 할 기준

1. Give-to-Get 기반을 유지한다.
2. 사용자가 업로드하는 파일은 CSV가 아니라 POS 매출리포트 XLSX다.
3. 핵심 시트는 `데이터 기준`, `결제 합계`, `상품 주문 상세내역`이다.
4. ClickHouse를 분석 저장소로 사용한다고 가정한다.
5. PostgreSQL과 ClickHouse 역할을 구분한다.
6. 공공데이터 팀과 POS 데이터 팀은 독립 구현하되, 최종 진단 화면에서 결합한다.
7. Kafka와 Airflow는 MVP 필수 구현이 아니라 향후 확장으로 둔다.
8. 진단은 규칙 기반이다.
9. AI가 매출을 예측하거나 원인을 임의로 단정하지 않는다.
10. 기본 업종·상권 통계 화면은 강남구·한식을 기본값으로 한다.

---

## 20. 최종 요약

이 문서는 공공데이터 팀과 POS 데이터 팀의 통합을 위한 인터페이스 계약이다.

공공데이터 팀은 `market_statistics`를 통해 업종·상권 통계 데이터를 제공한다.  
POS 데이터 팀은 POS 매출리포트 XLSX를 파싱하여 `raw_order_items`와 집계 데이터를 제공한다.  
두 팀의 결과는 `store_id`, `upload_id`, `region`, `industry`를 기준으로 내 가게 분석·진단 화면에서 결합된다.

최종 시스템은 사용자가 자신의 매출 하락을 업종·상권·메뉴·시간대·요일 관점에서 해석할 수 있도록 돕는 Give-to-Get 기반 소상공인 의사결정 지원 시스템이다.
