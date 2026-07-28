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
| DB | PostgreSQL (개인 Supabase 신규 프로젝트, 학습 단계 `ddl-auto: update` — 2026-07-13 create-drop 에서 전환. e2e 프로파일은 자체 create-drop 유지) |
| ORM | Spring Data JPA / Hibernate |
| 보안 | Spring Security 7 + BCrypt |
| 테스트 | JUnit 5 + H2 (`@DataJpaTest`) + Testcontainers (양 PC + CI ubuntu 러너) |

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

### spec 산출 규칙 → `ym-spec` 에이전트 정의로 이관 (2026-07-28)

3자산 정독 · 데이터 모델 gap 표 · 데이터 소비 지점 · write→read 왕복 시나리오 규칙은 CLAUDE.md 소관이 아니라 **특정 에이전트의 작업 절차**이므로 `~/.claude/agents/ym-spec.md` 로 옮겼다. spec 산출 시에만 필요한 50줄이 상시 컨텍스트를 차지하고 있었다.

> 참고: `prototype.html` 과 `prototype.tsx` 는 **같은 소스**다 (단일 파일 React 앱, 컴포넌트 목록 차이 0건, `html 라인 = tsx 라인 + 35`). 둘을 "충돌 시 우선순위" 로 비교하는 절차는 실행할 일이 없다.

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
- **Docker 사용 가능** (2026-07-10 실증 — Docker Desktop 29.x, Testcontainers 포함 전체 테스트 로컬 통과). 이전 "Docker 없음" 기록은 폐기
  - 데몬이 안 떠 있으면 Docker Desktop 기동 후 `docker version` 으로 엔진 확인
  - 기동 crash (`remove ...sock: The file cannot be accessed by the system`) 시: stale AF_UNIX 소켓 이슈 — 조치법은 메모리 `reference_docker_stale_socket_fix.md` (부모 디렉토리 rename 격리, settings-store.json 은 BOM 없이 저장)
- 매핑 검증은 `JpaMappingTest` (H2) 우선, 통합 검증은 `YouthMoaApplicationTests` (Testcontainers) 로 가능
- CI `integration-test` job 이 ubuntu 러너(Docker 내장)에서 전체 테스트를 항상 실행하므로, 로컬 Docker 상태와 무관하게 매 PR 통합 검증됨
- **bootRun 자체는 가능** — `.claude/scripts/bootrun-e2e.cmd` 로 e2e 프로파일(H2 in-memory + 시드) + 포트 8090 기동. Supabase 자격증명 불필요. IntelliJ 8080 무충돌. **회사 PC 에서 curl 렌더 실측을 이 경로로 필수 수행**한다. (2026-07-07 도입, 2026-07-09 F0h-c2 사고 후 재강조)

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
- **화면 변경 시 `*RenderTest` 도 반드시 함께 실행** — `compileJava` + `JpaMappingTest` 만으로는 Thymeleaf 파싱·SpEL 평가·`th:fragment` 인라인 실행 이슈를 감지 못 함 (2026-07-09 F0h-c2 사고 회고)
- `/build-check` 스킬이 자동으로 `*RenderTest` 를 포함하도록 갱신됨
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

### prototype 시각 대조 → 디자인 계약이 대체 (2026-07-28)

기존에는 착수 전·PR 단계에서 prototype 을 사람이 육안 대조하도록 산문 규칙 2개를 두었다. 그 방식은 **7/22 에 규칙을 신설한 뒤에도 7/27 전수 스캔에서 갭 60건**이 나와 실패로 확인됐다. 기준이 매 세션 재해석되는 원문(2,733줄)이라 판단이 흔들리고, 육안 비교로는 정량값(`width 400 vs 460`, `font 34 vs 42`)을 잡을 수 없었다.

지금은 **디자인 계약**이 이 역할을 기계적으로 수행한다.

- 사용법·계약 현황: [docs/design-contracts/README.md](docs/design-contracts/README.md)
- 전 화면 공통 정책: [docs/design-contracts/POLICY.md](docs/design-contracts/POLICY.md)
- 실행: `cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts`

**화면 작업 규칙**

