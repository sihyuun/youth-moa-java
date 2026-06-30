# CLAUDE.md

이 파일은 Claude Code 가 `youth-moa-java` 저장소에서 작업할 때 참조하는 최우선 가이드라인입니다.

## 프로젝트 정체성

기존 `youth-moa` (Next.js + TS) 를 **Java 풀스택 학습 목적**으로 재작성하는 신규 트랙. 두 레포는 코드 공유 없이 디자인 자산(prototype.html, 토큰, 이미지) 만 참조합니다.

| 영역 | 선택 |
|---|---|
| 런타임 | Java 21 (Foojay toolchain 자동 다운로드) |
| 프레임워크 | Spring Boot 4.1.0 |
| 빌드 | Gradle (Kotlin DSL) |
| 뷰 | Thymeleaf + HTMX 2.0.4 (webjar) |
| DB | PostgreSQL (개인 Supabase 신규 프로젝트, 학습 단계에선 `ddl-auto: create-drop`) |
| ORM | Spring Data JPA / Hibernate |
| 보안 | Spring Security 7 + BCrypt |
| 테스트 | JUnit 5 + H2 (`@DataJpaTest`) + Testcontainers (개인 PC 전용) |

자세한 환경/상태는 메모리 `~/.claude/.../memory/project_youth_moa_java.md` 를 참고합니다.

---

## 디자인 시스템 (참조 기준)

- **브랜드 컬러**: `#3F30E9` (`--color-primary`)
- **폰트**: Pretendard (CDN)
- **콘텐츠 최대폭**: 1440px, 좌우 패딩 80px (`--content-max`, `--content-px` 변수)
- **디자인 토큰 (`main.css :root`)**:
  - 컬러: `--color-primary{,-dark,-light,-bg}`, `--color-secondary`, `--color-success{,-light}`, `--color-warning{,-light}`, `--color-error`
  - 텍스트 계층: `--color-text` → `-sec` → `-tri` (prototype `T.text/textSec/textTri` 대응)
  - 표면/보더: `--color-surface`, `--color-bg`, `--color-border{,-light}`
  - radius: `--radius-md` (8) / `--radius-lg` (12) / `--radius-pill` (20)
  - shadow: `--shadow-sm` / `-md` / `-lg`
  - 새 컴포넌트 추가 시 위 변수 우선 사용 (직접 색·radius 박는 것 금지)
- **참조 자산** (저장소 내 사본, 2026-06-25 youth-moa 에서 mirror):
  - `docs/00_assets/prototype.html` — 사용자 화면 prototype (최우선 디자인 기준)
  - `docs/00_assets/prototype.tsx` — React 원본 소스 (Thymeleaf 재구성 시 컴포넌트 구조 참조용)
  - `docs/00_assets/admin/prototype.html` — 관리자 prototype
  - `docs/00_assets/HANDOFF.md` — 디자인 스펙
  - `docs/00_assets/wireframe.png` — 와이어프레임
  - `docs/00_assets/assets/` — 로고·SNS 아이콘·배너 등 원본 PNG
- **실제 서빙 이미지**: `src/main/resources/static/images/` (logo_*, sns_*, banner_*) → `/images/*` 로 접근

### 디자인 변경 시 체크리스트
CSS 변수·레이아웃·스타일을 변경하는 모든 작업은 아래 항목을 prototype.html 해당 섹션과 직접 대조합니다.

1. 컨테이너 크기·패딩·gap 수치 일치
2. flex / grid 레이아웃 속성 일치 (Thymeleaf 클래스와 인라인 스타일 충돌 없음)
3. CSS 변수(`--color-*`, `--shadow-*` 등) 가 의도한 값으로 렌더링
4. 이미지 크기·`object-fit` 사용 방식과 부모 컨테이너 `position` 일치

---

## 협업·작업 규칙

1. **개념 설명 우선** — 새 기술·패턴 등장 시 "무엇인지 + Java/Spring 다른 영역과의 비교" 를 먼저 설명.
2. **장단점 명시** — 기술 선택지가 있을 때 비교표로 제시.
3. **왜 이 방법인지 설명** — 코드/구조 제안 시 이유를 함께 제시. "이게 표준이라서" 만 쓰지 않음.
4. **단계적 진행** — 한 번에 큰 변경 대신 작은 단위로 끊어 검증 (compile → test → 다음 단계).
5. **기획서 일관성** — 화면/기능 규격은 prototype.html 우선. 충돌 시 사용자에게 고지.
6. **존댓말 + 단계별 설명** — 응답 톤은 존댓말, 무엇을·왜·어디에 변경하는지 단계별로 풀어 설명.

