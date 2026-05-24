# 02_POS_Team_Context.md

# TrendBench POS 데이터 팀 컨텍스트

이 문서는 TrendBench 프로젝트에서 **POS 데이터 팀**이 AI에게 작업 컨텍스트를 주입하기 위한 기준 문서다.  
POS 데이터 팀은 사용자가 업로드한 **POS 매출리포트 XLSX 파일**을 파싱하고, 주문·상품 단위 데이터를 분석 가능한 형태로 저장·집계하여 **내 가게 분석·진단 화면**에 필요한 데이터를 제공한다.

---

## 1. POS 데이터 팀의 역할

POS 데이터 팀은 다음 기능을 담당한다.

1. POS 매출리포트 XLSX 업로드 API 구현
2. XLSX 파일 구조 검증
3. `데이터 기준` 시트 파싱
4. `결제 합계` 시트 파싱
5. `상품 주문 상세내역` 시트 파싱
6. 주문·상품 단위 raw data 추출
7. ClickHouse `raw_order_items` 적재
8. 일별 매출 집계 생성
9. 메뉴별 매출 집계 생성
10. 시간대별 매출 집계 생성
11. 요일별 매출 패턴 집계 생성
12. 내 가게 분석 API 제공
13. 공공데이터 팀의 업종·상권 통계와 결합 가능한 지표 제공

POS 데이터 팀의 결과물은 최종적으로 **내 가게 분석·진단 화면**에서 공공데이터 팀의 `market_statistics` 데이터와 결합된다.

---

## 2. 프로젝트 전반 기준

POS 데이터 팀은 항상 다음 프로젝트 기준을 따라야 한다.

- TrendBench는 **Give-to-Get 기반 소상공인 의사결정 지원 시스템**이다.
- 사용자가 업로드하는 파일은 CSV가 아니라 **POS 매출리포트 XLSX 파일**이다.
- 핵심 목적은 AI가 매출을 예측하는 것이 아니라, 사용자가 판단할 수 있도록 **분석 지표와 비교 지표를 제공**하는 것이다.
- POS 데이터는 익명 집계되어 업종·상권 benchmark 데이터 풀에 기여한다.
- 진단은 LLM이 임의로 판단하지 않고, 계산된 지표를 기반으로 한 **규칙 기반 로직**으로 처리한다.
- ClickHouse를 분석 저장소로 사용한다고 가정한다.
- Kafka와 Airflow는 기본 MVP 구현 대상이 아니라 향후 확장 기능이다.

---

## 3. 입력 파일 규격

사용자가 업로드하는 파일은 POS 시스템에서 다운로드한 매출리포트 원본 파일이다.

### 기본 규격

| 항목 | 규격 |
|---|---|
| 파일 형식 | `.xlsx` |
| 파일 내용 | POS 매출리포트 원본 |
| 사용자 수정 | 원칙적으로 수정하지 않고 그대로 업로드 |
| 분석 기준 | 일간 매출 + 주문·상품 단위 상세 분석 |
| 내부 처리 | XLSX 파싱 후 표준 DB 스키마로 변환 |

### 주요 시트

업로드 파일에는 다음 시트들이 포함될 수 있다.

1. `데이터 기준`
2. `결제 합계`
3. `상품 주문 합계`
4. `결제 상세내역`
5. `상품 주문 상세내역`

### MVP 기준 사용 시트

| 시트명 | 사용 여부 | 역할 |
|---|---|---|
| `데이터 기준` | 필수 | 리포트 기간, 정산 기준, 집계 단위 검증 |
| `결제 합계` | 필수 | 일별 총매출, 결제건수, 결제수단별 금액 추출 |
| `상품 주문 상세내역` | 필수 | 메뉴·시간대·요일 분석을 위한 주문·상품 단위 raw data 추출 |
| `상품 주문 합계` | 권장 | 상품별 요약 분석 보조 |
| `결제 상세내역` | 후순위 | 결제 단위 상세 검증 및 확장 분석 |

