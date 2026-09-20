# TrendBench

예비 창업자가 서울 상권의 공개 지표를 참고하고, 자신의 매출·비용 가정으로 창업 필요자금과 상환 부담을 검토하는 서비스.

현재 상태: **F1 재무 계산, F2 인증·계획 저장, F3 상권 데이터 연결 완료**. 세 단계 모두 실제 PostgreSQL과 브라우저에서 검증했다. F3는 공공 원본 파일을 릴리스 단위로 적재하고 활성 릴리스만 공개 조회하며, 상권 원값을 개인 예상매출로 자동 대입하지 않는다. 자금 후보(F4)와 공개 출시 준비(F5)는 완료되지 않았다. 이전 POS 서비스는 `legacy/`에 보존하며 신규 개발 기준으로 사용하지 않는다.

## 먼저 읽을 문서

1. [에이전트 작업 지침](AGENTS.md)
2. [현재 제품 범위](docs/PRODUCT.md)
3. [기술 설계](docs/12_Simplified_MVP_Technical_Plan.md)
4. [실제 데이터 검증과 제약](docs/13_Data_API_Feasibility_Verification.md)
5. [구현 작업 목록](docs/TASKS.md)
6. [로컬 실행](docs/DEVELOPMENT.md)

문서가 충돌하면 사용자의 최신 명시적 지시 → AGENTS.md → PRODUCT.md → 기술 설계 순으로 적용한다. 데이터의 실제 확인 여부는 검증 보고서를 따른다. `legacy/` 문서는 역사 자료다.

## 구조

```text
app/                 Next.js 단일 애플리케이션
infra/               새 MVP용 PostgreSQL 개발 구성
docs/                현재 제품·기술·작업·검증 문서
docs/verification/   공공파일 검증 도구와 결과
legacy/              이전 앱·인프라·문서 보관
```

## 빠른 시작

Node.js 24 LTS와 npm을 사용한다. 명령은 저장소 루트에서 실행한다.

```sh
npm --prefix app ci
npm --prefix app run dev
```

환경변수와 PostgreSQL migration을 준비한 뒤 브라우저에서 http://localhost:3000 을 연다. 이메일·비밀번호 가입/로그인, 계획 초안 저장, 서버 계산, 불변 결과 재조회가 제공된다.

http://localhost:3000/markets — 로그인 없이 쓰는 공개 상권 탐색. 자치구 → 상권 → 업종 순으로 선택하면 가용 분기와 분기별 원본 지표를 표로 확인하고, 단위 미확정과 자료 부족 상태를 함께 본다.

```sh
npm --prefix app run lint
npm --prefix app run typecheck
npm --prefix app run test
npm --prefix app run build
```

루트 `node_modules`는 앱 의존성 경로가 아니다. 의존성은 `app/package-lock.json`으로 관리한다.

## 상권 데이터 적재

운영자가 공식 파일을 `data/raw/`에 준비하고 적재 명령을 직접 실행한다. 웹 요청 중에는 외부 데이터를 수집하지 않는다.

```sh
npm --prefix app run market:load
```

- 원본 5개 파일(`sales-2024.zip`, `sales-2025.zip`, `stores-2024.zip`, `stores-2025.zip`, `areas.zip`)과 입수 방법은 [검증 도구 안내](docs/verification/README.md)를 따른다. `data/raw/`는 Git에서 제외한다.
- 적재기는 인코딩(CP949 CSV·UTF-8 DBF), 예상 헤더, 코드·분기 형식, 키 누락·중복, 음수 금액, 파일 checksum을 검사한다. 기록된 원본과 다른 파일은 운영 적재하지 않으며 새 원본은 manifest와 정의 버전을 갱신해 검증한다.
- 검증이 끝난 릴리스만 트랜잭션으로 활성화한다. 실패하면 새 릴리스 데이터를 정리하고 `FAILED`로 남기며 기존 활성 릴리스는 그대로 유지한다.
- 같은 원본을 다시 적재하면 중복 행을 만들지 않고 `ALREADY_ACTIVE`로 끝난다.
- 처음 노출하는 업종은 한식(CS100001)과 커피·음료(CS100010)다. 나머지 업종은 원천 분류에만 보존하고 조회를 거부한다.

## 상권 데이터 제약

- 매출 원값의 월·분기 단위와 점포당 매출 모집단은 확정되지 않았다. 화면과 API는 단위 미확정을 표시하고 월 환산과 점포당 매출을 계산하지 않는다.
- 원천 열 `점포_수`(stor_co)는 프랜차이즈를 제외한 일반 점포 수이며 전체 점포 수가 아니다. 전체로 표시하지 않는다.
- 매출이 제공되지 않은 조합은 0이 아니라 자료 부족으로 응답한다.
- 상권 코드 3110024는 영역 파일 `혜회동주민센터`, 매출·점포 파일 `혜화동주민센터`로 원본 명칭이 다르다. 표시명은 영역 파일을 우선하고 관측 명칭을 함께 보존한다.

## 첫 개발 목표

사용자 입력 → 인증된 초안 저장 → 서버 재계산 → 불변 결과 저장 흐름을 구현하고 실제 PostgreSQL과 브라우저에서 두 사용자 권한·재접속·DB 재기동 보존을 확인했다. 이어 공공 상권 지표를 릴리스 단위로 적재해 자치구 → 상권 → 업종 탐색과 자료 부족 상태를 확인했다. 검수된 지원사업 연결은 이후 단계다.

개편 내용과 확인한 실행 결과는 [저장소 개편 기록](docs/REPOSITORY_REORGANIZATION.md)에 남긴다.