---

## 실행 환경 분리 규칙

### 회사 PC (현재 PC)
- Docker 없음 → **Testcontainers 테스트 실행 불가**
- 매핑 검증은 `JpaMappingTest` (H2 기반 `@DataJpaTest`) 로만 진행
- E2E / Playwright / Selenium 류 실행 불가 → 필요 시 메모리 "개인 PC 확인 필요" 섹션에 기록

### 개인 PC (Mac, 추후 작업 환경)
- Docker 사용 가능 → Testcontainers + 실 PostgreSQL 통합 테스트 실행
- 누적된 미실행 TC 일괄 실행 후 결과를 메모리에 기록

> **원칙**: 회사 PC 에서 검증할 수 없는 항목은 절대 "통과로 처리" 하지 않는다. "정적 검증만 통과 / 동적 검증 보류" 로 명시한다.

---

## 빌드 / 테스트 명령

```powershell
# JDK 17 부트스트랩 (Foojay 가 JDK 21 자동 다운로드)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"

# 컴파일만 확인 (~7초)
.\gradlew.bat compileJava

# JPA 매핑 검증 (H2, ~20초)
.\gradlew.bat test --tests JpaMappingTest

# 단일 테스트 클래스 실행
.\gradlew.bat test --tests ProgramSearchTest

# 개발 서버 실행 (port 8080)
.\gradlew.bat bootRun
```

Mac:
```bash
sdk use java 21-tem  # 또는 17
./gradlew bootRun
```

---

## 검증 규칙 (필수)

> **배경**: 2026-06-26 까지 4 PR 동안 `application.yml` 의 `static-path-pattern: /static/**` 잘못된 설정으로 모든 정적 리소스 (CSS/JS/IMG) 가 302 redirect 되어 화면 전체가 깨진 상태로 누적됨. 단 한 번도 직접 페이지를 열어 보지 않아 발견 못 함. 이 사고를 반복하지 않기 위한 규칙.

### 정적 검증 — 매 작업 필수

- `./gradlew compileJava test --tests <class>` 통과
- 보고 시 **"정적 검증 ✅"** 으로 명시

### 동적 검증 — 화면 변경 포함 작업은 매 PR 필수

**다음 중 하나라도 변경되면 commit 전 반드시 직접 동적 검증한다:**
- Thymeleaf 템플릿 (`.html`)
- CSS (`main.css` 등)
- 정적 리소스 (`static/**`)
- Controller 의 view 이름 / 모델 변수 변경
- SecurityConfig 의 URL 패턴 변경
- `application.yml` 의 `spring.mvc` / `spring.web` / `spring.thymeleaf` 설정 변경

**검증 절차:**

1. 사용자에게 bootRun 상태 확인 (안 떠 있으면 띄워 달라 요청)
2. **페이지 응답 코드 확인**
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/<path>
   ```
3. **정적 리소스 응답 코드 확인** — 모두 200 OK 여야 함
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/css/main.css
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/images/logo_symbol.png
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/webjars/htmx.org/dist/htmx.min.js
   ```
4. **HTML 마크업 검증** — 의도한 fragment / class / 컴포넌트가 렌더링됐는지
   ```bash
   curl -s http://localhost:8080/<path> | grep -A 10 "<특정 클래스 또는 마크업>"
   ```
5. **Thymeleaf 표현식 잔존 여부 확인** — `${...}`, `th:*` 가 응답 HTML 에 남아 있으면 렌더 실패
6. 모두 이상 없을 때만 보고에 **"동적 검증 ✅"** 명시 후 commit 진행

**금지 패턴:**

- ❌ "동적 검증 ⏸ 사용자 확인 필요" 떠넘기기 (예외: 시각 디테일·인터랙션·반응형 등 curl 로 검증 불가한 항목만 명시적으로 분리)
- ❌ 정적 검증만 통과하고 commit / PR / 머지 후 동적 검증
- ❌ 누적 미검증 PR 만들기 (1 PR 단위로 즉시 동적 검증, 그렇지 않으면 누적 사고)