---

## 4. XLSX 파싱 원칙

### 4.1 파일 단위 검증

업로드 시 다음을 검증한다.

- 확장자가 `.xlsx`인지 확인
- 파일이 비어 있지 않은지 확인
- 암호화 파일이 아닌지 확인
- 필수 시트가 존재하는지 확인
- 필수 시트명이 변경되지 않았는지 확인

### 4.2 시트 단위 검증

각 시트에서 다음을 검증한다.

- 필수 컬럼 존재 여부
- 날짜 형식 변환 가능 여부
- 금액 값이 0 이상인지 여부
- 수량 값이 0 이상인지 여부
- 결제상태 값이 분석 대상인지 여부
- 리포트 기간 범위 안에 포함되는지 여부
- 중복 주문번호 또는 중복 행 처리 기준

### 4.3 날짜와 시간 처리

Excel 파일에서는 날짜와 시간이 문자열이 아니라 Excel serial number로 저장될 수 있다.

허용해야 하는 날짜 형식:

- `2026-05-17`
- `2026.05.17`
- `2026/05/17`
- Excel serial date

허용해야 하는 시간 형식:

- `12:30:00`
- `12:30`
- Excel serial datetime

Apache POI를 사용할 때 날짜 셀, 숫자 셀, 문자열 셀을 모두 처리해야 한다.

---

## 5. `데이터 기준` 시트

### 역할

`데이터 기준` 시트는 업로드 파일의 분석 기준을 확인하는 메타데이터 시트다.

### 주요 컬럼

| 컬럼명 | 내부 필드 | 설명 |
|---|---|---|
| 시작일자 | `report_start_date` | 리포트 시작일 |
| 종료일자 | `report_end_date` | 리포트 종료일 |
| 매출 정산 기준 | `settlement_basis` | 주문한 날, 결제한 날 등 |
| 매출 시작 시간 | `sales_start_time` | 하루 매출 기준 시작 시간 |
| 집계 단위 | `aggregation_unit` | 일간, 주간, 월간 |

### 검증 조건

- `시작일자`가 존재해야 한다.
- `종료일자`가 존재해야 한다.
- `집계 단위`는 MVP 기준 `일간`이어야 한다.
- `결제 합계`와 `상품 주문 상세내역`의 날짜가 시작일자~종료일자 범위 안에 있어야 한다.
- 리포트 시작일이 종료일보다 늦으면 오류 처리한다.

---

## 6. `결제 합계` 시트

### 역할

`결제 합계` 시트는 일별 총매출과 결제건수 검증에 사용한다.  
이 시트는 전체 매출 요약과 `상품 주문 상세내역` 기반 집계 결과를 검산하는 데 사용한다.

### 주요 컬럼 및 내부 매핑

| 컬럼명 | 내부 필드 | 설명 |
|---|---|---|
| 기간 | `sales_date` | 매출 기준일 |
| 결제금액 | `gross_sales` | 일별 총 결제금액 |
| 부가세 | `vat_amount` | 일별 부가세 |
| 결제건수 | `payment_count` | 일별 결제 건수 |
| 현금 | `cash_sales` | 현금 결제금액 |
| 카드 | `card_sales` | 카드 결제금액 |
| QR결제 | `qr_sales` | QR 결제금액 |
| 계좌이체 | `bank_transfer_sales` | 계좌이체 금액 |
| 선불지급수단 | `prepaid_sales` | 선불 결제금액 |
| 기타 | `other_sales` | 기타 결제금액 |

### 계산 지표

- 일별 총매출
- 일별 결제건수
- 객단가 = 결제금액 / 결제건수
- 매출 변화율
- 결제건수 변화율
- 결제수단별 비중

### 활용

`결제 합계` 시트는 다음에 사용한다.

- 일별 매출 요약
- 결제건수 기반 객단가 계산
- `상품 주문 상세내역`에서 집계한 매출과 검산
- 진단 화면의 매출 추이 카드 생성

