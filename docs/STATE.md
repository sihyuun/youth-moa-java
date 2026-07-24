---
name: youth-moa-java 신규 프로젝트 (Spring Boot 4 + Java 21 + Thymeleaf)
description: 기존 youth-moa(Next.js)를 Java 풀스택으로 재작성하는 학습/전환 프로젝트. 진행 상황·DB·다음 작업·열린 PR 메모
type: project
originSessionId: 51da8e75-f7a2-4b05-b2b6-963ce41efb6a
---

> **마지막 갱신**: 2026-07-24 (**5개 PR 연속 머지 완료** — 시각 갭 fix + Flyway 활성화 + Observability PR-1/2 + E2E 12건 청산. 남은: Observability PR-3 dashboard + 포트폴리오 큐).

## 🟢 2026-07-22 ~ 2026-07-24 세션 — 5개 PR 연속 머지 (인프라 대전환)

### 머지 완료 (main 순서)

| PR | 커밋 | 스코프 |
|---|---|---|
| **#108** | `3d61a65` | F0f post-merge visual (프로토타입 갭 4건) + mypage 4탭 전면 재작업 |
| **#109** | `1509d04` | **P0-1 Flyway 활성화** — baseline v1 + V2 알림 컬럼 + Boot 4 auto-config 대응 |
| **#110** | `d165dbe` | **Observability PR-1** — Actuator 9091 분리 + Prometheus + Fly checks/metrics + buildInfo |
| **#111** | `e3768ce` | **Observability PR-2** — 커스텀 메트릭 (신청 수·로그인 실패) + Counter 단위 테스트 |
| **#112** | `5f6b1f5` | **E2E 12건 청산** — STATE.md 2026-07-13 명시 "E2E red 방치 마스킹" 청산. 65 tests / 0 failures 달성 |

### 이번 세션 주요 학습·정착

1. **Flyway 활성화 (2026-07-22)**
   - Boot 4 는 `spring-boot-flyway` auto-config 모듈이 분리됨 (없으면 flyway 의존성 있어도 auto-config 미동작)
   - pg_dump 17.10 이 `\restrict`/`\unrestrict` psql meta-command 자동 삽입 → V1 에서 수동 제거 필요
   - baseline-version=1 로 기존 Supabase 는 V1 스킵, 빈 DB 는 V1 실행
   - Supabase 실 baseline 검증 통과: 데이터 무손실 + 재기동 시 "Schema is up to date" 멱등
   - PR #108 이후 추가된 컬럼(notify_remind_d1 등) 은 V2 로 정식 마이그레이션 (`ALTER TABLE ... IF NOT EXISTS`)

2. **Observability 활성화 (2026-07-23~24)**
   - management.server.port 별도 지정 시 Boot 이 servlet child ApplicationContext 를 생성 → **render 통합 테스트에서 Thymeleaf 리졸버 미상속** → template not found
   - 해결: `src/test/resources/application.properties` 에 `management.server.port=-1` (management 서버 비활성화)
   - Actuator 자체 테스트는 opt-in: `@SpringBootTest(properties="management.server.port=0")`
   - `Counter.builder("youthmoa.application.submitted").register(registry)` 로 도메인 메트릭 등록
   - `AbstractAuthenticationFailureEvent` @EventListener 로 SecurityConfig 무수정 로그인 실패 카운터

