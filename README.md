# Board-Hole

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](#)

> Spring Boot MVC + CQRS 패턴을 학습하기 위한 교육용 게시판 애플리케이션

**Board-Hole**은 현대적인 Spring Boot 기반의 게시판 시스템으로, **CQRS(Command Query Responsibility Segregation)** 패턴과 **이벤트 기반 아키텍처**를 학습할 수 있도록 설계된 프로젝트입니다.

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Architecture](#-architecture)
- [Development](#-development)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

- 🔐 **JWT 기반 인증 시스템** - Spring Security + JWT 토큰
- 🏗️ **CQRS 패턴 적용** - Command/Query 책임 분리
- 🌍 **다국어 지원** - 한국어/영어 메시지 (i18n)
- ⚡ **비동기 이벤트 처리** - 조회수 증가 등 비동기 작업
- 📊 **RESTful API** - REST 원칙 준수한 API 설계
- 🎯 **권한 기반 접근 제어** - Role-based Access Control
- 📖 **API 문서화** - Swagger/OpenAPI 3.0 지원
- 🧪 **테스트 환경** - Testcontainers 기반 통합 테스트

## 🎯 Learning Objectives

이 프로젝트를 통해 다음을 학습할 수 있습니다:

- **Spring Boot MVC** 패턴과 레이어드 아키텍처
- **CQRS** 패턴을 통한 읽기/쓰기 분리
- **Spring Security**를 활용한 인증/인가
- **이벤트 기반 아키텍처**와 비동기 처리
- **MapStruct**를 이용한 객체 매핑
- **다국어 지원(i18n)** 구현
- **Docker**를 활용한 개발 환경 구축

## 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| **Java** | 21 | 기본 언어 |
| **Spring Boot** | 3.5.4 | 프레임워크 |
| **Spring Web** | - | MVC 패턴 |
| **MySQL** | 9.4 | 운영/로컬 DB (Compose)
| **Testcontainers** | 1.20+ | 테스트용 컨테이너 DB |
| **Lombok** | - | 코드 간소화 |
| **SpringDoc OpenAPI** | 2.3.0 | API 문서화 |
| **Gradle** | 8.14.3 | 빌드 도구 |

## 프로젝트 구조

```
src/main/java/bunny/boardhole/
├── BoardHoleApplication.java           # Spring Boot 메인 클래스
├── controller/                         # Controller 레이어
│   ├── UserController.java             #   - 사용자 CRUD API
│   ├── BoardController.java            #   - 게시판 CRUD API
│   └── AuthController.java             #   - 로그인 API
├── application/                        # CQRS(Application) 계층
│   ├── command/                        #   - 쓰기용 서비스/커맨드/핸들러
│   │   ├── BoardCommandService.java
│   │   ├── UserCommandService.java
│   │   └── ...Commands/Handlers
│   ├── query/                          #   - 읽기용 서비스/쿼리
│   │   ├── BoardQueryService.java
│   │   ├── UserQueryService.java
│   │   └── GetBoardQuery.java
│   ├── result/                         #   - 서비스 반환 모델(Result)
│   │   ├── BoardResult.java
│   │   └── UserResult.java
│   └── event/                          #   - 이벤트/리스너
│       ├── ViewedEvent.java
│       └── ViewedEventListener.java
├── service/                            # 도메인 보조 서비스 (비동기/공통)
│   └── ViewCountService.java
├── repository/                         # Spring Data JPA Repositories (엔티티 반환)
├── domain/                             # 엔티티/도메인 모델
└── controller/                         # REST 컨트롤러(요청/응답 모델)
```

### 레이어드 아키텍처

```
┌─────────────────┐
│   Controller    │  ← HTTP 요청/응답 처리
│    (API 계층)    │
└─────────────────┘
         ↓
┌─────────────────────────────┐
│      Application(CQRS)      │  ← Command/Query/Result/Event
│  (비즈니스 진입점/흐름 제어)   │
└─────────────────────────────┘
         ↓
┌─────────────────┐
│   Repository    │  ← 데이터 접근 (엔티티 중심)
│   (데이터 계층)   │
└─────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Docker & Docker Compose** (for local development)
- **Git** 2.20 or higher
- **IDE**: IntelliJ IDEA (recommended) or VS Code

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/board-hole.git
   cd board-hole
   ```

2. **Start infrastructure services**
   ```bash
   # Start MySQL database
   docker-compose up -d
   ```

3. **Run the application**
   ```bash
   # Using Gradle Wrapper (recommended)
   ./gradlew bootRun
   
   # Or run from IDE
   # Open BoardHoleApplication.java and run main method
   ```

4. **Verify installation**
   ```bash
   # Check application health
   curl http://localhost:8080/actuator/health
   
   # Access Swagger UI
   open http://localhost:8080/swagger-ui.html
   ```

### Default Accounts

| Role | Username | Password | Description |
|------|----------|----------|--------------|
| Admin | `admin` | `admin123` | 관리자 계정 |
| User | `user` | `user123` | 일반 사용자 계정 |

### Internationalization

Add `lang` parameter to any request for language support:
- `?lang=ko` - 한국어 (기본값)
- `?lang=en` - English

## 📖 API Documentation

### Interactive API Explorer
```
Swagger UI: http://localhost:8080/swagger-ui.html
OpenAPI Spec: http://localhost:8080/v3/api-docs
```

### Core Endpoints

#### Authentication
- `POST /api/auth/login` - 로그인
- `GET /api/auth/me` - 현재 사용자 정보

#### Boards
- `GET /api/boards` - 게시글 목록 (페이징, 검색)
- `POST /api/boards` - 게시글 작성 🔒
- `GET /api/boards/{id}` - 게시글 조회
- `PUT /api/boards/{id}` - 게시글 수정 🔒
- `DELETE /api/boards/{id}` - 게시글 삭제 🔒

#### Users
- `GET /api/users` - 사용자 목록 🔒
- `POST /api/users` - 사용자 생성
- `GET /api/users/{id}` - 사용자 조회
- `PUT /api/users/{id}` - 사용자 정보 수정 🔒

🔒 = Authentication required

### Example Usage

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Create board (with JWT token)
curl -X POST http://localhost:8080/api/boards \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hello World","content":"First post!"}'

# Get boards with English messages
curl "http://localhost:8080/api/boards?lang=en"
```

## 🏗 Architecture

자세한 아키텍처 정보는 [ARCHITECTURE.md](./ARCHITECTURE.md)를 참고하세요.

### CQRS Pattern Overview

```
┌─────────────────┐
│   Controllers   │  ← HTTP API Layer
└─────────────────┘
         ↓
┌─────────────────────────────┐
│      Application Layer      │  ← CQRS Commands & Queries
│  Commands │ Queries │Events │
└─────────────────────────────┘
         ↓
┌─────────────────┐
│ Domain & Infra  │  ← Entities & Repositories
└─────────────────┘
```

- **Commands**: 데이터 변경 작업 (Create, Update, Delete)
- **Queries**: 데이터 조회 작업 (Read)
- **Events**: 비동기 처리 (조회수 증가, 알림 등)

## 🛠 Development

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests BoardControllerTest
```

### Development with Docker
```bash
# Start MySQL database
docker-compose up -d mysql

# Stop all services
docker-compose down
```

For detailed development setup, see [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md).

## 🤝 Contributing

이 프로젝트는 Spring Boot 학습을 위한 오픈소스 프로젝트입니다.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

자세한 기여 가이드는 [CONTRIBUTING.md](./CONTRIBUTING.md)를 참고하세요.

## 📚 Documentation

- [🏗️ Architecture Guide](./ARCHITECTURE.md) - 시스템 아키텍처 및 CQRS 패턴
- [🔐 Security Guide](./SECURITY.md) - JWT 인증 및 보안 정책
- [🛠️ Development Guide](./docs/DEVELOPMENT.md) - 개발 환경 설정
- [📖 API Reference](./docs/API.md) - REST API 명세서
- [🌍 Internationalization](./docs/I18N.md) - 다국어 지원 가이드

## 📄 License

이 프로젝트는 MIT License 하에 배포됩니다. 자세한 내용은 [LICENSE](./LICENSE) 파일을 참고하세요.