### 시각 검증 — 사용자 확인 영역

- 색감·폰트·반응형·인터랙션·HTMX 동작 같은 시각·동작 요소는 사용자 브라우저 확인
- 이 경우에도 **정적/동적(curl) 검증은 Claude 가 선행** — "다 깨졌는데 왜 검증 안 함?" 사고 재발 방지
- 사용자에게 시각 확인 요청 시 정확한 URL + 확인 포인트 명시

### 결과 보고 시 분리 표기

PR 본문·커밋 본문·메시지 모두 정적·동적·시각 검증 범위를 분리해서 표기:

```
## 정적 검증
- compile + N 클래스 / M TC PASS ✅

## 동적 검증
- GET /<path> 200 OK + 정적 리소스 200 OK ✅
- HTML 에 <expected markup> 정상 렌더 확인 ✅

## 시각 확인 (사용자 영역)
- 브라우저에서 ⟨A⟩ 토글 시 ⟨B⟩ 동작
```

---

## Git 브랜치·커밋 컨벤션

### 전략: GitHub Flow 변형 (1인 학습 프로젝트 맞춤)

- `main` — **항상 빌드 + 테스트 통과 상태 유지**. 직접 push 가능하지만 PR 경로 권장
- `feature/*`, `fix/*`, `chore/*`, `refactor/*`, `docs/*` — short-lived branch
- 작업 완료 시 self-PR → squash merge → branch 삭제

### 브랜치 prefix (5종)

| prefix | 용도 |
|---|---|
| `feature/` | 새 기능 (학습 단계 D1~D5 등) |
| `fix/` | 버그 수정 |
| `chore/` | 설정·인프라·DB·CI |
| `refactor/` | 코드 정리 (동작 변경 없음) |
| `docs/` | 문서만 변경 (CLAUDE.md, README, memory 동기화 등) |

이름 패턴: `<prefix>/<단계코드 또는 영문 식별자>-<짧은 영문 설명>`

예시:
- `feature/D1-application-form` — 신청 폼 (학습 단계 D1)
- `feature/D4-search-bar` — 헤더 검색 (학습 단계 D4)
- `chore/supabase-setup` — Supabase 연결 셋업
- `refactor/design-tokens` — 디자인 토큰 정리
- `docs/git-conventions` — Git 컨벤션 문서화

### 커밋 메시지 형식

```
YYMMDD_<식별자> - 한 줄 요약

- 항목 1
- 항목 2
- 항목 3

Co-Authored-By: Claude <noreply@anthropic.com>
```

식별자 예: `apply_form`, `bookmark_toggle`, `supabase_setup`, `token_cleanup`, `initial`
요약은 화면에서 보이는 변화 위주 (라벨 변경·필드 추가·노출 조건 등)

### 표준 워크플로우 (3단계)

**① 시작**
```powershell
git checkout main
git pull origin main
git checkout -b feature/D1-application-form
```

**② 진행** (작은 단위로 commit + push, PC 이동 직전엔 무조건 push)
```powershell
git add <명시 파일들>
git commit -m "260626_apply_form_entity - ApplicationService 1차 구조"
git push -u origin feature/D1-application-form   # 첫 push 시 -u
git push                                          # 이후
```

**③ 완료** (self-PR → squash merge → 정리)
```powershell
gh pr create --base main --title "D1: 신청 폼" --body "..."
gh pr merge --squash --delete-branch
git checkout main
git pull
```

### Squash vs Merge 선택 기준

- **`--squash` (권장)**: 학습 단계 1개 = 커밋 1개. main 히스토리 깔끔
- **`--merge --no-ff`**: 중간 WIP 커밋까지 보존하고 싶을 때

### 듀얼 PC (Win + Mac) 동기화

- ✅ **이동 직전 반드시 push** — WIP commit 이어도 OK
- ✅ **이동 직후 반드시** `git fetch origin && git checkout <branch>`
- ❌ **두 PC 에서 동시에 같은 브랜치 작업 금지** — push/pull 충돌 위험

### 스테이징 규칙

- ❌ `git add -A` / `git add .` **사용 금지** — 우발적 시크릿·임시파일 커밋 차단
- ✅ 명시적 파일/디렉토리만 add (`git add src/ docs/ build.gradle.kts` 등)
- ✅ 커밋 전 `git diff --cached` 로 변경 내역 직접 검토
- ✅ 시크릿 (`DATABASE_URL`, `password`, `API_KEY` 등) 패턴 한 번 더 검색

