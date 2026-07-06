---
name: youth-moa-java 신규 프로젝트 (Spring Boot 4 + Java 21 + Thymeleaf)
description: 기존 youth-moa(Next.js)를 Java 풀스택으로 재작성하는 학습/전환 프로젝트. 진행 상황·DB·다음 작업·열린 PR 메모
type: project
originSessionId: 51da8e75-f7a2-4b05-b2b6-963ce41efb6a
---

> **마지막 갱신**: 2026-07-06.
> 프로젝트 루트의 `CLAUDE.md` 가 디자인 시스템·작업 규칙·검증 규칙·환경 분리·패키지 구조·엔티티 규칙·Git 컨벤션 등 코드 작업 컨텍스트의 최우선 가이드라인. 본 메모리는 **시점적 진행 상태·DB·열린 PR·다음 액션** 중심으로 유지.

---

## 기본 정보

- **GitHub**: https://github.com/sihyuuun/youth-moa-java (PRIVATE, branch: `main`)
- **경로**: `C:\Users\User\IdeaProjects\youth-moa-java` / Mac: `~/IdeaProjects/youth-moa-java`
- **개발 환경**: Windows + macOS 듀얼
- **패키지**: `io.github.sihyuuun.youthmoa`
- **포트**: 8080
- **Spring Boot 4.1.0 / Spring Security 7.1.0 / Java 21 (Foojay)**

### Mac 첫 clone + 즉시 검증

```bash
cd ~/IdeaProjects
gh repo clone sihyuuun/youth-moa-java
cd youth-moa-java
sdk use java 21-tem   # 또는 17

# 환경변수 설정 (개인 PC IntelliJ Run Config 또는 export)
export DATABASE_URL="jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres"
export DATABASE_USERNAME="postgres.jlurjcmwlmcwaucxohkk"
export DATABASE_PASSWORD="<NEW_PASSWORD — Supabase 콘솔에서 재설정 후 값>"

# 정적 검증
./gradlew compileJava test --tests JpaMappingTest --tests ProgramSearchTest --tests ProgramServiceTest --tests ApplicationServiceTest --tests BookmarkServiceTest

# 부팅
./gradlew bootRun
# → http://localhost:8080
```

### git push 실패 시 (workflow 스코프)
```bash
gh auth setup-git
```

---

## 🟢 2026-07-06 세션 진행 완료 (7 PR 머지 예정 — F0g E2E · 통합 렌더 테스트 · F0e-2 · F0i · F2)

### 머지된/진행 PR

| # | PR | 성격 | 핵심 |
|---|---|---|---|
| #54 | 공지사항 E2E spec (7 TC) | test | HTMX 탭 UX 이슈 발견 → 별도 fix |
| #56 | 실 렌더 통합 테스트 도입 | chore | `PageRenderIntegrationTest` 6 TC (WebMvcTest 사각지대 커버) |
| #57 | HTMX 공지 탭 active state fix | fix | wrapper 확장 (`#notice-content`) |
| #58 | F0e-2 Hero 배경 6종 크로스페이드 | 기능 | 8초 로테이션, `prefers-reduced-motion` 대응 |
| #59 | F0i 아이디/비번 찾기 | 기능 | Stepper·본인 확인 후 즉시 재설정 (SMTP 무의존) |
| #60 | F2 헤더 알림 종 + 드롭다운 | 기능 | `@ControllerAdvice` 자동 주입 (CI 재실행 중) |
| — | Dependabot htmx 2.0.10 재제안 | — | close (memory 규칙) |

### 확립된 추가 규칙 (CLAUDE.md 및 memory 반영)
- HTMX `outerHTML` swap wrapper 는 트리거의 시각 상태 변경 요소 모두 포함해야 (탭 active 등)
- Thymeleaf HTML 주석 `<!-- ... -->` 은 응답에 유지됨 → 회귀 시그니처 문자열 담지 말 것. 개발 메모용은 파서 전용 `<!--/* ... */-->`
- `@ControllerAdvice` 도입 시 기존 WebMvcTest 슬라이스 모두 의존성 `@MockitoBean` 필요

