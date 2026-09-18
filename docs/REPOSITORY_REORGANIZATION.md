# 저장소 개편 기록

작성일: 2026-09-18.

## 변경 결과

- 신규 개발 위치를 `app/`의 Next.js 단일 앱으로 고정했다.
- `backend/`, `frontend/`, 기존 `infra/`, 과거 팀 컨텍스트·환경·인터페이스 문서를 `legacy/`로 이동했다. Git 기준 기존 추적 파일 134개의 보관 파일 존재와 내용 동일성을 검사했다(텍스트 줄바꿈 정규화).
- 이전 `.env.example`도 `legacy/`에 보존했다. 새 환경 예시는 `app/.env.example`, `infra/.env.example`로 분리했다.
- 루트 README·AGENTS, PRODUCT·TASKS·DEVELOPMENT를 추가했다. 11번 문서는 과거 기록임을 표시했고, 12번 계획에는 새 구현 위치와 초기 업종 범위를 반영했다.
- 공공데이터 검증 결과는 그대로 유지하고, 개인 PC 경로 대신 원본을 준비하여 재현할 수 있는 검증 안내를 추가했다.
- Next.js 시작 화면, 프로세스 health API, npm lockfile, 정적 검사와 빌드 CI를 구성했다.
- PostgreSQL 전용 Compose를 추가했다. 프로젝트명·볼륨·DB명을 분리하고 호스트 접속은 127.0.0.1:5433으로 제한했다.

## 실행 확인

| 검사 | 결과 |
|---|---|
| npm 의존성 설치 및 lockfile 생성 | 성공 |
| ESLint | 성공. 내부 링크를 Next Link로 수정 후 재검사 |
| TypeScript | 성공 |
| Next.js production build | 성공 |
| 시작 화면 HTTP | 200 |
| /api/health | 정상 JSON 응답 |
| 시작 화면 브라우저 확인 | 제목·준비 중 안내·예정된 흐름 표시 확인 |
| 기존 파일 보존 | 134개 대조, 차이 없음(줄바꿈 제외) |
| Docker Compose 설정 검사 | 성공 |
| PostgreSQL 컨테이너 실행 | 미확인. 로컬 Docker 데몬 연결 불가 |
| GitHub 원격 CI 실행 | 미수행 |

DB 실제 실행·앱 DB 연결·인증·계산·저장·상권 API·상품 매칭은 구현 완료가 아니다. TASKS의 F1부터 진행한다. 이전 검증 보고서의 공고·데이터 확인일도 현재 시점으로 갱신하지 않았다.

## 보존 및 Git 상태

기존 DB 볼륨, 업로드, Git 이력은 삭제하지 않았다. 기존 코드의 로컬 빌드 산출물도 폴더와 함께 보존하되 Git에서 제외했다. 루트의 환경 제공 node_modules 연결은 건드리지 않았으며 새 앱 의존성은 app/node_modules에 설치했다.

이 개편은 작업 트리에 반영했다. 자동 커밋·푸시·배포는 수행하지 않았다. Git에서 이전 경로 삭제와 legacy 경로 추가로 보일 수 있으며, 함께 커밋하면 Git이 내용 기반으로 이동을 식별한다.

## 다음 작업

TASKS의 F1: 입력 스키마·decimal 계산·의미 있는 검산 테스트. 이후 Prisma·Better Auth·계획 저장을 연결한다. 사용하지 않는 빈 모듈·가짜 상품 목록·작동하는 것처럼 보이는 저장 API는 생성하지 않았다.
