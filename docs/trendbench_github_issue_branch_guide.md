# TrendBench GitHub Issue & Branch 운영 가이드

이 문서는 TrendBench 프로젝트에서 GitHub Issue, Branch, Pull Request를 일관되게 운영하기 위한 기준 문서다.

TrendBench는 공공데이터 팀과 POS 데이터 팀이 병렬로 개발하고, 최종적으로 내 가게 분석·진단 화면에서 통합되는 구조다.  
따라서 GitHub 작업 단위도 팀, 기능, 통합 기준으로 명확히 분리한다.

---

## 1. 기본 운영 원칙

```text
GitHub Issue = 작업 단위
Branch = Issue 구현 공간
Pull Request = 리뷰 및 병합 단위
main = 발표 가능한 안정 버전
dev = 통합 개발 브랜치
```

운영 규칙은 다음과 같다.

1. `main` 브랜치에 직접 push하지 않는다.
2. 모든 기능 개발은 `dev`에서 새 브랜치를 만들어 진행한다.
3. Issue 하나당 Branch 하나를 원칙으로 한다.
4. Pull Request 하나는 가능하면 Issue 하나만 해결한다.
5. 공공데이터 팀과 POS 데이터 팀은 독립적으로 개발하되, 통합 지점은 Interface Contract 기준을 따른다.
6. 실제 POS 매출리포트 XLSX 파일, `.env`, 개인정보, 매출 데이터는 GitHub에 올리지 않는다.

---

## 2. 브랜치 구조

```text
main
dev
feature/*
fix/*
docs/*
chore/*
```

| 브랜치 | 용도 |
|---|---|
| `main` | 발표/제출 가능한 안정 버전 |
| `dev` | 통합 개발 브랜치 |
| `feature/*` | 기능 개발 |
| `fix/*` | 버그 수정 |
| `docs/*` | 문서 수정 |
| `chore/*` | 설정, 의존성, 폴더 구조, Docker 등 |

권장 흐름:

```text
feature/* → dev → main
```

---

## 3. 브랜치 네이밍 규칙

### 3.1 기본 형식

```text
<type>/<issue-number>-<area>-<short-description>
```

### 3.2 Type 목록

| Type | 의미 |
|---|---|
| `feature` | 기능 개발 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `chore` | 설정, 환경, 의존성, 폴더 구조 |
| `refactor` | 기능 변화 없는 구조 개선 |
| `test` | 테스트 추가 또는 수정 |

### 3.3 예시

```text
chore/1-project-structure
chore/2-docker-compose
feature/3-health-api
feature/4-market-statistics-table
feature/5-market-summary-api
feature/6-pos-upload-api
feature/7-pos-sheet-validation
feature/8-pos-raw-order-items-insert
feature/9-store-diagnosis-api
docs/10-update-interface-contract
fix/11-clickhouse-date-parsing
```

주의:

```text
좋음: feature/6-pos-upload-api
나쁨: feature/#6-pos-upload-api
```

브랜치명에는 `#` 문자를 넣지 않는 것이 안전하다.

---

## 4. Issue 라벨 체계

### 4.1 Team 라벨

```text
team:public-data
team:pos
team:frontend
team:backend
team:integration
team:infra
```

### 4.2 Type 라벨

```text
type:feature
type:bug
type:docs
type:chore
type:refactor
type:test
```

### 4.3 Priority 라벨

```text
priority:p0
priority:p1
priority:p2
```

| Priority | 의미 |
|---|---|
| `priority:p0` | MVP 필수 |
| `priority:p1` | 중요하지만 p0 이후 |
| `priority:p2` | 있으면 좋은 개선 |

### 4.4 Status 라벨

```text
status:todo
status:in-progress
status:review
status:done
```

### 4.5 라벨 사용 예시

POS 업로드 API:

```text
team:pos
team:backend
type:feature
priority:p0
status:todo
```

공공데이터 통계 화면:

```text
team:public-data
team:frontend
type:feature
priority:p1
status:todo
```

