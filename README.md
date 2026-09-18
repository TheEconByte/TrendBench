# TrendBench

예비 창업자가 서울 상권의 공개 지표를 참고하고, 자신의 매출·비용 가정으로 창업 필요자금과 상환 부담을 검토하는 서비스.

현재 상태: **F1 재무 계산 핵심과 첫 입력·결과 화면 구현**. 인증·저장·상권·자금 후보를 포함한 서비스 전체 구현이나 출시 완료 상태는 아니다. 이전 POS 서비스는 `legacy/`에 보존하며 신규 개발 기준으로 사용하지 않는다.

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

브라우저에서 http://localhost:3000 을 연다. 재무계획 입력·검증·계산·시나리오·대출 일정 화면은 DB 없이 실행된다. 계획 저장·로그인·상권 조회는 작업 목록에 따라 구현한다.

```sh
npm --prefix app run lint
npm --prefix app run typecheck
npm --prefix app run test
npm --prefix app run build
```

루트 `node_modules`는 앱 의존성 경로가 아니다. 의존성은 `app/package-lock.json`으로 관리한다.

## 첫 개발 목표

사용자 입력 → 필요자금·운영수지 계산까지 완료했다. 다음 목표는 인증된 계획 저장이다. 상권 연결과 검수된 지원사업 연결은 이후 단계다. POS·파일 업로드·예측 점수·자동 금융 승인 기능은 범위 밖이다.

개편 내용과 확인한 실행 결과는 [저장소 개편 기록](docs/REPOSITORY_REORGANIZATION.md)에 남긴다.
