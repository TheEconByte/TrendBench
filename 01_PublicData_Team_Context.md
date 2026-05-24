# 01_PublicData_Team_Context.md

# TrendBench 공공데이터 팀 컨텍스트

이 문서는 TrendBench 프로젝트에서 **공공데이터 팀**이 AI에게 작업 컨텍스트를 주입하기 위한 기준 문서다.  
AI에게 공공데이터 수집, 테이블 설계, API 설계, 통계 화면 설계, ClickHouse 쿼리, 샘플 데이터 생성 등을 요청할 때 이 문서를 함께 제공한다.

---

## 1. 공공데이터 팀의 역할

공공데이터 팀은 TrendBench에서 **업종·상권 통계 데이터**를 담당한다.

공공데이터 팀의 결과물은 다음 화면에서 사용된다.

1. 업종·상권 통계 화면
2. 통계 상세 조회 화면
3. 내 가게 분석·진단 화면의 업종·상권 비교 영역

공공데이터 팀의 핵심 목적은 사용자가 POS 매출리포트 XLSX 파일을 업로드하기 전에도, 특정 지역과 업종의 시장 상황을 확인할 수 있도록 하는 것이다.

즉, 공공데이터 팀은 다음 질문에 답하는 데이터를 제공한다.

- 이 지역의 해당 업종은 성장 중인가, 하락 중인가?
- 해당 업종의 점포 수는 증가하고 있는가?
- 신규 개업과 폐업은 얼마나 발생하고 있는가?
- 상권이 포화 상태인가?
- 사용자의 가게 매출 변화가 업종·상권 흐름과 유사한가?

---

## 2. 프로젝트 전반과의 관계

TrendBench는 **Give-to-Get 기반 소상공인 의사결정 지원 시스템**이다.

전체 구조는 다음과 같다.

```text
공공데이터 팀:
공공 업종·상권 데이터 수집/적재
→ market_statistics 생성
→ 업종·상권 통계 API 제공
→ 업종·상권 통계 화면에 사용

POS 데이터 팀:
POS 매출리포트 XLSX 업로드
→ 데이터 기준/결제 합계/상품 주문 상세내역 파싱
→ raw_order_items 저장
→ daily/menu/hourly/weekday 집계
→ 내 가게 분석 화면에 사용

통합:
내 가게 분석 결과
+ 업종·상권 통계
+ 익명 peer benchmark
→ 진단 요약
```

공공데이터 팀의 데이터는 POS 데이터 팀의 결과와 최종 진단 화면에서 결합된다.

예를 들어 POS 데이터 팀이 다음 값을 제공한다.

```text
내 가게 최근 매출 변화율: -18%
내 가게 결제건수 변화율: -22%
점심 시간대 매출 변화율: -31%
```

공공데이터 팀은 다음 값을 제공한다.

```text
강남구 한식 업종 평균 매출 변화율: -15%
강남구 한식 점포 수 증가율: +4%
상권 포화도: 보통
```

최종 진단 로직은 두 데이터를 결합하여 다음과 같은 요약을 만든다.

```text
내 가게 매출 하락은 개별 매장 문제만으로 보기 어렵고, 같은 업종의 평균 매출 하락과 함께 나타나고 있습니다. 다만 점심 시간대 매출 하락폭이 업종 평균보다 크므로, 특정 시간대 운영 문제도 함께 점검할 필요가 있습니다.
```

---

## 3. 공공데이터 팀 MVP 범위

처음부터 전국 단위 데이터나 모든 업종 자동 수집을 목표로 하지 않는다.

### 1차 MVP 기준

```text
지역 기본값: 강남구
업종 기본값: 한식
기간 단위: 월별
저장소: ClickHouse
핵심 테이블: market_statistics
```

### 1차 MVP에서 제공할 지표

- 평균 매출
- 평균 결제건수
- 점포 수
- 신규 개업 수
- 폐업 수
- 매출 변화율
- 점포 수 변화율
- 상권 포화도
- 업종·상권 상태

### 1차 MVP에서 하지 않는 것

- 전국 모든 지역 자동 수집
- 모든 업종 코드 매핑
- Airflow DAG 자동화
- 실시간 데이터 갱신
- 반경 500m 동적 계산
- ECOS 등 거시경제 데이터 결합

위 항목은 후순위 확장으로 둔다.

---

## 4. 기본 화면 기준