---

## 5. Milestone 구조

권장 Milestone은 다음과 같다.

```text
M1. 개발 환경 구축
M2. 공공데이터 MVP
M3. POS 업로드/파싱 MVP
M4. 분석 API MVP
M5. 통합 진단 화면
M6. 발표 전 안정화
```

| Milestone | 목적 |
|---|---|
| `M1. 개발 환경 구축` | 저장소, Docker Compose, Spring Boot, React 기본 구성 |
| `M2. 공공데이터 MVP` | `market_statistics`, 샘플 데이터, 통계 API, 통계 화면 |
| `M3. POS 업로드/파싱 MVP` | XLSX 업로드, 필수 시트 검증, Apache POI 파싱 |
| `M4. 분석 API MVP` | daily/menu/hourly/weekday 집계 및 분석 API |
| `M5. 통합 진단 화면` | POS 지표와 공공데이터 지표 결합, 진단 화면 구현 |
| `M6. 발표 전 안정화` | 버그 수정, 문서 정리, 데모 시나리오 점검 |

---

## 6. Issue 템플릿

`.github/ISSUE_TEMPLATE/feature.md` 등에 아래 형식을 사용할 수 있다.

```md
## 작업 목적
<!-- 왜 이 작업이 필요한지 작성 -->

## 작업 범위
- [ ] 작업 1
- [ ] 작업 2
- [ ] 작업 3

## 구현 위치
- Backend:
- Frontend:
- Infra:
- Docs:

## 완료 조건
- [ ] 로컬에서 실행 확인
- [ ] API 응답 확인
- [ ] 화면 동작 확인
- [ ] 관련 문서 반영

## 참고 사항
<!-- Interface Contract, API 필드명, DB 테이블명 등 -->
```

---

## 7. Pull Request 템플릿

`.github/pull_request_template.md`에 아래 형식을 사용할 수 있다.

```md
## 작업 내용
- 

## 관련 Issue
closes #

## 확인 방법
- [ ] 로컬 실행 확인
- [ ] API 테스트 완료
- [ ] 화면 확인
- [ ] 빌드 성공

## 변경된 파일
- 

## 주의사항
- 
```

---

## 8. 초기 Issue 목록

## M1. 개발 환경 구축

| 번호 | Issue 제목 | Branch |
|---:|---|---|
| #1 | `[CHORE] 프로젝트 기본 폴더 구조 생성` | `chore/1-project-structure` |
| #2 | `[INFRA] Docker Compose로 PostgreSQL, ClickHouse 실행 환경 구성` | `chore/2-docker-compose` |
| #3 | `[BACKEND] Spring Boot 프로젝트 생성 및 health API 구현` | `feature/3-health-api` |
| #4 | `[FRONTEND] React Vite 프로젝트 생성 및 기본 라우팅 구성` | `feature/4-react-init` |
| #5 | `[DOCS] README와 .env.example 작성` | `docs/5-readme-env-example` |

---

## M2. 공공데이터 MVP

| 번호 | Issue 제목 | Branch |
|---:|---|---|
| #6 | `[PUBLIC-DATA] market_statistics ClickHouse 테이블 생성` | `feature/6-market-statistics-table` |
| #7 | `[PUBLIC-DATA] 강남구·한식 샘플 데이터 적재` | `feature/7-market-sample-data` |
| #8 | `[PUBLIC-DATA] 업종·상권 통계 요약 API 구현` | `feature/8-market-summary-api` |
| #9 | `[PUBLIC-DATA] 월별 통계 상세 API 구현` | `feature/9-market-monthly-api` |
| #10 | `[FRONTEND] 업종·상권 통계 화면 구현` | `feature/10-market-overview-page` |
| #11 | `[FRONTEND] 통계 상세 조회 화면 구현` | `feature/11-market-detail-page` |

---

## M3. POS 업로드/파싱 MVP