### 확정 결정 반영
- **F0e-2**: SiteImage `slot` unique 제거 (동일 slot 다건), 초기 로드 시 6장 preload, 정적 이미지 (로테이션 후속 없음), Unsplash A fallback
- **F0i**: 이름+휴대폰 매칭, 본인 확인 후 즉시 재설정 (SMTP 무의존), email 앞 3자 마스킹, "계정 없음" 명시 + 200ms 지연
- **F2**: 종 위 dot only + 패널 헤더 숫자 뱃지, `/notifications` stub, 자동 발행 별도 F2b 티켓, HTMX post 로 자동 read

### 후속 백로그 (2026-07-06 세션 종료 시점 확정)
- **F2b**: Application 승인/반려 시 알림 자동 발행 (domain event)
- **F2c**: HTMX 30s 폴링 unread 갱신
- **F2d**: `/notifications` 전체보기 완전 구현 (페이지네이션·필터)
- **F2 tests**: `HeaderNotificationAdviceTest` / `NotificationControllerTest` / `NotificationRepositoryTest`
- **F0h**: HANDOFF §14 정식 이행 — 임시 비밀번호 SMTP 이메일 발송
- **E2E specs**: `find-account.spec.ts` (8 TC), `notification-bell.spec.ts`
- **F0h 청년센터 + 카카오맵** (unchanged)
- **`hard-reset` 사고 재발 방지 규칙 강화**: 체인 명령에서 `checkout` fail 시 이후 명령이 원래 브랜치에서 실행되는 위험 (오늘 F0i 로컬 커밋 소실 → 원격 복구)

---

## 🟢 2026-07-03 세션 진행 완료 (13 PR 머지 — QA 대약진)

### 머지된 PR

| # | PR | 성격 | 핵심 |
|---|---|---|---|
| #39 | D1b 신청 완료 페이지 + 알림 채널 도메인 | 기능 | `/apply/complete`, `NotificationChannel` enum, `NotificationChannelResolver` |
| #40 | E2E Top 5 커버리지 (login/bookmark/apply/detail/header) | test | 5개 spec 신설, 총 8 spec |
| #41 | ApplicationCompleteControllerTest mock 동기화 | fix | `findById` → `findWithProgramAndUserById` |
| #42 | Spotless 8 bulk reformat | chore | 61 파일 재포맷 (Spotless 7→8 dependabot 대응) |
| #43 | `/apply/complete` E2E + helpers.ts + webServer 자동 기동 | test | 신규 helpers.ts, Playwright config `webServer` 도입 |
| #44 | htmx 2.0.10 → 2.0.4 revert | fix | 대량 E2E 실패 원인 (bump 후 hang) |
| #45 | E2E 실 앱 버그 3종 fix | fix | ①`<sec:authentication>` 태그 리터럴 렌더 ②bookmark styleClass 소실 ③privacy 에러 문구 미노출 |
| #46 | CLAUDE.md 규칙 문서화 | docs | Thymeleaf sec 태그 + HTMX 프래그먼트 hx-vals 왕복 패턴 |
| #47 | ApplyRequest `@AssertTrue` field 이동 | fix | error field name `privacyAccepted` → `privacyAgreed` (템플릿 렌더 완결) |
| #48 | CodeQL workflow 제거 | chore | private + GHAS 미도입 → 모든 PR fail 노이즈 제거 |
| #49 | Top 5 spec 5개 helpers.ts 완전 사용 | refactor | 중복 -31 lines |
| #50 | prototype/HANDOFF 갱신 + HeroBanner 시안 | docs | F0g 카테고리 탭·페이지네이션·NoticeDetail 신설, Hero 재설계 시안 |

### 실행 중 (병렬 ym-impl 2개)
- **F0g 공지사항** (`feature/F0g-notices`) — Notice/NoticeAttachment 엔티티·서비스·컨트롤러·템플릿·시드·CSS
- **F0e-hero-refresh** (`feature/F0e-hero-refresh`) — index.html Hero 재작성, main.css .hero 계열, home.spec.ts 문안 갱신

