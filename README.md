# WonkaoTalk Backend

WonkaoTalk 백엔드 API 서버입니다. 사용자/판매자 계정, 친구, 채팅, 상품, 장바구니, 주문/결제, 배송지, 이미지 업로드, 검색 기능을 제공합니다.

## 기술 스택

- Java 25
- Spring Boot 4.0.6
- Spring MVC, Spring Security, OAuth2 Client
- Spring Data JPA, Flyway
- PostgreSQL 17
- Redis 7.4
- Kafka 4.0.0
- MinIO, AWS S3 SDK v2
- JWT
- Gradle
- Testcontainers

## 주요 기능

| 도메인             | 기능                                                |
|-----------------|---------------------------------------------------|
| Auth            | 이메일 중복 확인, 로그인, 로그아웃, JWT 재발급                     |
| User            | 사용자 회원가입, 내 정보 조회/수정, 회원 탈퇴, 사용자 검색               |
| Friend          | 친구 검색, 등록, 정보 수정, 삭제                              |
| Seller / Store  | 판매자 가입/등록, 스토어 생성/조회/수정/삭제                        |
| Product         | 상품 생성, 상품 목록/상세 조회, 카테고리 조회                       |
| Cart            | 장바구니 조회, 상품 추가, 수량 변경, 옵션 변경, 삭제                  |
| Order / Payment | 주문 미리보기, 주문 생성, 토스페이먼츠 결제창 정보 조회, 결제 승인, 결제 실패 처리 |
| Shipping        | 배송지 생성/조회/수정/삭제, 기본 배송지 설정                        |
| Image           | MinIO/S3 기반 Presigned URL 발급                      |
| Chat            | 채팅방 생성/조회, 메시지 전송/조회                              |
| Search          | 상품/사용자 검색                                         |

## 시스템 아키텍처

> 시스템 아키텍처 이미지 삽입 예정

| 영역                    | 역할                       |
|-----------------------|--------------------------|
| Client                | 사용자/판매자 화면에서 REST API 호출 |
| Spring Security + JWT | 인증, 인가, 토큰 검증            |
| Controller            | API 요청/응답 처리, 입력값 검증     |
| Service               | 도메인 비즈니스 로직 처리           |
| Repository            | JPA 기반 데이터 접근            |
| PostgreSQL            | 핵심 도메인 데이터 저장            |
| Redis                 | 토큰/캐시성 데이터 저장            |
| Kafka                 | 이벤트 기반 메시징               |
| MinIO / S3            | 이미지 파일 업로드 스토리지          |
| Elasticsearch         | 검색 인덱싱 및 검색              |
| TossPayments          | 결제 승인 연동                 |

## ERD

> ERD 이미지 삽입 예정

## 프로젝트 구조

```text
src/main/java/com/example/WonkaoTalk
├── application      # 애플리케이션 파사드
├── common           # 공통 응답, 예외, 보안, 설정
└── domain
    ├── auth         # 로그인, 로그아웃, 토큰 재발급, 이메일 중복 확인
    ├── chat         # 채팅방, 채팅 메시지
    ├── image        # Presigned URL 발급
    ├── order        # 주문 생성, 주문 미리보기
    ├── payment      # 토스페이먼츠 결제 준비/승인/실패 처리
    ├── product      # 상품, 카테고리, 장바구니
    ├── search       # 검색
    ├── seller       # 판매자 가입/등록/조회/수정/탈퇴
    ├── shipping     # 배송지
    ├── store        # 스토어
    ├── term         # 약관
    └── user         # 사용자, 친구
```

## 실행 환경

로컬 실행은 `local`, `secret` 프로필을 기본으로 사용합니다.

```yaml
spring:
  profiles:
    active: local, secret
```

로컬 기본 포트는 다음과 같습니다.

| 항목            | 주소                      |
|---------------|-------------------------|
| API Server    | `http://localhost:8080` |
| PostgreSQL    | `localhost:5432`        |
| Redis         | `localhost:6379`        |
| Kafka         | `localhost:9092`        |
| MinIO API     | `http://localhost:9000` |
| MinIO Console | `http://localhost:9001` |
| Elasticsearch | `http://localhost:9200` |

## 로컬 실행

### 1. 인프라 실행

