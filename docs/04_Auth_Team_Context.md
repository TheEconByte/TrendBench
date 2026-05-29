# TrendBench Auth 팀 컨텍스트

이 문서는 TrendBench 프로젝트에서 **회원가입, 로그인, 로그아웃, JWT 인증 및 사용자·매장 관리 기능**을 담당하는 Auth 팀의 구현 기준 문서다.

Auth 팀은 서비스 이용을 위한 사용자 인증 및 권한 부여를 담당하며, POS 업로드 기능과 내 가게 분석 기능의 진입점을 제공한다.

---

# 1. Auth 팀의 역할

1. 회원가입 API 구현
2. 로그인 API 구현
3. 로그아웃 API 구현
4. JWT 발급 및 검증
5. Spring Security 인증 구성
6. 사용자 정보 조회 API 구현
7. 매장(Store) 등록 API 구현
8. 사용자-매장 관계 관리
9. 인증 예외 처리
10. 인증 기반 API 접근 제어

---

# 2. 프로젝트 전반 기준

- 사용자 계정 정보는 PostgreSQL에 저장한다.
- 인증 방식은 JWT 기반 Stateless 인증을 사용한다.
- Spring Security를 사용한다.
- OAuth 로그인은 MVP 범위에서 제외한다.
- Refresh Token은 MVP 범위에서 제외한다.
- 모든 인증 API는 REST 방식으로 구현한다.
- API 응답은 camelCase를 사용한다.
- DB 컬럼은 snake_case를 사용한다.

---

# 3. 사용자 인증 흐름

## 회원가입 → 로그인 → 서비스 이용

```text
회원가입
→ 로그인
→ Access Token 발급
→ 인증 완료
→ 매장 등록
→ POS 업로드
→ 내 가게 분석
```

## 요청 인증 흐름

```text
Client
→ Authorization: Bearer {accessToken}
→ JwtAuthenticationFilter
→ JwtTokenProvider
→ UserDetailsService
→ Controller
```

---

# 4. PostgreSQL 테이블

## 4.1 users

```sql
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    role VARCHAR(30) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 필드

- user_id
- email
- password_hash
- nickname
- role
- created_at

---

## 4.2 stores

```sql
CREATE TABLE stores (
    store_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    store_name VARCHAR(100) NOT NULL,
    business_number VARCHAR(30),
    region_name VARCHAR(50),
    business_area_name VARCHAR(50),
    industry_name VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    price_level VARCHAR(30),
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

# 5. Entity 설계

## User

```java
User

- userId
- email
- passwordHash
- nickname
- role
- createdAt
```

## Store

```java
Store

- storeId
- userId
- storeName
- businessNumber
- regionName
- businessAreaName
- industryName
- latitude
- longitude
- priceLevel
```

관계

```text
User (1)
 ↓
Store (N)
```

---

# 6. API 계약

## 6.1 회원가입 API

```http
POST /api/auth/signup
```

요청

```json
{
  "email":"test@test.com",
  "password":"1234abcd!",
  "nickname":"민석"
}
```

응답

```json
{
  "userId":1,
  "email":"test@test.com",
  "message":"회원가입이 완료되었습니다."
}
```

---

## 6.2 로그인 API

```http
POST /api/auth/login
```

요청

```json
{
  "email":"test@test.com",
  "password":"1234abcd!"
}
```

응답

```json
{
  "accessToken":"jwt-token",
  "tokenType":"Bearer",
  "userId":1
}
```

---

## 6.3 로그아웃 API

```http
POST /api/auth/logout
```

응답

```json
{
  "message":"로그아웃되었습니다."
}
```

---

## 6.4 내 정보 조회 API

```http
GET /api/users/me
```

응답

```json
{
  "userId":1,
  "email":"test@test.com",
  "nickname":"민석",
  "storeName":"민석식당"
}
```

---

## 6.5 매장 등록 API

```http
POST /api/stores
```

요청

```json
{
  "storeName":"민석식당",
  "businessNumber":"123-45-67890",
  "regionName":"강남구",
  "businessAreaName":"강남역",
  "industryName":"한식"
}
```

응답

```json
{
  "storeId":1,
  "storeName":"민석식당"
}
```

---

# 7. JWT 계약

JWT Payload 예시

```json
{
  "userId":1,
  "email":"test@test.com",
  "role":"USER"
}
```

Authorization Header

```http
Authorization: Bearer {accessToken}
```

---

# 8. Security 구성

```text
SecurityFilterChain
    ↓
JwtAuthenticationFilter
    ↓
JwtTokenProvider
    ↓
UserDetailsService
```

인증 제외

```text
/api/auth/signup
/api/auth/login
/api/health
```

인증 필요

```text
/api/stores/**
/api/pos/**
/api/diagnosis/**
```

---

# 9. 비밀번호 처리 기준

비밀번호는 절대 평문 저장하지 않는다.

```java
BCryptPasswordEncoder
```

암호화

```java
passwordEncoder.encode(password)
```

검증

```java
passwordEncoder.matches(rawPassword, encodedPassword)
```

---

# 10. 오류 응답 계약

```json
{
  "errorCode":"INVALID_CREDENTIAL",
  "message":"이메일 또는 비밀번호가 올바르지 않습니다."
}
```

오류 코드

- EMAIL_ALREADY_EXISTS
- INVALID_CREDENTIAL
- INVALID_TOKEN
- EXPIRED_TOKEN
- ACCESS_DENIED
- USER_NOT_FOUND

---

# 11. 구현 우선순위

```text
1. User Entity
2. Store Entity
3. User Repository
4. Store Repository
5. 회원가입 DTO
6. 회원가입 API
7. BCrypt 적용
8. JWT Provider
9. JWT Filter
10. Security Config
11. 로그인 API
12. 사용자 정보 API
13. 매장 등록 API
14. 로그아웃 API
15. 통합 테스트
```

---

# 12. MVP에서 하지 않을 것

- Google OAuth
- Kakao OAuth
- Naver OAuth
- Refresh Token
- Redis Session
- 이메일 인증
- SMS 인증
- 권한(Role) 세분화

---

# 13. AI에게 요청 시 기준

1. Spring Security 기준으로 설명한다.
2. JWT 인증 기준으로 설명한다.
3. PostgreSQL users, stores 테이블을 사용한다.
4. OAuth를 구현하지 않는다.
5. API 응답은 camelCase를 사용한다.
6. DB 컬럼은 snake_case를 사용한다.
7. Auth 팀은 POS XLSX 파싱을 담당하지 않는다.

---

# 14. Auth 팀 최종 요약

Auth 팀은 회원가입, 로그인, 로그아웃, JWT 인증 및 사용자·매장 관리를 담당한다.

회원가입 → 로그인 → JWT 발급 → 매장 등록 → POS 업로드 → 내 가게 분석 흐름을 지원한다.

사용자 정보는 PostgreSQL의 users, stores 테이블에서 관리하며 Spring Security + JWT 기반 인증을 사용한다.
