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

### 파괴적 명령 체이닝 금지

**배경**: 2026-07-06 F0i 세션에서 `git checkout main && git reset --hard origin/main` 을 F0i 브랜치에서 실행. `checkout main` 이 미스테이시 파일로 실패했으나 `&&` 다음 명령 (reset --hard) 이 여전히 F0i 브랜치에서 실행되어 **로컬 F0i 커밋이 origin/main 으로 초기화**됨. 원격에 push 된 상태라 `git reset --hard <sha>` 로 복구 가능했지만, 다음 재발 방지 규칙:

- ❌ **파괴적 명령을 `&&` 체인으로 실행 금지**:
  - `git reset --hard`, `git clean -f`, `git checkout -- .`, `git branch -D` 등
  - 체인 앞 명령이 fail 하면 뒤 명령이 **의도하지 않은 브랜치·상태에서 실행**됨
- ✅ **각 파괴적 명령 이전에 상태 검증**:
  ```bash
  git status --short           # 현재 브랜치·미커밋 파일 확인
  git branch --show-current    # 현재 브랜치 이름 재확인
  git reset --hard origin/main # 그 다음에만 실행
  ```
- ✅ **체이닝이 필요하면 `set -e` 또는 명시적 실패 감지**:
  ```bash
  git checkout main && git reset --hard origin/main
  # 이 형태는 체크아웃 실패 시 reset 이 뒤 브랜치에서 실행됨
  # → 대신 아래 형태:
  git checkout main || { echo "checkout failed"; exit 1; }
  git reset --hard origin/main
  ```
- ✅ **파괴적 명령 실행 전 로컬 커밋 원격 존재 확인**: `git log --oneline @{u}..HEAD` 로 unpushed 커밋 있는지. 있으면 push or 우회
- 사고 발생 시 첫 조치: **reflog 조회** (`git reflog -20`) — 최근 HEAD 이동 이력에서 사고 이전 SHA 확인 → `git reset --hard <sha>` 로 복구

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

### 정적 리소스(CSS/JS/이미지) 는 sourceResources 로도 즉시 반영 안 됨 — 별도 조치 필요

**배경**: 2026-07-02 D1b 작업 중 `main.css` 수정이 bootRun 서버에 반영 안 됐고, `curl /css/main.css` 로 확인 시 옛 버전이 계속 서빙됨. Java·`.html` 은 즉시 반영되는데 CSS 만 안 됨.

**원인**: IntelliJ `bootRun` 은 classpath 상 `build/resources/main/**` 을 먼저 서빙. `sourceResources` 가 `src/main/resources` 를 추가해도 static 파일은 build 산출물이 우선 로드됨 (Java·template 은 hot-reload 경로가 별도이므로 무관). 즉 CSS·JS·이미지 변경 후에는 반드시 `build/` 를 갱신해야 함.

**대응 패턴**:

```powershell
# 1. CSS/JS/이미지 변경 후
.\gradlew.bat processResources

# 2. 이미 서버가 로드한 CSS 는 브라우저 캐시도 잡고 있음 → 캐시 무효화
#    (a) Playwright/curl: URL 뒤에 ?v=timestamp 붙이거나 link[href] 를 JS 로 교체
#    (b) 브라우저: Ctrl+Shift+R (강제 새로고침)
```

**감지 방법**:
- `curl http://localhost:8080/css/main.css | grep "<변경한 클래스명>"` 로 실제 서빙 CSS 확인 → 옛 내용이면 processResources 미수행 상태
- 시각 확인 전 반드시 서빙 CSS 실측 필수 (변경 안 됐는데 눈으로만 확인하면 사고 재발)