---

## 7. `상품 주문 상세내역` 시트

### 역할

`상품 주문 상세내역` 시트는 POS 데이터 팀의 핵심 원천 데이터다.  
메뉴 분석, 시간대 분석, 요일 패턴 분석은 이 시트를 기반으로 한다.

### 주요 컬럼 및 내부 매핑

| 컬럼명 | 내부 필드 | 설명 |
|---|---|---|
| 주문기준일자 | `order_date` | 주문 기준 날짜 |
| 결제상태 | `payment_status` | 완료, 취소 등 |
| 주문시작시각 | `order_time` | 주문 시작 시각 |
| 주문채널 | `order_channel` | 포스, 배달 등 |
| 주문번호 | `order_no` | 주문 식별 번호 |
| 상품명 | `product_name` | 상품 이름 |
| 상품코드 | `product_code` | 상품 코드 |
| 카테고리 | `product_category` | 상품 카테고리 |
| 옵션 | `option_name` | 선택 옵션 |
| 수량 | `quantity` | 주문 수량 |
| 상품가격 | `product_price` | 상품 기본 가격 |
| 옵션가격 | `option_price` | 옵션 가격 |
| 상품할인 금액 | `product_discount_amount` | 상품별 할인액 |
| 주문할인 금액 | `order_discount_amount` | 주문 단위 할인 배분액 |
| 실판매금액 | `net_sales` | 최종 판매금액 |
| 과세여부 | `tax_type` | 과세, 면세 |
| 부가세액 | `vat_amount` | 부가세 |

### 필터링 기준

MVP에서는 다음 기준으로 분석 대상을 정한다.

- `결제상태 = 완료` 또는 정상 매출로 볼 수 있는 상태만 포함한다.
- 취소, 환불 데이터는 기본 분석에서 제외하거나 별도 집계한다.
- `수량 <= 0` 또는 `실판매금액 < 0`인 행은 오류 또는 예외 데이터로 처리한다.
- 상품명 또는 주문일자가 없는 행은 오류 처리한다.

---

## 8. POS 데이터 처리 파이프라인

POS 데이터 팀의 처리 흐름은 다음과 같다.

1. 사용자가 POS 매출리포트 XLSX 파일을 업로드한다.
2. PostgreSQL `sales_uploads`에 업로드 이력을 생성한다.
3. 업로드 상태를 `PENDING`으로 저장한다.
4. 파일 확장자와 필수 시트를 검증한다.
5. `데이터 기준` 시트를 파싱하여 리포트 기간과 집계 단위를 검증한다.
6. `결제 합계` 시트를 파싱하여 일별 결제 합계를 추출한다.
7. `상품 주문 상세내역` 시트를 파싱하여 주문·상품 단위 raw data를 추출한다.
8. `raw_order_items`를 ClickHouse에 batch insert한다.
9. `raw_order_items` 기반으로 집계 테이블을 생성한다.
10. `fact_daily_sales`를 생성한다.
11. `menu_sales_daily`를 생성한다.
12. `hourly_sales`를 생성한다.
13. `weekday_sales`를 생성한다.
14. PostgreSQL `sales_uploads` 상태를 `SUCCESS` 또는 `FAILED`로 갱신한다.
15. 내 가게 분석 API에서 집계 데이터를 반환한다.

---

## 9. 저장소 역할 분리

### PostgreSQL

PostgreSQL은 서비스 운영 메타데이터를 저장한다.

- 사용자 정보
- 매장 정보
- 업로드 이력
- 업로드 상태
- 진단 결과

### ClickHouse

ClickHouse는 분석 데이터를 저장한다.

- 주문·상품 단위 raw data
- 일별 매출 집계
- 메뉴별 매출 집계
- 시간대별 매출 집계
- 요일별 매출 집계

---

## 10. PostgreSQL 테이블

### `sales_uploads`

업로드 이력과 처리 상태를 관리한다.

필드:

- `upload_id`
- `store_id`
- `original_file_name`
- `file_type`
- `report_start_date`
- `report_end_date`
- `settlement_basis`
- `aggregation_unit`
- `status`
- `error_message`
- `uploaded_at`
- `processed_at`

상태값 예시:

- `PENDING`
- `PARSING`
- `SUCCESS`
- `FAILED`
- `SUPERSEDED`

중복 업로드 처리 시 `SUPERSEDED`를 사용할 수 있다.

---

## 11. ClickHouse 테이블

### 11.1 `raw_order_items`

주문·상품 단위 원천 데이터다.

필드:

- `store_id`
- `upload_id`
- `order_date`
- `order_time`
- `order_no`
- `order_channel`
- `payment_status`
- `product_name`
- `product_code`
- `product_category`
- `option_name`
- `quantity`
- `product_price`
- `option_price`
- `product_discount_amount`
- `order_discount_amount`
- `net_sales`
- `vat_amount`
- `created_at`

역할:

- 메뉴 분석의 원천 데이터
- 시간대 분석의 원천 데이터
- 요일 패턴 분석의 원천 데이터
- 일별 매출 집계의 원천 데이터

### 11.2 `fact_daily_sales`

일별 매출 집계 테이블이다.

필드:

- `store_id`
- `upload_id`
- `sale_date`
- `total_revenue`
- `total_quantity`
- `total_orders`
- `avg_order_value`
- `created_at`

### 11.3 `menu_sales_daily`

메뉴별 일별 매출 집계 테이블이다.

필드:

- `store_id`
- `upload_id`
- `sale_date`
- `product_name`
- `product_category`
- `total_quantity`
- `total_sales`
- `sales_share`
- `created_at`

### 11.4 `hourly_sales`

시간대별 매출 집계 테이블이다.

필드:

- `store_id`
- `upload_id`
- `sale_date`
- `sales_hour`
- `total_sales`
- `total_quantity`
- `order_count`
- `created_at`

### 11.5 `weekday_sales`

요일별 매출 패턴 테이블이다.

필드:

- `store_id`
- `upload_id`
- `weekday`
- `weekday_name`
- `total_sales`
- `total_quantity`
- `order_count`
- `avg_sales`
- `created_at`

---

## 12. 중복 업로드 처리

중복 업로드는 반드시 고려해야 한다.

추천 MVP 방식:

1. `sales_uploads`에서 동일 `store_id + report_start_date + report_end_date` 조합이 있는지 확인한다.
2. 동일 기간 업로드가 이미 있으면 사용자에게 덮어쓰기 여부를 묻거나 기존 업로드를 `SUPERSEDED` 처리한다.
3. 새 업로드는 새로운 `upload_id`를 가진다.
4. ClickHouse에는 새 `upload_id` 기준으로 데이터를 insert한다.
5. 조회 시 기본적으로 최신 `upload_id` 또는 `SUCCESS` 상태의 최신 업로드만 사용한다.

이 방식은 ClickHouse의 복잡한 삭제/갱신에 의존하지 않고 애플리케이션 레벨에서 중복을 제어한다.

---

## 13. 집계 로직

### 13.1 일별 매출 집계

기준:

- `order_date`
- `store_id`
- `upload_id`

계산:

- `total_revenue = sum(net_sales)`
- `total_quantity = sum(quantity)`
- `total_orders = countDistinct(order_no)`
- `avg_order_value = total_revenue / total_orders`

### 13.2 메뉴별 매출 집계

기준:

- `order_date`
- `product_name`
- `product_category`
- `store_id`
- `upload_id`

계산:

- `total_sales = sum(net_sales)`
- `total_quantity = sum(quantity)`
- `sales_share = 메뉴 매출 / 전체 매출`

### 13.3 시간대별 매출 집계

기준:

- `order_date`
- `sales_hour`
- `store_id`
- `upload_id`

계산:

- `sales_hour = hour(order_time)`
- `total_sales = sum(net_sales)`
- `total_quantity = sum(quantity)`
- `order_count = countDistinct(order_no)`

### 13.4 요일별 매출 집계

기준:

- `weekday`
- `store_id`
- `upload_id`

계산:

- `weekday = toDayOfWeek(order_date)`
- `total_sales = sum(net_sales)`
- `total_quantity = sum(quantity)`
- `order_count = countDistinct(order_no)`
- `avg_sales = avg(daily_total_sales)`

---

## 14. 내 가게 분석 API

POS 데이터 팀은 다음 API를 제공할 수 있어야 한다.

### 업로드 API

`POST /api/pos/uploads`

역할:

- XLSX 파일 업로드
- 파일 검증
- 파싱 시작
- 업로드 상태 반환

### 업로드 상태 조회 API

`GET /api/pos/uploads/{uploadId}`

역할:

- 업로드 상태 조회
- 오류 메시지 조회
- 분석 가능 여부 확인

### 매출 추이 API

`GET /api/stores/{storeId}/sales/daily?uploadId={uploadId}`

역할:

- 일별 매출 추이 반환
- 일별 주문 수 반환
- 일별 객단가 반환

### 메뉴 분석 API

`GET /api/stores/{storeId}/analysis/menu?uploadId={uploadId}`

역할:

- 메뉴별 매출 순위
- 메뉴별 판매량
- 메뉴별 매출 비중 반환

### 시간대 분석 API

`GET /api/stores/{storeId}/analysis/hourly?uploadId={uploadId}`

역할:

- 시간대별 매출
- 시간대별 주문 수
- 피크 시간대 반환

### 요일 분석 API

`GET /api/stores/{storeId}/analysis/weekday?uploadId={uploadId}`

역할:

- 요일별 평균 매출
- 요일별 주문 수
- 강한 요일/약한 요일 반환

---

## 15. 내 가게 분석 화면에 제공할 데이터

POS 데이터 팀은 내 가게 분석·진단 화면에 다음 데이터를 제공한다.

### 요약 탭

- 최근 기간 총매출
- 이전 기간 대비 매출 변화율
- 결제건수 변화율
- 객단가 변화율
- 주요 감소 원인 후보

### 매출 추이 탭

- 일별 총매출
- 일별 주문 수
- 일별 객단가

### 메뉴 분석 탭

- 매출 상위 메뉴
- 판매량 상위 메뉴
- 매출 감소 메뉴
- 카테고리별 매출 비중
- 특정 메뉴 의존도

### 시간대 분석 탭

- 시간대별 매출
- 시간대별 주문 수
- 점심 피크
- 저녁 피크
- 저활성 시간대

### 요일 패턴 탭

- 요일별 평균 매출
- 요일별 주문 수
- 평일/주말 비교
- 강한 요일
- 약한 요일

---

## 16. 진단에 제공할 POS 지표

최종 진단 요약에서 POS 데이터 팀이 제공해야 하는 지표는 다음과 같다.

- `store_growth_rate`
- `payment_count_growth_rate`
- `avg_ticket_growth_rate`
- `top_menu`
- `top_menu_sales_share`
- `declining_menu`
- `declining_menu_growth_rate`
- `peak_hour`
- `weak_hour_range`
- `weak_hour_growth_rate`
- `strong_weekday`
- `weak_weekday`
- `weekday_variance`
- `order_count_trend`
- `menu_issue_summary`
- `hourly_issue_summary`
- `weekday_issue_summary`

이 지표들은 공공데이터 팀의 다음 지표와 결합된다.

- `industry_growth_rate`
- `market_growth_rate`
- `store_count`
- `open_count`
- `close_count`
- `saturation_score`

---

## 17. 진단 규칙 중 POS 데이터 관련 규칙

POS 데이터 팀의 지표는 다음 진단 유형에 사용된다.

### 주문 수 감소

조건 예시:

- 매출 하락
- 결제건수 또는 주문 수 하락
- 객단가 유지