```bash
docker compose up -d
```

실행되는 컨테이너:

- PostgreSQL
- Redis
- Kafka
- MinIO
- MinIO bucket init
- Elasticsearch

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 테스트 실행

```bash
./gradlew test
```

## 테스트

단위 테스트와 통합 테스트를 함께 작성했습니다.

| 구분               | 내용                                                                                             |
|------------------|------------------------------------------------------------------------------------------------|
| Unit Test        | Auth, User, Seller, Store, Product, Cart, Order, Payment, Shipping, Image, Search, Chat 서비스 검증 |
| Integration Test | Security, Product 상세/검색/장바구니, Chat Repository 검증                                               |
| Testcontainers   | PostgreSQL, Redis, Kafka, MinIO 테스트 컨테이너 구성                                                    |

테스트 인프라는 `src/test/java/com/example/WonkaoTalk/config/TestContainerConfig.java`에서 관리합니다.

## 주요 환경 변수

`application-local.yaml`에는 로컬 개발용 기본값이 포함되어 있습니다. 운영/공유 환경에서는 환경 변수로 덮어쓰는 것을 권장합니다.

| 변수                          | 설명                 | 기본값                                      |
|-----------------------------|--------------------|------------------------------------------|
| `JWT_SECRET`                | JWT 서명 키           | 로컬 테스트 키                                 |
| `STORAGE_ENDPOINT`          | MinIO/S3 엔드포인트     | `http://localhost:9000`                  |
| `STORAGE_ACCESS_KEY`        | 스토리지 Access Key    | `minioadmin`                             |
| `STORAGE_SECRET_KEY`        | 스토리지 Secret Key    | `minioadmin`                             |
| `STORAGE_BUCKET`            | 스토리지 버킷            | `wonkao-talk`                            |
| `TOSS_PAYMENTS_CLIENT_KEY`  | 토스페이먼츠 클라이언트 키     | 테스트 키                                    |
| `TOSS_PAYMENTS_SECRET_KEY`  | 토스페이먼츠 시크릿 키       | 테스트 키                                    |
| `TOSS_PAYMENTS_BASE_URL`    | 토스페이먼츠 API URL     | `https://api.tosspayments.com`           |
| `TOSS_PAYMENTS_SUCCESS_URL` | 결제 성공 redirect URL | `http://localhost:3000/payments/success` |
| `TOSS_PAYMENTS_FAIL_URL`    | 결제 실패 redirect URL | `http://localhost:3000/payments/fail`    |

## API 명세 요약

### Auth

| Method | Path                       | 설명        |
|--------|----------------------------|-----------|
| `POST` | `/api/v1/auth/check-email` | 이메일 중복 확인 |
| `POST` | `/api/v1/auth/login`       | 로그인       |
| `POST` | `/api/v1/auth/logout`      | 로그아웃      |
| `POST` | `/api/v1/auth/reissue`     | 토큰 재발급    |

### User

| Method   | Path                     | 설명       |
|----------|--------------------------|----------|
| `POST`   | `/api/v1/users/signup`   | 사용자 회원가입 |
| `GET`    | `/api/v1/users`          | 내 정보 조회  |
| `PATCH`  | `/api/v1/users`          | 내 정보 수정  |
| `DELETE` | `/api/v1/users/withdraw` | 사용자 탈퇴   |
| `POST`   | `/api/v1/users/search`   | 사용자 검색   |

### Friend

| Method   | Path                                | 설명       |
|----------|-------------------------------------|----------|
| `POST`   | `/api/v1/friends`                   | 친구 추가    |
| `GET`    | `/api/v1/friends`                   | 친구 목록 조회 |
| `PATCH`  | `/api/v1/friends/{friendId}`        | 친구 정보 수정 |
| `PATCH`  | `/api/v1/friends/{friendId}/status` | 친구 상태 변경 |
| `DELETE` | `/api/v1/friends/{friendId}`        | 친구 삭제    |

### Seller / Store