1. 계약이 있는 화면은 prototype 원문 대신 **계약 + 갭 리포트**를 읽는다
2. 완료 기준은 "해당 화면 갭 0" 이다. 기능 E2E(`--project=chromium`) green 유지도 함께 확인한다
3. 계약이 없는 화면은 작업과 함께 계약을 신설한다 (`home.ts` / `home.md` 패턴)
4. **구현이 계약과 다르다고 계약 기대값을 구현에 맞춰 고치지 말 것** — `deviation`(영구 이탈) 또는 `deferred`(이월, 담당 티켓 명시) 필드로 처리한다. 계약을 구현에 맞추면 장치 전체가 무의미해진다

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

`git reset --hard`, `git clean -f`, `git checkout -- .`, `git branch -D` 등은 **`&&` 체인으로 실행하지 않는다.** 앞 명령이 실패해도 뒤 명령이 의도하지 않은 브랜치·상태에서 실행된다.

- 실행 전 `git status --short` + `git branch --show-current` 로 현재 상태 확인
- unpushed 커밋 유무는 `git log --oneline @{u}..HEAD`
- 사고 시 첫 조치는 `git reflog -20` → 사고 이전 SHA 로 `git reset --hard <sha>`

→ 실제 사고 경위: [docs/postmortems/2026-07-06-destructive-git-chain.md](docs/postmortems/2026-07-06-destructive-git-chain.md)

### main 보호 정책

- **GitHub branch protection 사용 불가** (2026-07-10 확인 — Free 플랜 + private repo 는 API 403 "Upgrade to GitHub Pro or make public". secret scanning push protection 도 동일 제약)
- 대체 통제: ① self-PR 워크플로우 유지 (main 직접 push 지양) ② CI/Lint/E2E 워크플로우가 매 PR 실행 — **머지 전 check 전부 green 확인은 사람(및 /wrap-up)이 수행** ③ 시크릿은 lint.yml 의 gitleaks job 이 스캔
- repo 를 public 전환하거나 GitHub Pro 결제 시 required status check 활성화 (설정 JSON: 세션 스크래치 참조 — contexts: "Build + Test (H2)", "Gradle Check (compile + spotless)")

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

## DB / 마이그레이션 (Flyway — 2026-07-22 활성화)

스키마의 진리 소스는 `src/main/resources/db/migration/V*.sql`. 엔티티는 스키마를 만들지 않으며 `ddl-auto: validate` 가 매핑·스키마 일치만 검사한다 (불일치 시 부팅 실패 = 조기 감지).

### 스키마 변경 절차 (엔티티 수정 시 반드시 세트로)

1. 엔티티 변경 + `V<N>__<snake_case_설명>.sql` 을 **같은 PR** 에 작성
   - N = main 의 db/migration 최신 버전 + 1. 병렬 브랜치가 같은 N 을 선점했으면 rebase 후 번호 재부여
   - 예: `V2__add_program_apply_period.sql`, `V3__create_daily_visit.sql`
2. DDL 작성이 막히면 로컬 Docker PG 에 `JPA_DDL_AUTO=update FLYWAY_ENABLED=false` 로 1회 띄워 Hibernate 가 찍는 DDL 로그 참고
3. 검증: `YouthMoaApplicationTests` (Testcontainers) 가 빈 PG 에 V1..VN 적용 + validate 를 매 PR 자동 수행
4. **한 번 main 에 머지된 V 파일은 절대 수정 금지** — 적용된 환경에서 checksum mismatch 로 부팅 실패. 잘못됐으면 다음 번호의 새 V 파일로 정정
5. 뷰·함수 등 재적용 가능 객체는 `R__<이름>.sql` (repeatable)

### 프로파일별 스키마 소스

| 경로 | DB | 스키마 소스 |
|---|---|---|
| bootRun (local/prod) | Supabase PG | Flyway (`validate`) |
| Testcontainers 테스트 | PG 컨테이너 | Flyway (`validate`) — V 파일 실전 게이트 |
| e2e 프로파일 / @DataJpaTest | H2 | Hibernate `create-drop` (Flyway off — V 파일은 PG 전용) |

### 금지·주의