진단:

- 방문 수 또는 주문 수 감소 가능성

### 객단가 하락

조건 예시:

- 매출 하락
- 주문 수 유지
- 객단가 하락

진단:

- 객단가 하락 가능성

### 주요 메뉴 부진

조건 예시:

- 매출 하락
- 주력 메뉴 판매량 또는 매출 감소

진단:

- 주요 메뉴 부진 가능성

### 특정 시간대 부진

조건 예시:

- 전체 매출 하락
- 특정 시간대 매출 급감

진단:

- 특정 시간대 수요 감소 가능성

### 요일 패턴 문제

조건 예시:

- 특정 요일 매출이 반복적으로 낮음
- 평일/주말 격차가 큼

진단:

- 요일 패턴 문제 가능성

---

## 18. POS 데이터 팀 구현 우선순위

우선순위는 다음과 같다.

1. `sales_uploads` 테이블 설계
2. XLSX 업로드 API 구현
3. Apache POI 기반 시트 목록 검증
4. `데이터 기준` 시트 파싱
5. `결제 합계` 시트 파싱
6. `상품 주문 상세내역` 시트 파싱
7. `raw_order_items` ClickHouse batch insert
8. `fact_daily_sales` 집계 SQL 구현
9. `menu_sales_daily` 집계 SQL 구현
10. `hourly_sales` 집계 SQL 구현
11. `weekday_sales` 집계 SQL 구현
12. 내 가게 분석 API 구현
13. 내 가게 분석·진단 화면 연결
14. 공공데이터 팀의 `market_statistics`와 결합

---

## 19. POS 데이터 팀이 피해야 할 것

다음은 MVP에서 피한다.

- CSV 업로드 기준으로 설계하기
- 일별 매출만 분석하고 메뉴·시간대·요일 분석을 빼기
- ClickHouse를 PostgreSQL처럼 row update/delete 중심으로 사용하기
- 파일 한 행마다 개별 insert 하기
- Kafka부터 붙이기
- Airflow부터 붙이기
- LLM이 원인을 직접 판단하게 하기
- 공공데이터 팀의 역할까지 과도하게 맡기기

---

## 20. AI에게 요청할 때 지켜야 할 기준

POS 데이터 팀 관련 질문을 AI에게 할 때는 다음 기준을 유지한다.

1. 프로젝트 전반 컨텍스트와 이 POS 팀 컨텍스트를 함께 따른다.
2. 업로드 파일은 POS 매출리포트 XLSX다.
3. CSV 기준 답변을 하지 않는다.
4. Apache POI 기반 파싱을 전제로 한다.
5. ClickHouse를 분석 저장소로 사용한다.
6. `raw_order_items`가 핵심 원천 데이터다.
7. 메뉴·시간대·요일 분석을 핵심 기능으로 유지한다.
8. Kafka와 Airflow는 후순위다.
9. 진단은 규칙 기반이다.
10. 최종 통합 지점은 내 가게 분석·진단 화면이다.

---

## 21. POS 데이터 팀 최종 요약

POS 데이터 팀은 사용자가 업로드한 POS 매출리포트 XLSX 파일을 파싱하여 내 가게 운영 분석 데이터를 만드는 팀이다.  
핵심 시트는 `데이터 기준`, `결제 합계`, `상품 주문 상세내역`이며, 특히 `상품 주문 상세내역`은 메뉴 분석, 시간대 분석, 요일 패턴 분석의 원천 데이터다.

POS 데이터 팀은 `raw_order_items`를 ClickHouse에 저장하고, 이를 기반으로 `fact_daily_sales`, `menu_sales_daily`, `hourly_sales`, `weekday_sales`를 생성한다. 이 집계 결과는 내 가게 분석 화면과 최종 진단 요약에 사용되며, 공공데이터 팀의 `market_statistics`와 결합되어 사용자가 매출 하락 원인을 더 합리적으로 판단할 수 있도록 돕는다.