| Method   | Path                       | 설명        |
|----------|----------------------------|-----------|
| `POST`   | `/api/v1/sellers/signup`   | 판매자 회원가입  |
| `POST`   | `/api/v1/sellers/register` | 판매자 등록    |
| `GET`    | `/api/v1/sellers`          | 판매자 정보 조회 |
| `PATCH`  | `/api/v1/sellers`          | 판매자 정보 수정 |
| `DELETE` | `/api/v1/sellers/withdraw` | 판매자 탈퇴    |
| `POST`   | `/api/v1/stores`           | 스토어 생성    |
| `GET`    | `/api/v1/stores`           | 스토어 조회    |
| `PATCH`  | `/api/v1/stores`           | 스토어 수정    |
| `DELETE` | `/api/v1/stores`           | 스토어 삭제    |

### Product / Cart

| Method   | Path                                        | 설명         |
|----------|---------------------------------------------|------------|
| `POST`   | `/api/v1/products`                          | 상품 생성      |
| `GET`    | `/api/v1/products`                          | 상품 목록 조회   |
| `GET`    | `/api/v1/products/{productId}`              | 상품 상세 조회   |
| `GET`    | `/api/v1/products/categories`               | 카테고리 조회    |
| `GET`    | `/api/v1/carts`                             | 장바구니 조회    |
| `POST`   | `/api/v1/carts/items`                       | 장바구니 상품 추가 |
| `PATCH`  | `/api/v1/carts/items/{cartItemId}/quantity` | 장바구니 수량 변경 |
| `PATCH`  | `/api/v1/carts/items/{cartItemId}/option`   | 장바구니 옵션 변경 |
| `DELETE` | `/api/v1/carts/items`                       | 장바구니 상품 삭제 |

### Order / Payment

| Method | Path                                    | 설명                  |
|--------|-----------------------------------------|---------------------|
| `POST` | `/api/v1/orders/preview`                | 주문/결제 금액 미리보기       |
| `POST` | `/api/v1/orders`                        | 주문 생성 및 결제 준비 정보 생성 |
| `GET`  | `/api/v1/orders`                        | 주문 목록 조회            |
| `GET`  | `/api/v1/orders/{orderId}`              | 주문 상세 조회            |
| `GET`  | `/api/v1/payments/{paymentId}/checkout` | 토스 결제창 호출 정보 조회     |
| `POST` | `/api/v1/payments/confirm`              | 토스 결제 승인            |
| `POST` | `/api/v1/payments/{paymentId}/fail`     | 결제 실패/취소 처리         |

주문/결제 흐름은 현재 다음 3가지 흐름을 중심으로 구성됩니다.

1. 결제 preview 생성: 상품 옵션과 수량으로 결제 예정 금액 계산
2. 상품 바로 결제 flow: 상품 상세에서 `variantId`, `quantity`로 주문 생성
3. 장바구니 결제 flow: 장바구니 선택 상품의 `variantId`, `quantity`로 주문 생성

### Shipping / Image / Search

| Method   | Path                                                     | 설명                        |
|----------|----------------------------------------------------------|---------------------------|
| `GET`    | `/api/v1/shipping/addresses`                             | 배송지 목록 조회                 |
| `POST`   | `/api/v1/shipping/addresses`                             | 배송지 생성                    |
| `PATCH`  | `/api/v1/shipping/addresses/{shippingAddressId}`         | 배송지 수정                    |
| `DELETE` | `/api/v1/shipping/addresses/{shippingAddressId}`         | 배송지 삭제                    |
| `PATCH`  | `/api/v1/shipping/addresses/{shippingAddressId}/default` | 기본 배송지 설정                 |
| `POST`   | `/api/v1/images/presigned-url`                           | 이미지 업로드용 Presigned URL 발급 |
| `GET`    | `/api/v1/search`                                         | 검색                        |

### Chat

| Method | Path                                  | 설명           |
|--------|---------------------------------------|--------------|
| `POST` | `/api/v1/chats`                       | 채팅방 생성       |
| `GET`  | `/api/v1/chats`                       | 채팅방 목록 조회    |
| `POST` | `/api/v1/chats/{chatRoomId}/messages` | 채팅 메시지 전송    |
| `GET`  | `/api/v1/chats/{chatRoomId}/messages` | 채팅 메시지 목록 조회 |

## 핵심 구현 포인트

### JWT 인증/인가