### main 보호 정책

- 현재: GitHub branch protection **미설정** (솔로 dev 마찰 회피)
- 추후 CI 도입 시: `gradlew test` status check 필수화 검토

---

## 패키지 구조 (도메인 중심)

```
io.github.sihyuuun.youthmoa/
├── YouthMoaApplication.java
├── common/
│   ├── BaseTimeEntity.java
│   ├── DataInitializer.java
│   └── config/{JpaConfig, SecurityConfig}.java
├── user/        (User, UserRole, UserPrincipal, UserService, UserController, SignUpRequest, UserRepository)
├── center/      (Center, CenterRepository)
├── program/     (Program, ProgramStatus, ProgramSpec, ProgramService, ProgramController, ProgramRepository)
├── application/ (Application, ApplicationStatus, ApplicationRepository)
├── bookmark/    (Bookmark, BookmarkRepository)
├── notice/      (Notice, NoticeRepository)
├── notification/(Notification, NotificationType, NotificationRepository)
└── web/HomeController.java
```

도메인별 폴더 안에 Entity / Repository / Service / Controller / DTO 를 함께 둡니다. 계층(Controller/Service/Repository) 패키지 분리는 **하지 않습니다** — 도메인 응집을 우선합니다.

---

## 엔티티 작성 규칙 (Lombok + JPA)

- **PK**: `Long` + `@GeneratedValue(IDENTITY)` (PG serial)
- **Lombok**: `@Getter` + `@NoArgsConstructor(PROTECTED)` + `@Builder` private 생성자
- **`@Setter` 금지** — 도메인 메서드(`approve()`, `cancel()` 등) 로 상태 변경
- **관계**: 모두 단방향 `@ManyToOne(LAZY)`, **양방향 `@OneToMany` 컬렉션 없음**
- **enum**: 모두 `@Enumerated(STRING)`
- **테이블명**: PostgreSQL 예약어 회피 (`user` → `users` 등)
- **Auditing**: `BaseTimeEntity` 상속 vs `@EntityListeners(AuditingEntityListener.class)` 직접 부착 — 필드 1개만 필요한 경우 후자 사용

---

## DB / 마이그레이션 (현재 학습 단계)

- `ddl-auto: create-drop` 사용 — 기동 시 스키마 재생성, `DataInitializer` 가 시드
- **Flyway / Liquibase 미도입** — 학습 진척에 따라 이후 도입 예정
- 도입 시점에는 별도 SKILL (`/db-migrate`) 추가하고 본 규칙도 갱신

---

## Spring Boot 4.x 주의사항

Boot 4 는 패키지를 모듈화하면서 다수 starter 의 클래스 경로가 이동했습니다. IDE auto-import 가 구 경로(Boot 3.x) 를 잡으면 수동 수정 필요.

| 어노테이션 | Boot 3.x | Boot 4.x |
|---|---|---|
| `@DataJpaTest` | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `@AutoConfigureTestDatabase` | `...test.autoconfigure.jdbc` | `org.springframework.boot.jdbc.test.autoconfigure` |

Spring Security 7 변경:
- `AntPathRequestMatcher` 제거 → POST 폼 기본 사용 또는 신 matcher 적용

---

## Thymeleaf / Spring Form 주의사항

이 프로젝트에서 실제로 사고 났던 패턴 모음. 새 화면 작업 전 일독 권장.

### `th:field` 의 type=password 동작
`<input type="password" th:field="*{password}">` 는 **보안상 value 를 강제로 빈 문자열로 출력**. 검증 실패 후 폼 재표시 시 비밀번호 입력값이 사라지는 사고 발생.

```html
<!-- ❌ 검증 실패 후 value 가 비어짐 -->
<input type="password" th:field="*{password}">

<!-- ✅ name + th:value 수동 명시 → 값 보존 -->
<input type="password" id="password" name="password" th:value="*{password}">
```

`th:errors` 는 form 의 `th:object` 만 있으면 그대로 동작하므로 위 변경은 에러 표시에 영향 없음.

