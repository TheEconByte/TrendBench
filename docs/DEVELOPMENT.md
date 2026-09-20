# 새 MVP 개발 환경

## 준비

- Node.js 24 LTS, npm. 정확한 앱 의존성은 `app/package-lock.json`으로 고정한다.
- DB 작업 시 Docker Compose v2 또는 로컬 PostgreSQL 17.
- Python은 공공파일 검증에만 필요하며 웹앱 실행 조건이 아니다.

모든 명령의 기본 위치는 저장소 루트다. 기존 `legacy/` 실행 파일은 사용하지 않는다.

## 앱

```sh
npm --prefix app ci
npm --prefix app run dev
```

http://localhost:3000 — 가입·로그인과 인증된 재무계획 초안/불변 결과 화면. 외부 API는 사용하지 않는다.
http://localhost:3000/markets — 로그인 없이 쓰는 공개 상권 탐색 화면. 자치구 → 상권 → 업종 선택과 분기별 표·차트, 출처·기준기간·단위 제한 표시를 제공한다.
http://localhost:3000/api/health — 프로세스 생존 상태. DB·인증·외부 API 준비 상태를 보장하는 응답이 아니다.

```sh
npm --prefix app run lint
npm --prefix app run typecheck
npm --prefix app run test
npm --prefix app run build
npm --prefix app run start
```

`start`는 먼저 빌드한 뒤 실행한다. 기본 포트가 사용 중이면 `npm --prefix app run dev -- --port 3001`을 사용한다. 개발 서버와 프로덕션 빌드를 동시에 같은 앱 폴더에서 실행하지 않는다.

## PostgreSQL

PowerShell:

```powershell
Copy-Item infra/.env.example infra/.env
Copy-Item app/.env.example app/.env.local
```

이미 파일이 있다면 덮어쓰지 말고 필요한 항목만 수정한다. `infra/.env`의 로컬 비밀번호를 설정하고 `app/.env.local`의 DATABASE_URL에도 같은 값을 사용한다. URL의 비밀번호 특수문자는 URL 인코딩한다.

```sh
docker compose --env-file infra/.env -f infra/compose.yaml config --quiet
docker compose --env-file infra/.env -f infra/compose.yaml up -d --wait
docker compose --env-file infra/.env -f infra/compose.yaml ps
```

- 새 프로젝트명: `trendbench-mvp`, DB명: `trendbench_mvp`.
- 로컬 접속: `127.0.0.1:5433`, 사용자 `trendbench`.
- 기존 POS용 5432 포트와 데이터 볼륨을 재사용하지 않는다.
- 컨테이너 정지: `docker compose --env-file infra/.env -f infra/compose.yaml stop`.
- `down -v`는 데이터를 삭제하므로 일상 종료 명령으로 쓰지 않는다.

DB 기동 후 최초 1회 migration과 client 생성을 실행한다. 기존 DB를 초기화하는 `prisma migrate reset`은 사용하지 않는다.

```sh
npm --prefix app run db:validate
npm --prefix app run db:generate
npm --prefix app run db:migrate -- --name f2_auth_and_plans
npm --prefix app run db:status
```

`prisma.config.ts`가 `app/.env.local`을 먼저 읽고 없으면 `app/.env`를 읽는다. Prisma CLI가 Next.js식 env 파일을 스스로 읽지 않기 때문이며, 그래서 `npm run db:*`와 `npm run market:load`는 앱과 같은 `DATABASE_URL`을 쓴다.

Better Auth 1.7.5는 이메일·비밀번호와 DB 세션을 담당한다. `BETTER_AUTH_SECRET`은 32자 이상의 고엔트로피 값으로 설정하고 저장소에 커밋하지 않는다. 실제 이메일 발송, 비밀번호 복구, 소셜 로그인은 제공하지 않는다.

## 코드 배치

