# youth-moa (Java)

[![CI](https://github.com/sihyuun/youth-moa-java/actions/workflows/ci.yml/badge.svg)](https://github.com/sihyuuun/youth-moa-java/actions/workflows/ci.yml)

기존 Next.js + TypeScript 기반 youth-moa를 **Spring Boot 4 + Java 21 + Thymeleaf + HTMX** 스택으로 재작성하는 개인 프로젝트입니다.

## 기술 스택

| 영역 | 선택 |
|---|---|
| 런타임 | Java 21 (LTS) |
| 프레임워크 | Spring Boot 4.1.0 |
| 빌드 | Gradle (Kotlin DSL) + Gradle Wrapper |
| 뷰 | Thymeleaf + HTMX 2.0.4 |
| DB | PostgreSQL (Supabase 신규 프로젝트) |
| ORM | Spring Data JPA / Hibernate |
| 보안 | Spring Security |
| 테스트 | JUnit 5 + Testcontainers (PostgreSQL) |

## 사전 요구사항

| 도구 | Windows | macOS |
|---|---|---|
| JDK 21 | [Adoptium Temurin 21 MSI](https://adoptium.net/temurin/releases/?version=21) | `brew install --cask temurin@21` 또는 [SDKMAN](https://sdkman.io/) `sdk install java 21-tem` |
| Git | [git-scm.com](https://git-scm.com/) | `brew install git` |
| Docker (테스트용) | Docker Desktop | Docker Desktop 또는 OrbStack |
| IDE | IntelliJ IDEA Community/Ultimate | 동일 |

`JAVA_HOME`은 **JDK 17 이상**이면 충분합니다 — `build.gradle.kts`의 `toolchain { languageVersion = JavaLanguageVersion.of(21) }` 설정과 `settings.gradle.kts`의 Foojay resolver 플러그인이 Gradle 실행 시 **JDK 21을 자동으로 다운로드**하여 컴파일·실행에 사용합니다. 따라서 JDK 17만 설치돼 있어도 `./gradlew` 실행이 가능합니다.

```powershell
# Windows — JDK 17이 이미 있으면 그대로 사용
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
```

```bash
# macOS — SDKMAN 사용 시
sdk install java 21-tem
sdk use java 21-tem
```

## 로컬 DB 준비

Supabase 신규 프로젝트 생성 후 Session Pooler URL을 환경변수에 설정합니다.

### Windows (PowerShell)
```powershell
$env:DATABASE_URL = "jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres?user=postgres.xxx&password=yyy"
$env:DATABASE_USERNAME = "postgres.xxx"
$env:DATABASE_PASSWORD = "yyy"
```

### macOS / Linux
```bash
export DATABASE_URL="jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres"
export DATABASE_USERNAME="postgres.xxx"
export DATABASE_PASSWORD="yyy"
```

또는 프로젝트 루트에 `.env`를 만들어 IDE Run Configuration에서 EnvFile 플러그인으로 주입하는 방법도 가능합니다 (`.env`는 `.gitignore` 등재됨).

## 빌드 & 실행

### Windows (PowerShell)
```powershell
.\gradlew.bat build         # 빌드 + 테스트
.\gradlew.bat bootRun       # 개발 서버 기동 (http://localhost:8080)
```

### macOS / Linux
```bash
./gradlew build
./gradlew bootRun
```

서버 기동 후 브라우저에서 `http://localhost:8080` 접속 → "Ping 보내기" 버튼 클릭 시 HTMX가 `POST /api/ping`을 호출하고 응답 HTML 조각이 페이지에 삽입되면 동작 OK입니다.

## 디렉토리 구조

```
youth-moa-java/
├── build.gradle.kts                                          # Kotlin DSL
├── settings.gradle.kts
├── gradlew / gradlew.bat                                     # Gradle Wrapper (JDK만 있으면 동작)
├── .editorconfig                                             # 들여쓰기 / EOL 통일
├── .gitattributes                                            # CRLF/LF 자동 정규화
└── src/
    ├── main/
    │   ├── java/io/github/sihyuuun/youthmoa/
    │   │   ├── YouthMoaApplication.java                     # 진입점
    │   │   ├── config/SecurityConfig.java                   # Spring Security
    │   │   └── web/HomeController.java                      # 샘플 컨트롤러
    │   └── resources/
    │       ├── application.yml                              # 모든 설정 (env var 우선)
    │       ├── templates/
    │       │   ├── index.html                               # 홈 + HTMX 데모
    │       │   └── fragments/ping.html                      # HTMX 응답 조각
    │       └── static/css/main.css                          # 디자인 토큰 (youth-moa Primary)
    └── test/
        └── java/io/github/sihyuuun/youthmoa/
            ├── YouthMoaApplicationTests.java
            ├── TestYouthMoaApplication.java
            └── TestcontainersConfiguration.java             # PostgreSQL Testcontainers
```

## 다음 작업 후보

1. **도메인 모델링** — `domain/` 패키지에 `User`, `Program`, `Center`, `Application`, `Notification` JPA Entity (기존 `docs/02_design/02_db-schema.md` 참고)
2. **인증/회원가입** — Spring Security `UserDetailsService` 구현, BCrypt 비밀번호 해싱
3. **레이아웃 fragment** — Thymeleaf `th:fragment`로 Header/Footer 분리, `prototype.html` 디자인 토큰 이식
4. **자산 복사** — 기존 youth-moa `docs/00_assets/assets/*` → `src/main/resources/static/images/`
5. **DB 마이그레이션** — Flyway 또는 Liquibase 도입 (Prisma 마이그레이션 SQL 변환)

## 참고

- 디자인 기준: `../youth-moa/docs/00_assets/prototype.html` (사용자 화면), `../youth-moa/docs/00_assets/admin/prototype.html` (관리자)
- 디자인 토큰 규칙: `../youth-moa/docs/02_design/03_token-policy.md`
- API 스펙: `../youth-moa/docs/02_design/01_api-spec.md`
- DB 스키마: `../youth-moa/docs/02_design/02_db-schema.md`