- Access Token과 Refresh Token을 발급합니다.
- Refresh Token은 Redis에 저장합니다.
- 로그아웃 시 Redis의 Refresh Token을 삭제하고, 남은 만료 시간만큼 Access Token을 블랙리스트에 등록합니다.
- 요청마다 JWT 필터에서 토큰 유효성 및 블랙리스트 여부를 검증합니다.

### 주문/결제

- 주문 생성 시 주문 금액과 결제 준비 정보를 함께 생성합니다.
- 결제 승인 전 `orderId`, 결제 금액, 결제 상태, 결제 소유자를 검증합니다.
- 결제 승인 전 재고를 원자적으로 차감하고, 실패 시 토스 승인 요청을 보내지 않습니다.
- 토스페이먼츠 승인 응답의 주문번호, 금액, 상태를 다시 검증합니다.

### 이미지 업로드

- 이미지 업로드는 Presigned URL 방식으로 처리합니다.
- 허용 확장자는 `jpg`, `jpeg`, `png`, `webp`입니다.
- 임시 경로 `temp/`에 업로드한 뒤 상품 생성 트랜잭션 커밋 후 `products/` 경로로 이동합니다.

## 트러블슈팅

| 이슈                          | 원인                                                         | 해결                                                              |
|-----------------------------|------------------------------------------------------------|-----------------------------------------------------------------|
| 로그아웃 후 Access Token 재사용 가능성 | JWT는 stateless 구조라 서버에서 즉시 폐기하기 어려움                        | Redis에 `BlackList:{accessToken}` 키를 남은 만료 시간만큼 저장하고 JWT 필터에서 차단 |
| Refresh Token 탈취 의심 상황      | 재발급 요청 토큰과 Redis에 저장된 Refresh Token이 불일치                   | 기존 Redis 토큰 삭제 후 `AUTH_SUSPECT_THEFT_TOKEN` 예외 처리               |
| 결제 금액 위변조 가능성               | 클라이언트가 결제 승인 금액을 임의로 보낼 수 있음                               | 토스 승인 전 DB에 저장된 결제 금액과 요청 금액 비교, 불일치 시 `INVALID` 처리             |
| 결제 승인 중 재고 초과 판매 가능성        | 동시에 여러 사용자가 같은 옵션을 결제할 수 있음                                | `stock >= quantity` 조건이 포함된 DB update로 원자적 재고 차감 처리             |
| 업로드되지 않은 이미지 키 사용 가능성       | 클라이언트가 임의의 object key를 보낼 수 있음                             | `temp/{uuid}.{ext}` 패턴 검증 후 S3 `headObject`로 실제 파일 존재 여부 확인     |
| 테스트 환경 의존성                  | 로컬 PostgreSQL, Redis, Kafka, MinIO 상태에 따라 테스트 결과가 달라질 수 있음 | Testcontainers로 테스트 실행 시 필요한 인프라를 격리된 컨테이너로 구성                  |

## 응답 형식

API 응답은 공통 응답 객체 `ApiResponse<T>`로 감싸서 반환합니다.

```json
{
  "status": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {},
  "error": null,
  "timestamp": "2026-05-20T00:00:00Z"
}
```

예외 응답은 `GlobalExceptionHandler`에서 처리하며, 도메인별 에러 코드는 `ErrorCode`에 정의되어 있습니다.

## 참고 사항

- ~~데이터베이스 스키마는 JPA `ddl-auto: update`와 Flyway를 함께 사용합니다.~~
  </br>>>> Flyway 사용으로 ddl-auto: 설정은 validate로 해주어야 합니다(배포환경과 동일)
- 로컬 MinIO 실행 시 `wonkao-talk` 버킷이 자동 생성됩니다.
- 토스페이먼츠 결제 승인 API는 `TossPaymentsClient`에서 `/v1/payments/confirm`을 호출합니다.
- 주문 목록/상세 API는 컨트롤러 경로가 존재하며, 현재 브랜치에서 구현 작업 중인 상태일 수 있습니다.

## 향후 개선 사항

- 시스템 아키텍처 이미지 추가
- ERD 이미지 추가
- API 상세 Request/Response 명세 추가
- CI/CD 및 배포 환경 문서화
- 결제 `PENDING` 상태 장기 방치 건 정리 배치 추가
- 포인트/쿠폰 정책 연동
- Elasticsearch 기반 검색 고도화