- ❌ `JPA_DDL_AUTO=update` 로 Supabase 스키마 변경 (Flyway 이력 밖의 drift 발생)
- ❌ Supabase SQL Editor 로 직접 DDL (동일 — 필요 시 V 파일로 작성 후 부팅으로 적용)
- 시드 데이터는 계속 `DataInitializer` (멱등 체크) 담당. 마이그레이션은 스키마 전용
- 활성화 이전 이력: `V1__baseline.sql` = 2026-07-22 시점 스냅샷, 기존 Supabase 는 baseline-version 1 로 스킵

---

## 프레임워크 함정 모음 → `docs/patterns/`

Boot 4 / Thymeleaf / JPA 에서 **이 프로젝트가 실제로 사고 낸 패턴**은 별도 문서로 분리했다 (2026-07-28). 상시 준수 규칙이 아니라 해당 영역을 건드릴 때 펼쳐 보는 자료라 상시 컨텍스트에서 덜어냈다.

| 문서 | 언제 읽나 |
|---|---|
| [patterns/thymeleaf-spring.md](docs/patterns/thymeleaf-spring.md) | 템플릿·폼·HTMX·`th:fragment` 작업 전 (**새 화면 작업 시 일독 권장**) |
| [patterns/jpa-postgres.md](docs/patterns/jpa-postgres.md) | 엔티티·연관관계·`@Lob`·컬렉션 작업 전 |
| [patterns/spring-boot-4.md](docs/patterns/spring-boot-4.md) | Boot 3.x 예제를 참고할 때 (패키지 경로가 이동했음) |

여기 있는 사고는 전부 재발 이력이 있다. `@WebMvcTest` 는 이 유형을 못 잡으므로 **화면 변경 PR 은 동적 검증이 필수**라는 결론만 기억한다.

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

### 파생 시드 금지 (2026-07-09 추가)

**배경 (2026-07-09 F0h 좌표 사고)**: `DataInitializer` 가 `regionCoords.put("양평군", {37.xxx, 127.xxx})` 로 시·군청 대표 좌표만 매핑하고, 모든 Center 의 lat/lng 를 `base + offset(idx*15)` 파생으로 채웠음. 결과:
- 양평군의 3개 센터(딴딴회관·내일스퀘어·오름) 가 모두 양평군청 근처 100m offset 지점에 렌더 → 카카오맵 클러스터가 "3" 으로 뭉침
- 실주소 데이터(`DataInitializer.java:663~665`)는 존재하나 좌표에 반영 안 됨
- 관리자가 좌표 편집해도 재기동 시 파생 로직이 덮어씀 → **admin CRUD 무력화**

**규칙**:
1. **엔티티 필드는 각 row 자체가 진리 소스** (single source of truth). 다른 필드나 컬렉션 규칙으로부터 파생 시드하지 말 것. 예: Center.lat/lng 는 Center row 자체에 개별 저장. Region 이나 시·군 대표 좌표 파생 금지
2. **시드 데이터는 자원 파일에서 로드** (`src/main/resources/data/*.csv` or `.yml`). Java 코드 내 Map 하드코딩은 소규모(≤ 10건) 데이터에 한함. 대규모 데이터는 자원 파일 분리
3. **파생이 필요하면 도메인 메서드** (예: `Center.isCurrentlyOpen(now)`) 로 런타임 계산. DB 저장값은 원천 필드만
4. **감지 방법**: DataInitializer 리뷰 시 `Map<String, ...>` 내부에 지역·타입 등 그룹 키가 있고, 이걸 forEach 로 각 row 에 파생 적용하는 패턴 발견 시 → 즉시 자원 파일 로드 방식으로 전환

**언제 발동**: 신규 엔티티 시드 작성 시. 기존 DataInitializer 리팩터 시.

### 관리자 CRUD 실효성 체크