공공데이터 팀이 우선 지원해야 하는 기본 화면은 다음이다.

```text
지역: 강남구
업종: 한식
```

이 기본값은 사용자가 별도 설정을 하지 않았을 때 보여주는 기본 통계다.

---

## 5. 업종·상권 통계 화면

### 5.1 화면 목적

사용자가 파일을 업로드하기 전에도 특정 지역과 업종의 전체 시장 상황을 확인할 수 있게 한다.

### 5.2 주요 카드 지표

| 카드 | 의미 |
|---|---|
| 평균 매출 | 선택 지역·업종의 월 평균 매출 |
| 평균 결제건수 | 선택 지역·업종의 월 평균 결제건수 |
| 점포 수 | 해당 지역·업종의 전체 점포 수 |
| 신규 개업 수 | 해당 월 신규 개업 점포 수 |
| 폐업 수 | 해당 월 폐업 점포 수 |
| 매출 변화율 | 전월 대비 평균 매출 변화율 |
| 상권 포화도 | 경쟁 강도 또는 점포 밀집 정도 |
| 상태 | 성장 / 보합 / 하락 / 포화 등 |

### 5.3 화면 예시

```text
강남구 / 한식 업종 통계

평균 매출: 28,900,000원
평균 결제건수: 5,600건
점포 수: 1,281개
신규 개업 수: 39개
폐업 수: 31개
매출 변화율: -5.2%
상권 포화도: 0.74
상태: 하락 추세
```

---

## 6. 통계 상세 조회 화면

### 6.1 화면 목적

업종·상권 통계의 월별 추세와 세부 지표를 확인한다.

### 6.2 제공할 데이터

- 월별 평균 매출 추이
- 월별 평균 결제건수 추이
- 월별 점포 수 추이
- 월별 신규 개업 수
- 월별 폐업 수
- 월별 상권 포화도
- 상권별 비교
- 업종별 비교

### 6.3 상세 화면 예시

| 월 | 평균 매출 | 평균 결제건수 | 점포 수 | 신규 개업 | 폐업 | 포화도 |
|---|---:|---:|---:|---:|---:|---:|
| 2026-01 | 32,000,000 | 6,200 | 1,248 | 37 | 22 | 0.62 |
| 2026-02 | 30,500,000 | 5,900 | 1,265 | 42 | 25 | 0.68 |
| 2026-03 | 28,900,000 | 5,600 | 1,281 | 39 | 31 | 0.74 |

---

## 7. ClickHouse 테이블 설계

공공데이터 팀의 핵심 테이블은 `market_statistics`다.

### 7.1 테이블 목적

`market_statistics`는 지역·상권·업종·월 단위의 시장 통계를 저장한다.

이 테이블은 다음 용도로 사용된다.

- 업종·상권 통계 화면
- 통계 상세 조회 화면
- 내 가게 분석·진단 화면의 비교 기준
- 업종 평균 변화율 계산
- 상권 평균 변화율 계산
- 상권 포화도 계산

### 7.2 ClickHouse DDL 예시

