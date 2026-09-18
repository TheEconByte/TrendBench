# TrendBench

예비 창업자가 서울 상권의 공개 지표를 참고하고, 자신의 매출·비용 가정으로 창업 필요자금과 상환 부담을 검토하는 서비스.

현재 상태: **F1 재무 계산과 F2 인증·계획 저장 완료**. F2는 실제 PostgreSQL과 브라우저에서 권한·재접속·DB 재기동 보존까지 검증했다. 상권·자금 후보와 공개 출시 준비는 완료되지 않았다. 이전 POS 서비스는 `legacy/`에 보존하며 신규 개발 기준으로 사용하지 않는다.

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

```sh
npm --prefix app run lint
npm --prefix app run typecheck
npm --prefix app run test
npm --prefix app run build
```

루트 `node_modules`는 앱 의존성 경로가 아니다. 의존성은 `app/package-lock.json`으로 관리한다.

## 첫 개발 목표

사용자 입력 → 인증된 초안 저장 → 서버 재계산 → 불변 결과 저장 흐름을 구현하고 실제 PostgreSQL과 브라우저에서 두 사용자 권한·재접속·DB 재기동 보존을 확인했다. 상권 연결과 검수된 지원사업 연결은 이후 단계다.

개편 내용과 확인한 실행 결과는 [저장소 개편 기록](docs/REPOSITORY_REORGANIZATION.md)에 남긴다.