| 번호 | Issue 제목 | Branch |
|---:|---|---|
| #12 | `[POS] sales_uploads PostgreSQL 테이블 확인 및 Entity 구성` | `feature/12-sales-uploads-table` |
| #13 | `[POS] POS 매출리포트 XLSX 업로드 API 구현` | `feature/13-pos-upload-api` |
| #14 | `[POS] Apache POI 기반 필수 시트 검증 구현` | `feature/14-pos-sheet-validation` |
| #15 | `[POS] 데이터 기준 시트 파싱 구현` | `feature/15-parse-data-standard-sheet` |
| #16 | `[POS] 결제 합계 시트 파싱 구현` | `feature/16-parse-payment-summary-sheet` |
| #17 | `[POS] 상품 주문 상세내역 시트 파싱 구현` | `feature/17-parse-order-items-sheet` |
| #18 | `[POS] raw_order_items ClickHouse batch insert 구현` | `feature/18-raw-order-items-insert` |

---

## M4. 분석 API MVP

| 번호 | Issue 제목 | Branch |
|---:|---|---|
| #19 | `[POS] fact_daily_sales 집계 SQL 구현` | `feature/19-daily-sales-aggregation` |
| #20 | `[POS] menu_sales_daily 집계 SQL 구현` | `feature/20-menu-sales-aggregation` |
| #21 | `[POS] hourly_sales 집계 SQL 구현` | `feature/21-hourly-sales-aggregation` |
| #22 | `[POS] weekday_sales 집계 SQL 구현` | `feature/22-weekday-sales-aggregation` |
| #23 | `[POS] 일별 매출 API 구현` | `feature/23-daily-sales-api` |
| #24 | `[POS] 메뉴 분석 API 구현` | `feature/24-menu-analysis-api` |
| #25 | `[POS] 시간대 분석 API 구현` | `feature/25-hourly-analysis-api` |
| #26 | `[POS] 요일 분석 API 구현` | `feature/26-weekday-analysis-api` |

---

## M5. 통합 진단 화면

| 번호 | Issue 제목 | Branch |
|---:|---|---|
| #27 | `[INTEGRATION] POS 지표와 공공데이터 지표 결합 필드 정리` | `feature/27-diagnosis-contract-check` |
| #28 | `[INTEGRATION] 진단 규칙 기반 diagnosis API 구현` | `feature/28-diagnosis-api` |
| #29 | `[FRONTEND] POS 매출리포트 XLSX 업로드 화면 구현` | `feature/29-upload-page` |
| #30 | `[FRONTEND] 내 가게 분석·진단 화면 구현` | `feature/30-store-diagnosis-page` |
| #31 | `[INTEGRATION] 업종·상권 비교 탭 연결` | `feature/31-market-comparison-tab` |
| #32 | `[INTEGRATION] 최종 시나리오 데이터 연결 및 점검` | `feature/32-demo-scenario` |

---

## 9. 커밋 메시지 규칙

### 9.1 기본 형식

```text
<type>: <내용>
```

### 9.2 예시

```text
chore: initialize project structure
feat: add market statistics summary API
feat: add POS XLSX upload endpoint
fix: handle missing required sheet error
docs: update interface contract
refactor: separate ClickHouse repository layer
test: add upload validation test
```

### 9.3 Type 목록

```text
feat
fix
docs
chore
refactor
test
```

---

## 10. 작업 흐름

### 10.1 Issue 생성

예:

```text
[POS] Apache POI 기반 필수 시트 검증 구현
```

### 10.2 브랜치 생성

```bash
git checkout dev
git pull origin dev
git checkout -b feature/14-pos-sheet-validation
```

### 10.3 작업 후 커밋

```bash
git add .
git commit -m "feat: add required sheet validation for POS XLSX"
```

### 10.4 원격 브랜치 push

```bash
git push origin feature/14-pos-sheet-validation
```

### 10.5 Pull Request 생성

```text
base: dev
compare: feature/14-pos-sheet-validation
```

### 10.6 리뷰 후 merge