```sql
CREATE TABLE market_statistics (
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

### 7.3 필드 설명

| 필드 | 설명 |
|---|---|
| stat_month | 통계 기준 월 |
| region_code | 지역 코드 |
| region_name | 지역명. 예: 강남구 |
| business_area_code | 상권 코드. 초기 MVP에서는 ALL 가능 |
| business_area_name | 상권명. 초기 MVP에서는 전체 가능 |
| industry_code | 업종 코드 |
| industry_name | 업종명. 예: 한식 |
| avg_sales | 평균 매출 |
| avg_payment_count | 평균 결제건수 |
| store_count | 점포 수 |
| open_count | 신규 개업 수 |
| close_count | 폐업 수 |
| sales_growth_rate | 전월 대비 평균 매출 변화율 |
| store_growth_rate | 전월 대비 점포 수 변화율 |
| saturation_score | 상권 포화도 |
| status | 성장, 보합, 하락, 포화 등 상태 |
| source_name | 데이터 출처 |
| loaded_at | 적재 시각 |

---

## 8. 샘플 데이터 적재

MVP 초기에는 실제 공공 API 연동보다 샘플 또는 수동 정제 데이터를 먼저 적재해 API와 UI를 완성한다.

### 8.1 샘플 INSERT 예시

```sql
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
```

---

## 9. 주요 API 설계

공공데이터 팀은 Spring Boot API를 통해 ClickHouse의 통계 데이터를 제공한다.

---

### 9.1 업종·상권 통계 요약 API

```http
GET /api/markets/statistics?region=강남구&industry=한식
```

### 목적

선택 지역·업종의 최신 월 통계 요약을 반환한다.

### 응답 예시

```json
{
  "region": "강남구",
  "industry": "한식",
  "period": "2026-03",
  "avgSales": 28900000,
  "avgPaymentCount": 5600,
  "storeCount": 1281,
  "openCount": 39,
  "closeCount": 31,
  "salesGrowthRate": -0.052,
  "storeGrowthRate": 0.013,
  "saturationScore": 0.74,
  "status": "DECLINING"
}
```

---

### 9.2 월별 통계 상세 API

```http
GET /api/markets/statistics/monthly?region=강남구&industry=한식
```

### 목적

선택 지역·업종의 월별 추세 데이터를 반환한다.

### 응답 예시

```json
{
  "region": "강남구",
  "industry": "한식",
  "items": [
    {
      "month": "2026-01",
      "avgSales": 32000000,
      "avgPaymentCount": 6200,
      "storeCount": 1248,
      "openCount": 37,
      "closeCount": 22,
      "salesGrowthRate": 0.0,
      "saturationScore": 0.62,
      "status": "STABLE"
    },
    {
      "month": "2026-02",
      "avgSales": 30500000,
      "avgPaymentCount": 5900,
      "storeCount": 1265,
      "openCount": 42,
      "closeCount": 25,
      "salesGrowthRate": -0.047,
      "saturationScore": 0.68,
      "status": "DECLINING"
    },
    {
      "month": "2026-03",
      "avgSales": 28900000,
      "avgPaymentCount": 5600,
      "storeCount": 1281,
      "openCount": 39,
      "closeCount": 31,
      "salesGrowthRate": -0.052,
      "saturationScore": 0.74,
      "status": "DECLINING"
    }
  ]
}
```

---

### 9.3 상권 비교 API

```http
GET /api/markets/statistics/areas?region=강남구&industry=한식
```

### 목적

동일 지역 내 상권별 통계를 비교한다.

### 응답 예시

```json
{
  "region": "강남구",
  "industry": "한식",
  "items": [
    {
      "businessArea": "강남역",
      "avgSales": 31000000,
      "storeCount": 312,
      "openCount": 12,
      "closeCount": 8,
      "salesGrowthRate": -0.032,
      "saturationScore": 0.81,
      "status": "SATURATED"
    },
    {
      "businessArea": "역삼역",
      "avgSales": 29500000,
      "storeCount": 245,
      "openCount": 6,
      "closeCount": 4,
      "salesGrowthRate": 0.011,
      "saturationScore": 0.58,
      "status": "STABLE"
    }
  ]
}
```

---

## 10. 상권 포화도 계산

MVP에서는 복잡한 지리 계산보다 단순한 점수식을 사용한다.

### 10.1 기본 아이디어

상권 포화도는 다음 요소를 조합해 계산한다.

- 점포 수
- 점포 수 증가율
- 신규 개업 수
- 폐업 수
- 평균 매출 하락 여부

### 10.2 단순 포화도 산식 예시

```text
saturation_score =
  0.4 * normalized_store_count
+ 0.3 * normalized_store_growth_rate
+ 0.2 * normalized_open_count
+ 0.1 * normalized_close_count
```

또는 MVP에서는 0~1 사이의 샘플 점수를 직접 적재해도 된다.

### 10.3 상태 분류 예시

```text
saturation_score < 0.4
→ LOW

0.4 <= saturation_score < 0.7
→ MEDIUM

saturation_score >= 0.7
→ HIGH
```

API 응답에서는 사용자가 이해하기 쉽게 다음과 같이 표현할 수 있다.

```text
LOW → 낮음
MEDIUM → 보통
HIGH → 높음
```

---

## 11. 업종·상권 상태 분류

시장 상태는 평균 매출 변화율과 포화도를 기준으로 간단히 분류한다.

### 11.1 상태 분류 규칙 예시

```text
sales_growth_rate >= 0.05
→ GROWING

-0.05 < sales_growth_rate < 0.05
→ STABLE

sales_growth_rate <= -0.05 and saturation_score < 0.7
→ DECLINING