3. **E2E 12건 청산 원인 (앱 코드 문제 아님, 테스트 스크립트가 옛 markup 참조)**
   - hover→click (U-COMMON-02 2026-07-16): notification-bell 4 + header-nav + login 로그아웃
   - CapacityBar 재작성 (PR #90): `.detail-capacity-bar-fill` → `.capacity-bar-fill`, 카운트 컨테이너 클래스명 변경
   - CLOSED → ENDED enum 리네임 (PR #107): "빈자리 알림 받기 disabled" → "비슷한 프로그램 보기 outline anchor"
   - featured 시드 확장 (F-signup-03): 5개 → 10개
   - signup 카드형 재디자인 (PR #105): gender radio 가 display:none → `.signup-gender-pill[data-value]` 시각 button click 으로 대체
   - Daum Postcode 실 모달 (PR #91): E2E 는 readonly 우회하여 zipcode/address 값 직접 세팅

### 다른 PC 재개 절차 (2026-07-24 기준)

```bash
cd ~/IdeaProjects/youth-moa-java
git fetch origin
git checkout main && git pull
git worktree list  # 회사 PC 잔여 worktree 잠금 여부 확인
git log --oneline -6  # 5f6b1f5 이후 커밋 확인
```

**환경변수 (Mac 개인 PC)**:
```bash
export DATABASE_URL="jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres"
export DATABASE_USERNAME="postgres.jlurjcmwlmcwaucxohkk"
export DATABASE_PASSWORD="<노트북 비밀번호 관리자>"
# Actuator (선택): MANAGEMENT_PORT=9091 default
```

**첫 기동 시 Supabase**: 이번 세션에서 이미 Flyway V1 baseline + V2 완료됨 → 아무 조치 없이 `./gradlew bootRun` 실행 시 "Schema is up to date. No migration necessary." 로그 확인.

**남은 우선 작업 큐**:
1. **Observability PR-3** `docs/observability-dashboard` — 로컬 docker-compose (prometheus+grafana) + provisioning + 대시보드 JSON (JVM community 4701 + youthmoa 커스텀: 신청·로그인실패 패널). spec `docs/specs/chore-observability.md` §6 상세.
2. **fix-6 후속 개선 잔여** — `.form-*` vs `.signup-*` 완전 통일 (이번 세션 커밋 `e04e3c5` 로 이미 대부분 완료. 확인만)
3. **포트폴리오 큐 남은 spec** (`docs/spec-queue-portfolio` 브랜치 5개): chore-caching-loadtest · feature-oauth2-kakao · feature-F2c-sse-notifications · ADMIN-01-approval-cycle-and-upload · (그 외)
4. **Q8 Supabase drift diff** — 정보성. Supabase pg_dump vs V1 비교로 update 시대 잔여 죽은 컬럼 문서화 (선택)
5. **admin 트랙 착수** — Flyway·Security(P0-2 CSRF 확인) 선행 완료 상태. A1 shell 부터 가능

**참고 파일**:
- `docs/STATE.md` — 이 메모리와 동기화된 repo 미러 (원격 루틴용)
- `docs/specs/chore-observability.md` — PR-1·PR-2 impl_done, PR-3 미착수
- `docs/specs/chore-flyway-activation.md` — impl_done
- `CLAUDE.md` — "작업 착수 전 프로토타입 시각 대조" 규칙 신설 (2026-07-22)

## 🟢 2026-07-20 세션 — F0f fix 5개 PR 병렬 개발·검증·머지 완료

### 확정 정책 (2026-07-20 프로그램 상태 체계)

**명시 상태 (enum, DB 저장) — 4개**:
- `UPCOMING` 진행예정 · `OPEN` 모집중 · `ENDED` 종료(기간 만료, 자연) · `SUSPENDED` 운영중단(관리자, 복구가능)

**파생 (계산, 저장 X)**:
- `isFull` = applied ≥ capacity → UI "마감" (OPEN 부분집합)
- `isClosing` = 90% 이상

**enum 리네임**: ACTIVE→OPEN, CLOSED→ENDED, INACTIVE→SUSPENDED. CLOSED 완전 폐기.

### 머지 완료 PR (main 순서대로)

| PR | 커밋 | 브랜치 | 내용 |
|---|---|---|---|
| **#102** | `7333476` | ~feature/F0f-fix-1-cta~ | CTA 5분기 + 상태 탭 라벨 정합 ("진행중"→"모집중", "마감" 탭 제거) + assets 2026-07-20 sync |
| **#107** | `c0c05a3` | ~feature/F0f-fix-3-ended-status~ | ProgramStatus 리네임 (OPEN/ENDED/SUSPENDED) + 종료 탭·그레이스케일 카드·"비슷한 프로그램 보기" CTA + ENDED 시드 (자소서 첨삭 카페) |
| **#105** | `53c2873` | ~feature/F0f-fix-6-signup-card~ | 회원가입 카드 3개 (계정·개인·약관) + 성별 pill + fullWidth CTA + `.signup-input`/`.signup-section` |
| **#104** | `f185b7b` | ~feature/F0f-fix-4-profile-noti~ | 개인정보 수정 카드형 폼 + 회원 탈퇴 + 알림 즉시 저장 + `.form-input`/`.form-card`/`.gender-pill` |
| **#106** | `fbe2a8f` | ~feature/F0f-fix-5-noti-page~ | 알림 전체 페이지 뒤로가기·전체/안읽음 pill·오늘/지난 7일/이전 그룹핑·안읽음 border-left |

### ym-verify 반영 (재수정 반영된 후 머지)

- **fix-4 #8 CRITICAL**: `withdrawModal` 에 `hidden` 속성 누락 → 페이지 진입 시 모달 즉시 노출 사고. `hidden` 추가 + JS 를 `modal.hidden = false/true` 로 토글. 2026-07-14 history.html 사고 재발 방지
- **fix-4 #7**: 성별 pill 편집 불가 (hidden radio 없음). "성별은 변경할 수 없어요" 힌트 + `aria-readonly` 추가. prototype 상 신청자 정보 Row 는 read-only 라 정책 정합
- **fix-5 #17**: `findGrouped` 가 `listAll(0,20)` 사용 → earlier 그룹 누락 + 필터 pill 카운트와 목록 개수 불일치. `NotificationRepository.findAllByUserOrderByCreatedAtDesc(User)` 오버로드 추가로 전체 조회

### 잔여 이슈 (후속 개선, 블로킹 아님)

- **fix-6 #7**: 커밋 메시지·주석에는 "공통 `.form-*` 토큰" 이라고 표기했으나 실제로는 `.signup-input`/`.signup-section` 로컬 클래스만 정의됨. fix-4 는 `.form-input`/`.form-card` 를 별도 정의 → 두 브랜치가 공통 이름을 실제로 공유하지 않음. 후속 리팩터로 통합 필요
- **fix-6 #13**: SignupRenderTest 에 카드형 마크업 (`.signup-gender-pill`, `.signup-login-link`, fullWidth CTA) assertion 미추가. 렌더 회귀 방어 약함
- **동적 검증**: 5개 PR 모두 사용자 시각 검증 (bootRun 후 브라우저) 미완료 상태로 머지됨

### 병렬 작업 방식 & 실패 사례

- **worktree 격리 병렬**: fix-4 (agent-a4e6593c) / fix-5 / fix-6 (agent-ac764cb9) — 3개 ym-impl 동시 실행. 파일 겹침 없어 성공
- **sandbox 이슈**: ym-impl 이 gradle·bash 실행 거부됨. 파일 편집·git 은 가능. 옵션 B (소스만 편집 + 메인 세션이 git 처리) 로 우회. **fix-5 는 sandbox 초기 stop 되어 메인 세션이 직접 구현**
- **SendMessage 툴 미제공**: 중단된 에이전트를 재개할 수 없음. 새 에이전트 spawn 하거나 메인이 처리
- **PR 재타겟 이슈**: fix-3 base=fix-1 상태에서 fix-1 머지·삭제 → fix-3 PR 자동 close. 브랜치 rebase --onto origin/main + force-push + 신규 PR (#107) 로 재생성

### 다른 PC 재개 시 (Mac)

**환경 확인 우선**:
```bash
git fetch origin
git checkout main && git pull
git worktree list  # 회사 PC 잔여 worktree 잠금 여부 확인
```

**미커밋 상태**: 없음. main 클린. `.claude/worktrees/agent-a4e6593c22f4a1ac0` `.claude/worktrees/agent-ac764cb96d6ab5cfa` 는 회사 PC 로컬 잔여 (원격 미영향)

**다음 작업 후보**:

1. **시각 검증** (bootRun 후 브라우저) — 5개 PR 시각 확인
   - `/programs` — 상태 탭 (전체·모집중·진행예정·종료), 종료 카드 그레이스케일
   - `/programs/{ended_id}` — "비슷한 프로그램 보기" CTA (a href /programs)
   - `/notifications` — 뒤로가기·전체/안읽음 pill·3그룹 헤더·안읽음 border-left
   - `/mypage/profile/edit` — 카드형 폼, 성별 pill readonly 힌트, 탈퇴 다이얼로그 (초기 숨김 확인)
   - `/mypage?tab=notifications` — toggle change 시 즉시 저장 토스트
   - `/signup` — 카드 3개, 성별 pill, fullWidth CTA
2. **fix-6 후속 개선** — SignupRenderTest 카드형 assertion 추가 + `.form-*` vs `.signup-*` 클래스명 통일
3. **포트폴리오 트랙 큐** — 브랜치 `docs/spec-queue-portfolio` (신규 push, PR 미생성)
   - 6 spec 파일: chore-flyway-activation (최우선) · chore-observability · chore-caching-loadtest · feature-oauth2-kakao · feature-F2c-sse-notifications · ADMIN-01-approval-cycle-and-upload
   - 트랙 머지 순서: Flyway → Observability PR-1 → 캐싱/OAuth2/SSE (병렬) → ADMIN-01 Phase 1~3
4. **admin 트랙 착수** — Flyway·Security(P0-1·P0-2) 선행 후 A1 shell (사용자 트랙 완료 후 착수 원칙)

### 사용자 선호 확인

- 병렬 작업 후 시각 검증 일괄 수행 방식 선호 (개별 즉시 검증 아님)
- ym-verify FAIL 발생 시 옵션 4 (모든 재수정 후 일괄 머지) 선택
- 존댓말 + 단계별 설명 유지

## 🟢 2026-07-13 세션 — PR #85·#84 머지 완료 + 신규 E2E 회귀 발견

- **머지 순서**: #85(e2e apply fix) squash 머지 → #84 를 최신 main 으로 rebase (#87 #88 #90 #91 #85 유입, 충돌 0) + #90 유입 포맷위반 2파일 spotless 추가 정리(8f2954f) → 체크 확인 → #84 squash 머지. worktree `youth-moa-java-ci` 제거 완료
- **main 발효**: deploy startup_failure 해소(vars 게이트+workflow_run) / integration-test(Testcontainers)·gitleaks·커버리지 게이트 가동 / Lint spotless 해소 / CLAUDE.md Docker·main 보호 정책 갱신
- **"커밋마다 CI 오류" 원인 분석 결과**: 전부 미머지 #84·#85 에 수정이 잠겨 있던 기존 문제였음 (Lint=Center 2파일 spotless, E2E=apply 4건). 신규 PR 잘못 아님
- **⚠️ 신규 E2E 회귀 5건** (main run 29235682122, #84 rebase 런과 동일 — #84 무관 입증 후 머지): program-detail 2건(#90 CapacityBar 개편) + signup 3건(#91 Daum Postcode). **E2E red 방치가 새 회귀를 가린 마스킹 사고** — 칩 task_a3acd426 발행. E2E green 유지가 앞으로 중요
- **worktree 잔여**: `youth-moa-java-e2efix` (fix/e2e-apply-wizard, #85 머지됐으므로 제거 가능 — 다른 세션 소유라 미触)

## 🟠 2026-07-10 세션 (별도) — E2E apply 스펙 4건 fix (**PR #85 오픈, 사용자 머지 대기**)

- **원인 확정**: PR #75 (F0c 3단계 위저드) 가 apply.html 을 개편하며 E2E 스펙 미갱신 → `#applyReason`(step 2)·`privacyAgreed`(step 3) 가 초기 화면에서 display:none → 4건 fill 타임아웃. 7/7 이후 main E2E 상시 61/4 red. (이전 세션 spawn_task 칩 task_5d3341d1 건 해소)
- **수정** (worktree `youth-moa-java-e2efix`, 브랜치 `fix/e2e-apply-wizard`, e2e/* 만 변경):
  - helpers.ts `applyNextStep()` 위저드 스텝 전환 헬퍼
  - 동의 미체크 시나리오 재설계 — 위저드가 미체크 제출을 disabled 차단 → 버튼 비활성 검증 + form 직접 제출로 서버 @AssertTrue 검증
  - `@Size(min=10)` 시나리오 폐기 (F0c-remainder Q2 에서 min 제거, `@Size(max=1000)` 만 잔존) → 1000자 초과로 교체
  - playwright.config.ts `BASE_URL` 환경변수 지원 (CI 는 넘기는데 config 가 8080 하드코딩으로 무시하던 버그) — **회사 PC 에서 bootrun-e2e(8090) 로 Playwright 로컬 실행 실증됨** (CLAUDE.md 의 "E2E 실행 불가" 기술은 낡음)
- **검증**: 로컬 fresh 서버 전체 65/65 PASS + PR CI E2E green (run 29080960892). Lint red 는 main 기존 spotless 위반 18파일 (PR #84 ci-hardening 이 해소 예정, 본 건과 무관)
- **주의**: H2 서버 재기동 없이 스위트 2회 연속 실행 시 apply-complete 가 중복신청(seed29→program7 누적)으로 실패 — rerun-safe 아님. CI 는 매회 fresh boot 라 무관
- **잔여**: PR #85 사용자 리뷰 후 squash merge (자동 머지는 권한 차단됨) / 머지 후 worktree `youth-moa-java-e2efix` 제거 가능

## 🟢 2026-07-09 세션 (별도) — 관리자 페이지 개발지시서 산출 (`spec_confirmed`, 미커밋)

- **산출물**: `docs/specs/ADMIN-00-master-directive.md` — admin 3자산(HANDOFF/prototype.tsx/wireframe 8타일 분할 판독) + 엔티티 인벤토리 대조 기반 마스터 지시서
- **Q1~Q10 사용자 확정 완료** (전부 제안대로 + 2건 추가): Q1 센터 CRUD **A9 추가** (repo prototype.html 17개 screen 에 센터 관리 화면 없음 확인 — claude design 산출물 있으면 착수 전 `docs/00_assets/admin/` 갱신 필요), Q8 추가 — **사용자↔관리자 상호 진입 동선** (헤더 드롭다운 role 조건부 링크, A1 편입)
- **핵심 갭**: Program 에 Center FK 없음(organization 문자열 — RBAC 전제), 신청기간·조회수·장소·문의처·강좌·질문·첨부 없음, CSRF disable, 관리자 시드 없음, multipart/파일업로드 인프라 없음
- **P0 선행**: P0-1 Flyway(create-drop 탈피 — admin CRUD 실효성) → P0-2 Security(/admin/** hasRole + CSRF 활성화) → A1 shell 부터. 파일 저장소는 Supabase Storage 확정
- **CI/CD 발견**: deploy.yml `if: secrets.FLY_API_TOKEN != ''` 는 job-level if 에서 secrets 컨텍스트 미지원 → deploy 상시 skip 가능성. Actions 이력 확인 + vars 게이트 전환 필요
- **착수 시점 확정 (사용자 결정)**: admin 화면 트랙(A1~A9)은 **사용자 트랙 완료 후**. 단 P0-1 Flyway·P0-2 Security(CSRF) 는 공유 인프라라 조기 착수 권장으로 지시서에 명시

## 🟠 2026-07-10 세션 — CI/CD 하드닝 실행 (PR #84 오픈, 체크 확인 대기)

- **PR #84** `chore/ci-hardening` (worktree `youth-moa-java-ci` 에서 작업 — 본 트리는 다른 세션이 fix/F0h-center-desc-image 작업 중이었음):
  - deploy.yml 수정 — **매 push 0초 startup_failure 원인 = job-level if 의 secrets 참조 (미지원 컨텍스트 → 워크플로우 검증 실패)**. vars.FLY_DEPLOY_ENABLED 게이트 + workflow_run 전환. FLY_API_TOKEN 시크릿 자체가 미등록 상태였음
  - ci.yml integration-test job (ubuntu Docker 로 Testcontainers 전체 테스트) + jacocoTestCoverageVerification LINE 55% (실측 64.6%)
  - lint.yml gitleaks secret-scan job
  - spotlessApply 16파일 (PR #78~#81 로 main Lint red 누적 → 해소)
  - CLAUDE.md: 회사 PC Docker 실증 반영 + main 보호 정책 현실화
- **branch protection / push protection 불가 확정**: Free 플랜 + private repo → API 403 "Upgrade to GitHub Pro or make public" (2026-07-10 실측)
- **회사 PC Testcontainers 최초 실증**: 전체 170 TC PASS (실 PostgreSQL 컨테이너). Docker Desktop crash 루프는 stale socket + (내가 유발한) settings-store.json BOM 문제 — 조치법 [[docker-stale-socket-fix]]. EnableDockerAI=false 로 변경됨 (백업 .bak-20260710)
- **E2E Playwright 4건 실패 중** (apply-complete 1 + apply 3) — 최근 머지 회귀로 추정, spawn_task 칩 발행됨 (task_5d3341d1). 워크플로우 자체는 정상 트리거
- **PR #84 최종 체크 (rebase 후)**: Build+Test ✅ / **Integration Test(Testcontainers+커버리지 게이트) ✅** / **Secret Scan(gitleaks) ✅** (권한 fix ea3cc5f 후 통과, 유출 0건) / Gradle Check ✅ / Anti-Pattern ✅ / E2E ❌ (기존 apply 4건 — **PR #85 가 수정, 본 PR 과 무관**). **사용자 머지 대기** — #85 와 순서 무관, 둘 다 머지되면 main 전 워크플로우 green 예상
- **중간 사건**: 다른 세션이 main 에 #82(Flyway prep — 지시서 P0-1 착수!)·#83(CenterContent) 머지 → 내 PR 충돌 → **충돌 PR 은 pull_request 워크플로우가 아예 실행 안 됨** (merge ref 생성 불가, 체크 30분 무반응의 원인). rebase + 충돌 2건 main 채택 + #82/#83 유입 포맷위반 6파일 spotless 추가 정리(1c47800) 로 해소
- **잔여**: Docker Desktop GUI ChunkLoadError 는 재기동으로 해소 예정 / stale 격리 디렉토리 (%LOCALAPPDATA%\Docker\run_stale*, docker-secrets-engine_stale*) 재부팅 후 삭제 가능 / worktree `youth-moa-java-ci` 는 #84 머지 후 제거 가능

---

## 🟠 2026-07-09 세션 — feature/F0h-centers-rework 재설계 (미커밋)

**세션 흐름**: ym-verify 워킹트리 검증 → FAIL #12 필터 풀리로드 fix → 사용자 시각 확인 → X 버튼 액션 prototype 불일치 지적 → F0h-c2 spec 개정(client-state) → 실 렌더 NPE 두 번(부모 body fragment / th:if+th:replace 조합) → build-check 사각지대 인지·preview 인프라 재확인 → SVG 아이콘 이식(FAIL 5건) → list↔map 양방향 연동(FAIL 3건) → panTo 추가 → 좌표 파생 시드 사고 발견 → 확장성 원칙 강화.

### 반영 완료 (미커밋, 브랜치 feature/F0h-centers-rework)
- `center-map.js` requestSubmit — 필터 조작 부분 swap
- `center-map.js` + `centers-detail.js` — CustomEvent 3종 (`centers:detail-open`/`detail-close`/`request-detail`), `selectMarker`/`clearSelection` module-scope 노출, 인포윈도우 CTA `<button data-info-detail>`, htmx:afterSwap 스코프 `[data-centers-list-scroll]` 로 축소, `_mapInitialized` 캐시, `selectMarker` 에 `map.panTo`
- `centers-detail.js` — client-state + pushState + popstate + HTMX ajax 2건 (`/centers/{id}/detail-fragment`, `/centers/cards`)
- `templates/center/list-fragments.html` **신규** — `card-list-content` / `detail-panel-content` fragment 정의 (부모 body 인라인 실행 사고 회피)
- `templates/fragments/icons.html` **신규** — pin/calendar/user/close SVG (prototype.tsx L54~77 이식)
- `templates/center/list.html` — `.centers-list-col` + `.has-detail` 구조, anchor href 유지, fragments 참조
- `static/css/main.css` — `.centers-detail-meta-badge` (26×26 primaryLight), placeholder/close 이모지 규칙 제거
- `CenterController.java` — 신규 endpoint 2개 (`/detail-fragment`, `/cards`)
- `templates/index.html:190` — "전체 센터 보기" 링크 `/programs` → `/centers` (F1 fix)
- `docs/specs/F0h-c2-list-3col.md` — 개정 (client-state, CustomEvent 3종, 상태머신·E2E 시나리오)
- `docs/specs/F0h-c4-map-interactions.md` — 개정 (인포윈도우 CTA client-state, list→map/map→list 양방향 표, panTo 명시, zoom MAX_LEVEL=7 확정)
- `CenterListRenderTest` — 14 TC (F0h-c2 5건 + F0h-c4 아이콘 2건 + list↔map 3건 + panTo 1건 + 기존 3건)
- `docs/STATE.md` — 오늘 세션 프리펜드 + 큐 갱신
- `docs/00_assets/region_center_list.md` — 부록 "CSV 작성 팁" 추가 (좌표 티켓 인계 문서)

### 재발방지 규칙 추가 (CLAUDE.md)
1. **`th:fragment` 별도 파일 배치 필수** — 부모 body 내 정의 시 인라인 실행 NPE (7/9 F0h-c2 사고)
2. **`th:if + th:replace` 같은 element 조합 금지** — short-circuit 미동작 (7/9 사고)
3. **prototype SVG 대신 이모지 금지** — lucide Icon path 이식 (7/9 F0h-c4 사고)
4. **파생 시드 금지** — 각 row 가 진리 소스. 대규모는 자원 파일 (`resources/data/*.csv`). DataInitializer Map 파생 forEach 패턴 발견 시 즉시 자원 파일 로드로 전환 (7/9 좌표 사고)
5. **관리자 CRUD 실효성 체크** — 편집값이 재기동 시 시드에 덮어써지지 않는지

### build-check 스킬 확장
- `--tests "*RenderTest" --tests "*RenderingTest"` 자동 포함
- Step 4 신설 — 화면 변경 시 preview + curl 실측 절차
- "회사 PC bootRun 가능 (e2e 프로파일 + 8090)" 재명시

### 검증 인프라 사고
- `build-check` (compileJava + JPA test) 만으로 Thymeleaf 파싱 오류 감지 불가 (사각지대). 실 렌더 테스트 `CenterListRenderTest` 확장 + build-check 스킬에 편입으로 재발 방지
- 회사 PC KAKAO_MAP_APP_KEY 는 **이미 설정** (이전 memory 잘못된 추정). `/centers` 실측 결과 `dapi.kakao.com` script 1회 렌더 확인

### 다음 티켓 (별도 세션)
- **`fix/F0h-real-coords`** — CSV 시드 (`src/main/resources/data/centers.csv`) 로 48개 실 좌표 seed. `regionCoords + offset` 파생 로직 삭제. 회귀 테스트 `CenterCoordinateSeedTest` 신설. **사용자가 CSV 를 다른 세션에서 작성** 후 impl 착수. 참고 문서: `docs/00_assets/region_center_list.md` 부록
- **`fix/F0h-operating-hours-badge`** — 좌표 티켓 완료 후. `Center.isActive` 를 "영업 중단/폐업 kill-switch" 로 의미 재정의 + `Center.isCurrentlyOpen(now)` 도메인 메서드 추가 (구조화 operatingHours). 배지 = `isActive && isCurrentlyOpen(now)`
- **`fix/program-detail-tx`** — `ProgramController.detail()` 에 `@Transactional(readOnly=true)` (ym-verify 전체화면 검증 FAIL #2 잔여)

### F0h ym-verify UNVERIFIED (개인 PC 시각 확인 필요)
- Kakao MarkerClusterer CustomOverlay 수용 여부
- 인포윈도우 좌·우 경계 300px 보정
- `overlay.setZIndex()` hover 시 wrapper stacking
- zoom MAX_LEVEL=7 초기 뷰포트 이탈 → **유지 결정 (사용자)**. spec 확정 완료

---

## 🟢 2026-07-07 Claude Code 인프라 하드닝 (**PR #71 머지 완료** `a7bdaa8` — 훅·launch.json·Preview 권한·명세 3건 전부 main 발효. settings.local.json 24줄 축약도 적용됨. PR #70 지도 fix 도 머지됨)

- **Claude Preview 검증 루프 구축·실증 완료**: `.claude/launch.json` (repo + `~/.claude/launch.json` 홈용) → `preview_start(name: "youth-moa-e2e")` 로 bootRun 자동 기동. 실제 기동은 `.claude/scripts/bootrun-e2e.cmd` (JDK 17 강제 + e2e 프로파일 H2 시드 + **8090 포트** — IntelliJ 8080 무충돌). 홈 렌더·브랜드 컬러 #3F30E9 실측·콘솔 0 오류까지 검증 성공.
- **함정 3개 해결 (재발 방지)**: ① 시스템 JAVA_HOME=JDK8 → gradlew 런처 거부 → `~/.gradle/gradle.properties` 에 `org.gradle.java.home` (데몬용) + cmd 래퍼에서 JAVA_HOME (런처용) 이중 처리 ② .cmd 배치에 한글 주석 금지 (cp 문제로 명령 오파싱) ③ 이 PC 는 cwd 실행파일 탐색 안 됨 → gradlew 명시 경로 필수. **preview 기동 직후 `preview_resize(1280x800)` 필수** (패널 네이티브 ~350px 라 가짜 줄바꿈 오판).
- **ym-qa.md 에 B-0 (preview 우선) 절차 반영** (`~/.claude/agents/ym-qa.md`)
- **검토 대기 제안 파일 2건** (자동 실행 코드라 auto mode 에서 직접 수정 차단됨 — 사용자 검토 후 적용): `.claude/hooks/post-edit-css.sh.proposed` (static/** 수정 시 build 미러 복사 → CSS stale 서빙 사고 차단), `.claude/settings.local.json.proposed` (86줄 → 24줄 범용 패턴)
- **ym-spec 병렬 명세 3건 산출** → `docs/specs/` 저장: F0c-remainder (신청 폼 — 최신 prototype 은 3단계 위저드로 변경됨, Q1~Q5), F2c-header-transparent (Q1~Q4), F4-detail-requirements-grid (UI 는 PR #12 기구현, 데이터 하드코딩만 잔여, Q1~Q4). **총 13개 사용자 결정 대기**
- 에이전트 보고 오류 정정 (3건): pre-bash-commit-msg.sh 는 settings.json 에 이미 등록 / claude-work.sh 실존 / **원격 루틴 5종도 2026-07-01 이미 등록·가동 중** (RemoteTrigger list 로 확인, trigger_id 는 docs/routines/README.md 에 기록). 탐색 에이전트의 "없음/미등록" 류 소극적 사실은 반드시 직접 재검증할 것
- Playwright 로컬 E2E 의 JDK 문제도 `~/.gradle/gradle.properties` 로 함께 해결됨 (JAVA_HOME=JDK8 셸에서 gradlew --version 정상 실측)
- PR #70 (F0h 지도 회색 fix) 오픈 상태
> 프로젝트 루트의 `CLAUDE.md` 가 디자인 시스템·작업 규칙·검증 규칙·환경 분리·패키지 구조·엔티티 규칙·Git 컨벤션 등 코드 작업 컨텍스트의 최우선 가이드라인. 본 메모리는 **시점적 진행 상태·DB·열린 PR·다음 액션** 중심으로 유지.

---

## 기본 정보

- **GitHub**: https://github.com/sihyuun/youth-moa-java (PRIVATE, branch: `main`) — owner 는 `sihyuun` (u 2개 아님, 2026-07-10 gh api 404 로 확인)
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

## 🟢 2026-07-01 세션 진행 완료 (4 PR 머지 — F0f / F0e / 사용자 인프라 2건)

- **PR #23** F0f 프로그램 목록 필터 재설계 + 자동화 5개 SKILL
  - Region 엔티티 신규 + Center.isFeatured 컬럼 (관리자 확장 대비)
  - Region/Center 30/48 시드 (`region_center_list.md` 기반, 가나다 상위 5 isFeatured)
  - 사이드바 다중 체크박스 + "전체보기" 팝오버 (검색 박스 8개 초과 자동)
  - 활성 필터 chip × 제거 + 전체 초기화
  - 정렬 3종 (newest / deadline / popular 임시 currentApplicants/capacity)
  - htmx 부분 갱신 (HX-Request 분기)
  - 캘린더 토글 disabled placeholder
  - 자동화: /wrap-up, /memory-sync, /pm-review "모두 권장 OK", /prototype-check 체크리스트, Playwright 템플릿
- **PR #25** F0e 홈 prototype 정렬 + 확장성 원칙
  - Hero: SiteImage HERO_BANNER slot + 검색바 UI-only (F0e-search 후속)
  - Quick Stats 3지표: 모집중 프로그램 / 참여 청년센터 / 누적 참여자 (distinct userId)
  - 프로그램: 비로그인 Top4 (isActive+endDate ASC 마감임박) / 로그인 맞춤추천 (interests+region 스코어링)
  - 공지: Pinned 대표 1 + 리스트 3 (primaryBg)
  - 공간: SiteImage HOME_SPACE_1~3 slot
  - **SiteImage 엔티티** 신규 — admin 페이지 대비 첫 실사용
  - 카테고리 그리드 완전 삭제 (주석까지) / HTMX Ping 완전 삭제
  - CLAUDE.md 확장성 원칙 섹션 명문화
- **PR #24, #26** (사용자 다른 세션) Claude Code 자동화 인프라 + 세션 병렬 조율

### 사용자 결정 (2026-07-01)
- **Q-F0f 9개**: 카테고리 코드 완전 삭제 / region_center_list.md 30·48 시드 / 청년센터 F0f 포함 / 정렬 3종 / 빈 상태 임시 카피 / capacity bar D5 분리 / 캘린더 disabled placeholder / htmx 부분 갱신 F0f 포함 / 모바일 별도 PR
- **Q-F0e 7개**: Quick Stats prototype 3지표 / 참여자 distinct userId / Top4 마감임박 / 맞춤추천 interests+region / 공간 이미지 prototype unsplash 3장 (SiteImage slot) / SiteImage F0e 에 포함 / 검색바 UI-only
- **확장성 원칙 (CLAUDE.md 반영)**: admin 페이지 대비 슬롯 기반 설계. 하드코딩 금지 대상 (지역·청년센터·이미지·카테고리·정렬 default 등) 은 엔티티 + 시드 + Repository 관리

### 화면 일치도 (2026-07-01 기준)
| 화면 | 이전 | 현재 |
|---|---|---|
| 홈 `/` | 30% | **90%+** (F0e) |
| 프로그램 목록 `/programs` | 60% | **85%+** (F0f) |
| 프로그램 상세 | 90% | 90% |
| 회원가입 | 90% | 90% |
| 로그인 | 95% | 95% |
| 신청 폼 | 75% | 75% |
| 헤더 | 70% | 70% |
| 푸터 | 80% | 80% |

### 발견된 기술 부채 (다음 큐 등재)
- `WebJarsResourceResolver` Boot 4 패키지 이슈 — F0f 하드코딩 `/2.0.4/` 청산 별도 PR
- 팝오버 fragment call 원인 재규명 — F0f 에서 inline 회피, list.html TODO 주석
- Testcontainers `YouthMoaApplicationTests.contextLoads()` Docker daemon 이슈 (계속)

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

**현재 모두 머지 완료. 진행 중 없음.** (2026-07-01 기준 origin/main 클린)

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

## 🟡 다음 작업 큐 (2026-07-01 갱신)

| 순서 | 브랜치 후보 | 범위 |
|---|---|---|
| ① | `feature/F0e-search` | Hero 검색바 활성화 — `/programs?q=<value>` 파라미터 지원 (F0e 후속) |
| ② | `feature/D5-card-capacity-bar` | 카드 CapacityBar (목록·홈 공통, HANDOFF 5-E.1) |
| ③ | `feature/D1b-apply-complete` | 신청 완료 페이지 `/apply/complete` |
| ④ | `feature/F0g-notices` | 공지사항 목록·상세 (헤더 네비 404 해소) |
| ⑤ | `feature/F0h-centers` | 청년센터 목록·상세 (카카오맵 SDK) |
| ⑥ | `fix/webjars-resource-resolver` | Spring Boot 4 `WebJarsResourceResolver` 등록 (F0f 하드코딩 `/2.0.4/` 청산) |
| ⑦ | `fix/popover-fragment-call` | 팝오버 Thymeleaf fragment call 원인 재규명 (F0f 에서 inline 회피, list.html TODO) |
| ⑧ | `chore/testcontainers-fix` | YouthMoaApplicationTests Docker daemon 연결 |
| ⑨ | `fix/text-tri-token` | main.css 자기참조 변수 정정 (있으면) |
| ⑩ | SiteImage admin CRUD | 관리자 페이지 진입 시 first CRUD 대상 |

### 인프라 작업 (완료·잔여)
- ✅ /qa Skill (2026-06-30 도입)
- ✅ /prototype-check Skill (2026-06-30 도입)
- ✅ /wrap-up Skill (2026-07-01 도입)
- ✅ /memory-sync Skill (2026-07-01 도입 — 이번 세션 첫 실사용)
- ✅ /pm-review Skill + ym-pm 에이전트
- ⏳ Flyway 도입
- ⏳ GitHub Actions e2e-playwright.yml (PR #24 로 셋업됨) 실제 트리거 검증
- ⏳ P1 회귀 루프
- ⏳ P2 갭 루프

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

## prototype 일치도 (2026-07-01 기준)

F0f #23 + F0e #25 머지 후:

| 화면 | 일치도 | 잔여 갭 |
|---|---|---|
| **홈** `/` | 🟢 90%+ | 검색바 활성화 (F0e-search) / SiteImage admin CRUD |
| **프로그램 목록** `/programs` | 🟢 85%+ | capacity bar (D5) / 팝오버 fragment call fix / 모바일 |
| 프로그램 상세 `/programs/{id}` | 🟢 90% | 자격요건 4-grid (F4) |
| 신청 폼 `/programs/{id}/apply` | 🔴 45% | 신청자 정보·개인정보 동의·완료 페이지 (D1b) |
| 로그인 `/login` | 🟢 95% | find-id / find-password 페이지 |
| 회원가입 `/signup` | 🟢 90% | 성별 옵션 확장 (Q-D3 미결정) |
| 헤더 | 🟡 70% | 검색 활성화 / 알림 종 / transparent 모드 |
| 푸터 | 🟢 95% | 정책 링크 실제 페이지 |
| 공지사항 목록·상세 | ❌ 미구현 | F0g |
| 청년센터 목록 | ❌ 미구현 | F0h (카카오맵) |

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
