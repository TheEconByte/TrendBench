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

Better Auth 1.7.5는 이메일·비밀번호와 DB 세션을 담당한다. `BETTER_AUTH_SECRET`은 32자 이상의 고엔트로피 값으로 설정하고 저장소에 커밋하지 않는다. 실제 이메일 발송, 비밀번호 복구, 소셜 로그인은 제공하지 않는다.

## 코드 배치

- `app/src/app`: 페이지·레이아웃·얇은 Route Handler.
- `app/src/features/finance`: Zod 입력, decimal 계산, 대출 일정, 시나리오. 계산 순수 함수는 React·DB·외부 API에 의존하지 않는다.
- `app/src/features/plans`: 인증 후 계획 목록·입력·결과 이력 UI와 API 경계 테스트.
- `app/src/lib`: Prisma 단일 client, Better Auth, 세션·오류 응답.
- `app/prisma`: Better Auth core tables, `plans`, `plan_results`, 순서가 있는 migration.
- 가짜 데이터와 미구현 서비스의 빈 코드를 미리 생성하지 않는다.

## CI

`.github/workflows/check.yml`은 Node 24에서 `npm ci`, lint, typecheck, Vitest, build를 실행한다. Vitest는 F1 계산과 미로그인 차단, 소유자 조건, revision 충돌, 서버 계산, 중복 결과 재사용을 검증한다. 실제 PostgreSQL 종단 검증은 별도로 실행한다.

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

## 데이터 검증

[검증 도구 안내](verification/README.md)를 따른다. 원본 ZIP은 저장소에 포함하지 않는다. 지난 검증 보고서의 다운로드 시점과 지금의 최신 제공 시점을 구분한다.