- `app/src/app`: 페이지·레이아웃·얇은 Route Handler.
- `app/src/features/finance`: Zod 입력, decimal 계산, 대출 일정, 시나리오. 계산 순수 함수는 React·DB·외부 API에 의존하지 않는다.
- `app/src/features/plans`: 인증 후 계획 목록·입력·결과 이력 UI와 API 경계 테스트.
- `app/src/features/market`: 상권 원본 ZIP·CSV·DBF 파싱, 릴리스 적재·활성화, 공개 조회. 순수 파싱·검증 함수는 React·DB에 의존하지 않는다.
- `app/src/lib`: Prisma 단일 client, Better Auth, 세션·오류 응답.
- `app/prisma`: Better Auth core tables, `plans`, `plan_results`, 순서가 있는 migration.
- `app/scripts`: 운영자가 직접 실행하는 적재 명령. 웹 요청 경로에서 호출하지 않는다.
- 가짜 데이터와 미구현 서비스의 빈 코드를 미리 생성하지 않는다.

## CI

`.github/workflows/check.yml`은 Node 24에서 `npm ci`, lint, typecheck, Vitest, build를 실행한다. Vitest는 F1 계산과 미로그인 차단, 소유자 조건, revision 충돌, 서버 계산, 중복 결과 재사용을 검증한다. 실제 PostgreSQL 종단 검증은 별도로 실행한다.

F3 테스트는 원본 파싱·헤더 매핑·checksum·거부 규칙을 항상 검증하고, `data/raw/`의 실제 파일이 있으면 기록된 행 수·결합·표본까지 대조한다. 합성 릴리스를 PostgreSQL에 적재하는 변경 테스트는 일반 `DATABASE_URL`을 절대 사용하지 않으며, `TEST_DATABASE_URL` 환경변수 또는 `app/.env.test.local`의 전용 테스트 DB가 있을 때만 실행한다. 테스트는 합성 릴리스만 만들고 끝나면 삭제하며, 실행 전에 ACTIVE였던 테스트 DB 릴리스 상태를 복원한다.

## F2 API 정책

- `/api/auth/*`: Better Auth handler.
- `/api/plans`: 내 계획 생성·목록.
- `/api/plans/{id}`: 소유자만 상세·revision 수정·삭제.
- `/api/plans/{id}/calculations`: 저장된 입력만 서버에서 계산하고 결과를 append-only로 저장.
- `/api/plans/{id}/results[/{resultId}]`: 소유자만 결과 목록·상세 조회.
- 미로그인은 401, 타인 리소스와 없는 ID는 404, 입력 오류는 400, 오래된 revision은 409다.

## F1 계산 정책

- 금액 입력·출력은 원 단위 정수 문자열이며 계산은 `decimal.js`를 사용한다.
- 원 단위 결과는 `ROUND_HALF_UP`으로 반올림한다. 대출 일정은 월별 이자·원금을 같은 규칙으로 반올림하고 마지막 달 원금으로 잔액을 0원에 맞춘다.
- 빈 선택값은 `null`로 유지하고 0과 구분한다. 대출금 0 또는 `null`은 무차입으로 처리한다.
- 거치는 이자만 지급하며 전체 상환기간에 포함한다. 만기일시상환·변동금리·보증료·일수별 이자는 지원하지 않는다.

## F3 적재 정책