### 확립된 규칙 (CLAUDE.md 반영)
- `<sec:authentication>` element 태그는 Spring Security 7 에서 리터럴 렌더됨 → `#authentication` 유틸리티 사용
- HTMX `outerHTML` swap 프래그먼트가 컨텍스트 파라미터 요구 시 `hx-vals` 로 왕복 전달
- 의존성 bump 후 반드시 E2E CI 확인 필수 (unit test 통과만 신뢰 금지)
- E2E CI 대량 실패 시 로컬 `bootRun --spring.profiles.active=e2e` + `curl` 로 실 마크업 먼저 확인 (spec 결함 vs 앱 버그 분간)
- `git reset --hard` 전 항상 사용자 미커밋 편집 확인 (`git status --short`)
- 병렬 세션용 git worktree 존재 → 브랜치 삭제·정리 전 반드시 `git worktree list`

### QA 대약진 요약
- Playwright spec 8개 → 사용자 여정 전면 커버
- 실 앱 버그 4종 (`<sec:>`, bookmark styleClass, privacy 에러 렌더, @AssertTrue field mismatch) 발견·수정
- E2E CI 안정화 (15m 타임아웃 → 2m 정상)
- CI 노이즈 제거 (CodeQL fail 제거) → 모든 check green 상태로 판단 명료

### F0g / F0e-hero-refresh 확정 결정 사항 (진행 중 ym-impl 이 반영)
- **F0g**: 카테고리 탭 5종 · 페이지네이션 (10건/페이지, 그룹 5개) · 첨부 NoticeAttachment 엔티티 · 조회수 매 진입 +1 · HTMX 부분 갱신 · 시드 8~12건
- **F0e-hero**: full-bleed · 좌측 정렬 · 기존 CTA 제거 · 2겹 scrim · 태그 칩 3개 (UI-only) · 배경 로테이션 미도입 (F0e-2 후속)

---

## 🟢 2026-06-30 세션 진행 완료 (4 PR 머지 + 인프라 구축)

- PR #16 (fix/signup-pw-message-and-retention) — 비밀번호 정책 어조 통일·입력값 보존·중복확인 강제
- PR #17 (chore/devtools-source-resources) — bootRun sourceResources → DevTools 즉시 반영
- PR #18 (docs/framework-notes) — CLAUDE.md 에 Thymeleaf · 메시지 어조 · 에이전트 인덱스
- PR #19 (chore/seed-housing-disable) — 시드 '주거' 카테고리 비활성화 (prototype 4종 정합)

### 인프라 신설
- **ym-spec / ym-impl / ym-qa 에이전트** (`~/.claude/agents/ym-*.md`) — prototype → 명세 → 구현 → 검증 라이프사이클
- **DevTools 즉시 반영** — `.html` / `.css` 변경 후 `processResources` 불필요
- **Docker 29.1.3 회사 PC 설치 확인** (메모리 정정 — 이전엔 "Docker 없음" 으로 잘못 기록)

### 사용자 결정 (2026-06-30)
- **prototype.html 절대 기준**. prototype.tsx 충돌 시 prototype.html 채택. wireframe.png 충돌 시 사용자 질문.
- **Q1**: 홈 카테고리 그리드 제거 → Quick Stats + 프로그램 4건 + 공지 + 공간 (F0e 작업)
- **Q2**: prototype 4종(취업/창업/힐링/교육) 정합. '주거' 시드 주석 처리 (살릴지 미정). WELCOME_CATS 7종은 회원 관심사용 (별개 체계)
- **Q3**: 상세 페이지엔 카테고리 뱃지 없음. 상태 뱃지만.
- **Q4**: 사이드바 체크박스 + 상단 FilterPopChip 둘 다 (목록 페이지)

### 발견된 기술 부채
- ⚠️ `/webjars/htmx.org/dist/htmx.min.js` 302 redirect — 경로 명시화 필요
- ⚠️ `YouthMoaApplicationTests` Testcontainers ServiceConnection 실패 — Docker daemon 확인 필요 (Docker 자체는 설치됨)