**자동화 상태 (2026-07-07)**:
- `bootRun` 은 Gradle 태스크 그래프상 이미 `classes → processResources` 에 의존하므로 **기동 시점** 산출물은 항상 최신 — 문제는 서버 실행 중 변경분만임.
- 실행 중 변경분 대응: `.claude/hooks/post-edit-css.sh.proposed` 에 static/** 수정 시 `build/resources/main` 으로 즉시 미러 복사하는 훅 확장안 준비됨 (검토 후 본 파일로 교체 시 활성화).

### Form binding 의 boolean
hidden input 의 `value="true"` / `"false"` 를 Spring Form Binder 가 자동으로 boolean 으로 변환. JS 에서 `hidden.value = 'true'` 처럼 문자열로 set 해도 OK.

### Thymeleaf 모델 attribute 이름 예약어 충돌

Thymeleaf/Spring MVC 는 `application` / `session` / `request` 같은 이름을 **ServletContext scope 예약어** 로 취급. 모델 attribute 를 이 이름으로 넣으면 shadowing 되어 우리 객체가 아닌 servletContext 가 resolve 되며, 필드가 없으니 모두 `null` 로 렌더됨 (에러 안 남 → 시각 확인 없으면 놓치기 쉬움).

**금지 이름**: `application`, `session`, `request`, `response`, `servletContext`, `param`

```java
// ❌ shadowing — ${application.id} → null, ${application.appliedAt} → null
model.addAttribute("application", application);

// ✅ 다른 이름 사용
model.addAttribute("myApplication", application);
// 또는 도메인 별칭
model.addAttribute("apply", application);
```

**감지 방법**: 시각 확인 시 특정 객체의 여러 필드가 일제히 `null` 로 출력되면 이름 충돌 의심. `${application}` 을 통째로 출력해 보면 ServletContext 객체가 찍힘.

**2026-07-02 D1b 사고**: 신청 완료 페이지에 `#Anull`, `신청일시 null` 출력. 원인은 `application` 이름 shadowing.

### `<sec:authentication>` 태그는 Spring Security 7 에서 리터럴 렌더됨 → `#authentication` 유틸리티 사용

Spring Boot 4.1 (Spring Security 7) + `thymeleaf-extras-springsecurity6` 조합에서 **element 형태의 `<sec:authentication property="..."/>` 태그는 Thymeleaf 가 처리 못 하고 HTML 리터럴로 출력됨**. 브라우저에서 unknown element 로 무시되어 빈 텍스트로 보임.

```html
<!-- ❌ 리터럴로 렌더됨 (렌더 결과에 <sec:authentication .../> 이 그대로 남음) -->
<span class="header-user-name">
    <sec:authentication property="principal.displayName"/>님
</span>

<!-- ✅ Thymeleaf 표현식 유틸리티 사용 -->
<span class="header-user-name"
      th:text="|${#authentication.principal.displayName}님|">이름님</span>
```

- attribute 형태의 `sec:authorize="isAuthenticated()"` 는 **정상 동작**. element 형태만 문제.
- `#authentication` 유틸리티는 정상 → `${#authentication.principal.<field>}` 조합 안전.
- 감지 방법: `curl /` 응답에 `<sec:authentication`이 grep 되면 문제. 정상이면 그런 문자열 없음.
- **2026-07-03 E2E 대량 실패 사고**: 헤더 사용자 이름이 `. header-user-name` 안에서 whitespace + "님" 만 렌더됨 → login/header-nav spec 3개 실패.

### HTMX 프래그먼트 재렌더 시 스타일 파라미터 왕복 (`hx-vals` 패턴)

HTMX `outerHTML` swap 으로 부분 렌더할 때, 프래그먼트가 **자신을 렌더한 컨텍스트를 다시 필요로 하면** (예: card 인지 detail 인지 구분하는 `styleClass`) 그 값을 서버가 알 방법이 없다. HTTP 요청은 stateless 이므로 클라이언트가 `hx-vals` 로 되돌려주는 패턴을 사용한다.

```html
<!-- ❌ 최초 render 는 되지만 outerHTML 응답에서 styleClass 가 null -->
<button th:hx-post="@{/toggle}" hx-swap="outerHTML"
        th:class="${styleClass + ' bookmark-btn'}">☆</button>

<!-- ✅ hx-vals 로 styleClass 왕복 -->
<button th:hx-post="@{/toggle}" hx-swap="outerHTML"
        th:attr="hx-vals=|{&quot;styleClass&quot;:&quot;${styleClass}&quot;}|"
        th:class="${styleClass + ' bookmark-btn'}">☆</button>
```

컨트롤러에서 `@RequestParam` 으로 받아 model 에 다시 넣는다. 누락하면 렌더 결과가 `class="null bookmark-btn ..."` 처럼 나와 카드/상세 스타일이 무너진다.

- **2026-07-03 사고**: `BookmarkController.toggle()` 이 model 에 `styleClass` 를 안 넣어 HTMX 응답이 `class="null bookmark-btn is-bookmarked"` → bookmark spec 3개 실패.
- 검증: `curl -X POST /bookmarks/programs/{id}/toggle` 응답의 `class="..."` 확인.

---

## JPA / PostgreSQL 주의사항

이 프로젝트에서 실제로 사고 났던 패턴. 새 엔티티·화면 작업 전 일독 권장.

### `@Lob` + `open-in-view: false` — LOB streaming 오류
`application.yml` 의 `spring.jpa.open-in-view: false` (현재 설정) 환경에서 `@Lob` 필드(예: `Program.content`, `Program.requirements`) 를 컨트롤러 반환 이후 템플릿에서 읽거나, 트랜잭션 밖에서 접근하면 다음 예외 발생:

```
org.postgresql.util.PSQLException: Large Objects may not be used in auto-commit mode.
```

**원인**: PostgreSQL 은 CLOB 을 `LargeObjectManager` 로 streaming 하며, streaming 은 트랜잭션 안에서만 가능. auto-commit 모드에서는 큰 객체 스트림을 열 수 없음.

**해결 패턴 (택 1)**:
```java
// A. Controller 메서드에 read-only 트랜잭션 부착 (권장 — 스코프 최소)
@GetMapping("/apply/complete")
@Transactional(readOnly = true)
public String complete(...) { ... }

// B. Service 로 옮기고 서비스 메서드에 @Transactional 부착
```

**어떻게 감지되는가**: `@WebMvcTest` 는 실 DB 를 안 쓰므로 이 사고를 못 잡음. **화면 변경 PR 은 curl 동적 검증 필수**.

### `@ManyToOne(LAZY)` + 템플릿 접근 → `LazyInitializationException`
`open-in-view: false` 상태에서 컨트롤러가 엔티티를 반환하고 템플릿에서 lazy 연관을 접근하면:

```
org.hibernate.LazyInitializationException: Could not initialize proxy [X] - no session
```

**해결 패턴 (권장)**: Repository 메서드에 `@EntityGraph` 로 fetch join.

```java
@EntityGraph(attributePaths = {"program", "user"})
Optional<Application> findWithProgramAndUserById(Long id);
```

컨트롤러에 `@Transactional(readOnly=true)` 만 부착해도 lazy 로딩은 되지만, 트랜잭션이 뷰 렌더 끝날 때까지 열려 있어야 하므로 커넥션 점유 시간이 길어짐. `@EntityGraph` 로 필요한 그래프만 로드하는 편이 성능·확장성 모두 유리.

**주의**: `@EntityGraph` 는 필요한 관계만 명시. 지나치게 많이 넣으면 카티션 곱 발생 → 별도 쿼리 필요.

### `@WebMvcTest` 는 실제 렌더링 하지 않음
`@WebMvcTest(Controller.class)` 는 view name / model attribute 만 검증. Thymeleaf 실제 파싱·EL 평가·엔티티 lazy 접근은 실행되지 않아 위 두 사고 유형 모두 통과함.

**대응**:
- 화면 변경 PR 은 **반드시** curl 동적 검증 (CLAUDE.md "검증 규칙" 재강조).
- 주요 렌더 경로는 `@SpringBootTest + MockMvc` 통합 렌더링 테스트 병행 검토 (후속 티켓 `chore/integration-test-render`).

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

## 확장성 원칙 (관리자 페이지 대비)

사용자 페이지 개발 후 **관리자 페이지 확장 예정**. 매 기획·개발에서 다음 원칙 적용.

### 하드코딩 금지 대상 (엔티티 + 시드 + Repository 관리)
아래 값은 admin 페이지에서 CRUD 가능하도록 처음부터 DB 관리:
- 지역 / 청년센터 리스트 (완료: `Region.isFeatured` / `Center.isFeatured` — F0f)
- 홈 Hero 배너 / 공간 안내 이미지 (F0e — `SiteImage` slot 기반)
- Notice / Program 데이터 (완료: 시드 + Repository)
- 프로그램 카테고리 (`Category` 엔티티 예정, 현재 코드 삭제 상태)
- 카드 정렬 default·홈 표시 지표 순서 등 화면 policy

### 하드코딩 OK
- CSS 토큰 (색·spacing·radius) — 디자인 시스템 일관성
- 라우팅 경로 (`/programs`, `/signup` 등)
- Bean Validation 메시지 (UX 라이팅)
- 백엔드 상수 (`@GroupSequence` 그룹명, remember-me key 등)

### 신규 기능 개발 체크리스트
- [ ] 사용자가 화면에서 바꿀 만한 값인가? → 엔티티 도입
- [ ] admin 페이지에서 편집 필요한가? → 시드 + Repository
- [ ] 향후 정책 변경 가능성 있는가? → 상수 대신 설정 파일 or DB
- [ ] 이미지·아이콘·문자열 리소스인가? → `SiteImage` 등 별도 리소스 엔티티

### 슬롯 기반 설계 패턴 (SiteImage 예시)
```
SiteImage(slot="HERO_BANNER", imageUrl="...", sortOrder=0, isActive=true)
SiteImage(slot="HOME_SPACE_1", imageUrl="...", sortOrder=1, ...)
```
→ Controller 는 `findBySlotAndIsActiveTrue(...)` 로 조회. admin 이 URL 만 교체하면 화면 반영.

---

## 검증 자산 — 에이전트 / 스킬

| 자산 | 경로 | 호출 |
|---|---|---|
| `ym-pm` 에이전트 | `.claude/agents/ym-pm.md` (repo) → fallback `~/.claude/agents/ym-pm.md` | PM Review — 화면·정책 검토 (read-only). 원격 루틴에서도 사용 |
| `ym-spec` 에이전트 | `~/.claude/agents/ym-spec.md` | 새 화면 작업 명세 산출 (prototype 3자산 비교) |
| `ym-impl` 에이전트 | `~/.claude/agents/ym-impl.md` | 명세 → 풀스택 구현 |
| `ym-qa` 에이전트 | `~/.claude/agents/ym-qa.md` | 단위 테스트 + 정적/동적/회귀 검증 |
| `/pm-review` Skill | `.claude/skills/pm-review/SKILL.md` | ym-pm 페르소나 단발성 슬래시 호출 |
| `/qa` Skill | `.claude/skills/qa/SKILL.md` | 정적·동적·E2E·시각 4영역 분리 리포트 |
| `/prototype-check` Skill | `.claude/skills/prototype-check/SKILL.md` | prototype vs Thymeleaf 갭 정기 스캔 |
| `/wrap-up` Skill | `.claude/skills/wrap-up/SKILL.md` | commit·push·PR·merge·pull·prune 자동 |
| `/memory-sync` Skill | `.claude/skills/memory-sync/SKILL.md` | git log + gh PR 기반 메모리 자동 갱신 |
| `/build-check` Skill | `.claude/skills/build-check/SKILL.md` | Gradle 빌드 + JPA 매핑 테스트 실행 |
| `/resume` Skill | `.claude/skills/resume/SKILL.md` | 세션 재개 시 메모리 읽고 다음 작업 우선순위 제시 |
| Claude Preview | `.claude/launch.json` | `preview_start(name: "youth-moa-e2e")` 로 bootRun 자동 기동 (H2+시드, 자격증명 불필요) 후 snapshot/inspect/console_logs/network 로 동적·시각 검증. 실 DB 필요 시 `youth-moa` 설정 (DATABASE_* 환경변수 필요) |

화면 작업 표준 사이클: **ym-spec → 사용자 컨펌 → ym-impl → ym-qa → 머지**.
선택 0단계 (사고): **ym-pm** — prototype·정책 검토, 대안 제시 후 ym-spec 인계.

에이전트 파일 우선순위: repo `.claude/agents/` 가 전역 `~/.claude/agents/` 보다 우선. 같은 이름이면 repo 판이 채택됨. 원격 루틴(CCR) 은 전역을 못 보므로 repo 판이 필수.

---

## 메모리 미러링 규칙

로컬 메모리 파일은 PC 별로 존재하고 원격 루틴(claude.ai routines) 이 접근할 수 없다. 원격 세션이 진행 상황을 파악하려면 repo 내 파일 미러가 필요.

- **원본** (로컬 전용): `~/.claude/projects/.../memory/project_youth_moa_java.md`
- **미러** (repo 공유): `docs/STATE.md` — 전문 복사

### 미러 시점
세션 wrap-up 시 (사용자가 "커밋", "정리", "wrap-up" 등 마무리 명시할 때) 최신 메모리를 `docs/STATE.md` 로 전문 복사. 세션 중간에는 미러하지 않음 (다중 세션 동시 작업 시 충돌 방지).

### 커밋 메시지 형식
```
YYMMDD_memory_mirror - STATE.md sync
```

### 원격 루틴에서 참조
루틴 프롬프트는 `docs/STATE.md` 만 신뢰. 로컬 메모리 파일 경로는 언급하지 않음 (원격에서 접근 불가).

### 이중 PC (Win ↔ Mac) 정책
- 각 PC 의 로컬 메모리는 개별 유지 (자동 동기화 없음)
- `docs/STATE.md` 만 git 을 통해 공유 — 다른 PC 는 pull 로 최신 상태 확보
- 상충 시 파일 상단 `> 마지막 갱신: YYYY-MM-DD` 최신 값이 정답

---

## Claude Code 기술 활용 제안 규칙

이 프로젝트는 **학습용** 이므로 Claude Code 의 기능을 적극 활용해 작업을 자동화·가속한다. 작업 중 아래 조건 감지 시 사용자에게 **근거·방안**을 함께 제안한다.

| 감지 조건 | 제안 기술 | 근거 |
|---|---|---|
| 동일 절차가 대화 중 3회 이상 반복 | **Skill** 신설 (`.claude/skills/<name>/SKILL.md`) | 슬래시로 재사용, 프롬프트 오염 방지 |
| 여러 파일·경로 병렬 분석 필요 | **Explore subagent** 또는 병렬 `Agent` 호출 | 메인 컨텍스트 보호. 결과만 요약해 회신 |
| 화면·정책 결정 필요 | `ym-pm` subagent 또는 `/pm-review` skill | PM 6관점 자동 적용 |
| 새 화면 명세 필요 | `ym-spec` subagent | prototype 3자산 비교 명세 자동 산출 |
| 명세 → 구현 | `ym-impl` subagent | 표준 사이클 유지 |
| QA 4영역 (정적·동적·회귀·시각) | `ym-qa` subagent 또는 `/qa` skill | 분리 표기 자동 |
| 로컬 반복 작업 (테스트·빌드) | `Bash run_in_background` + `Monitor` | 대기 시간 활용, 이벤트 알림 |
| CLAUDE.md 규칙 자동 강제 | **Hook** (`.claude/settings.json`) | Claude 실수 방어 |
| 응답 포맷 반복 지시 | **Output style** (`.claude/output-styles/`) | 프롬프트 길이 감축 |
| 매일·주기적 리포트 필요 | **Remote routine** (`docs/routines/*.md` + `RemoteTrigger`) | 무인 실행, GitHub 연동 자동 인증 |
| CI 로 처리 가능한 검증 | **GitHub Actions workflow** (`.github/workflows/`) | 로컬 부담 완화 |
| 세션 종료·미커밋 상태 | `/wrap-up` skill | 정적/E2E 검증 → 명시적 stage → PR |
| 메모리 갱신 | `/memory-sync` skill | main 로그·PR·큐 자동 종합 |
| 세션 재개 | `/resume` skill | 진행 현황 요약 자동 |

### 적용 방식

1. 사용자가 요청한 직접 작업을 **먼저 수행** (도구 제안 우선 X)
2. 완료 응답 마지막에 `💡 다음에 더 빠르게` 별도 섹션으로 개선안 1~3개 제시
3. 각 제안에 **근거 · 예상 이득 · 도입 방법** 명시
4. **사용자 승인 없이 도입 금지** — 새 스킬·훅·워크플로우 파일 생성 전 반드시 컨펌

### 예시

```
[작업 완료 응답 후]

💡 다음에 더 빠르게

1. **`/build-check` 후 자동 test**
   - 근거: 최근 5회 대화에서 build-check 후 항상 test 를 이어서 실행함
   - 방안: `.claude/hooks/post-build-check.sh` 로 자동 chaining
   - 이득: 매 반복당 30초 절약

2. **prototype gap 리포트 자동화**
   - 근거: 매주 수동 갱신 관찰됨
   - 방안: 이미 등록된 `prototype-gap.yml` workflow 재확인. 수동 실행 필요 시 `workflow_dispatch`
```

---

## 미완성 / 다음 작업

메모리 `project_youth_moa_java.md` 또는 repo 미러 `docs/STATE.md` 의 "다음 작업 후보" 섹션을 우선 확인.