- 실행: `npm --prefix app run market:load [-- --source-dir <경로>]`. 기본 원본 경로는 `<저장소 루트>/data/raw`다.
- 원본은 운영자가 준비한다. 웹 요청 처리 중에는 원본 수집이나 DB 적재를 하지 않는다.
- 인코딩(ZIP 안 CSV는 CP949, 영역 DBF 속성은 `.cpg`가 선언한 UTF-8), 예상 헤더 집합, 상권·업종·자치구 코드 형식, 분기 형식(`YYYYQ`), 키 누락·중복, 음수 금액, 파일 크기와 SHA-256을 검사한다.
- 2024년과 2025년 파일은 헤더가 다르므로 `app/src/features/market/headers.ts`의 명시적 매핑을 쓴다. 열 순서가 아니라 열 이름으로 위치를 찾는다.
- 적재 스키마·정의 버전과 원본 checksum으로 릴리스 키를 만든다. 릴리스 키가 같고 이미 ACTIVE면 쓰기 없이 `ALREADY_ACTIVE`로 끝난다.
- 새 릴리스는 `PENDING`으로 적재하고 적재 후 검증까지 통과한 뒤 한 트랜잭션으로 활성화한다. 활성화는 상태 전환만 하며 이전 릴리스의 행은 지우거나 고치지 않는다.
- 실패하면 새 릴리스의 상권·분기 행을 삭제하고 릴리스를 `FAILED`와 실패 사유로 남긴다. 기존 활성 릴리스는 그대로 서비스된다.
- 처음 노출하는 업종은 한식(CS100001), 커피·음료(CS100010)다. 다른 업종은 `industries`에 원천 분류로만 남기고 조회를 거부한다.
- 상권 표시명 우선순위: 영역 파일 명칭 → 매출·점포 파일 관측 명칭. 업종 표시명 우선순위: 제품 문서 표시명 → 원천 명칭. 두 명칭은 응답에 함께 담는다.
- 운영 적재는 manifest에 기록된 파일 크기와 SHA-256이 일치하는 원본만 허용한다. 새 원본은 입수일·기준기간·checksum·예상 행 수를 검증해 새 manifest와 정의 버전으로 추가하며, 명령행 옵션으로 검증을 우회하지 않는다.
- 업종 메타데이터는 릴리스별로 저장한다. PENDING·FAILED 릴리스의 업종 원본 명칭이 현재 ACTIVE 릴리스 응답을 바꾸지 않는다.
- 적재 명령은 Node 24의 TypeScript 실행을 그대로 쓴다. 생성된 Prisma client가 확장자 없는 상대 import를 만들지 않도록 `app/prisma/schema.prisma`의 generator에 `moduleFormat = "esm"`, `importFileExtension = "ts"`를 둔다.

## F3 공개 API 정책

- `/api/industries`: 로그인 없이 지원 업종과 현재 활성 릴리스를 반환한다. 활성 릴리스가 없어도 200이며 `activeRelease`가 null이다.
- `/api/markets/areas?districtCode=...`: 자치구 목록은 항상 반환하고, `districtCode`가 있으면 그 자치구의 상권을 반환한다. 없는 자치구 코드는 400 `INVALID_DISTRICT_CODE`다.
- `/api/markets/summary?areaCode=...&industryCode=...[&areaType=...]`: 가용 분기를 오래된 순으로, 분기별 원본 지표를 반환한다. 매출 원값은 원 단위 정수 문자열이고 점포 수는 숫자다.
- 상태 구분: 활성 릴리스 없음 503 `RELEASE_UNAVAILABLE`, 형식 오류 400 `INVALID_AREA_CODE`·`INVALID_INDUSTRY_CODE`, 없는 상권 404 `AREA_NOT_FOUND`, 같은 코드가 여러 상권 구분에 있으면 400 `AMBIGUOUS_AREA_CODE`, 원천에 있으나 미지원 업종은 400 `UNSUPPORTED_INDUSTRY`다.
- 자료가 없는 조합은 0이 아니라 `dataStatus: NOT_PROVIDED`와 빈 `quarters`로, 매출만 없는 분기는 `salesStatus: NOT_PROVIDED`와 `salesAmount: null`로 응답한다.
- 응답에는 지표 정의(원본 열·단위·해석 제한)와 제한 문구, 릴리스 출처·기준기간·입수일·파일별 SHA-256을 함께 담는다.

## 데이터 검증

[검증 도구 안내](verification/README.md)를 따른다. 원본 ZIP은 저장소에 포함하지 않는다. 지난 검증 보고서의 다운로드 시점과 지금의 최신 제공 시점을 구분한다.