PR이 merge되면 로컬에서 브랜치를 정리한다.

```bash
git checkout dev
git pull origin dev
git branch -d feature/14-pos-sheet-validation
```

원격 브랜치가 남아 있다면 GitHub에서 삭제하거나 다음 명령어를 사용한다.

```bash
git push origin --delete feature/14-pos-sheet-validation
```

---

## 11. GitHub에 올리면 안 되는 것

TrendBench는 POS 매출리포트 XLSX를 다루므로 실제 매출 데이터와 개인정보가 포함될 수 있다.  
다음 항목은 GitHub에 올리지 않는다.

```text
.env
.env.local
실제 POS 매출리포트 XLSX
개인정보가 포함된 테스트 데이터
실제 매출 데이터
DB dump 파일
IDE 개인 설정
```

권장 `.gitignore`:

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

---

## 12. 팀별 작업 기준

## 12.1 공공데이터 팀

공공데이터 팀은 다음 작업을 우선한다.

```text
1. ClickHouse market_statistics 테이블 생성
2. 강남구·한식 샘플 데이터 적재
3. 업종·상권 통계 요약 API 구현
4. 월별 통계 상세 API 구현
5. 업종·상권 통계 화면 연결
6. 통계 상세 조회 화면 연결
```

초기 MVP에서는 전국 데이터 자동 수집, Airflow, 모든 업종 코드 매핑을 먼저 구현하지 않는다.

---

## 12.2 POS 데이터 팀

POS 데이터 팀은 다음 작업을 우선한다.

```text
1. sales_uploads 테이블 확인
2. POS 매출리포트 XLSX 업로드 API 구현
3. Apache POI 기반 필수 시트 검증
4. 데이터 기준 시트 파싱
5. 결제 합계 시트 파싱
6. 상품 주문 상세내역 시트 파싱
7. raw_order_items ClickHouse batch insert
8. fact_daily_sales 집계
9. menu_sales_daily 집계
10. hourly_sales 집계
11. weekday_sales 집계
12. 내 가게 분석 API 구현
```

CSV 업로드 기준으로 구현하지 않는다.  
반드시 POS 매출리포트 XLSX 기준으로 구현한다.

---

## 12.3 통합 작업

통합 작업에서는 다음 기준을 확인한다.

```text
1. store_id, upload_id 연결 확인
2. region_name, industry_name 연결 확인
3. API 응답 필드 camelCase 확인
4. DB 컬럼 snake_case 확인
5. 진단 API에서 POS 지표와 공공데이터 지표 결합
6. 내 가게 분석·진단 화면 연결
```

---

## 13. Pull Request 리뷰 체크리스트

PR 리뷰 시 다음을 확인한다.

```text
[ ] Issue 범위 밖 작업이 섞이지 않았는가?
[ ] main이 아니라 dev를 대상으로 PR을 열었는가?
[ ] API 응답 필드명이 Interface Contract와 맞는가?
[ ] DB 컬럼명은 snake_case를 따르는가?
[ ] API JSON은 camelCase를 따르는가?
[ ] POS 업로드가 CSV 기준으로 잘못 구현되지 않았는가?
[ ] ClickHouse를 row update/delete 중심으로 사용하지 않았는가?
[ ] 실제 POS XLSX 파일이나 .env가 커밋되지 않았는가?
[ ] 로컬 실행 또는 테스트 방법이 PR에 적혀 있는가?
```

---

## 14. 최종 요약

TrendBench GitHub 운영은 다음 규칙으로 충분하다.

```text
main 직접 push 금지
dev 기준 개발
Issue 하나당 Branch 하나
PR 하나당 Issue 하나
공공데이터 팀과 POS 팀은 독립 개발
통합 작업은 Interface Contract 기준 확인
실제 POS XLSX 파일 GitHub 업로드 금지
```

복잡한 Git Flow보다 `main + dev + feature/*` 구조가 졸업프로젝트 규모에 더 적합하다.