### `th:field` 가 checkbox id 를 자동 변경
`<input type="checkbox" th:field="*{termsAgreed}">` 의 실제 id 는 `termsAgreed1` 처럼 숫자 접미사가 붙음.
JS 에서 같은 폼 내 체크박스 제어 시 `getElementById('termsAgreed')` 는 null. **항상 name 으로 querySelector 사용**.

```javascript
// ❌ null
document.getElementById('termsAgreed')

// ✅
document.querySelector('input[type="checkbox"][name="termsAgreed"]')
```

### 정적 리소스 `static-path-pattern` 함정
`application.yml` 의 `spring.mvc.static-path-pattern: /static/**` 같은 설정을 두면 모든 정적 리소스가 `/static/` prefix 가 없는 한 404 → SecurityConfig 가 매칭 못 함 → 302 redirect → 화면 전체 깨짐.

해당 설정은 **두지 않는다**. `/css/**`, `/images/**`, `/webjars/**`, `/favicon.ico` 가 자동으로 서빙되는 기본 동작 유지.

### Thymeleaf cache + bootRun 의 sourceResources
- `application.yml`: `spring.thymeleaf.cache: false` 기본 적용 (개발 시 즉시 반영)
- `build.gradle.kts`: `bootRun { sourceResources(sourceSets["main"]) }` 적용 (2026-06-30 도입) → src/main/resources 가 classpath 에 직접 들어가 `.html` 변경 즉시 반영. `./gradlew processResources` 강제 실행 **불필요**.
- DevTools (`spring-boot-devtools` developmentOnly) 와 함께 작동 → Java 파일 변경 시 자동 restart.

### Form binding 의 boolean
hidden input 의 `value="true"` / `"false"` 를 Spring Form Binder 가 자동으로 boolean 으로 변환. JS 에서 `hidden.value = 'true'` 처럼 문자열로 set 해도 OK.

---

## 메시지 어조 통일 규칙 (안 B)

회원가입·신청 등 폼 검증 메시지는 **`~해야 합니다.`** 패턴으로 통일.

### 적용 형식
- 단일 조건 누락: `"X 는 Y 이어야 합니다."` / `"X 를 포함해야 합니다."`
- 다중 조건 누락: `"X 는 Y 이어야 하고, Z 해야 합니다."`
- 필수 입력 (NotBlank): `"X 를 입력해주세요."` (요청형 유지)
- 상태/사실 안내 (도메인 에러): `"이미 신청한 프로그램입니다."` 같은 서술형 유지

### 적용 위치
| 위치 | 예시 |
|---|---|
| `@Pattern(message=...)` | `"비밀번호는 영문과 숫자를 모두 포함해야 합니다."` |
| `@Size(message=...)` | `"비밀번호는 8자 이상이어야 합니다."` |
| `@AssertTrue(message=...)` | `"아이디 중복확인을 진행해주세요."` |
| 클라이언트 JS 동적 메시지 | 누락 조건 조립: `"비밀번호는 8자 이상이어야 하고, 영문을 포함해야 합니다."` |

### 통일 안 한 어조 (혼란 사례)
- ❌ `"비밀번호 조건: 8자 이상 · 숫자 포함 필요합니다."` ("필요합니다" 는 어디에도 안 맞음)
- ❌ `"~해주세요"` 가 검증/제약 메시지에 섞임 (요청형은 NotBlank 만)

---

## 검증 자산 — 에이전트 / 스킬

| 자산 | 경로 | 호출 |
|---|---|---|
| `ym-spec` 에이전트 | `~/.claude/agents/ym-spec.md` | 새 화면 작업 명세 산출 (prototype 3자산 비교) |
| `ym-impl` 에이전트 | `~/.claude/agents/ym-impl.md` | 명세 → 풀스택 구현 |
| `ym-qa` 에이전트 | `~/.claude/agents/ym-qa.md` | 단위 테스트 + 정적/동적/회귀 검증 |
| `/qa` Skill | (도입 예정) | ym-qa 의 표준 절차 일괄 실행 |
| `/prototype-check` Skill | (도입 예정) | prototype vs Thymeleaf 갭 정기 스캔 |

화면 작업 표준 사이클: **ym-spec → 사용자 컨펌 → ym-impl → ym-qa → 머지**.

---

## 미완성 / 다음 작업

메모리 `project_youth_moa_java.md` 의 "다음 작업 후보" 섹션을 우선 확인.