---

## 🟠 진행 중 브랜치 / 열린 PR

(현재 모두 머지 완료 — 진행 중 없음)

### `fix/signup-pw-message-and-retention` (2026-06-29 진행 중) — 시각 확인 대기

- **마지막 커밋**: `686d242` "260629_signup_validation - 비밀번호 정책 메시지 어조 통일 + 입력값 보존 + 아이디 중복확인 강제"
- **변경 파일 4개**:
  - `SignUpRequest.java` — `emailChecked` 필드 + `@AssertTrue("아이디 중복확인을 진행해주세요.")`, `@Pattern` 메시지에 "비밀번호는" 주어 추가
  - `UserController.java` — `GET /api/users/check-email?email=...` → `{available}` JSON
  - `SecurityConfig.java` — `/api/users/check-email` permitAll
  - `templates/user/signup.html` — password/passwordConfirm input `th:field` → `name + th:value` (입력값 보존), 정책 메시지 안 B 어조 (누락 조건별 동적 조립), 중복확인 버튼 fetch + emailChecked hidden + 이메일 변경 시 reset
- **이어서 진행**:
  1. **시각 확인** (사용자 브라우저): ① 정책 메시지 안 B 출력 ② 중복확인 alert 동작 ③ submit 검증 실패 시 비밀번호 input 값 보존 ④ 중복확인 안 누르고 submit → "아이디 중복확인을 진행해주세요." ⑤ 중복확인 후 이메일 변경 → 다시 안내 (reset)
  2. 통과 시 → push → self-PR → squash merge → branch 삭제
  3. **확인 사항**: `th:value` 로 비밀번호 평문이 HTML 응답에 포함되는 트레이드오프 — 응답에는 이미 `Cache-Control: no-cache, no-store` 가 있음. 학습 단계 OK 판단. 운영 시 재검토.
- **메시지 어조 결정 (안 B)**: "비밀번호는 ~해야 합니다" 패턴. 1개 누락 = `"비밀번호는 X 합니다."`, 2개 누락 = `"비밀번호는 X 하고, Y 합니다."`

### PR #7 — feature/D2-bookmark-toggle (시각 검증 대기)
- **상태**: push 됨, CI 진행 중일 수 있음, 머지 전
- **내용**: D2 즐겨찾기 토글 (상세 + 카드 ★ overlay) + CLAUDE.md 검증 규칙 섹션
- **머지 전 확인 필요**:
  - 시각 확인 (개인 PC 브라우저): 상세/카드 ★ 클릭 시 페이지 리로드 없이 토글, 비인증 시 /login 리다이렉트
  - CI 통과 (자동)
- **확인 후**: `gh pr merge 7 --squash --delete-branch`

### 자기참조 변수 이슈 (별도 fix 필요)
`main.css` line 19: `--color-text-tri: var(--color-text-tri);` (자기참조 → invalid → text-tri 색 작동 안 함). main 에 머지된 상태. 의도된 변경이라는 system reminder 있었지만, 실제로는 색이 unset 되어 버그.

→ 사용자에게 확인 받고 `fix/text-tri-token` 으로 정정 권장. 원래 값: `oklch(0.7 0.02 280)`

---

## DB 셋업 — ✅ 완료 (2026-06-26)

### Supabase 신규 프로젝트
- **이름**: `youth-moa-java`
- **Region**: Northeast Asia (Seoul) — `aws-1-ap-northeast-2`
- **Project ref**: `jlurjcmwlmcwaucxohkk`
- **Connection**: Session pooler (port 5432)
- **DATABASE_URL** (JDBC):
  ```
  jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres
  ```
- **DATABASE_USERNAME**: `postgres.jlurjcmwlmcwaucxohkk`
- **DATABASE_PASSWORD**: ⚠️ 2026-06-26 노출 사고로 재설정. 노트북 비밀번호 관리자에서 가져올 것. **Claude 에게 보내지 말 것.**

