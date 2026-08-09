# 크맵 - 백엔드

교통사고 사례를 공유하고 다른 사용자의 의견과 과실 비율을 확인할 수 있는 커뮤니티 서비스 **크맵**의 백엔드 저장소입니다.

게시글과 댓글, JWT 인증, 이미지 업로드, 과실 투표를 위한 API를 제공하며 AWS 환경에서 Docker 컨테이너로 운영됩니다.

## 주요 기능

- 회원가입, 로그인, 토큰 재발급 및 로그아웃
- 사용자 이메일·닉네임 중복 확인과 회원 정보 관리
- 비회원 게시글 목록 및 상세 조회
- 게시글 작성·수정·삭제와 사용자별 좋아요
- 사용자당 한 번 집계되는 게시글 조회수
- Keyset Cursor 기반 게시글·댓글 조회
- 댓글 작성·수정·삭제
- S3 Presigned URL 기반 게시글·프로필 이미지 업로드
- 게시글 원본·썸네일 Object Key 관리
- 기간이 설정된 과실 비율 투표 생성, 참여, 재투표와 결과 집계
- 탈퇴한 사용자의 콘텐츠를 유지하면서 작성자를 삭제된 사용자로 표시
- 공통 형식의 비즈니스·인증·인가 오류 응답

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4, Spring MVC |
| 데이터 | Spring Data JPA, MySQL, H2(Test) |
| 마이그레이션 | Flyway |
| 보안 | Spring Security, JWT |
| 스토리지 | Amazon S3, AWS SDK for Java |
| 테스트 | JUnit 5, Spring Boot Test |
| 빌드 | Gradle |
| 배포 | Docker, Docker Compose, Nginx, GitHub Actions, AWS EC2·RDS·S3 |

## 주요 구현 사항

### 인증 및 인가

- 로그인 성공 시 Access Token은 응답으로, Refresh Token은 HttpOnly 쿠키로 발급합니다.
- JWT 필터에서 Access Token을 검증하고 인증 객체를 Security Context에 저장합니다.
- 인증 실패와 권한 부족은 각각 AuthenticationEntryPoint와 AccessDeniedHandler가 공통 오류 형식으로 반환합니다.
- 게시글과 댓글의 변경 작업은 작성자만 수행할 수 있습니다.

### 조회 성능

- 게시글과 댓글 목록은 Offset 대신 Keyset Cursor 방식을 사용합니다.
- 필요한 연관 엔티티는 `@EntityGraph`를 적용해 N+1 조회를 방지합니다.
- 목록의 댓글 수와 같은 집계값은 Projection과 집계 쿼리로 조회합니다.
- 게시글 조회수는 사용자당 한 번만 반영되도록 별도 기록을 관리합니다.

### 이미지 저장

1. 업로드 목적, 파일명, MIME 타입과 크기를 검증합니다.
2. Object Key를 서버에서 생성합니다.
3. 제한된 시간 동안만 유효한 S3 Presigned PUT URL을 반환합니다.
4. 클라이언트가 S3 업로드를 완료한 뒤 게시글 또는 사용자 정보에 Object Key를 전달합니다.
5. 조회 시 저장된 Object Key를 Presigned GET URL로 변환해 반환합니다.

애플리케이션에는 AWS 장기 Access Key를 저장하지 않습니다. 운영 컨테이너의 AWS SDK는 EC2 Instance Profile의 임시 자격 증명을 사용합니다.

### 과실 투표

- 게시글 생성 시 선택적으로 두 개의 투표 대상과 종료 시간을 설정합니다.
- 사용자는 0부터 10까지의 왼쪽 과실 점수를 선택하며 오른쪽 점수는 합계가 10이 되도록 계산합니다.
- 사용자별 응답은 한 행만 유지하고 재투표 시 기존 응답을 갱신합니다.
- 비관적 락과 데이터베이스 제약 조건을 사용해 동시 요청에서도 중복 투표 응답 생성을 방지합니다.
- 종료된 투표는 추가 참여를 막고 집계 결과를 제공합니다.

### 데이터베이스 마이그레이션

- 운영 데이터베이스의 스키마 변경은 Flyway가 버전 순서대로 수행합니다.
- Hibernate는 운영 환경에서 스키마를 생성하지 않고 엔티티와 실제 스키마가 일치하는지 검증합니다.
- 통합 테스트는 외부 데이터베이스 없이 H2 인메모리 데이터베이스와 `create-drop` 설정을 사용합니다.

## 시스템 구조

