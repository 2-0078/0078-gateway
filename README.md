# 🌐 Gateway Service

> **API Gateway** - MSA 아키텍처의 단일 진입점 (Single Entry Point)

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [기술 스택](#-기술-스택)
- [주요 기능](#-주요-기능)
- [라우팅 규칙](#-라우팅-규칙)
- [환경 설정](#-환경-설정)
- [실행 방법](#-실행-방법)
- [팀원 정보](#-팀원-정보)

## 🎯 프로젝트 개요

Gateway Service는 **Spring Cloud Gateway**를 기반으로 하는 **API Gateway**입니다. 

### 🌟 주요 특징

- **단일 진입점**: 15개 마이크로서비스에 대한 통합 진입점
- **JWT 인증**: 토큰 기반 인증 및 권한 관리
- **CORS 처리**: 크로스 오리진 요청 허용 설정
- **라우팅**: 경로 기반 서비스 라우팅
- **반응형 프로그래밍**: Spring WebFlux 기반 비동기 처리

### 🏆 핵심 역할

- **API 통합**: 모든 클라이언트 요청의 중앙 집중식 처리
- **보안 게이트웨이**: JWT 토큰 검증 및 인증
- **부하 분산**: 서비스 인스턴스 간 로드 밸런싱
- **모니터링**: 요청/응답 로깅 및 성능 모니터링

## 🛠️ 기술 스택

### Backend
- **Java 17** - OpenJDK LTS 버전
- **Spring Boot 3.5.0** - 최신 Spring Boot 프레임워크
- **Spring Cloud Gateway** - 리액티브 API 게이트웨이
- **Spring WebFlux** - 반응형 웹 프레임워크
- **Spring Security** - JWT 인증 및 보안

### Service Discovery & Communication
- **Netflix Eureka Client** - 서비스 디스커버리 클라이언트
- **Spring Cloud LoadBalancer** - 클라이언트 사이드 로드 밸런싱

### Infrastructure
- **Docker** - 컨테이너화
- **AWS EC2** - 클라우드 배포

## 🚀 주요 기능

### 1. API 라우팅
- **경로 기반 라우팅**: URL 패턴에 따른 서비스 라우팅
- **로드 밸런싱**: Eureka 기반 자동 로드 밸런싱
- **필터 체인**: 요청/응답 전처리 및 후처리

### 2. JWT 인증 필터
- **토큰 검증**: 모든 보호된 API의 JWT 토큰 검증
- **권한 관리**: 역할 기반 접근 제어
- **토큰 갱신**: Refresh Token을 통한 자동 갱신

### 3. CORS 설정
- **크로스 오리진 허용**: 웹 애플리케이션의 API 접근 허용
- **헤더 관리**: 필요한 HTTP 헤더 자동 설정
- **프리플라이트 처리**: OPTIONS 요청 자동 처리

### 4. 로드 밸런싱
- **Eureka 기반**: 서비스 인스턴스 자동 발견
- **라운드 로빈**: 기본 로드 밸런싱 전략
- **헬스체크**: 장애 인스턴스 자동 제외

## 🛤️ 라우팅 규칙

### 현재 등록된 라우트

| 서비스 | 경로 | 목적지 | 인증 필요 |
|:------:|:----:|:------:|:--------:|
| **Auth Service** | `/auth-service/**` | `lb://AUTH-SERVICE` | ❌ |
| **Member Service** | `/member-service/**` | `lb://MEMBER-SERVICE` | ✅ |
| **Product Service** | `/product-service/**` | `lb://PRODUCT-SERVICE` | ❌ |
| **Product Read Service** | `/product-read-service/**` | `lb://PRODUCT-READ-SERVICE` | ❌ |
| **Piece Service** | `/piece-service/**` | `lb://PIECE-SERVICE` | ✅ |
| **Funding Service** | `/funding-service/**` | `lb://FUNDING-SERVICE` | ✅ |
| **Auction Service** | `/auction-service/**` | `lb://AUCTION-SERVICE` | ✅ |
| **Payment Service** | `/payment-service/**` | `lb://PAYMENT-SERVICE` | ✅ |
| **Board Service** | `/board-service/**` | `lb://BOARD-SERVICE` | ✅ |
| **Reply Service** | `/reply-service/**` | `lb://REPLY-SERVICE` | ✅ |
| **Alert Service** | `/alert-service/**` | `lb://ALERT-SERVICE` | ❌ |
| **Batch Service** | `/batch-service/**` | `lb://BATCH-SERVICE` | ❌ |

### 라우팅 동작 방식
Gateway는 요청 경로를 분석하여 적절한 마이크로서비스로 라우팅합니다. 예를 들어, `/auth-service/api/v1/login` 요청은 Auth Service로 전달되어 실제 `http://auth-service:8081/api/v1/login`에서 처리됩니다.

## ⚙️ 환경 설정

### 필수 요구사항
- Java 17 이상
- Eureka Server 실행 중
- Docker (선택사항)

### 환경 변수
- **EUREKA_HOST**: Eureka 서버 호스트 정보
- **JWT_SECRET**: JWT 토큰 검증을 위한 시크릿 키

## 🚀 실행 방법

### 1. 저장소 클론
프로젝트 저장소를 로컬에 클론합니다.

### 2. 환경 변수 설정
필요한 환경 변수를 설정합니다.

### 3. 애플리케이션 실행
Gradle을 통해 애플리케이션을 실행합니다.

### 4. 접속 확인
- Gateway 엔드포인트: http://localhost:8000
- 기본 포트: 8000

## 🔧 주요 설정

### 게이트웨이 라우트
모든 마이크로서비스에 대한 라우트가 설정되어 있으며, 각 서비스의 경로 패턴에 따라 자동으로 라우팅됩니다.

### 보안 설정
JWT 토큰 검증이 필요한 서비스들은 Gateway에서 사전 인증 처리를 수행합니다.

### CORS 설정
프론트엔드 애플리케이션에서 API 호출이 가능하도록 CORS 설정이 구성되어 있습니다.

## 👥 팀원 정보

**Piece of Cake 팀**
- **팀장**: 이수진 (Backend & Leader)
- **팀원**: 정동섭 (Backend), 이영인 (Backend), 오은서 (Backend), 정진우 (Frontend)

**Gateway Service 담당**
- **오은서**: API Gateway 설계, 라우팅 규칙, 보안 설정

---

## 📞 연락처

- **프로젝트 홈페이지**: https://mobile.pieceofcake.site/
- **개발 기간**: 2025년 4월 30일 ~ 7월 10일

---

*"Investment is Easy and Fun" - Piece of Cake 프로젝트*