### 환경변수 설정 위치 (Windows 회사 PC)
- IntelliJ Run Config → `YouthMoaApplication` → Environment variables (3개 분리)

### 환경변수 설정 위치 (Mac 개인 PC)
- IntelliJ Run Config 또는 `~/.zshrc` / `~/.bash_profile` export

---

## 진행 상황 (2026-06-26 기준)

### main 머지 완료 (7 PR)
1. **PR #1** docs: GitHub Flow 변형 컨벤션 (`54c4c5a`)
2. **PR #2** refactor: WAITING 제거 (`c32df83`)
3. **PR #3** feature: D1 신청 폼 (`5f9c2c8`)
4. **PR #4** chore: GitHub Actions CI 도입 (`df744b6`)
5. **PR #5** fix: 정적 리소스 routing 버그 (`00ed051`) — `static-path-pattern: /static/**` 제거
6. **PR #6** feature: F0a 로그인 prototype 재작성 (`5bddb28`)
7. **initial** Spring Boot 4 + Java 21 + Thymeleaf 골격 (`f1d81cc`)

### push 됨 / 머지 대기
- **PR #7** feature: D2 즐겨찾기 토글 + CLAUDE 검증 규칙

### 인프라
- CI: GitHub Actions push/PR 자동 검증 (compile + 5 클래스 / 32 TC)
- DB: Supabase Session pooler 연결됨
- 정적 검증: 5 클래스 / 32 TC (JpaMappingTest 1 + ProgramSearch 7 + ProgramService 3 + Application 9 + Bookmark 12)
- 동적 검증: PR #5 + #6 머지 후 정적 리소스 200 OK 확인됨

---

## 🟡 다음 작업 큐 (2026-06-30 ym-spec 갭 분석 + 사용자 결정 반영)

| 순서 | 브랜치 후보 | 범위 | 결정 |
|---|---|---|---|
| ① | ✅ `feature/F0e-home-prototype` | 완료 (2026-07-01 머지) | done |
| ② | ✅ `feature/D5-card-capacity-bar` | 완료 (2026-07-01 머지) | done |
| ③ | ✅ `feature/D1b-apply-complete` | 완료 (2026-07-02 머지) | done |
| ④ | ✅ `feature/F0f-list-filter-redesign` | 완료 (F0f 시리즈, region/center featured 포함) | done |
| ⑤ | 🔄 **`feature/F0g-notices`** | ym-impl 진행 중 (2026-07-03) — 카테고리 탭·페이지네이션·상세 인라인 이미지·NoticeAttachment | 착수 |
| ⑤-2 | 🔄 **`feature/F0e-hero-refresh`** | ym-impl 진행 중 (2026-07-03) — Hero 재설계 (2겹 scrim·좌측 정렬·태그 칩·CTA 제거) | 착수 |
| ⑥ | `feature/F0h-centers` | 청년센터 목록 (카카오맵 SDK 연동) — 대기 | — |
| ⑦ | `fix/text-tri-token` | main.css 자기참조 변수 정정 (실제 위험도 재확인 필요) | 작은 fix |
| ⑧ | `fix/webjars-htmx-path` | /webjars/htmx 302 redirect — 사고 이후 확인, htmx 로드 정상이면 close | 재확인 |
| ⑨ | `chore/testcontainers-fix` | YouthMoaApplicationTests Docker daemon 연결 확인·수정 | — |
| ⑩ | `chore/integration-test-render` | 주요 렌더 경로에 `@SpringBootTest + MockMvc` 통합 렌더링 테스트 도입 | 사고 후 |
| ⑪ | `feature/F0e-hero-rotation` (F0e-2) | Hero 배경 8종 크로스페이드 로테이션 — F0e-hero-refresh 머지 후 | 후속 |
| ⑫ | `feature/D5-mypage` | 마이페이지 (신청 내역 / 즐겨찾기 탭 / 개인정보 수정) | — |
| ⑬ | `feature/D4-search-bar` | 헤더 검색바 + 결과 페이지 | — |