```text
사용자
  └─ Nginx (React 정적 파일 + /api 리버스 프록시)
       └─ Spring Boot A 또는 B 컨테이너
            ├─ Amazon RDS for MySQL
            └─ Amazon S3
```

프론트엔드와 백엔드는 Docker Bridge Network에서 서비스 이름으로 통신합니다. 외부에는 Nginx의 HTTP 포트만 노출하고 백엔드의 8080 포트는 Docker 내부 네트워크에서만 사용합니다.

## 디렉터리 구조

```text
src/main/java/com/example/board/
├── configuration/  # Security, JWT, S3 설정
├── controller/     # HTTP 요청 진입점
├── domain/         # User, Board, Comment, Vote 도메인
├── dto/            # 요청·응답 데이터 구조
├── exception/      # 비즈니스 및 보안 오류 처리
├── repository/     # JPA Repository와 Projection
├── response/       # 공통 API 응답
├── service/        # 트랜잭션과 비즈니스 로직
└── validation/     # 입력값 검증

src/main/resources/db/migration/  # Flyway 마이그레이션
scripts/                          # 백엔드·프론트엔드 배포 스크립트
```

## 로컬 실행

### 요구 사항

- Java 21
- MySQL 8 이상

애플리케이션 실행에는 MySQL, JWT와 S3 관련 런타임 설정이 필요하며 해당 값은 저장소에 포함하지 않습니다.

### 실행

```bash
./gradlew bootRun
```

기본 애플리케이션 포트는 `8080`입니다.

### 빌드

```bash
./gradlew clean build
java -jar build/libs/app.jar
```

## 테스트

```bash
./gradlew test
```

테스트에서는 H2 인메모리 데이터베이스와 테스트 전용 JWT·S3 설정을 사용하므로 로컬 MySQL이나 실제 AWS 접속 없이 실행할 수 있습니다.

주요 테스트 범위는 다음과 같습니다.

- 인증과 사용자 API 통합 흐름
- 게시글·댓글·좋아요 API
- 게시글 생성 트랜잭션
- S3 Presigned URL 발급 정책
- 이미지 Object Key 저장 로직
- 과실 투표 도메인, API와 동시성 제어
- 작성자 로딩 시 쿼리 수 고정 여부

## Docker 및 운영 설정

- 멀티 스테이지 Docker 빌드에서 전체 테스트를 통과한 실행 JAR만 JRE 이미지로 복사합니다.
- 컨테이너는 root가 아닌 별도 사용자로 애플리케이션을 실행합니다.
- 저사양 EC2 환경을 고려해 JVM 힙 크기를 제한합니다.
- Actuator Health 엔드포인트로 컨테이너 준비 상태를 확인합니다.
- Graceful Shutdown으로 처리 중인 요청을 기다린 뒤 컨테이너를 종료합니다.
- Docker JSON 로그의 파일 크기와 보관 개수를 제한합니다.
- Flyway가 스키마를 변경하고 Hibernate `validate`가 엔티티 매핑을 검증합니다.

## CI/CD와 A/B 배포

1. `main` 브랜치에 Push하면 GitHub Actions가 테스트와 빌드를 수행합니다.
2. 검증에 성공한 Docker 이미지를 `latest`와 Commit SHA 태그로 Docker Hub에 Push합니다.
3. GitHub OIDC로 AWS IAM Role의 임시 자격 증명을 발급받습니다.
4. GitHub-hosted Runner의 IP만 EC2 SSH에 임시 허용합니다.
5. 현재 비활성 상태인 A 또는 B 슬롯에 새 백엔드 컨테이너를 실행합니다.
6. 새 컨테이너가 제한 시간 안에 Health Check를 통과하는지 확인합니다.
7. Nginx Upstream을 새 슬롯으로 변경하고 Reload하여 트래픽을 전환합니다.
8. 이전 슬롯을 종료하고 임시 SSH 보안 그룹 규칙을 제거합니다.

프론트엔드는 단일 컨테이너를 새 이미지로 교체하며, 백엔드는 A/B 슬롯을 이용해 사용자 요청을 유지하면서 배포합니다.

## 관련 저장소

- [크맵 프론트엔드](https://github.com/100-hours-a-week/KTB-won-week12-FE)

## 향후 개선 사항

- Redis 기반 Refresh Token 관리
- Redis를 활용한 HOT 게시글 및 명예의 전당 기능
- 장애 발생 시 이전 이미지로 전환하는 롤백 절차 자동화