sales_growth_rate <= -0.05 and saturation_score >= 0.7
→ SATURATED_DECLINE
```

### 11.2 상태 설명 예시

| 상태 | 의미 |
|---|---|
| GROWING | 성장 중 |
| STABLE | 보합 |
| DECLINING | 하락 추세 |
| SATURATED | 포화 |
| SATURATED_DECLINE | 포화 상태에서 하락 |

---

## 12. POS 데이터 팀과의 인터페이스

공공데이터 팀은 POS 데이터 팀과 다음 값을 맞춰야 한다.

### 12.1 공통 기준 필드

| 필드 | 설명 |
|---|---|
| region_name | 지역명. 예: 강남구 |
| business_area_name | 상권명. 예: 강남역, 전체 |
| industry_name | 업종명. 예: 한식 |
| stat_month | 통계 기준 월 |

### 12.2 통합 진단 화면에 제공할 값

공공데이터 팀은 최종 진단 화면에 다음 값을 제공한다.

- industry_growth_rate
- market_growth_rate
- avg_sales
- avg_payment_count
- store_count
- open_count
- close_count
- saturation_score
- market_status

POS 데이터 팀은 다음 값을 제공한다.

- store_growth_rate
- payment_count_growth_rate
- avg_ticket_growth_rate
- top_menu_change
- weak_hour_range
- weak_weekday

최종 진단은 두 팀의 데이터를 결합한다.

---

## 13. 개발 우선순위

공공데이터 팀의 작업 순서는 다음과 같다.

```text
1. ClickHouse에 market_statistics 테이블 생성
2. 강남구·한식 샘플 데이터 또는 수동 정제 데이터 적재
3. 업종·상권 통계 요약 API 구현
4. 월별 통계 상세 API 구현
5. 업종·상권 통계 화면 연결
6. 통계 상세 조회 화면 연결
7. 상권 비교 API 구현
8. 실제 공공데이터 파일 적재로 교체
9. 실제 공공 API 연동
10. Airflow 자동화는 후순위
```

---

## 14. 하지 말아야 할 것

공공데이터 팀은 MVP 단계에서 다음을 피한다.

```text
- Airflow부터 구현하지 않는다.
- 전국 데이터 자동 수집부터 시작하지 않는다.
- 모든 업종 코드 매핑을 먼저 끝내려 하지 않는다.
- 행정동/상권 코드 매핑에 과도하게 시간을 쓰지 않는다.
- ECOS 등 거시경제 지표를 초기에 붙이지 않는다.
- POS XLSX 파싱 로직을 공공데이터 팀에서 구현하지 않는다.
- 진단 전체 로직을 공공데이터 팀 단독으로 구현하지 않는다.
```

---

## 15. AI에게 답변을 요청할 때 지켜야 할 기준

AI가 공공데이터 팀 작업에 대해 답변할 때는 다음 기준을 지켜야 한다.

1. 공공데이터 팀은 업종·상권 통계 데이터를 담당한다.
2. ClickHouse를 사용한다고 가정한다.
3. 핵심 테이블은 market_statistics다.
4. 기본값은 강남구·한식이다.
5. 처음부터 전국 데이터 자동 수집이나 Airflow를 구현하지 않는다.
6. MVP에서는 샘플 데이터 또는 수동 정제 데이터 적재를 허용한다.
7. API는 업종·상권 통계 화면과 통계 상세 화면을 지원해야 한다.
8. POS 데이터 팀의 raw_order_items 파싱과 집계는 공공데이터 팀의 책임이 아니다.
9. 최종 진단 화면에서 POS 데이터 팀의 결과와 결합된다는 점을 유지한다.
10. 사용자의 판단을 대신하는 것이 아니라 판단에 필요한 업종·상권 비교 정보를 제공하는 방향으로 설명한다.

---

## 16. 공공데이터 팀 최종 요약

공공데이터 팀은 TrendBench에서 업종·상권 통계 데이터를 담당한다.  
1차 MVP에서는 강남구·한식 기준의 샘플 또는 수동 정제 데이터를 ClickHouse의 `market_statistics` 테이블에 적재하고, 업종·상권 통계 요약 API와 월별 상세 API를 구현한다. 이 데이터는 업종·상권 통계 화면, 통계 상세 조회 화면, 내 가게 분석·진단 화면의 비교 기준으로 사용된다. 실제 공공데이터 자동 수집과 Airflow 기반 배치 파이프라인은 후순위 확장으로 둔다.