### 인프라 작업 (남은 단계)
- 4️⃣ `/qa` Skill 셋업
- 5️⃣ `/prototype-check` Skill 셋업
- 6️⃣ Flyway 도입
- 7️⃣ GitHub Actions 동적 검증 확장
- 8️⃣ P1 `/qa` 회귀 루프
- 9️⃣ P2 `/prototype-check` 갭 루프

### 🧪 E2E 커버리지 확장 (2026-07-02 사용자: QA 최우선 원칙)

이번 PR (`test/e2e-top5-coverage`) 로 5개 spec 추가:
- login, bookmark, apply, program-detail, header-nav (기존 home / program-list / signup 에 이어 총 8개 spec)

**남은 backlog** (memory `project_e2e_backlog.md` 참조):

| 시나리오 | 선행 조건 |
|---|---|
| `/apply/complete` 신청 완료 화면 | D1b 머지 후 |
| 공지사항 목록·상세 | F0g 구현 후 |
| 청년센터 + 카카오맵 | F0h 구현 후 |
| 마이페이지 (신청 내역/즐겨찾기/개인정보) | D5-mypage 구현 후 |
| 아이디·비밀번호 찾기 | F0a 후속 페이지 구현 후 |
| 검색바 + 결과 | D4-search-bar 구현 후 |
| 반응형 (모바일 뷰포트) | 우선순위 낮음 |

**즉시 확장 가능** (이미 구현됨, Top 5 에서 뺀 것):
- CapacityBar 색상 경계값 (89%/90%) — 카드 fragment 대상
- 회원가입 성공 후 로그인 흐름 (성공 path)
- 프로그램 목록 페이징·정렬 조합

**인프라 리팩터**:
- Playwright `webServer` 자동 기동
- `helpers.ts` 로 login / waitForHtmx 헬퍼 추출 (현재 spec 마다 중복)
- CI 병렬 실행 (DB 격리 확보 후)

### 더 옛 작업 큐 (참고)

| 순서 | 브랜치 | 범위 |
|---|---|---|
| (구) | `feature/F0c-apply-prototype` | 신청 폼 prototype 일부 흡수 (Q 결정에 따라 부분만 살아남음) |
| (구) | `feature/F1-home-restoration` | F0e 와 통합됨 |
| ⑤ | `feature/F2-header-enhancement` | 헤더 검색 아이콘·알림 종·아바타 드롭다운 |
| ⑥ | `feature/F3-card-enhancement` | 카드 capacity bar + CTA 버튼 |
| ⑦ | `feature/F4-detail-requirements-grid` | 상세 자격요건 4-grid (entity 확장 필요) |
| ⑧ | `feature/D3-notices` | 공지사항 목록·상세 |
| ⑨ | `feature/D4-search-bar` | 헤더 검색바 + 검색 결과 |
| ⑩ | `feature/D5-mypage` | 마이페이지 (신청 내역 / 즐겨찾기 탭 / 개인정보 수정) |

---

## prototype 일치도 (2026-06-26 기준)

PR #6 (F0a 로그인) + #7 (D2 카드 ★) 머지 후:

| 화면 | 일치도 | 잔여 갭 |
|---|---|---|
| 홈 `/` | 🟡 65% | Hero 검색바·통계·프로그램 4개·공지 (F1) |
| 프로그램 목록 `/programs` | 🟢 85% | capacity bar / CTA / 캘린더 (F3·F5) |
| 프로그램 상세 `/programs/{id}` | 🟢 90% | 자격요건 4-grid (F4) |
| 신청 폼 `/programs/{id}/apply` | 🔴 45% | 신청자 정보·개인정보 동의 (F0c) |
| **로그인** `/login` | 🟢 95% | find-id / find-password 페이지 (별도) |
| **회원가입** `/signup` | 🔴 — | F0b 작업 예정 (HANDOFF 5.7 기준) |
| 헤더 | 🟡 70% | 검색·알림·아바타 (F2) |
| 푸터 | 🟢 95% | — |

