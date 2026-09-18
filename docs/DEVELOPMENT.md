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

http://localhost:3000 — 재무계획 입력·검증·계산 화면. DB나 외부 API를 사용하지 않는다.
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

현재 시작 화면은 DB에 연결하지 않는다. Prisma migration과 인증 설정은 TASKS의 F2에서 구현한다. 존재하지 않는 DB 테이블이나 인증 API를 준비 완료로 간주하지 않는다.

## 코드 배치

- `app/src/app`: 페이지·레이아웃·얇은 Route Handler.
- `app/src/features/finance`: Zod 입력, decimal 계산, 대출 일정, 시나리오, 첫 화면. 계산 순수 함수는 React·DB·외부 API에 의존하지 않는다.
- 이후 기능 구현 시 `app/src/features/plans`, `market`, `funding`에 필요한 파일을 추가한다.
- 공통 DB·인증 연결은 도입 시 `app/src/lib`에 둔다.
- Prisma, catalog, scripts는 실제 저장·적재 작업을 만들 때 `app/` 아래 추가한다.
- 가짜 데이터와 미구현 서비스의 빈 코드를 미리 생성하지 않는다.

## CI

`.github/workflows/check.yml`은 Node 24에서 `npm ci`, lint, typecheck, Vitest, build를 실행한다. 현재 CI는 F1 계산 규칙을 검증하지만 DB 연결·로그인·저장 권한은 아직 검증하지 않는다.

## F1 계산 정책

- 금액 입력·출력은 원 단위 정수 문자열이며 계산은 `decimal.js`를 사용한다.
- 원 단위 결과는 `ROUND_HALF_UP`으로 반올림한다. 대출 일정은 월별 이자·원금을 같은 규칙으로 반올림하고 마지막 달 원금으로 잔액을 0원에 맞춘다.
- 빈 선택값은 `null`로 유지하고 0과 구분한다. 대출금 0 또는 `null`은 무차입으로 처리한다.
- 거치는 이자만 지급하며 전체 상환기간에 포함한다. 만기일시상환·변동금리·보증료·일수별 이자는 지원하지 않는다.

## 데이터 검증

[검증 도구 안내](verification/README.md)를 따른다. 원본 ZIP은 저장소에 포함하지 않는다. 지난 검증 보고서의 다운로드 시점과 지금의 최신 제공 시점을 구분한다.