관리자 페이지에서 편집 가능하도록 만든 필드가 실제 편집 결과가 유지되는지 검증:
- 재기동 시 시드가 덮어쓰지 않는가? (`ddl-auto: update` 로 전환된 이후 `existsByEmail` 등 idempotent 체크로 중복 방지 확인. Flyway 도입 후 최종 검증)
- 파생 로직이 편집값을 무력화하지 않는가? (좌표 사고 재발 방지)

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
| `ym-spec` 에이전트 | `~/.claude/agents/ym-spec.md` | 새 화면 작업 명세 산출. **계약이 있는 화면은 계약을 먼저 읽는다 (0단계)**. spec 산출 필수 규칙 4개도 이 파일에 있음 |
| `ym-impl` 에이전트 | `~/.claude/agents/ym-impl.md` | 명세 → 풀스택 구현 |
| `ym-qa` 에이전트 | `~/.claude/agents/ym-qa.md` | 단위 테스트 + 정적/동적/회귀 검증 |
| `ym-verify` 에이전트 | `~/.claude/agents/ym-verify.md` | 적대적 검증 (refute-first) — 커밋 전 최종 관문. spec 구현 매핑 행 단위 재대조, PASS/FAIL/UNVERIFIED 3단 판정 |
| `/pm-review` Skill | `.claude/skills/pm-review/SKILL.md` | ym-pm 페르소나 단발성 슬래시 호출 |
| `/qa` Skill | `.claude/skills/qa/SKILL.md` | 정적·동적·E2E·시각 4영역 분리 리포트 |
| **디자인 계약** | `docs/design-contracts/` + `e2e/contracts/` | prototype 대비 **정량 갭 자동 검사**. `npx playwright test --project=contracts` → 갭 리포트 `e2e/gap-reports/`. 화면 작업의 기준이자 완료 판정. 공통 정책은 `POLICY.md` |
| `/prototype-check` Skill | `.claude/skills/prototype-check/SKILL.md` | **계약이 없는 화면**의 갭 정기 스캔·계약 신설용. 계약이 있는 화면은 계약 검사가 대체 |
| `/wrap-up` Skill | `.claude/skills/wrap-up/SKILL.md` | commit·push·PR·merge·pull·prune 자동 |
| `/memory-sync` Skill | `.claude/skills/memory-sync/SKILL.md` | git log + gh PR 기반 메모리 자동 갱신 |
| `/build-check` Skill | `.claude/skills/build-check/SKILL.md` | Gradle 빌드 + JPA 매핑 테스트 실행 |
| `/resume` Skill | `.claude/skills/resume/SKILL.md` | 세션 재개 시 메모리 읽고 다음 작업 우선순위 제시 |
| Claude Preview | `.claude/launch.json` | `preview_start(name: "youth-moa-e2e")` 로 bootRun 자동 기동 (H2+시드, 자격증명 불필요) 후 snapshot/inspect/console_logs/network 로 동적·시각 검증. 실 DB 필요 시 `youth-moa` 설정 (DATABASE_* 환경변수 필요) |
| 확정 명세 큐 | `docs/specs/` | ym-spec 산출 + 사용자 결정 반영된 명세. `spec_confirmed` 상태면 ym-impl 이 바로 인계 가능. 병렬 실행 규칙은 `docs/specs/README.md` |

화면 작업 표준 사이클: **ym-spec → 사용자 컨펌 → ym-impl → ym-qa → ym-verify → 머지**.
선택 0단계 (사고): **ym-pm** — prototype·정책 검토, 대안 제시 후 ym-spec 인계.

**계약이 있는 화면은 사이클이 짧아진다**: 계약 + 갭 리포트가 spec 역할을 하므로 ym-spec 을 건너뛰고 구현 → 계약 검사 갭 0 확인으로 끝낼 수 있다. 계약이 없는 화면은 위 전체 사이클을 따르되 **계약을 함께 신설**한다.

참조 문서 지도:

| 찾는 것 | 위치 |
|---|---|
| 화면이 어떻게 생겨야 하는가 | `docs/design-contracts/<screen>.md` + `e2e/contracts/<screen>.ts` |
| 전 화면 공통 디자인 정책 | `docs/design-contracts/POLICY.md` |
| 프레임워크 함정 (Thymeleaf·JPA·Boot 4) | `docs/patterns/` |
| 과거 사고 경위 | `docs/postmortems/` |
| 아키텍처 결정 근거 | `docs/adr/` |
| 확정 명세 큐 | `docs/specs/` |
| 현재 진행 상황 | `docs/STATE.md` (메모리 미러) |

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