---

## 디자인 자산 참조 (저장소 내 사본)

- `docs/00_assets/prototype.html` — **최우선 디자인 기준**
- `docs/00_assets/HANDOFF.md` — 디자인 스펙
- `docs/00_assets/prototype.tsx` — React 원본
- `docs/00_assets/wireframe.png` — 와이어프레임
- `docs/00_assets/admin/` — 관리자 prototype
- `src/main/resources/static/images/` — 런타임 이미지 (logo / sns / banner)

---

## 회원가입 (F0b) 작업 명세 — HANDOFF 5.7

Mac 에서 F0b 시작 시 참고:

```
max-width 800px, 중앙 정렬, Header + Footer 있음

계정 정보 섹션 (2단 그리드: 라벨 140px + 필드)
├─ 아이디(이메일) + 중복확인 버튼
├─ 비밀번호
└─ 비밀번호 확인

개인 정보 섹션
├─ 이름
├─ 핸드폰 번호 + 인증요청 버튼
├─ 성별 (라디오) — 우리 User entity 에 없음. 미포함 or entity 확장
├─ 생년월일 (캘린더 피커)
└─ 주소 (우편번호 검색 + 주소 + 상세주소)

이용약관 동의 (체크박스 + 약관보기 링크)
취소 (L/ghost) + 회원가입 (L/primary)
```

**우리 User entity 매칭**:
- email (아이디) ✓
- password / passwordConfirm (DTO 만) ✓
- name, phone, zipcode, address, addressDetail, birthDate ✓
- gender — entity 에 없음 (작업 시 결정)
- interests — entity 에 있음, signup 시 미포함

---

## 신청 폼 (F0c) 작업 명세 — prototype ProgramApply (line 930~985)

```
max-width 700px

← 이전으로
"프로그램 신청" h2 (가운데 정렬, 26px / 700)

프로그램 요약 카드 (80x80 이미지 + 정보)

신청자 정보 섹션 (2px 텍스트 하단 보더)
├─ 이름 (readonly, 회색)
├─ 핸드폰 번호 (readonly, 회색)
└─ 이메일 (readonly, 회색 + 안내 메모)

추가 정보 섹션
└─ 지원 동기 (textarea 88px, focus border 색)

개인정보 수집 동의 섹션
├─ 동의 내용 박스 (border + maxHeight 130 스크롤)
└─ 라디오 체크: "동의합니다"

가운데 신청하기 버튼 (width 220)
Footer
```

→ 우리 User entity 의 name/phone/email 자동 채움.

---

## 듀얼 PC 동기화 규칙

- 이동 직전: 반드시 push (WIP commit OK)
- 이동 직후: `git fetch origin && git checkout <branch>`
- 두 PC 동시에 같은 브랜치 작업 금지
- 환경변수 (DATABASE_*) 는 각 PC 의 IntelliJ Run Config 에 별도 입력

---

## 사용 가능한 스킬 (`.claude/skills/`)

- `/build-check` — Gradle compile + 단위 테스트 결과 요약
- `/resume` — 본 메모리 읽고 진행 상황·다음 작업 우선순위 제시

---

## 개인 PC 시각 확인 항목

PR #7 머지 전 또는 그 후:

| # | 확인 항목 | 출처 |
|---|---|---|
| 1 | 카드 목록 ★ overlay 위치 (이미지 우상단) + 호버 transform | D2 |
| 2 | ★ 클릭 시 페이지 리로드 없이 amber 토글 | D2 (HTMX) |
| 3 | 비인증 ★ 클릭 시 /login 리다이렉트 | D2 |
| 4 | 상세 페이지 ★ 활성화 (큰 detail-action-icon) | D2 |
| 5 | 로그인 페이지 prototype 일치 (로고 이미지, 400px, "로그인" h2, 옵션 row, secondary 회원가입 버튼) | F0a |
| 6 | 푸터 fragment 정상 렌더 | F0a |
| 7 | text-tri 색 작동 여부 (자기참조 변수 이슈) | main.css fix 후 |
